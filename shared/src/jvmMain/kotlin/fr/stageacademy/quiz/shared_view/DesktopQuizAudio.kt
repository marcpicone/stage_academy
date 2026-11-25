package fr.stageacademy.quiz.shared_view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import fr.stageacademy.quiz.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

@Composable
internal actual fun rememberQuizSoundPlayer(): QuizSoundPlayer {
    val player = remember { DesktopQuizSoundPlayer() }
    LaunchedEffect(player) { withContext(Dispatchers.IO) { player.load() } }
    DisposableEffect(player) {
        onDispose { player.close() }
    }
    return player
}

@OptIn(ExperimentalResourceApi::class)
private class DesktopQuizSoundPlayer : QuizSoundPlayer {
    private val lock = Any()
    private val clips = mutableMapOf<QuizSound, Clip>()
    private var active: Clip? = null
    private var closed = false

    suspend fun load() {
        for (sound in QuizSound.entries) {
            val bytes = Res.readBytes("files/${sound.fileName}")
            val clip = openClip(bytes) ?: continue
            synchronized(lock) {
                if (closed) clip.close() else clips[sound] = clip
            }
        }
    }

    private fun openClip(bytes: ByteArray): Clip? {
        var clip: Clip? = null
        return try {
            AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { input ->
                AudioSystem.getClip().also {
                    clip = it
                    it.open(input)
                }
            }
        } catch (_: Exception) {
            clip?.close()
            null
        }
    }

    override fun play(sound: QuizSound) {
        synchronized(lock) {
            val clip = clips[sound] ?: return
            active?.stop()
            clip.framePosition = 0
            clip.start()
            active = clip
        }
    }

    fun close() {
        synchronized(lock) {
            closed = true
            clips.values.forEach { it.close() }
            clips.clear()
            active = null
        }
    }
}
