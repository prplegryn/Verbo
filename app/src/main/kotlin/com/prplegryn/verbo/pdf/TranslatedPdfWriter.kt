package com.prplegryn.verbo.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.prplegryn.verbo.translation.TranslatedPage
import com.prplegryn.verbo.translation.TranslatedTextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class TranslatedPdfWriter(
    private val context: Context,
) {
    suspend fun write(
        fileName: String,
        title: String,
        pages: List<TranslatedPage>,
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "translated").apply { mkdirs() }
        val output = File(outputDir, fileName)
        val document = PdfDocument()
        val state = PdfState(document, title)

        try {
            pages.forEach { translatedPage ->
                state.ensureStarted()
                state.drawSourcePageTitle(translatedPage.index)
                translatedPage.blocks.forEach { block ->
                    state.drawBlock(block)
                }
                state.addVerticalSpace(12f)
            }
            state.finish()
            FileOutputStream(output).use { stream -> document.writeTo(stream) }
        } finally {
            document.close()
        }
        output
    }

    private class PdfState(
        private val document: PdfDocument,
        private val title: String,
    ) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = TOP_MARGIN

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 33, 33)
            textSize = BODY_TEXT_SIZE
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        fun ensureStarted() {
            if (page == null) startPage()
        }

        fun finish() {
            page?.let(document::finishPage)
            page = null
            canvas = null
        }

        fun addVerticalSpace(space: Float) {
            y += space
            if (y > PAGE_HEIGHT - BOTTOM_MARGIN) startPage()
        }

        fun drawSourcePageTitle(sourcePage: Int) {
            ensureSpace(44f)
            applyPaintStyle(paint, BlockStyle.PageTitle)
            drawTextLine("第 $sourcePage 页", LEFT_MARGIN, y, paint)
            y += 28f
        }

        fun drawBlock(block: TranslatedTextBlock) {
            val style = BlockStyle.from(block.type)
            val text = block.text.trim()
            if (text.isBlank()) return
            applyPaintStyle(paint, style)

            val availableWidth = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN - style.extraIndent
            val lines = wrapText(text, paint, availableWidth)
            val lineHeight = style.lineHeight
            val blockTopPadding = style.topPadding
            val blockBottomPadding = style.bottomPadding
            ensureSpace(blockTopPadding + lineHeight + blockBottomPadding)
            y += blockTopPadding

            lines.forEach { line ->
                ensureSpace(lineHeight + blockBottomPadding)
                if (style.backgroundColor != null && line.isNotEmpty()) {
                    canvas?.drawRect(
                        LEFT_MARGIN + style.extraIndent - 4f,
                        y - lineHeight + 5f,
                        PAGE_WIDTH - RIGHT_MARGIN + 4f,
                        y + 5f,
                        Paint().apply { color = style.backgroundColor },
                    )
                }
                val prefix = if (style == BlockStyle.List && !line.startsWith("•")) "• " else ""
                drawTextLine(prefix + line.removePrefix("- ").removePrefix("* "), LEFT_MARGIN + style.extraIndent, y, paint)
                y += lineHeight
            }
            y += blockBottomPadding
        }

        private fun startPage() {
            page?.let(document::finishPage)
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create(),
            )
            canvas = page?.canvas
            y = TOP_MARGIN
            drawRunningHeader()
        }

        private fun drawRunningHeader() {
            applyPaintStyle(paint, BlockStyle.Header)
            drawTextLine(title, LEFT_MARGIN, 30f, paint)
            drawTextLine(pageNumber.toString(), PAGE_WIDTH - RIGHT_MARGIN - paint.measureText(pageNumber.toString()), PAGE_HEIGHT - 28f, paint)
        }

        private fun ensureSpace(required: Float) {
            if (y + required > PAGE_HEIGHT - BOTTOM_MARGIN) startPage()
        }

        private fun drawTextLine(text: String, x: Float, baseline: Float, paint: Paint) {
            canvas?.drawText(text, x, baseline, paint)
        }

        private fun applyPaintStyle(paint: Paint, style: BlockStyle) {
            paint.textSize = style.textSize
            paint.typeface = style.typeface
            paint.color = style.color
        }

        private fun wrapText(text: String, paint: Paint, width: Float): List<String> {
            val result = mutableListOf<String>()
            text.lines().forEach { rawLine ->
                val line = rawLine.trimEnd()
                if (line.isBlank()) {
                    result += ""
                    return@forEach
                }
                var start = 0
                while (start < line.length) {
                    val count = max(1, paint.breakText(line, start, line.length, true, width, null))
                    val roughEnd = (start + count).coerceAtMost(line.length)
                    val end = findBreakPoint(line, start, roughEnd)
                    result += line.substring(start, end).trim()
                    start = end
                    while (start < line.length && line[start].isWhitespace()) start++
                }
            }
            return result
        }

        private fun findBreakPoint(line: String, start: Int, roughEnd: Int): Int {
            if (roughEnd >= line.length) return line.length
            for (index in roughEnd downTo start + 8) {
                if (line[index - 1].isWhitespace()) return index
            }
            return roughEnd
        }
    }

    private enum class BlockStyle(
        val textSize: Float,
        val typeface: Typeface,
        val color: Int,
        val lineHeight: Float,
        val topPadding: Float,
        val bottomPadding: Float,
        val extraIndent: Float = 0f,
        val backgroundColor: Int? = null,
    ) {
        Header(9f, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL), Color.rgb(116, 116, 116), 12f, 0f, 0f),
        PageTitle(15f, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), Color.rgb(20, 20, 20), 20f, 0f, 8f),
        Heading(18f, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), Color.rgb(18, 42, 82), 25f, 12f, 8f),
        Paragraph(12.6f, Typeface.create(Typeface.SERIF, Typeface.NORMAL), Color.rgb(34, 34, 34), 19f, 3f, 5f),
        Quote(12.2f, Typeface.create(Typeface.SERIF, Typeface.ITALIC), Color.rgb(79, 79, 79), 19f, 7f, 7f, 16f),
        List(12.4f, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL), Color.rgb(35, 35, 35), 19f, 3f, 5f, 10f),
        Code(10.4f, Typeface.MONOSPACE, Color.rgb(24, 45, 62), 16f, 7f, 7f, 8f, Color.rgb(242, 245, 247)),
        Formula(10.8f, Typeface.MONOSPACE, Color.rgb(28, 28, 28), 17f, 6f, 6f, 8f),
        Table(10.2f, Typeface.MONOSPACE, Color.rgb(28, 28, 28), 16f, 7f, 7f, 4f),
        Footnote(9.5f, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL), Color.rgb(84, 84, 84), 14f, 6f, 6f);

        companion object {
            fun from(type: String): BlockStyle = when (type.lowercase()) {
                "heading", "title", "subtitle" -> Heading
                "quote", "blockquote" -> Quote
                "list", "list_item", "listitem" -> List
                "code", "command" -> Code
                "formula", "math" -> Formula
                "table" -> Table
                "footnote", "note" -> Footnote
                else -> Paragraph
            }
        }
    }

    companion object {
        private const val PAGE_WIDTH = 595f
        private const val PAGE_HEIGHT = 842f
        private const val LEFT_MARGIN = 50f
        private const val RIGHT_MARGIN = 48f
        private const val TOP_MARGIN = 58f
        private const val BOTTOM_MARGIN = 56f
        private const val BODY_TEXT_SIZE = 12.6f
    }
}
