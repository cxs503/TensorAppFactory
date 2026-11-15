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
    fun extractsMaximumWordCountFromInstruction() {
        assertTrue(extractMaxWordCount("Answer using no more than six words.") == 6)
    }

    @Test
    fun extractsNumericMaximumWordCount() {
        assertTrue(extractMaxWordCount("Answer using no more than 6 words.") == 6)
    }

    @Test
    fun acceptsAnswerWithinWordLimit() {
        assertTrue(isWithinWordLimit("reliable access to land and support", 6))
    }

    @Test
    fun rejectsAnswerOverWordLimit() {
        assertFalse(isWithinWordLimit("reliable access to land and long-term support", 5))
    }

    @Test
    fun unansweredQuestionIsNotCorrect() {
        assertFalse(isCorrectAnswer(null, "B"))
    }
}
