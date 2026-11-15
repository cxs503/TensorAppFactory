package dev.tensorappfactory.reference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentPackageLoaderTest {
    private val valid = """
        {
          "schema_version":"1.0",
          "package_id":"demo-reading",
          "title":"Demo Reading",
          "language":"en",
          "questions":[
            {"id":"q1","type":"single_choice","prompt":"Choose one","options":["A","B"],"answer":"A","explanation":"A is supported."},
            {"id":"q2","type":"true_false_not_given","prompt":"Is it true?","answer":"NOT GIVEN","explanation":"No evidence."},
            {"id":"q3","type":"short_answer","prompt":"Answer in three words","answer":"green field","accepted_answers":["green field"],"max_answer_words":3,"explanation":"The text states this."}
          ]
        }
    """.trimIndent()

    @Test fun parsesVersionedMixedQuestionPackage() {
        val result = ContentPackageLoader.parse(valid)
        assertEquals("demo-reading", result.packageId)
        assertEquals(3, result.questions.size)
        assertEquals(ContentQuestionType.TRUE_FALSE_NOT_GIVEN, result.questions[1].type)
        assertEquals(3, result.questions[2].maxAnswerWords)
    }

    @Test fun parsesRequiredAnswerGroups() {
        val packageJson = valid.replace(
            "\"accepted_answers\":[\"green field\"]",
            "\"required_answer_groups\":[[\"green field\",\"a green field\"],[\"fertile soil\"]]"
        )
        val result = ContentPackageLoader.parse(packageJson)
        assertEquals(
            listOf(listOf("green field", "a green field"), listOf("fertile soil")),
            result.questions[2].requiredAnswerGroups
        )
    }

    @Test fun rejectsUnsupportedSchemaVersion() {
        assertThrows(ContentPackageException.Invalid::class.java) {
            ContentPackageLoader.parse(valid.replace("\"1.0\"", "\"2.0\""))
        }
    }

    @Test fun rejectsDuplicateQuestionIds() {
        val duplicate = valid.replace("\"q2\"", "\"q1\"")
        assertThrows(ContentPackageException.Invalid::class.java) { ContentPackageLoader.parse(duplicate) }
    }

    @Test fun rejectsChoiceAnswerNotInOptions() {
        val invalid = valid.replace("\"answer\":\"A\"", "\"answer\":\"C\"")
        assertThrows(ContentPackageException.Invalid::class.java) { ContentPackageLoader.parse(invalid) }
    }

    @Test fun rejectsMissingShortAnswerAlternativesAndGroups() {
        val invalid = valid.replace(",\"accepted_answers\":[\"green field\"]", "")
        assertThrows(ContentPackageException.Invalid::class.java) { ContentPackageLoader.parse(invalid) }
    }
}
