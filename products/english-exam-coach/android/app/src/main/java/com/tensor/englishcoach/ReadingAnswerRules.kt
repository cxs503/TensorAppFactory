package com.tensor.englishcoach

import java.util.Locale

private val answerWhitespace = Regex("[\\p{Z}\\s]+")
private val typographicHyphens = Regex("[\\u2010\\u2011]")

// Accept only a declared complete form; the only equivalences are case,
// spacing and the equivalent typographic hyphen glyphs. Do not remove punctuation.
internal fun isCorrectReadingExtract(
    response: String,
    acceptedForms: List<String>,
    maxWords: Int?,
): Boolean {
    val normalized = normalizeReadingExtract(response)
    if (normalized.isEmpty() || !isWithinWordLimit(response, maxWords)) {
        return false
    }
    return acceptedForms.any { normalizeReadingExtract(it) == normalized }
}

internal fun legacyReadingPhraseForms(parts: List<String>): List<String> {
    if (parts.size < 2 || parts.any { it.isBlank() }) return emptyList()
    // The shipped sample's array means all required parts, not alternative keys.
    // Delimiters are enumerated; unlisted wording or intervening text cannot pass.
    return listOf(" ", " and ", "; ", ", ").flatMap { separator ->
        val answer = parts.joinToString(separator)
        listOf(answer, "$answer.")
    }
}

private fun normalizeReadingExtract(value: String): String =
    value.lowercase(Locale.ROOT).replace(typographicHyphens, "-")
        .replace(answerWhitespace, " ").trim()

internal fun isCorrectAnswer(selected: String?, expected: String): Boolean =
    selected?.trim()?.equals(expected.trim(), ignoreCase = true) == true

internal fun extractMaxWordCount(instruction: String): Int? {
    val token = Regex("no more than ([\\w-]+) words", RegexOption.IGNORE_CASE)
        .find(instruction)?.groupValues?.getOrNull(1)?.lowercase(Locale.ROOT) ?: return null
    val numberWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
    )
    return token.toIntOrNull() ?: numberWords[token]
}

internal fun isWithinWordLimit(response: String, maxWords: Int?): Boolean {
    if (maxWords == null) return true
    val normalized = normalizeReadingExtract(response)
    return normalized.isEmpty() || normalized.split(' ').size <= maxWords
}
