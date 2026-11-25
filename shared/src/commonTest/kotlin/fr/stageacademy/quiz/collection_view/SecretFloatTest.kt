package fr.stageacademy.quiz.collection_view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SecretFloatTest {
    @Test
    fun decimalFormattingPreservesAllTenDigitsAndBothExtremes() {
        assertEquals("0.0000000000", SecretFloat(0).decimal)
        assertEquals("0.0000000001", SecretFloat(1).decimal)
        assertEquals("0.9999999999", SecretFloat(9_999_999_999L).decimal)
        assertFailsWith<IllegalArgumentException> { SecretFloat(SecretFloatScale) }
    }

    @Test
    fun largerFloatsReachTheBetterQualities() {
        val thresholds = listOf(
            0L to "BS",
            5_499_999_999L to "BS",
            5_500_000_000L to "WW",
            6_200_000_000L to "FT",
            8_500_000_000L to "MW",
            9_299_999_999L to "MW",
            9_300_000_000L to "FN",
            9_999_999_999L to "FN",
        )
        thresholds.forEach { (units, code) -> assertEquals(code, SecretFloat(units).quality.abbreviation) }
    }

    @Test
    fun previouslyFoundSecretsKeepTheFloatSavedDuringMigration() {
        val old = setOf(QuizSecret.Star.name, QuizSecret.Bug.name, "Unknown")
        var serialized = "Star=9500000000"
        var writes = 0
        fun load() = loadSecretFloats(serialized, old) {
            serialized = encodeSecretFloats(it)
            writes++
        }

        val firstLoad = load()
        assertEquals(setOf(QuizSecret.Star, QuizSecret.Bug), firstLoad.keys)
        assertEquals(SecretFloat(9_500_000_000L), firstLoad[QuizSecret.Star])
        assertEquals(firstLoad, load())
        assertEquals(1, writes)
    }
}
