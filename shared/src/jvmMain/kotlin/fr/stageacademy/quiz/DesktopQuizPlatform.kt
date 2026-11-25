package fr.stageacademy.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import fr.stageacademy.quiz.collection_view.CollectionProgress
import fr.stageacademy.quiz.collection_view.encodeCollection
import fr.stageacademy.quiz.collection_view.loadCollection
import fr.stageacademy.quiz.leaderboard_view.DesktopLeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.DesktopSessionVault
import fr.stageacademy.quiz.leaderboard_view.LeaderboardStore
import fr.stageacademy.quiz.leaderboard_view.leaderboardStore
import fr.stageacademy.quiz.profile_view.DesktopPhotoFiles
import fr.stageacademy.quiz.profile_view.DesktopProfileScreen
import fr.stageacademy.quiz.profile_view.AccountProfileGateway
import fr.stageacademy.quiz.profile_view.AccountProfileStore
import fr.stageacademy.quiz.profile_view.ProfileIdentity
import fr.stageacademy.quiz.profile_view.ProfilePanel
import fr.stageacademy.quiz.profile_view.ProfilePresenter
import fr.stageacademy.quiz.profile_view.ProfileStore
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.settings_view.QuizAppearance
import fr.stageacademy.quiz.settings_view.QuizSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.prefs.Preferences

internal actual fun elapsedMillis(): Long = System.nanoTime() / 1_000_000L
internal actual val platformIo: CoroutineDispatcher get() = Dispatchers.IO
internal actual typealias StorageException = java.io.IOException

internal val LocalQuizWindow = staticCompositionLocalOf<ComposeWindow> { error("Missing desktop window") }
internal val LocalQuizWindowFocused = staticCompositionLocalOf { false }
private val LocalBackActions = staticCompositionLocalOf<BackActions> { error("Missing desktop navigation") }

fun launchDesktopQuiz() = application {
    val backActions = remember { BackActions() }
    val availableScreen = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds }
    Window(
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.quiz_icon),
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            width = minOf(526, availableScreen.width - 24).dp,
            height = minOf(820, availableScreen.height - 24).dp,
        ),
        onPreviewKeyEvent = {
            it.key == Key.Escape && it.type == KeyEventType.KeyDown && backActions.goBack()
        },
    ) {
        var focused by remember(window) { mutableStateOf(window.isFocused) }
        DisposableEffect(window) {
            window.minimumSize = Dimension(360, 460)
            val focusListener = object : WindowAdapter() {
                override fun windowGainedFocus(event: WindowEvent) { focused = true }
                override fun windowLostFocus(event: WindowEvent) { focused = false }
            }
            window.addWindowFocusListener(focusListener)
            onDispose { window.removeWindowFocusListener(focusListener) }
        }
        CompositionLocalProvider(
            LocalQuizWindow provides window,
            LocalQuizWindowFocused provides focused,
            LocalBackActions provides backActions,
        ) {
            QuizApp()
        }
    }
}

private val dataDirectory: File
    get() = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }?.let { File(it, "StageAcademy") }
        ?: File(System.getProperty("user.home"), ".stage-academy")

@Composable
internal actual fun rememberQuizPreferences(): QuizPreferences =
    remember { DesktopQuizPreferences(Preferences.userRoot().node("fr/stageacademy/quiz")) }

@Composable
internal actual fun rememberLeaderboardStore(): LeaderboardStore =
    remember {
        val localRoot = System.getenv("LOCALAPPDATA")?.let { File(it, "StageAcademy") } ?: dataDirectory
        leaderboardStore(DesktopLeaderboardStore(dataDirectory), DesktopSessionVault(localRoot), "http://127.0.0.1:8000")
    }

@Composable
internal actual fun rememberProfile(scope: CoroutineScope, gateway: AccountProfileGateway, accountId: String?): ProfilePanel =
    remember(scope, gateway, accountId) {
        val root = dataDirectory
        val photos = DesktopPhotoFiles(root)
        val repository = if (accountId == null) ProfileStore(root) else AccountProfileStore(root, accountId, gateway, photos::encodeAvatar)
        val presenter = ProfilePresenter(photos, repository, scope)
        object : ProfilePanel, ProfileIdentity by presenter {
            @Composable
            override fun Content(isProfileSelected: () -> Boolean, accountContent: @Composable () -> Unit) =
                DesktopProfileScreen(presenter, isProfileSelected, accountContent)
        }
    }

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val actions = LocalBackActions.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentAction by rememberUpdatedState(onBack)
    DisposableEffect(actions) {
        val handler = {
            if (currentEnabled) currentAction()
            currentEnabled
        }
        actions.handlers.add(handler)
        onDispose { actions.handlers.remove(handler) }
    }
}

private class BackActions {
    val handlers = mutableListOf<() -> Boolean>()

    fun goBack(): Boolean = handlers.toList().asReversed().any { it() }
}

private class DesktopQuizPreferences(private val preferences: Preferences) : QuizPreferences {
    override fun settings() = QuizSettings(
        appearance = QuizAppearance.entries.find { it.name == preferences.get("appearance", "") }
            ?: QuizAppearance.System,
        showCorrections = preferences.getBoolean("showCorrections", true),
        horizontalAnswers = preferences.getBoolean("horizontalAnswers", false),
        soundEnabled = preferences.getBoolean("soundEnabled", true),
    )

    override fun collection() = loadCollection(
        preferences.get("secretCollection", null),
        preferences.get("secretFloats", ""),
        preferences.get("foundSecrets", "").split(',').toSet(),
        ::saveCollection,
    )

    override fun saveSettings(settings: QuizSettings) {
        preferences.put("appearance", settings.appearance.name)
        preferences.putBoolean("showCorrections", settings.showCorrections)
        preferences.putBoolean("horizontalAnswers", settings.horizontalAnswers)
        preferences.putBoolean("soundEnabled", settings.soundEnabled)
    }

    override fun saveCollection(progress: CollectionProgress) {
        preferences.put("secretCollection", encodeCollection(progress))
    }

    override fun loadAccountCollection(id: String) = preferences.get("accountCollection.$id", null)

    override fun saveAccountCollection(id: String, value: String) {
        preferences.put("accountCollection.$id", value)
    }

    override fun collectionImportOwner() = preferences.get("collectionImportOwner", null)

    override fun saveCollectionImportOwner(id: String) {
        preferences.put("collectionImportOwner", id)
    }

    override fun hasNightCitySignal() = preferences.getBoolean("nightCitySignal", false)

    override fun saveNightCitySignal() {
        preferences.putBoolean("nightCitySignal", true)
    }
}
