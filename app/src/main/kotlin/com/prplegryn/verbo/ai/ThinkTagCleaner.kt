package com.prplegryn.verbo.ai

object ThinkTagCleaner {
    private val completeThinkBlock = Regex("(?is)<think\\b[^>]*>.*?</think>")
    private val incompleteThinkBlock = Regex("(?is)<think\\b[^>]*>.*$")
    private val looseThinkTags = Regex("(?is)</?think\\b[^>]*>")

    fun clean(text: String): String =
        text.replace(completeThinkBlock, "")
            .replace(incompleteThinkBlock, "")
            .replace(looseThinkTags, "")
            .trim()
}
