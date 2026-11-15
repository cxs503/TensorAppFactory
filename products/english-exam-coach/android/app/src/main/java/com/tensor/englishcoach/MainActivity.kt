package com.tensor.englishcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("english_exam_coach", MODE_PRIVATE)
        val benchmarkJson = assets.open("reading-original-v0.1.json").bufferedReader().use { it.readText() }
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    CoachApp(preferences, benchmarkJson)
                }
            }
        }
    }
}

private data class ReadingQuestion(
    val instruction: String,
    val passage: String,
    val options: List<Pair<String, String>>,
    val answer: String,
    val explanation: String
)

private fun loadFirstQuestion(json: String): ReadingQuestion {
    val item = JSONObject(json).getJSONArray("items").getJSONObject(0)
    val prompt = item.getJSONObject("prompt")
    val optionsJson = prompt.getJSONArray("options")
    val options = (0 until optionsJson.length()).map { index ->
        val option = optionsJson.getJSONObject(index)
        option.getString("id") to option.getString("text")
    }
    val answer = item.getJSONObject("answer_key")
    return ReadingQuestion(
        instruction = prompt.getString("instruction"),
        passage = prompt.getString("stimulus"),
        options = options,
        answer = answer.getString("expected"),
        explanation = answer.getString("explanation")
    )
}

@Composable
private fun CoachApp(preferences: android.content.SharedPreferences, benchmarkJson: String) {
    val question = remember(benchmarkJson) { loadFirstQuestion(benchmarkJson) }
    var targetBand by remember { mutableStateOf(preferences.getString("target_band", null)) }
    var completedSessions by remember { mutableStateOf(preferences.getInt("completed_sessions", 0)) }
    var screen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("en") }
    val zh = language == "zh"
    fun t(en: String, cn: String) = if (zh) cn else en

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("English Exam Coach", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { language = if (zh) "en" else "zh" }) {
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
        } else if (screen == "practice") {
            Text(t("Reading practice", "阅读练习"), style = MaterialTheme.typography.headlineSmall)
            Text(question.instruction, style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) {
                Text(question.passage, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
            question.options.forEach { option ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    RadioButton(
                        selected = selected == option.first,
                        onClick = { if (!submitted) selected = option.first },
                        enabled = !submitted
                    )
                    Text("${option.first}. ${option.second}", Modifier.weight(1f).padding(top = 12.dp))
                }
            }
            Button(
                onClick = { submitted = true },
                enabled = selected != null && !submitted,
                modifier = Modifier.fillMaxWidth()
            ) { Text(t("Check answer", "检查答案")) }
            if (submitted) {
                val correct = selected == question.answer
                Text(if (correct) t("Correct", "回答正确") else t("Not quite", "还不正确"),
                    color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium)
                Text(question.explanation)
                Button(onClick = {
                    if (correct) {
                        completedSessions += 1
                        preferences.edit().putInt("completed_sessions", completedSessions).apply()
                    }
                    selected = null
                    submitted = false
                    screen = "home"
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("Finish session", "完成本次练习"))
                }
                Text(t("A session is counted only when the answer is correct. Review and retry if needed.", "答对后才计入完成记录；答错可以返回复习后重试。"), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { screen = "home"; selected = null; submitted = false }) {
                Text(t("Back to study plan", "返回学习计划"))
            }
        } else {
            Text(t("Your study space", "你的学习空间"), style = MaterialTheme.typography.headlineSmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("Target band", "目标分数"), style = MaterialTheme.typography.labelMedium)
                    Text(targetBand ?: "—", style = MaterialTheme.typography.headlineMedium)
                    Text(t("Completed practice sessions: $completedSessions", "已完成练习：$completedSessions 次"))
                }
            }
            Text(t("Today's plan", "今日计划"), style = MaterialTheme.typography.titleLarge)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("Reading · Main idea and evidence", "阅读 · 主旨与证据定位"), style = MaterialTheme.typography.titleMedium)
                    Text(t("One original benchmark question with an explained answer.", "一道原创基准题，附答案解释。"))
                    Button(onClick = { screen = "practice" }, modifier = Modifier.fillMaxWidth()) {
                        Text(t("Start reading practice", "开始阅读练习"))
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
