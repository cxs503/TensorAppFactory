package dev.tensorappfactory.reference

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

enum class ContentQuestionType { SINGLE_CHOICE, TRUE_FALSE_NOT_GIVEN, SHORT_ANSWER }

data class ContentQuestion(
    val id: String,
    val type: ContentQuestionType,
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val acceptedAnswers: List<String>,
    val maxAnswerWords: Int?,
    val explanation: String,
    val requiredAnswerGroups: List<List<String>> = emptyList()
)

data class ContentPackage(
    val schemaVersion: String,
    val packageId: String,
    val title: String,
    val language: String,
    val questions: List<ContentQuestion>
)

sealed class ContentPackageException(message: String) : Exception(message) {
    class Invalid(message: String) : ContentPackageException(message)
}

object ContentPackageLoader {
    fun load(input: InputStream): ContentPackage {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parse(text)
    }

    fun parse(json: String): ContentPackage {
        try {
            val root = JSONObject(json)
            require(root.optString("schema_version") == "1.0") { "unsupported schema_version" }
            val packageId = root.optString("package_id")
            require(packageId.matches(Regex("^[a-z][a-z0-9-]{2,63}$"))) { "invalid package_id" }
            val title = root.optString("title")
            require(title.isNotBlank() && title.length <= 120) { "invalid title" }
            val language = root.optString("language")
            require(language.matches(Regex("^[a-z]{2}(-[A-Z]{2})?$"))) { "invalid language" }
            val source = root.getJSONArray("questions")
            require(source.length() > 0) { "questions must not be empty" }
            val ids = mutableSetOf<String>()
            val questions = (0 until source.length()).map { index ->
                val item = source.getJSONObject(index)
                val id = item.optString("id")
                require(id.matches(Regex("^[a-zA-Z0-9_-]{1,64}$"))) { "question[$index] has invalid id" }
                require(ids.add(id)) { "duplicate question id: $id" }
                val type = when (item.optString("type")) {
                    "single_choice" -> ContentQuestionType.SINGLE_CHOICE
                    "true_false_not_given" -> ContentQuestionType.TRUE_FALSE_NOT_GIVEN
                    "short_answer" -> ContentQuestionType.SHORT_ANSWER
                    else -> throw IllegalArgumentException("question[$index] has unsupported type")
                }
                val prompt = item.optString("prompt")
                require(prompt.isNotBlank()) { "question[$index] prompt is empty" }
                val options = item.optJSONArray("options").toStringList()
                val answer = item.optString("answer")
                require(answer.isNotBlank()) { "question[$index] answer is empty" }
                val accepted = item.optJSONArray("accepted_answers").toStringList()
                val groups = item.optJSONArray("required_answer_groups").toStringGroups()
                val maxWords = if (item.has("max_answer_words")) item.optInt("max_answer_words", -1) else null
                val explanation = item.optString("explanation")
                require(explanation.isNotBlank()) { "question[$index] explanation is empty" }
                when (type) {
                    ContentQuestionType.SINGLE_CHOICE -> {
                        require(options.size >= 2 && options.distinct().size == options.size) { "question[$index] requires unique options" }
                        require(options.contains(answer)) { "question[$index] answer must match an option" }
                    }
                    ContentQuestionType.TRUE_FALSE_NOT_GIVEN ->
                        require(answer in setOf("TRUE", "FALSE", "NOT GIVEN")) { "question[$index] has invalid TFNG answer" }
                    ContentQuestionType.SHORT_ANSWER -> {
                        require(accepted.isNotEmpty() || groups.isNotEmpty()) {
                            "question[$index] requires accepted_answers or required_answer_groups"
                        }
                        require(groups.all { group -> group.all { it.isNotBlank() } }) {
                            "question[$index] required_answer_groups must contain non-empty phrases"
                        }
                        require(maxWords == null || maxWords > 0) { "question[$index] max_answer_words must be positive" }
                    }
                }
                ContentQuestion(id, type, prompt, options, answer, accepted, maxWords, explanation, groups)
            }
            return ContentPackage("1.0", packageId, title, language, questions)
        } catch (error: ContentPackageException) {
            throw error
        } catch (error: Exception) {
            throw ContentPackageException.Invalid(error.message ?: "invalid content package")
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }

    private fun JSONArray?.toStringGroups(): List<List<String>> {
        if (this == null) return emptyList()
        return (0 until length()).map { index ->
            getJSONArray(index).toStringList()
        }
    }
}
