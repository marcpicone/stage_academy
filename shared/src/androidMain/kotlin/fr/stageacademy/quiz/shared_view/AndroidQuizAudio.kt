package fr.stageacademy.quiz.shared_view

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.stageacademy.quiz.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
internal actual fun rememberQuizSoundPlayer(): QuizSoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { AndroidQuizSoundPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.close() }
    }
    return player
}

@OptIn(ExperimentalResourceApi::class)
private class AndroidQuizSoundPlayer(context: Context) : QuizSoundPlayer {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
        .build()
    private val loaded = mutableSetOf<Int>()
    private val samples: Map<QuizSound, Int>

    init {
        pool.setOnLoadCompleteListener { _, sample, status ->
            if (status == 0) loaded.add(sample)
        }
        samples = QuizSound.entries.mapNotNull { sound ->
            val sample = runCatching {
                val path = Res.getUri("files/${sound.fileName}").removePrefix("file:///android_asset/")
                context.assets.openFd(path).use { pool.load(it, 1) }
            }.getOrNull()
            if (sample != null && sample != 0) sound to sample else null
        }.toMap()
    }

    override fun play(sound: QuizSound) {
        val sample = samples[sound] ?: return
        if (sample in loaded) pool.play(sample, 1f, 1f, 1, 0, 1f)
    }

    fun close() {
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}
