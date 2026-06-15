package com.prplegryn.verbo.ai

object JsonExtractor {
    fun firstObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaping = false
        for (index in start until text.length) {
            val char = text[index]
            if (escaping) {
                escaping = false
                continue
            }
            when {
                char == '\\' && inString -> escaping = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }
}
