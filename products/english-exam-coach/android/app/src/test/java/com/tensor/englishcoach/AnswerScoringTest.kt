package com.tensor.englishcoach

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerScoringTest {
    @Test
    fun exactExpectedOptionIsCorrect() {
        assertTrue(isCorrectAnswer("B", "B"))
    }

    @Test
    fun wrongOptionIsIncorrect() {
        assertFalse(isCorrectAnswer("A", "B"))
    }

    @Test
    fun shortAnswerRequiresAllExpectedPhrases() {
        assertTrue(isCorrectShortAnswer("reliable access to land and long-term support", listOf("reliable access to land", "long-term support")))
    }

    @Test
    fun shortAnswerRejectsMissingExpectedPhrase() {
        assertFalse(isCorrectShortAnswer("reliable access to land", listOf("reliable access to land", "long-term support")))
    }

    @Test
    fun shortAnswerIgnoresCaseAndPunctuation() {
        assertTrue(isCorrectShortAnswer("Reliable access to land; LONG-TERM support.", listOf("reliable access to land", "long-term support")))
    }

    @Test
    fun unansweredQuestionIsNotCorrect() {
        assertFalse(isCorrectAnswer(null, "B"))
    }
}
