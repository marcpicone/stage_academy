package fr.stageacademy.quiz

import androidx.compose.runtime.Composable
import fr.stageacademy.quiz.collection_view.CollectionProgress
import fr.stageacademy.quiz.leaderboard_view.LeaderboardStore
import fr.stageacademy.quiz.profile_view.ProfilePanel
import fr.stageacademy.quiz.profile_view.AccountProfileGateway
import fr.stageacademy.quiz.settings_view.QuizSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

internal interface QuizPreferences {
    fun settings(): QuizSettings
    fun collection(): CollectionProgress
    fun saveSettings(settings: QuizSettings)
    fun saveCollection(progress: CollectionProgress)
    fun loadAccountCollection(id: String): String? = null
    fun saveAccountCollection(id: String, value: String) = Unit
    fun collectionImportOwner(): String? = null
    fun saveCollectionImportOwner(id: String) = Unit
    fun hasNightCitySignal(): Boolean
    fun saveNightCitySignal()
}

internal expect fun elapsedMillis(): Long
internal expect val platformIo: CoroutineDispatcher
internal expect open class StorageException : Exception

@Composable
internal expect fun rememberQuizPreferences(): QuizPreferences

@Composable
internal expect fun rememberLeaderboardStore(): LeaderboardStore

@Composable
internal expect fun rememberProfile(scope: CoroutineScope, gateway: AccountProfileGateway, accountId: String?): ProfilePanel

@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
