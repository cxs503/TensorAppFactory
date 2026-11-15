package dev.tensorappfactory.reference

enum class QuizOutcome {
    CORRECT,
    INCORRECT,
    EMPTY_ANSWER,
    TOO_MANY_WORDS
}

data class QuizEvaluation(
    val outcome: QuizOutcome,
    val submittedAnswer: String,
    val normalizedAnswer: String,
    val wordCount: Int,
    val isCorrect: Boolean
)

object QuizEngine {
    fun evaluate(question: ContentQuestion, response: String): QuizEvaluation {
        val normalized = normalize(response)
        val words = normalized.split(" ").filter { it.isNotBlank() }
        if (normalized.isBlank()) {
            return QuizEvaluation(QuizOutcome.EMPTY_ANSWER, response, normalized, 0, false)
        }

        if (question.type == ContentQuestionType.SHORT_ANSWER &&
            question.maxAnswerWords != null && words.size > question.maxAnswerWords
        ) {
            return QuizEvaluation(QuizOutcome.TOO_MANY_WORDS, response, normalized, words.size, false)
        }

        val correct = when (question.type) {
            ContentQuestionType.SINGLE_CHOICE ->
                response.trim() == question.answer.trim()
            ContentQuestionType.TRUE_FALSE_NOT_GIVEN ->
                normalizeLabel(response) == normalizeLabel(question.answer)
            ContentQuestionType.SHORT_ANSWER ->
                question.acceptedAnswers.any { normalize(it) == normalized }
        }
        return QuizEvaluation(
            if (correct) QuizOutcome.CORRECT else QuizOutcome.INCORRECT,
            response,
            normalized,
            words.size,
            correct
        )
    }

    private fun normalize(value: String): String =
        value.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun normalizeLabel(value: String): String =
        value.trim().uppercase().replace(Regex("\\s+"), " ")
}

class QuizAttemptTracker {
    var attemptCount: Int = 0
        private set

    private val correctIds = linkedSetOf<String>()

    val correctQuestionCount: Int
        get() = correctIds.size

    val correctlyAnsweredQuestionIds: Set<String>
        get() = correctIds.toSet()

    fun record(question: ContentQuestion, evaluation: QuizEvaluation) {
        if (evaluation.outcome != QuizOutcome.EMPTY_ANSWER) attemptCount += 1
        if (evaluation.isCorrect) correctIds += question.id
    }
}
