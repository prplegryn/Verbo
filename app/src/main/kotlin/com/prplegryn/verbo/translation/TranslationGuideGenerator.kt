package com.prplegryn.verbo.translation

import com.prplegryn.verbo.ai.AiConfiguration
import com.prplegryn.verbo.ai.OpenAiChatClient

class TranslationGuideGenerator(
    private val client: OpenAiChatClient,
) {
    suspend fun generate(config: AiConfiguration, pages: List<PdfPage>): String {
        val sample = pages
            .take(20)
            .joinToString("\n\n") { page ->
                "第 ${page.index} 页\n${page.text.take(1600)}"
            }
            .take(24000)

        val first = runCatching {
            client.chat(
                config = config,
                systemPrompt = GUIDE_SYSTEM_PROMPT,
                userPrompt = GUIDE_USER_PROMPT.format(pages.size, sample),
            )
        }.getOrElse {
            return FALLBACK_GUIDE
        }

        val cleaned = enforceGuidePolicy(first)
        if (!containsExampleCue(cleaned)) return cleaned.ifBlank { FALLBACK_GUIDE }

        val rewritten = runCatching {
            client.chat(
                config = config,
                systemPrompt = GUIDE_REWRITE_SYSTEM_PROMPT,
                userPrompt = cleaned,
            )
        }.getOrNull()?.let(::enforceGuidePolicy).orEmpty()

        return if (rewritten.isNotBlank() && !containsExampleCue(rewritten)) rewritten else stripExampleLines(cleaned)
            .ifBlank { FALLBACK_GUIDE }
    }

    private fun enforceGuidePolicy(text: String): String =
        text.lines()
            .map { it.trimEnd() }
            .filterNot { line -> line.trim().startsWith("```") }
            .joinToString("\n")
            .trim()

    private fun containsExampleCue(text: String): Boolean {
        val cue = Regex("示例|例子|举例|example|e\\.g\\.", RegexOption.IGNORE_CASE)
        return cue.containsMatchIn(text)
    }

    private fun stripExampleLines(text: String): String =
        text.lines()
            .filterNot { containsExampleCue(it) }
            .joinToString("\n")
            .trim()

    companion object {
        private const val GUIDE_SYSTEM_PROMPT = """
你是资深中文出版翻译策划。任务是根据一本 PDF 的正文片段，生成给后续译者使用的翻译指南。
只输出指南本身，不翻译正文，不写样本文本，不列举模拟译文，不使用 Markdown 表格。
指南必须用清晰解释说明翻译方向、语体、术语处理、保留内容、排版处理和质量约束。
指南应明确哪些内容不需要翻译，包括代码、URL、文件路径、命令、数学公式、变量名、参考文献编号、页眉页脚噪声、版权页固定信息和专有标识。
不得输出思考过程、分析过程或 <think> 标签内容。
"""

        private const val GUIDE_USER_PROMPT = """
PDF 总页数：%d

请基于以下页面文本生成出版级中译指南。指南需要适配书的主题、读者、叙述视角和术语密度。不要使用例句或模拟译文来说明规则。

页面文本：
%s
"""

        private const val GUIDE_REWRITE_SYSTEM_PROMPT = """
将用户给出的翻译指南重写为合规版本：保留有效规则，去除任何样本文本、模拟译文和举例说明。
只输出重写后的指南，不输出解释，不输出 <think> 标签内容。
"""

        private const val FALLBACK_GUIDE = """
译文目标：输出可直接进入中文出版流程的简体中文稿，语言自然、准确、克制，避免机翻腔和过度解释。

主题与术语：先判断全书主题和目标读者，术语需前后一致。已有通行中文译名时采用通行译名；无稳定译名时保留原文并在首次出现处给出简洁中文释义。

内容边界：代码、命令、URL、文件路径、变量名、接口名、数学公式、计量符号、参考文献编号、版权页固定标识、商标和专有产品名通常保留原样。明显的页眉页脚、页码、扫描噪声和重复目录线不进入正文译文。

语体：学术或技术内容保持严谨清楚，叙事内容保持顺畅和节奏，说明性段落避免生硬逐词对应。必要时重组语序，但不得增删事实。

排版：保留标题、段落、列表、引用、脚注、代码块、表格和公式的结构语义。中文 PDF 写出时应根据结构使用不同层级的字号、字重、斜体或等宽字体，而不是把所有内容套成同一格式。

质量约束：只输出最终译文或结构化译文块；不得输出思考过程、审稿意见、提示词残留、分析说明或任何 <think> 标签中的内容。
"""
    }
}
