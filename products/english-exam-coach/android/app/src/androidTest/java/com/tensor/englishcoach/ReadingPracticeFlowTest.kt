package com.tensor.englishcoach

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingPracticeFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearLocalState() {
        composeRule.activityRule.scenario.onActivity {
            check(it.getSharedPreferences("english_exam_coach", Context.MODE_PRIVATE).edit().clear().commit())
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun learnerCompletesThreeQuestionSessionAndCanRetry() {
        composeRule.onNodeWithText("Target band 6.5").performClick()
        composeRule.onNodeWithText("Start reading practice").performScrollTo().performClick()

        composeRule.onNodeWithText("Reading practice · Question 1 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("option_A").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Not quite").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()

        composeRule.onNodeWithTag("option_B").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Correct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next question").performScrollTo().performClick()

        composeRule.onNodeWithText("Reading practice · Question 2 of 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("option_NOT GIVEN").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Correct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next question").performScrollTo().performClick()

        composeRule.onNodeWithText("Reading practice · Question 3 of 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Your answer (up to 6 words)").performScrollTo().performTextInput(
            "reliable access to land; long-term support"
        )
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Correct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Finish practice").performScrollTo().performClick()

        composeRule.onNodeWithText("Correct completions: 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Answer attempts: 4").assertIsDisplayed()
    }

    @Test
    fun resumedDraftReviewAndFeedbackSurviveActivityRecreation() {
        val scenario = composeRule.activityRule.scenario
        composeRule.onNodeWithText("Target band 6.5").performClick()
        composeRule.onNodeWithText("Start reading practice").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_A").performScrollTo().performClick()

        scenario.recreate()
        composeRule.onNodeWithText("Resume reading practice").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_A").performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        scenario.recreate()
        composeRule.onNodeWithText("Answer attempts: 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Review mistakes (1)").performScrollTo().performClick()
        composeRule.onNodeWithText("Last incorrect answer:", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Explanation:", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Back to study plan").performScrollTo().performClick()
        composeRule.onNodeWithText("Resume reading practice").performScrollTo().performClick()
        composeRule.onNodeWithText("Not quite").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_B").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Next question").performScrollTo().performClick()

        composeRule.onNodeWithTag("option_NOT GIVEN").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Next question").performScrollTo().performClick()
        composeRule.onNodeWithText("Your answer (up to 6 words)").performScrollTo()
            .performTextInput("reliable access to land")
        scenario.recreate()
        composeRule.onNodeWithText("Answer attempts: 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Resume reading practice").performScrollTo().performClick()
        composeRule.onNodeWithText("Reading practice · Question 3 of 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("reliable access to land").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun focusedRetryPreservesNormalDraftAndTracksEvidenceAcrossRecreation() {
        val scenario = composeRule.activityRule.scenario
        composeRule.onNodeWithText("Target band 6.5").performClick()
        composeRule.onNodeWithText("Start reading practice").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_A").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_B").performScrollTo().performClick()
        composeRule.onNodeWithText("Back to study plan").performScrollTo().performClick()
        composeRule.onNodeWithText("Review mistakes (1)").performScrollTo().performClick()
        composeRule.onNodeWithText("Needs practice").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Retry question 1").performScrollTo().performClick()
        composeRule.onNodeWithText("Focused retry · Question 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("option_C").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        scenario.recreate()
        composeRule.onNodeWithText("Resume focused retry").performScrollTo().performClick()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_B").performScrollTo().performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Finish focused retry").performScrollTo().performClick()
        composeRule.onNodeWithText("Corrected on a later attempt").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Incorrect submissions: 2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Earlier incorrect answers:").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Back to study plan").performScrollTo().performClick()
        composeRule.onNodeWithText("Resume reading practice").performScrollTo().performClick()
        composeRule.onNodeWithTag("option_B").performScrollTo().assertIsSelected()
    }

    @Test
    fun loadsV1MistakeWithoutResettingExistingCounters() {
        val scenario = composeRule.activityRule.scenario
        scenario.onActivity {
            val legacySnapshot = """
                {"pack":"ielts-academic-reading-original-v0.1:0.1.0","correctCount":2,"attemptCount":4,
                 "mistakes":{"read-urban-gardens-001":"A"}}
            """.trimIndent()
            check(it.getSharedPreferences("english_exam_coach", Context.MODE_PRIVATE).edit()
                .putString("target_band", "6.5").putString("reading_progress_v1", legacySnapshot).commit())
        }
        scenario.recreate()
        composeRule.onNodeWithText("Correct completions: 2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Answer attempts: 4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Review mistakes (1)").performScrollTo().performClick()
        composeRule.onNodeWithText("Incorrect submissions: 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Last incorrect answer: A.", substring = true).performScrollTo().assertIsDisplayed()
    }
}
