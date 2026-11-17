package com.tensor.englishcoach

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal class ReadingProgressStore(
    private val preferences: SharedPreferences,
    private val packRevision: String
) {
    fun load(): ReadingProgress {
        val legacy = ReadingProgress(
            correctCount = preferences.getInt("completed_sessions", 0),
            attemptCount = preferences.getInt("attempted_answers", 0)
        )
        val raw = preferences.getString(KEY, null) ?: return legacy
        return try {
            val json = JSONObject(raw)
            val samePack = json.optString("pack") == packRevision
            val attempt = json.optJSONObject("attempt")?.takeIf { samePack }?.toAttempt()
            val paused = json.optJSONObject("pausedAttempt")?.takeIf { samePack && attempt?.focused == true }?.toAttempt()
            val mistakes = json.optJSONObject("mistakes")?.takeIf { samePack }?.let { entries ->
                entries.keys().asSequence().mapNotNull { id ->
                    val history = when (val entry = entries.opt(id)) {
                        is String -> MistakeHistory().record(entry) // v1 held one wrong response
                        is JSONObject -> {
                            val responses = entry.optJSONArray("responses") ?: JSONArray()
                            val count = entry.getInt("wrongCount")
                            MistakeHistory(
                                count,
                                (maxOf(0, responses.length() - MistakeHistory.MAX_RESPONSES) until responses.length())
                                    .map(responses::getString),
                                entry.optBoolean("corrected")
                            )
                        }
                        else -> null
                    }
                    history?.let { id to it }
                }.toMap()
            }.orEmpty()
            ReadingProgress(attempt, json.getInt("correctCount"), json.getInt("attemptCount"), mistakes, paused)
        } catch (_: org.json.JSONException) {
            legacy
        }
    }

    fun save(progress: ReadingProgress) {
        val mistakes = JSONObject()
        progress.mistakes.forEach { (id, history) ->
            mistakes.put(id, JSONObject()
                .put("wrongCount", history.wrongCount)
                .put("responses", JSONArray(history.responses))
                .put("corrected", history.corrected))
        }
        val json = JSONObject()
            .put("pack", packRevision)
            .put("correctCount", progress.correctCount)
            .put("attemptCount", progress.attemptCount)
            .put("mistakes", mistakes)
        progress.attempt?.let { json.put("attempt", it.toJson()) }
        progress.pausedAttempt?.let { json.put("pausedAttempt", it.toJson()) }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    private fun JSONObject.toAttempt() = ReadingAttempt(
        itemId = getString("item"),
        selected = if (isNull("selected")) null else getString("selected"),
        typed = optString("typed"),
        correct = if (isNull("correct")) null else getBoolean("correct"),
        focused = optBoolean("focused")
    )

    private fun ReadingAttempt.toJson() = JSONObject()
        .put("item", itemId)
        .put("selected", selected ?: JSONObject.NULL)
        .put("typed", typed)
        .put("correct", correct ?: JSONObject.NULL)
        .put("focused", focused)

    private companion object {
        const val KEY = "reading_progress_v1"
    }
}
