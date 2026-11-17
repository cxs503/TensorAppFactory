package com.tensor.englishcoach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingProgressTest {
    @Test fun selectionDraftAndProgressSurviveStartingAnAlreadyActiveSession() {
        val initial = ReadingProgress(correctCount = 4, attemptCount = 8).start("one").choose("B").type("draft")
        val resumed = initial.start("another")
        assertEquals(ReadingAttempt("one", selected = "B", typed = "draft"), resumed.attempt)
        assertEquals(4, resumed.correctCount)
        assertEquals(8, resumed.attemptCount)
    }

    @Test fun repeatSubmitsDoNotDuplicateStatsAndReviewKeepsBoundedEvidence() {
        val wrong = ReadingProgress().start("one").choose("A").submit("A", false).submit("A", false)
        assertEquals(1, wrong.attemptCount)
        assertEquals(MistakeHistory(1, listOf("A")), wrong.mistakes["one"])
        assertEquals(wrong, wrong.advance("two"))
        val retried = wrong.retry().choose("C").submit("C", false)
        val right = retried.retry().choose("B").submit("B", true)
        assertEquals(right, right.retry())
        val advanced = right.advance("two").advance("two")
        assertEquals(3, advanced.attemptCount)
        assertEquals(1, advanced.correctCount)
        assertEquals(MistakeHistory(2, listOf("A", "C"), corrected = true), advanced.mistakes["one"])
        assertEquals(ReadingAttempt("two"), advanced.attempt)
    }

    @Test fun finishingResetsDraftButRetainsStatsForFutureSessions() {
        val finished = ReadingProgress().start("one").type("answer").submit("answer", true).advance(null).advance(null)
        assertNull(finished.attempt)
        assertEquals(1, finished.correctCount)
        assertEquals(1, finished.attemptCount)
        assertEquals(ReadingAttempt("one"), finished.start("one").attempt)
    }

    @Test fun focusedRetryRestoresTheOriginalDraftAndUpdatesOnlyItsOwnEvidence() {
        val firstWrong = ReadingProgress().start("one").choose("A").submit("A", false)
        val draft = firstWrong.retry().choose("B").submit("B", true).advance("two").type("saved")
        val focus = draft.focus("one")
        assertEquals(ReadingAttempt("one", focused = true), focus.attempt)
        assertEquals(ReadingAttempt("two", typed = "saved"), focus.pausedAttempt)
        assertEquals(focus, focus.focus("one"))
        val wrong = focus.choose("C").submit("C", false)
        assertEquals(MistakeHistory(2, listOf("A", "C")), wrong.mistakes["one"])
        assertEquals(draft.attempt, wrong.leaveFocus().attempt)
        assertEquals(wrong.mistakes, wrong.leaveFocus().mistakes)
        val right = wrong.retry().choose("B").submit("B", true).advance(null)
        assertEquals(draft.attempt, right.attempt)
        assertNull(right.pausedAttempt)
        assertEquals(MistakeHistory(2, listOf("A", "C"), corrected = true), right.mistakes["one"])
        assertEquals(4, right.attemptCount)
        assertEquals(2, right.correctCount)
    }

    @Test fun mistakeHistoryRetainsFiveLatestResponsesButCountsAllWrongSubmissions() {
        val history = (1..8).fold(MistakeHistory(corrected = true)) { value, count -> value.record("$count") }
        assertEquals(8, history.wrongCount)
        assertEquals(listOf("4", "5", "6", "7", "8"), history.responses)
        assertEquals(false, history.corrected)
    }
}
