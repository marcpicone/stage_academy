package fr.stageacademy.quiz.collection_view

import fr.stageacademy.quiz.resources.*
import org.jetbrains.compose.resources.StringResource
import kotlin.random.Random

internal enum class CrateRarity(val label: StringResource) {
    Common(Res.string.crate_common),
    Rare(Res.string.crate_rare),
    Epic(Res.string.crate_epic),
}

internal enum class SecretCrate(val title: StringResource, val cost: Int, private val weights: List<Int>) {
    Basic(Res.string.crate_basic, 5, listOf(80, 18, 2)),
    Advanced(Res.string.crate_advanced, 12, listOf(50, 37, 13)),
    Prestige(Res.string.crate_prestige, 25, listOf(20, 45, 35)),
    ;

    fun odds(found: Set<QuizSecret>): Map<CrateRarity, Int> {
        val remaining = availableWeights(found)
        if (remaining.isEmpty()) return emptyMap()
        val total = remaining.sumOf { it.second }
        var cumulative = 0
        return remaining.associate { (rarity, weight) ->
            val previous = cumulative * 100 / total
            cumulative += weight
            rarity to (cumulative * 100 / total - previous)
        }
    }

    fun open(progress: CollectionProgress, random: Random = Random.Default): CrateReward? {
        if (progress.points < cost) return null
        val chances = odds(progress.floats.keys)
        if (chances.isEmpty()) return null
        var ticket = random.nextInt(100)
        val rarity = chances.entries.first { (_, chance) ->
            ticket -= chance
            ticket < 0
        }.key
        val secret = QuizSecret.entries.filter {
            it.crateRarity == rarity && it !in progress.floats
        }.random(random)
        val updated = progress.copy(
            points = progress.points - cost,
            floats = progress.floats + (secret to SecretFloat.roll()),
        )
        return CrateReward(secret, updated)
    }

    private fun availableWeights(found: Set<QuizSecret>): List<Pair<CrateRarity, Int>> =
        CrateRarity.entries.zip(weights).filter { (rarity, _) ->
            QuizSecret.entries.any { it.crateRarity == rarity && it !in found }
        }
}

internal data class CrateReward(val secret: QuizSecret, val progress: CollectionProgress)
