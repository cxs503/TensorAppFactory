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
    fun unansweredQuestionIsNotCorrect() {
        assertFalse(isCorrectAnswer(null, "B"))
    }
}
