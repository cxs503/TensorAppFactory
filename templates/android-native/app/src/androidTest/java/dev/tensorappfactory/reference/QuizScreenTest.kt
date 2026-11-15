package dev.tensorappfactory.reference

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
        val context = composeRule.activity
        QuizProgressStore(context).clear(content.packageId)
        composeRule.activity.setContent { QuizScreen(content) }

        composeRule.onNodeWithText("Choose the correct option").assertIsDisplayed()
        composeRule.onNodeWithTag("answer_option_B").performClick()
        composeRule.onNodeWithText("Check answer").performClick()
        composeRule.onNodeWithText("Correct").assertIsDisplayed()
        composeRule.onNodeWithText("Next").performClick()

        composeRule.onNodeWithText("Is this statement true?").assertIsDisplayed()
        composeRule.onNodeWithTag("answer_option_TRUE").performClick()
        composeRule.onNodeWithText("Check answer").performClick()
        composeRule.onNodeWithText("Correct").assertIsDisplayed()
        composeRule.onNodeWithText("Finish").performClick()

        composeRule.onNodeWithText("Practice complete").assertIsDisplayed()
        composeRule.onNodeWithText("Correct questions: 2 / 2").assertIsDisplayed()
        composeRule.onNodeWithText("Attempts: 2").assertIsDisplayed()
        composeRule.waitForIdle()
        val saved = QuizProgressStore(context).load(content.packageId)
        assertEquals(2, saved.attempts)
        assertTrue(saved.correctQuestionIds.containsAll(setOf("q1", "q2")))
    }

    @Test
    fun progressStoreRestoresCurrentAnswerAndResults() {
        val store = QuizProgressStore(composeRule.activity)
        store.clear("restore-test")
        store.save(
            "restore-test",
            QuizProgress(1, "TRUE", 3, setOf("q1"), true)
        )
        val restored = store.load("restore-test")
        assertEquals(1, restored.questionIndex)
        assertEquals("TRUE", restored.response)
        assertEquals(3, restored.attempts)
        assertEquals(setOf("q1"), restored.correctQuestionIds)
        assertTrue(restored.hasSubmitted)
        store.clear("restore-test")
    }
}
