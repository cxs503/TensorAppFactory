package com.tensor.englishcoach

/** The current draft and aggregate updates are one snapshot so a restart cannot split them. */
internal data class ReadingProgress(
    val attempt: ReadingAttempt? = null,
    val correctCount: Int = 0,
    val attemptCount: Int = 0,
    val mistakes: Map<String, MistakeHistory> = emptyMap(),
    val pausedAttempt: ReadingAttempt? = null
) {
    fun start(itemId: String): ReadingProgress =
        if (attempt == null) copy(attempt = ReadingAttempt(itemId)) else this

    fun focus(itemId: String): ReadingProgress {
        if (attempt?.focused == true || itemId !in mistakes) return this
        return copy(attempt = ReadingAttempt(itemId, focused = true), pausedAttempt = attempt)
    }

    fun leaveFocus(): ReadingProgress =
        if (attempt?.focused == true && attempt.correct != true) copy(attempt = pausedAttempt, pausedAttempt = null) else this

    fun choose(option: String): ReadingProgress = editDraft { copy(selected = option) }

    fun type(answer: String): ReadingProgress = editDraft { copy(typed = answer.take(512)) }

    private fun editDraft(edit: ReadingAttempt.() -> ReadingAttempt): ReadingProgress {
        val current = attempt ?: return this
        return if (current.correct == null) copy(attempt = current.edit()) else this
    }

    fun submit(answer: String, correct: Boolean): ReadingProgress {
        val current = attempt ?: return this
        if (current.correct != null || answer.isBlank()) return this
        val priorMistake = mistakes[current.itemId]
        val updatedMistake = if (correct) priorMistake?.copy(corrected = true)
            else (priorMistake ?: MistakeHistory()).record(answer)
        return copy(
            attempt = current.copy(correct = correct),
            attemptCount = attemptCount + 1,
            mistakes = if (updatedMistake == null) mistakes else mistakes + (current.itemId to updatedMistake)
        )
    }

    fun retry(): ReadingProgress {
        val current = attempt ?: return this
        return if (current.correct == false) copy(attempt = ReadingAttempt(current.itemId, focused = current.focused)) else this
    }

    fun advance(nextItemId: String?): ReadingProgress {
        val current = attempt ?: return this
        if (current.correct != true) return this
        return if (current.focused) copy(attempt = pausedAttempt, pausedAttempt = null, correctCount = correctCount + 1)
            else copy(attempt = nextItemId?.let { ReadingAttempt(it) }, correctCount = correctCount + 1)
    }
}

internal data class ReadingAttempt(
    val itemId: String,
    val selected: String? = null,
    val typed: String = "",
    val correct: Boolean? = null,
    val focused: Boolean = false
)

internal data class MistakeHistory(
    val wrongCount: Int = 0,
    val responses: List<String> = emptyList(),
    val corrected: Boolean = false
) {
    fun record(response: String): MistakeHistory =
        MistakeHistory(wrongCount + 1, (responses + response).takeLast(MAX_RESPONSES), corrected = false)

    companion object {
        const val MAX_RESPONSES = 5
    }
}
