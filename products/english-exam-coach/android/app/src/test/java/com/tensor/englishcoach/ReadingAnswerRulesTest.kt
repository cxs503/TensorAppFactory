package com.tensor.englishcoach

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingAnswerRulesTest {
    @Test fun matchesDeclaredWholeFormsAndNormalizesOnlyFormatting() {
        val forms = listOf("long-term support", "reliable access")
        assertTrue(isCorrectReadingExtract("  LONG‑TERM\u00a0support ", forms, 2))
        assertTrue(isCorrectReadingExtract("reliable access", forms, 2))
        assertFalse(isCorrectReadingExtract("long-term/support", forms, 2))
        assertFalse(isCorrectReadingExtract("not/long-term support", forms, 2))
        assertFalse(isCorrectReadingExtract("long term support", forms, 3))
        assertFalse(isCorrectReadingExtract("reliable access.", forms, 2))
        assertFalse(isCorrectReadingExtract(" ", listOf(" "), null))
    }

    @Test fun checksTheWordLimitInTheSamePathAsTheKey() {
        val forms = listOf("fixed bracket")
        assertTrue(isCorrectReadingExtract("fixed bracket", forms, 2))
        assertFalse(isCorrectReadingExtract("fixed bracket", forms, 1))
        assertFalse(isCorrectReadingExtract("unfixed bracket", forms, 2))
        assertFalse(isCorrectReadingExtract("fixed bracket etc", forms, null))
    }

    @Test fun publishedLegacyPartsAreNotIndependentAlternatives() {
        val forms = legacyReadingPhraseForms(listOf("reliable access to land", "long-term support"))
        assertTrue(isCorrectReadingExtract("reliable access to land long-term support", forms, 6))
        assertTrue(isCorrectReadingExtract("Reliable access to land; LONG-TERM support.", forms, 6))
        assertFalse(isCorrectReadingExtract("reliable access to land", forms, 6))
        assertFalse(isCorrectReadingExtract("reliable access to land and long-term support", forms, 6))
        assertTrue(isCorrectReadingExtract("reliable access to land and long-term support", forms, 7))
    }
}
