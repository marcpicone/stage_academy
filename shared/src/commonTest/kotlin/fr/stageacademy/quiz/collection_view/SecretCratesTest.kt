package fr.stageacademy.quiz.collection_view

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecretCratesTest {
    @Test
    fun theMoreExpensiveCratesHaveBetterOddsForEpicRewards() {
        assertEquals(listOf(2, 13, 35), SecretCrate.entries.map { it.odds(emptySet())[CrateRarity.Epic] })
        assertEquals(100, SecretCrate.Prestige.odds(emptySet()).values.sum())
        val common = QuizSecret.entries.filter { it.crateRarity == CrateRarity.Common }.toSet()
        assertEquals(mapOf(CrateRarity.Rare to 90, CrateRarity.Epic to 10), SecretCrate.Basic.odds(common))
    }

    @Test
    fun purchasesOnlyDebitOnceAndNeverReturnDuplicates() {
        val random = Random(3)
        var progress = CollectionProgress(points = 155)
        val owned = mutableSetOf<QuizSecret>()
        repeat(6) {
            val reward = assertNotNull(SecretCrate.Prestige.open(progress, random))
            assertTrue(owned.add(reward.secret))
            assertEquals(progress.points - 25, reward.progress.points)
            progress = reward.progress
        }
        assertEquals(5, progress.points)
        assertNull(SecretCrate.Basic.open(progress, random))
        assertNull(SecretCrate.Basic.open(CollectionProgress(points = 4), random))
    }

    @Test
    fun theBalanceAndRewardRoundTripTogetherAndOlderFloatsRemain() {
        val floats = mapOf(QuizSecret.Bug to SecretFloat(123), QuizSecret.PocketGalaxy to SecretFloat(789))
        val progress = CollectionProgress(points = 12, floats = floats)
        val restored = loadCollection(encodeCollection(progress), "", emptySet()) { error("Unexpected migration") }
        assertEquals(progress, restored)
        assertEquals(CollectionProgress(floats = floats), loadCollection(null, encodeSecretFloats(floats), emptySet()) {})
    }
}
