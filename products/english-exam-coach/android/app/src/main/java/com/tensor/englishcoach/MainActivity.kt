package com.tensor.englishcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("english_exam_coach", MODE_PRIVATE)
        val benchmarkJson = assets.open("reading-original-v0.1.json").bufferedReader().use { it.readText() }
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                    CoachApp(preferences, benchmarkJson)
                }
            }
        }
    }
}

private data class ReadingQuestion(
    val id: String,
    val taskType: String,
    val instruction: String,
    val passage: String,
    val options: List<Pair<String, String>>,
    val expected: List<String>,
    val maxAnswerWords: Int?,
    val explanation: String
)

private fun ReadingQuestion.describeAnswer(answer: String): String {
    val label = options.firstOrNull { it.first == answer }?.second
    return if (label == null || label == answer) answer else "$answer. $label"
}

private fun loadQuestions(json: String): List<ReadingQuestion> {
    val items = JSONObject(json).getJSONArray("items")
    return (0 until items.length()).map { index ->
        val item = items.getJSONObject(index)
        val prompt = item.getJSONObject("prompt")
        val optionsJson = prompt.optJSONArray("options") ?: JSONArray()
        val options = (0 until optionsJson.length()).map { optionIndex ->
            val option = optionsJson.getJSONObject(optionIndex)
            option.getString("id") to option.getString("text")
        }.ifEmpty {
            if (item.getString("task_type") == "true_false_not_given") {
                listOf("TRUE" to "TRUE", "FALSE" to "FALSE", "NOT GIVEN" to "NOT GIVEN")
            } else emptyList()
        }
        val answer = item.getJSONObject("answer_key").get("expected")
        val expected = when (answer) {
            is JSONArray -> (0 until answer.length()).map { answer.getString(it) }
            else -> listOf(answer.toString())
        }
        ReadingQuestion(
            id = item.getString("item_id"),
            taskType = item.getString("task_type"),
            instruction = prompt.getString("instruction"),
            passage = prompt.getString("stimulus").replace("\\\\n", "\n"),
            options = options,
            expected = expected,
            maxAnswerWords = extractMaxWordCount(prompt.getString("instruction")),
            explanation = item.getJSONObject("answer_key").getString("explanation")
        )
    }
}

