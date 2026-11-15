package dev.tensorappfactory.reference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizEngineTest {
    private fun question(
        type: ContentQuestionType,
        answer: String,
        accepted: List<String> = emptyList(),
        maxWords: Int? = null,
        options: List<String> = emptyList()
    ) = ContentQuestion(
        id = "q1",
        type = type,
        prompt = "Prompt",
        options = options,
        answer = answer,
        acceptedAnswers = accepted,
        maxAnswerWords = maxWords,
        explanation = "Explanation"
    )

    @Test fun scoresSingleChoiceByExactOption() {
        val q = question(ContentQuestionType.SINGLE_CHOICE, "B", options = listOf("A", "B"))
        assertTrue(QuizEngine.evaluate(q, "B").isCorrect)
        assertFalse(QuizEngine.evaluate(q, "b").isCorrect)
    }

    @Test fun scoresTfngWithoutCaseOrWhitespaceSensitivity() {
        val q = question(ContentQuestionType.TRUE_FALSE_NOT_GIVEN, "NOT GIVEN")
        assertTrue(QuizEngine.evaluate(q, " not   given ").isCorrect)
        assertFalse(QuizEngine.evaluate(q, "FALSE").isCorrect)
    }

    @Test fun normalizesShortAnswersAndPunctuation() {
        val q = question(
            ContentQuestionType.SHORT_ANSWER,
            answer = "green field",
            accepted = listOf("green field", "a green field"),
            maxWords = 3
        )
        assertTrue(QuizEngine.evaluate(q, "A GREEN FIELD!").isCorrect)
        assertFalse(QuizEngine.evaluate(q, "green forest").isCorrect)
    }

    @Test fun enforcesShortAnswerWordLimit() {
        val q = question(
            ContentQuestionType.SHORT_ANSWER,
            answer = "green field",
            accepted = listOf("green field"),
            maxWords = 2
        )
        val result = QuizEngine.evaluate(q, "the green field")
        assertEquals(QuizOutcome.TOO_MANY_WORDS, result.outcome)
        assertEquals(3, result.wordCount)
    }

    @Test fun reportsEmptyAnswersWithoutCountingAnAttempt() {
        val q = question(ContentQuestionType.TRUE_FALSE_NOT_GIVEN, "TRUE")
        val evaluation = QuizEngine.evaluate(q, "   ")
        assertEquals(QuizOutcome.EMPTY_ANSWER, evaluation.outcome)
        val tracker = QuizAttemptTracker()
        tracker.record(q, evaluation)
        assertEquals(0, tracker.attemptCount)
    }

    @Test fun tracksAttemptsAndUniqueCorrectQuestionsAcrossRetries() {
        val q = question(ContentQuestionType.TRUE_FALSE_NOT_GIVEN, "TRUE")
        val tracker = QuizAttemptTracker()
        tracker.record(q, QuizEngine.evaluate(q, "FALSE"))
        tracker.record(q, QuizEngine.evaluate(q, "TRUE"))
        tracker.record(q, QuizEngine.evaluate(q, "TRUE"))
        assertEquals(3, tracker.attemptCount)
        assertEquals(1, tracker.correctQuestionCount)
        assertEquals(setOf("q1"), tracker.correctlyAnsweredQuestionIds)
    }
}
