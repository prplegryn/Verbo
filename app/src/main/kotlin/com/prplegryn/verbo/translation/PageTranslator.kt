package com.prplegryn.verbo.translation

import com.prplegryn.verbo.ai.AiConfiguration
import com.prplegryn.verbo.ai.JsonExtractor
import com.prplegryn.verbo.ai.OpenAiChatClient
import com.prplegryn.verbo.ai.ThinkTagCleaner
import kotlinx.serialization.json.Json

class PageTranslator(
    private val client: OpenAiChatClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun translate(
        config: AiConfiguration,
        guide: String,
        page: PdfPage,
    ): TranslatedPage {
        if (page.text.isBlank()) {
            return TranslatedPage(
                index = page.index,
                sourceText = page.text,
                blocks = listOf(TranslatedTextBlock(type = "paragraph", text = "")),
            )
        }

        val response = client.chat(
            config = config,
            systemPrompt = TRANSLATE_SYSTEM_PROMPT,
            userPrompt = buildUserPrompt(guide, page),
        )
        val cleaned = ThinkTagCleaner.clean(response)
        val blocks = parseBlocks(cleaned).ifEmpty { classifyPlainText(cleaned) }
        return TranslatedPage(
            index = page.index,
            sourceText = page.text,
            blocks = blocks.filter { it.text.isNotBlank() },
        )
    }

    private fun parseBlocks(text: String): List<TranslatedTextBlock> {
        val jsonObject = JsonExtractor.firstObject(text) ?: return emptyList()
        return runCatching {
            json.decodeFromString(AiPageTranslation.serializer(), jsonObject).blocks
        }.getOrDefault(emptyList())
    }

    private fun classifyPlainText(text: String): List<TranslatedTextBlock> =
        text.split(Regex("\\n{2,}"))
            .mapNotNull { chunk ->
                val trimmed = chunk.trim()
                if (trimmed.isEmpty()) return@mapNotNull null
                val firstLine = trimmed.lineSequence().firstOrNull().orEmpty()
                val type = when {
                    firstLine.startsWith("```") || firstLine.startsWith("    ") -> "code"
                    firstLine.startsWith(">") || firstLine.startsWith("“") -> "quote"
                    firstLine.matches(Regex("^[-*•]\\s+.*")) -> "list"
                    firstLine.length <= 34 && !firstLine.endsWith("。") && !firstLine.endsWith("，") -> "heading"
                    else -> "paragraph"
                }
                TranslatedTextBlock(type = type, text = trimmed)
            }

    private fun buildUserPrompt(guide: String, page: PdfPage): String = """
翻译指南：
$guide

当前页码：${page.index}

源文本：
${page.text}

输出要求：
只输出一个 JSON 对象，顶层字段为 blocks。blocks 是数组，每项包含 type 和 text。
type 只能使用 heading、paragraph、quote、list、code、formula、table、footnote。
text 写最终中文译文或需要保留的原文内容。不得输出解释、提示词、审稿意见、Markdown 代码围栏、思考过程或 <think> 标签内容。
""".trimIndent()

    companion object {
        private const val TRANSLATE_SYSTEM_PROMPT = """
你是出版级英译中译者和版式整理编辑。请严格按用户给出的指南翻译。
你可以在内部判断术语和结构，但最终回答中绝不能包含推理、分析、草稿、<think> 标签或模型思考内容。
保留不应翻译的代码、命令、URL、路径、变量名、公式、引用编号和专有标识。
译文要忠实准确、中文自然、术语一致，允许为中文出版质量调整语序与句式，但不得添加源文没有的信息。
"""
    }
}
