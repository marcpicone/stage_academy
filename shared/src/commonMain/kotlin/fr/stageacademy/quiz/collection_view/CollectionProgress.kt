package fr.stageacademy.quiz.collection_view

internal data class CollectionProgress(
    val points: Int = 0,
    val floats: Map<QuizSecret, SecretFloat> = emptyMap(),
) {
    fun earnPoint() = if (points < Int.MAX_VALUE) copy(points = points + 1) else this
}

internal fun encodeCollection(progress: CollectionProgress): String =
    "${progress.points}|${encodeSecretFloats(progress.floats)}"

internal fun loadCollection(
    encoded: String?,
    previousFloats: String,
    legacyNames: Set<String>,
    save: (CollectionProgress) -> Unit,
): CollectionProgress {
    val points = encoded?.substringBefore('|')?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val knownFloats = encoded?.substringAfter('|', previousFloats) ?: previousFloats
    val floats = loadSecretFloats(knownFloats, legacyNames) { save(CollectionProgress(points, it)) }
    return CollectionProgress(points, floats)
}
