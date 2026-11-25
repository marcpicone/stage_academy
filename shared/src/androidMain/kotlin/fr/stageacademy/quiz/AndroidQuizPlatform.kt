package fr.stageacademy.quiz

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.stageacademy.quiz.collection_view.CollectionProgress
import fr.stageacademy.quiz.collection_view.encodeCollection
import fr.stageacademy.quiz.collection_view.loadCollection
import fr.stageacademy.quiz.leaderboard_view.AndroidLeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.AndroidSessionVault
import fr.stageacademy.quiz.leaderboard_view.LeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.leaderboardStore
import fr.stageacademy.quiz.profile_view.PhotoFiles
import fr.stageacademy.quiz.profile_view.AccountProfileGateway
import fr.stageacademy.quiz.profile_view.AccountProfileStore
import fr.stageacademy.quiz.profile_view.ProfileIdentity
import fr.stageacademy.quiz.profile_view.ProfilePanel
import fr.stageacademy.quiz.profile_view.ProfilePresenter
import fr.stageacademy.quiz.profile_view.ProfileScreen
import fr.stageacademy.quiz.profile_view.ProfileStore
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.settings_view.QuizSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal actual fun elapsedMillis(): Long = SystemClock.elapsedRealtime()
internal actual val platformIo: CoroutineDispatcher get() = Dispatchers.IO
internal actual typealias StorageException = java.io.IOException

@Composable
internal actual fun rememberQuizPreferences(): QuizPreferences {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidQuizPreferences(context.getSharedPreferences("quiz_settings", Context.MODE_PRIVATE))
    }
}

@Composable
internal actual fun rememberLeaderboardStore(): LeaderboardStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { leaderboardStore(AndroidLeaderboardStore(context), AndroidSessionVault(context)) }
}

@Composable
internal actual fun rememberProfile(scope: CoroutineScope, gateway: AccountProfileGateway, accountId: String?): ProfilePanel {
    val context = LocalContext.current.applicationContext
    return remember(context, scope, gateway, accountId) {
        val photos = PhotoFiles(context)
        val root = context.noBackupFilesDir
        val repository = if (accountId == null) ProfileStore(root) else AccountProfileStore(root, accountId, gateway, photos::encodeAvatar)
        val presenter = ProfilePresenter(photos, repository, scope)
        object : ProfilePanel, ProfileIdentity by presenter {
            @Composable
            override fun Content(isProfileSelected: () -> Boolean, accountContent: @Composable () -> Unit) =
                ProfileScreen(presenter, isProfileSelected, accountContent)
        }
    }
}

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) =
    BackHandler(enabled, onBack)

private class AndroidQuizPreferences(private val preferences: SharedPreferences) : QuizPreferences {
    override fun settings() = QuizSettings(
        appearance = QuizAppearance.entries.find { it.name == preferences.getString("appearance", null) }
            ?: QuizAppearance.System,
        showCorrections = preferences.getBoolean("showCorrections", true),
        horizontalAnswers = preferences.getBoolean("horizontalAnswers", false),
        soundEnabled = preferences.getBoolean("soundEnabled", true),
    )

    override fun collection() = loadCollection(
        preferences.getString("secretCollection", null),
        preferences.getString("secretFloats", "").orEmpty(),
        preferences.getStringSet("foundSecrets", emptySet()).orEmpty(),
        ::saveCollection,
    )

    override fun saveSettings(settings: QuizSettings) {
        preferences.edit()
            .putString("appearance", settings.appearance.name)
            .putBoolean("showCorrections", settings.showCorrections)
            .putBoolean("horizontalAnswers", settings.horizontalAnswers)
            .putBoolean("soundEnabled", settings.soundEnabled)
            .apply()
    }

    override fun saveCollection(progress: CollectionProgress) {
        preferences.edit().putString("secretCollection", encodeCollection(progress)).apply()
    }

    override fun loadAccountCollection(id: String) = preferences.getString("accountCollection.$id", null)

    override fun saveAccountCollection(id: String, value: String) {
        preferences.edit().putString("accountCollection.$id", value).apply()
    }

    override fun collectionImportOwner() = preferences.getString("collectionImportOwner", null)

    override fun saveCollectionImportOwner(id: String) {
        preferences.edit().putString("collectionImportOwner", id).apply()
    }

    override fun hasNightCitySignal() = preferences.getBoolean("nightCitySignal", false)

    override fun saveNightCitySignal() {
        preferences.edit().putBoolean("nightCitySignal", true).apply()
    }
}
