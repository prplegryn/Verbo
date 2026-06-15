package com.prplegryn.verbo.translation

import kotlinx.serialization.Serializable
import java.io.File

data class PdfPage(
    val index: Int,
    val text: String,
)

@Serializable
data class TranslatedTextBlock(
    val type: String = "paragraph",
    val text: String,
)

@Serializable
data class AiPageTranslation(
    val blocks: List<TranslatedTextBlock> = emptyList(),
)

data class TranslatedPage(
    val index: Int,
    val sourceText: String,
    val blocks: List<TranslatedTextBlock>,
) {
    val plainText: String = blocks.joinToString("\n\n") { it.text }
}

enum class TranslationPhase {
    Idle,
    Extracting,
    BuildingGuide,
    PreviewTranslating,
    AwaitingPreviewApproval,
    FullTranslating,
    Completed,
    Failed,
}

enum class PageWorkState {
    Queued,
    Running,
    Done,
    Failed,
}

data class PageProgress(
    val page: Int,
    val state: PageWorkState,
    val attempts: Int = 0,
    val message: String = "",
)

data class TranslationProgress(
    val phase: TranslationPhase = TranslationPhase.Idle,
    val fileName: String = "",
    val totalPages: Int = 0,
    val translatedPages: Int = 0,
    val guide: String = "",
    val previewFile: File? = null,
    val fullFile: File? = null,
    val pageProgress: Map<Int, PageProgress> = emptyMap(),
    val statusMessage: String = "等待选择 PDF",
    val errorMessage: String? = null,
    val workerCount: Int = 5,
) {
    val isWorking: Boolean
        get() = phase in setOf(
            TranslationPhase.Extracting,
            TranslationPhase.BuildingGuide,
            TranslationPhase.PreviewTranslating,
            TranslationPhase.FullTranslating,
        )

    val canStartPreview: Boolean
        get() = !isWorking

    val canApproveFull: Boolean
        get() = phase == TranslationPhase.AwaitingPreviewApproval && previewFile != null

    val progressFraction: Float
        get() = if (totalPages <= 0) 0f else translatedPages.toFloat() / totalPages.toFloat()
}
