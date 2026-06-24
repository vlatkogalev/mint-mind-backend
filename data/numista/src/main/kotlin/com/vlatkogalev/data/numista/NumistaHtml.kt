package com.vlatkogalev.data.numista

import org.jsoup.Jsoup

private const val NEWLINE_MARKER = "\uE000"
private const val IMAGE_MARKER = "\uE001"
private const val CAPTION_MAX_LENGTH = 40
private const val SENTENCE_ENDINGS = ".!?:;,"

internal fun htmlToPlainText(html: String?): String? {
    if (html.isNullOrBlank()) return null

    val doc = Jsoup.parse(html)
    doc.select("br").before(NEWLINE_MARKER)
    doc.select("p, div, li").before(NEWLINE_MARKER)
    doc.select("img").before(IMAGE_MARKER)

    val lines = doc.text()
        .replace(NEWLINE_MARKER, "\n")
        .split("\n")
        .map { it.trim() }

    val result = mutableListOf<String>()
    for (line in lines) {
        if (line.contains(IMAGE_MARKER)) {
            val lastIndex = result.indexOfLast { it.isNotBlank() }
            if (lastIndex >= 0 && isCaptionLike(result[lastIndex])) {
                result.removeAt(lastIndex)
            }
            continue
        }
        result.add(line)
    }

    return result
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
        .ifBlank { null }
}

private fun isCaptionLike(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.isEmpty() || trimmed.length > CAPTION_MAX_LENGTH) return false
    return trimmed.last() !in SENTENCE_ENDINGS
}
