package dev.tensorappfactory.reference

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QuizProgress(
    val questionIndex: Int = 0,
    val response: String = "",
    val attempts: Int = 0,
    val correctQuestionIds: Set<String> = emptySet(),
    val hasSubmitted: Boolean = false
)

class QuizProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences("tensor_quiz_progress_v1", Context.MODE_PRIVATE)

    fun load(packageId: String): QuizProgress {
        val raw = preferences.getString(key(packageId), null) ?: return QuizProgress()
        return runCatching {
            val json = JSONObject(raw)
            val ids = json.optJSONArray("correct_question_ids") ?: JSONArray()
            QuizProgress(
                questionIndex = json.optInt("question_index", 0).coerceAtLeast(0),
                response = json.optString("response", ""),
                attempts = json.optInt("attempts", 0).coerceAtLeast(0),
                correctQuestionIds = (0 until ids.length()).map { ids.getString(it) }.toSet(),
                hasSubmitted = json.optBoolean("has_submitted", false)
            )
        }.getOrDefault(QuizProgress())
    }

    fun save(packageId: String, progress: QuizProgress) {
        val ids = JSONArray()
        progress.correctQuestionIds.sorted().forEach(ids::put)
        val json = JSONObject()
            .put("question_index", progress.questionIndex.coerceAtLeast(0))
            .put("response", progress.response)
            .put("attempts", progress.attempts.coerceAtLeast(0))
            .put("correct_question_ids", ids)
            .put("has_submitted", progress.hasSubmitted)
        preferences.edit().putString(key(packageId), json.toString()).apply()
    }

    fun clear(packageId: String) {
        preferences.edit().remove(key(packageId)).apply()
    }

    private fun key(packageId: String) = "package:$packageId"
}