@Composable
private fun CoachApp(preferences: android.content.SharedPreferences, benchmarkJson: String) {
    val questions = remember(benchmarkJson) { loadQuestions(benchmarkJson) }
    var targetBand by remember { mutableStateOf(preferences.getString("target_band", null)) }
    val packRevision = remember(benchmarkJson) {
        val pack = JSONObject(benchmarkJson)
        "${pack.getString("pack_id")}:${pack.getString("pack_version")}"
    }
    val store = remember(preferences, packRevision) { ReadingProgressStore(preferences, packRevision) }
    var progress by remember(store) {
        val saved = store.load()
        fun belongsToCurrentPack(attempt: ReadingAttempt?) = questions.any { it.id == attempt?.itemId }
        val paused = saved.pausedAttempt?.takeIf(::belongsToCurrentPack)
        mutableStateOf(
            if (belongsToCurrentPack(saved.attempt)) saved.copy(pausedAttempt = paused)
            else saved.copy(attempt = paused, pausedAttempt = null)
        )
    }
    fun update(next: ReadingProgress) {
        if (next != progress) {
            store.save(next)
            progress = next
        }
    }
    var screen by remember { mutableStateOf("home") }
    var language by remember { mutableStateOf(preferences.getString("language", "en")) }
    val zh = language == "zh"
    fun t(en: String, cn: String) = if (zh) cn else en
    val attempt = progress.attempt
    val questionIndex = questions.indexOfFirst { it.id == attempt?.itemId }
    val question = questions.getOrNull(questionIndex)
    val selected = attempt?.selected
    val typedAnswer = attempt?.typed.orEmpty()
    val submitted = attempt?.correct != null
    val lastWasCorrect = attempt?.correct == true
    val focused = attempt?.focused == true
    BackHandler(screen != "home") { screen = "home" }
    val isShortAnswer = question?.taskType == "short_answer"
    val scrollState = key(screen, question?.id, focused) { rememberScrollState() }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("English Exam Coach", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                val next = if (zh) "en" else "zh"
                language = next
                preferences.edit().putString("language", next).apply()
            }) {
                Text(if (zh) "English" else "中文")
            }
        }
        Text(t("IELTS Academic · Early access", "雅思学术类 · 早期版本"), style = MaterialTheme.typography.labelLarge)

        if (targetBand == null) {
            Text(t("Set your preparation target", "设置备考目标"), style = MaterialTheme.typography.headlineSmall)
            Text(t("Choose a target band to personalize your local study space.", "选择目标分数，建立你的本地学习空间。"))
            listOf("6.0", "6.5", "7.0", "7.5", "8.0").forEach { band ->
                OutlinedButton(
                    onClick = {
                        targetBand = band
                        preferences.edit().putString("target_band", band).apply()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(t("Target band $band", "目标分数 $band")) }
            }
            Text(t("Your progress stays on this device in this early build.", "当前早期版本将学习进度保存在本设备。"), style = MaterialTheme.typography.bodySmall)
        } else if (screen == "practice" && question != null) {
            Text(
                if (focused) t("Focused retry · Question ${questionIndex + 1}", "专项重练 · 第 ${questionIndex + 1} 题")
                else t("Reading practice · Question ${questionIndex + 1} of ${questions.size}", "阅读练习 · 第 ${questionIndex + 1} / ${questions.size} 题"),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(question.instruction, style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) {
                Text(question.passage, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
            if (isShortAnswer) {
                if (question.id == "read-urban-gardens-003" && question.expected.size > 1) {
                    Text(t("Original practice item. This format is not used to estimate an IELTS band.",
                        "原创练习题。此题型不用于估算雅思分数。"), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = typedAnswer,
                    onValueChange = { if (progress.attempt?.let { draft -> draft.itemId == question.id && draft.focused == focused } == true) update(progress.type(it)) },
                    enabled = !submitted,
                    label = { Text(if (question.maxAnswerWords != null) t("Your answer (up to ${question.maxAnswerWords} words)", "填写答案（不超过${question.maxAnswerWords}个词）") else t("Your answer", "填写答案")) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(Modifier.selectableGroup()) {
                    question.options.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = selected == option.first,
                                enabled = !submitted,
                                role = Role.RadioButton,
                                onClick = { if (progress.attempt?.let { draft -> draft.itemId == question.id && draft.focused == focused } == true) update(progress.choose(option.first)) }
                            ).testTag("option_${option.first}").padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            RadioButton(selected = selected == option.first, onClick = null, enabled = !submitted)
                            Text(
                                if (option.first == option.second) option.second else "${option.first}. ${option.second}",
                                Modifier.weight(1f).padding(start = 8.dp, top = 12.dp)
                            )
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val draft = progress.attempt ?: return@Button
                    if (draft.itemId != question.id || draft.focused != focused) return@Button
                    val answer = if (isShortAnswer) draft.typed.trim() else draft.selected.orEmpty()
                    if (isShortAnswer && !isWithinWordLimit(answer, question.maxAnswerWords)) return@Button
                    val correct = if (isShortAnswer) {
                        val accepted = if (question.id == "read-urban-gardens-003" && question.expected.size > 1)
                            legacyReadingPhraseForms(question.expected) else question.expected
                        isCorrectReadingExtract(answer, accepted, question.maxAnswerWords)
                    } else isCorrectAnswer(answer, question.expected.firstOrNull().orEmpty())
                    update(progress.submit(answer, correct))
                },
                enabled = !submitted && if (isShortAnswer) typedAnswer.isNotBlank() && isWithinWordLimit(typedAnswer, question.maxAnswerWords) else selected != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(t("Check answer", "检查答案")) }
            if (submitted) {
                Text(
                    if (lastWasCorrect) t("Correct", "回答正确") else t("Not quite", "还不正确"),
                    color = if (lastWasCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(question.explanation)
                if (lastWasCorrect) {
                    Button(onClick = {
                        val finished = progress.attempt ?: return@Button
                        if (finished.itemId != question.id || finished.focused != focused || finished.correct != true) return@Button
                        val next = questions.getOrNull(questionIndex + 1)
                        update(progress.advance(next?.id))
                        if (focused) screen = "review"
                        else if (next == null) screen = "home"
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(when {
                            focused -> t("Finish focused retry", "完成专项重练")
                            questionIndex + 1 < questions.size -> t("Next question", "下一题")
                            else -> t("Finish practice", "完成练习")
                        })
                    }
                } else {
                    OutlinedButton(onClick = {
                        if (progress.attempt?.let { draft -> draft.itemId == question.id && draft.focused == focused } == true) update(progress.retry())
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(t("Try again", "重新作答"))
                    }
                }
            }
            if (focused && !lastWasCorrect) {
                TextButton(onClick = {
                    if (progress.attempt?.focused == true && progress.attempt?.itemId == question.id) {
                        update(progress.leaveFocus())
                        screen = "review"
                    }
                }) { Text(t("End this retry", "结束本次重练")) }
            }
            TextButton(onClick = { screen = "home" }) { Text(t("Back to study plan", "返回学习计划")) }
        } else if (screen == "review") {
            Text(t("Mistake review", "错题回顾"), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            val reviewed = questions.filter { it.id in progress.mistakes }
            if (reviewed.isEmpty()) Text(t("No mistakes to review yet.", "还没有可回顾的错题。"))
            val needsPractice = reviewed.count { !progress.mistakes.getValue(it.id).corrected }
            if (reviewed.isNotEmpty()) Text(t(
                "To revisit: $needsPractice · Corrected: ${reviewed.size - needsPractice}",
                "$needsPractice 题待重练 · ${reviewed.size - needsPractice} 题已纠正"
            ))
            if (focused) Text(t("Finish the current focused retry before starting another.", "请先完成当前专项重练。"))
            reviewed.sortedBy { progress.mistakes.getValue(it.id).corrected }.forEach { item ->
                val history = progress.mistakes.getValue(item.id)
                val position = questions.indexOf(item) + 1
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t("Question $position", "第 $position 题"),
                            Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                        Text(if (history.corrected) t("Corrected on a later attempt", "已在后续作答中纠正")
                            else t("Needs practice", "待重练"))
                        Text(t("Incorrect submissions: ${history.wrongCount}", "错答次数：${history.wrongCount}"))
                        Text(item.instruction)
                        Text(item.passage)
                        history.responses.lastOrNull()?.let {
                            Text(t("Last incorrect answer:", "最近一次错答：") + " " + item.describeAnswer(it))
                        }
                        if (history.responses.size > 1) {
                            Text(t("Earlier incorrect answers:", "更早的错答："))
                            history.responses.dropLast(1).asReversed().forEach { Text("• " + item.describeAnswer(it)) }
                        }
                        Text(t("Expected answer:", "参考答案：") + " " + item.expected.joinToString("; ") { item.describeAnswer(it) })
                        Text(t("Explanation:", "解析：") + " " + item.explanation)
                        OutlinedButton(
                            onClick = {
                                update(progress.focus(item.id))
                                if (progress.attempt?.focused == true && progress.attempt?.itemId == item.id) screen = "practice"
                            },
                            enabled = !focused,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(t("Retry question $position", "重练第 $position 题")) }
                    }
                }
            }
            TextButton(onClick = { screen = "home" }) { Text(t("Back to study plan", "返回学习计划")) }
        } else {
            Text(t("Your study space", "你的学习空间"), style = MaterialTheme.typography.headlineSmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("Target band", "目标分数"), style = MaterialTheme.typography.labelMedium)
                    Text(targetBand ?: "—", style = MaterialTheme.typography.headlineMedium)
                    Text(t("Correct completions: ${progress.correctCount}", "正确完成次数：${progress.correctCount}"))
                    Text(t("Answer attempts: ${progress.attemptCount}", "作答次数：${progress.attemptCount}"))
                }
            }
            Text(t("Today's plan", "今日计划"), style = MaterialTheme.typography.titleLarge)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("Reading · Main idea and evidence", "阅读 · 主旨与证据定位"), style = MaterialTheme.typography.titleMedium)
                    Text(t("${questions.size} original questions covering three response formats.", "${questions.size} 道原创题，覆盖三种作答形式。"))
                    Button(
                        onClick = {
                            questions.firstOrNull()?.let { update(progress.start(it.id)) }
                            screen = "practice"
                        },
                        enabled = questions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when {
                            focused -> t("Resume focused retry", "继续专项重练")
                            attempt == null -> t("Start reading practice", "开始阅读练习")
                            else -> t("Resume reading practice", "继续阅读练习")
                        })
                    }
                    OutlinedButton(onClick = { screen = "review" }, modifier = Modifier.fillMaxWidth()) {
                        Text(t("Review mistakes (${progress.mistakes.size})", "错题回顾（${progress.mistakes.size}）"))
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("Coming next", "后续建设"), style = MaterialTheme.typography.titleMedium)
                    Text(t("Listening audio, Writing feedback, Speaking practice, timed tests and adaptive planning are not implemented in this build.", "当前版本尚未实现听力音频、写作反馈、口语练习、计时模考和自适应计划。"))
                }
            }
            OutlinedButton(onClick = {
                targetBand = null
                preferences.edit().remove("target_band").apply()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(t("Change target", "修改目标"))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            t("Independent preparation aid. Not affiliated with IELTS and does not provide official scores.",
              "独立备考辅助产品，与 IELTS 官方无隶属关系，不提供官方考试分数。"),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
