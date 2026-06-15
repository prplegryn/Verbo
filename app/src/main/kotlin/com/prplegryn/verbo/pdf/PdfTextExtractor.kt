package com.prplegryn.verbo.pdf

import android.content.Context
import android.net.Uri
import com.prplegryn.verbo.translation.PdfPage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfTextExtractor(
    private val context: Context,
) {
    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extract(uri: Uri): List<PdfPage> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法打开 PDF 文件" }
            PDDocument.load(input).use { document ->
                (1..document.numberOfPages).map { pageIndex ->
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                        startPage = pageIndex
                        endPage = pageIndex
                    }
                    PdfPage(
                        index = pageIndex,
                        text = normalize(stripper.getText(document)),
                    )
                }
            }
        }
    }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("-\\n(?=[A-Za-z])"), "")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{4,}"), "\n\n\n")
            .trim()
}
