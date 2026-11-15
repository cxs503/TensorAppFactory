package dev.tensorappfactory.reference

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val content = ContentPackage(
        schemaVersion = "1.0",
        packageId = "ui-test",
        title = "Quiz UI Test",
        language = "en",
        questions = listOf(
            ContentQuestion(
                id = "q1",
                type = ContentQuestionType.SINGLE_CHOICE,
                prompt = "Choose the correct option",
                options = listOf("A", "B"),
                answer = "B",
                acceptedAnswers = emptyList(),
                maxAnswerWords = null,
                explanation = "B is correct."
            ),
            ContentQuestion(
                id = "q2",
                type = ContentQuestionType.TRUE_FALSE_NOT_GIVEN,
                prompt = "Is this statement true?",
                options = emptyList(),
                answer = "TRUE",
                acceptedAnswers = emptyList(),
                maxAnswerWords = null,
                explanation = "The statement is supported."
            )
        )
    )

    @Test
    fun learnerCanAnswerRetryAndFinishQuiz() {
        composeRule.setContent { QuizScreen(content) }

        composeRule.onNodeWithText("Choose the correct option").assertExists()
        composeRule.onNodeWithTag("answer_option_B").performClick()
        composeRule.onNodeWithText("Check answer").performClick()
        composeRule.onNodeWithText("Correct").assertExists()
        composeRule.onNodeWithText("Next").performClick()

        composeRule.onNodeWithText("Is this statement true?").assertExists()
        composeRule.onNodeWithTag("answer_option_TRUE").performClick()
        composeRule.onNodeWithText("Check answer").performClick()
        composeRule.onNodeWithText("Correct").assertExists()
        composeRule.onNodeWithText("Finish").performClick()

        composeRule.onNodeWithText("Practice complete").assertExists()
        composeRule.onNodeWithText("Correct questions: 2 / 2").assertExists()
        composeRule.onNodeWithText("Attempts: 2").assertExists()
    }
}
