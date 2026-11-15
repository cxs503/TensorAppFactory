package dev.tensorappfactory.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val tfngOptions = listOf("TRUE", "FALSE", "NOT GIVEN")

@Composable
fun QuizScreen(content: ContentPackage, modifier: Modifier = Modifier) {
    var questionIndex by remember(content.packageId) { mutableIntStateOf(0) }
    var response by remember(content.packageId) { mutableStateOf("") }
    var evaluation by remember(content.packageId) { mutableStateOf<QuizEvaluation?>(null) }
    var attempts by remember(content.packageId) { mutableIntStateOf(0) }
    val correctQuestionIds = remember(content.packageId) { mutableSetOf<String>() }

    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (questionIndex >= content.questions.size) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Practice complete", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Correct questions: ${correctQuestionIds.size} / ${content.questions.size}")
                    Text("Attempts: $attempts")
                    Text("Package: ${content.title}")
                }
            } else {
                val question = content.questions[questionIndex]
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(content.title, style = MaterialTheme.typography.headlineSmall)
                    Text("Question ${questionIndex + 1} of ${content.questions.size}")
                    LinearProgressIndicator(
                        progress = { (questionIndex + 1).toFloat() / content.questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(question.prompt, style = MaterialTheme.typography.titleMedium)
                    when (question.type) {
                        ContentQuestionType.SINGLE_CHOICE -> question.options.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = response == option,
                                    modifier = Modifier.testTag("answer_option_$option"),
                                    onClick = {
                                        response = option
                                        evaluation = null
                                    }
                                )
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        ContentQuestionType.TRUE_FALSE_NOT_GIVEN -> tfngOptions.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth().testTag("answer_option_$option"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = response == option,
                                    onClick = {
                                        response = option
                                        evaluation = null
                                    }
                                )
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        ContentQuestionType.SHORT_ANSWER -> OutlinedTextField(
                            value = response,
                            onValueChange = {
                                response = it
                                evaluation = null
                            },
                            modifier = Modifier.fillMaxWidth().testTag("short_answer_input"),
                            label = { Text("Your answer") },
                            supportingText = {
                                question.maxAnswerWords?.let { Text("No more than $it words") }
                            }
                        )
                    }
                    evaluation?.let { result ->
                        Card(modifier = Modifier.fillMaxWidth().testTag("answer_feedback")) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    when (result.outcome) {
                                        QuizOutcome.CORRECT -> "Correct"
                                        QuizOutcome.TOO_MANY_WORDS -> "Too many words (${result.wordCount})"
                                        QuizOutcome.EMPTY_ANSWER -> "Enter an answer"
                                        QuizOutcome.INCORRECT -> "Not quite. Try again."
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (result.isCorrect || result.outcome == QuizOutcome.INCORRECT) {
                                    Text(question.explanation)
                                }
                                if (result.isCorrect) Text("Answer: ${question.answer}")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val result = QuizEngine.evaluate(question, response)
                                evaluation = result
                                if (result.outcome != QuizOutcome.EMPTY_ANSWER) attempts += 1
                                if (result.isCorrect) correctQuestionIds += question.id
                            },
                            enabled = evaluation?.isCorrect != true && response.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("submit_answer")
                        ) { Text("Check answer") }
                        if (evaluation?.isCorrect == true) {
                            Button(
                                onClick = {
                                    questionIndex += 1
                                    response = ""
                                    evaluation = null
                                },
                                modifier = Modifier.weight(1f).testTag("next_question")
                            ) { Text(if (questionIndex == content.questions.lastIndex) "Finish" else "Next") }
                        }
                    }
                }
            }
        }
    }
}
