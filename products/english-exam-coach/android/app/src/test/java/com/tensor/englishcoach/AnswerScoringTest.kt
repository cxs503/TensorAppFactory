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

    private val legacyAccepted = legacyReadingPhraseForms(listOf("reliable access to land", "long-term support"))

    @Test
    fun shortAnswerRequiresAllExpectedPhrases() {
        assertTrue(isCorrectReadingExtract("reliable access to land; long-term support", legacyAccepted, 6))
    }

    @Test
    fun shortAnswerRejectsMissingExpectedPhrase() {
        assertFalse(isCorrectReadingExtract("reliable access to land", legacyAccepted, 6))
    }

    @Test
    fun shortAnswerAcceptsExplicitLegacyDelimiterAndCaseVariants() {
        assertTrue(isCorrectReadingExtract("Reliable access to land; LONG-TERM support.", legacyAccepted, 6))
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
