package fr.stageacademy.quiz.collection_view

import kotlin.jvm.JvmInline
import kotlin.random.Random

internal const val SecretFloatScale = 10_000_000_000L

@JvmInline
internal value class SecretFloat(val units: Long) {
    init {
        require(units in 0 until SecretFloatScale)
    }

    val decimal: String get() = "0.${units.toString().padStart(10, '0')}"
    val quality: SecretQuality get() = SecretQuality.entries.first { units >= it.minimum }

    companion object {
        fun roll(): SecretFloat = SecretFloat(Random.nextLong(SecretFloatScale))
    }
}

internal enum class SecretQuality(val abbreviation: String, val displayName: String, val minimum: Long) {
    FactoryNew("FN", "Factory New", 9_300_000_000L),
    MinimalWear("MW", "Minimal Wear", 8_500_000_000L),
    FieldTested("FT", "Field-Tested", 6_200_000_000L),
    WellWorn("WW", "Well-Worn", 5_500_000_000L),
    BattleScarred("BS", "Battle-Scarred", 0L),
}

internal fun encodeSecretFloats(floats: Map<QuizSecret, SecretFloat>): String =
    floats.entries.joinToString(";") { (secret, value) -> "${secret.name}=${value.units}" }

internal fun loadSecretFloats(
    encoded: String,
    legacyNames: Set<String>,
    save: (Map<QuizSecret, SecretFloat>) -> Unit,
): Map<QuizSecret, SecretFloat> {
    val saved = encoded.split(';').mapNotNull { entry ->
        val name = entry.substringBefore('=')
        val value = entry.substringAfter('=', "").toLongOrNull()
        if (value != null && value in 0 until SecretFloatScale) name to SecretFloat(value) else null
    }.toMap()
    val migrated = buildMap {
        for (secret in QuizSecret.entries) {
            val existing = saved[secret.name]
            if (existing != null) put(secret, existing)
            else if (secret.name in legacyNames) put(secret, SecretFloat.roll())
        }
    }
    if (migrated.keys.any { it.name !in saved }) save(migrated)
    return migrated
}
