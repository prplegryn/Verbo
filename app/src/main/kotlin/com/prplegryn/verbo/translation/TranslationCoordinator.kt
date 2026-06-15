package com.prplegryn.verbo.translation

import android.content.Context
import android.net.Uri
import com.prplegryn.verbo.ai.AiConfiguration
import com.prplegryn.verbo.ai.OpenAiChatClient
import com.prplegryn.verbo.pdf.PdfTextExtractor
import com.prplegryn.verbo.pdf.TranslatedPdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class TranslationCoordinator(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val aiClient = OpenAiChatClient()
    private val extractor = PdfTextExtractor(appContext)
    private val guideGenerator = TranslationGuideGenerator(aiClient)
    private val pageTranslator = PageTranslator(aiClient)
    private val pdfWriter = TranslatedPdfWriter(appContext)
    private val translatedPages = ConcurrentHashMap<Int, TranslatedPage>()
    private var sourcePages: List<PdfPage> = emptyList()
    private var activeConfig: AiConfiguration? = null
    private var activeGuide: String = ""

    private val _progress = MutableStateFlow(TranslationProgress())
    val progress: StateFlow<TranslationProgress> = _progress

    suspend fun startPreview(uri: Uri, fileName: String, config: AiConfiguration) {
        require(config.isUsable()) { "请先填写完整的 AI 配置" }
        activeConfig = config
        translatedPages.clear()
        _progress.value = TranslationProgress(
            phase = TranslationPhase.Extracting,
            fileName = fileName,
            statusMessage = "正在读取 PDF",
        )

        runCatching {
            sourcePages = extractor.extract(uri)
            require(sourcePages.isNotEmpty()) { "PDF 没有可读取页面" }
            _progress.update {
                it.copy(
                    phase = TranslationPhase.BuildingGuide,
                    totalPages = sourcePages.size,
                    statusMessage = "正在生成翻译指南",
                )
            }
            activeGuide = guideGenerator.generate(config, sourcePages)
            _progress.update {
                it.copy(
                    guide = activeGuide,
                    phase = TranslationPhase.PreviewTranslating,
                    statusMessage = "正在翻译前 15 页预览",
                    pageProgress = initialPageProgress(sourcePages.take(PREVIEW_PAGE_COUNT)),
                )
            }
            translatePages(sourcePages.take(PREVIEW_PAGE_COUNT), TranslationPhase.PreviewTranslating)
            val previewFile = pdfWriter.write(
                fileName = "verbo-preview.pdf",
                title = "Verbo 预览译文",
                pages = translatedPages.values.sortedBy { it.index },
            )
            _progress.update {
                it.copy(
                    phase = TranslationPhase.AwaitingPreviewApproval,
                    previewFile = previewFile,
                    statusMessage = "前 15 页预览已生成，请检查后确认是否继续全书翻译",
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            fail(throwable.message ?: "预览生成失败")
        }
    }

    suspend fun approveFullTranslation() {
        activeConfig ?: return fail("缺少 AI 配置")
        if (sourcePages.isEmpty()) return fail("缺少已读取的 PDF 页面")
        _progress.update {
            it.copy(
                phase = TranslationPhase.FullTranslating,
                statusMessage = "正在翻译全书，已复用预览页译文",
                pageProgress = it.pageProgress + initialPageProgress(sourcePages.filterNot { page ->
                    translatedPages.containsKey(page.index)
                }),
            )
        }

        runCatching {
            translatePages(
                pages = sourcePages.filterNot { translatedPages.containsKey(it.index) },
                phase = TranslationPhase.FullTranslating,
            )
            val fullFile = pdfWriter.write(
                fileName = "verbo-full.pdf",
                title = "Verbo 全本译文",
                pages = sourcePages.mapNotNull { translatedPages[it.index] },
            )
            _progress.update {
                it.copy(
                    phase = TranslationPhase.Completed,
                    fullFile = fullFile,
                    translatedPages = translatedPages.size,
                    statusMessage = "全本翻译完成",
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            fail(throwable.message ?: "全本翻译失败")
        }
    }

    private suspend fun translatePages(pages: List<PdfPage>, phase: TranslationPhase) = coroutineScope {
        val config = activeConfig ?: error("缺少 AI 配置")
        val gate = Semaphore(WORKER_COUNT)
        pages.map { page ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    translateWithRetry(config, page, phase)
                }
            }
        }.awaitAll()
    }

    private suspend fun translateWithRetry(
        config: AiConfiguration,
        page: PdfPage,
        phase: TranslationPhase,
    ) {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            updatePage(page.index, PageWorkState.Running, attempt + 1, "worker 正在处理")
            val translated = runCatching {
                pageTranslator.translate(config, activeGuide, page)
            }.getOrElse { throwable ->
                lastError = throwable
                updatePage(page.index, PageWorkState.Failed, attempt + 1, throwable.message.orEmpty())
                delay(900L * (attempt + 1))
                return@repeat
            }
            translatedPages[page.index] = translated
            updatePage(page.index, PageWorkState.Done, attempt + 1, "完成")
            _progress.update {
                it.copy(
                    phase = phase,
                    translatedPages = translatedPages.size,
                    statusMessage = "已翻译 ${translatedPages.size}/${sourcePages.size} 页",
                    errorMessage = null,
                )
            }
            return
        }
        error("第 ${page.index} 页翻译失败：${lastError?.message.orEmpty()}")
    }

    private fun updatePage(page: Int, state: PageWorkState, attempts: Int, message: String) {
        _progress.update { progress ->
            progress.copy(
                pageProgress = progress.pageProgress + (page to PageProgress(page, state, attempts, message)),
            )
        }
    }

    private fun initialPageProgress(pages: List<PdfPage>): Map<Int, PageProgress> =
        pages.associate { page -> page.index to PageProgress(page.index, PageWorkState.Queued) }

    private fun fail(message: String) {
        _progress.update {
            it.copy(
                phase = TranslationPhase.Failed,
                statusMessage = "任务失败",
                errorMessage = message,
            )
        }
    }

    companion object {
        private const val WORKER_COUNT = 5
        private const val PREVIEW_PAGE_COUNT = 15
        private const val MAX_ATTEMPTS = 3
    }
}
