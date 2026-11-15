package com.tensor.englishcoach

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingPracticeFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun learnerCompletesThreeQuestionSessionAndCanRetry() {
        composeRule.onNodeWithText("Target band 6.5").performClick()
        composeRule.onNodeWithText("Start reading practice").performScrollTo().performClick()

        composeRule.onNodeWithText("Reading practice · Question 1 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("option_A").performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Not quite").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performScrollTo().performClick()

        composeRule.onNodeWithTag("option_B").performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Correct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next question").performScrollTo().performClick()

        composeRule.onNodeWithText("Reading practice · Question 2 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag("option_NOT GIVEN").performClick()
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Correct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next question").performScrollTo().performClick()

        composeRule.onNodeWithText("Reading practice · Question 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("Your answer (up to 6 words)").performScrollTo().performTextInput(
            "reliable access to land and long-term support"
        )
        composeRule.onNodeWithText("Check answer").performScrollTo().performClick()
        composeRule.onNodeWithText("Correct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Finish practice").performScrollTo().performClick()

        composeRule.onNodeWithText("Correct questions: 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Answer attempts: 4").assertIsDisplayed()
    }
}
