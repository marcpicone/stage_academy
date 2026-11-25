package fr.stageacademy.quiz.profile_view

import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object DesktopCamera {
    private val session = Mutex()

    fun frames(): Flow<BufferedImage> = flow {
        session.withLock {
            val camera = Webcam.getDefault(3, TimeUnit.SECONDS) ?: throw IOException("No webcam available")
            try {
                if (!camera.open()) throw IOException("Cannot open webcam")
                while (true) {
                    emit(camera.image ?: throw IOException("No webcam frame"))
                    delay(65)
                }
            } finally {
                camera.close()
            }
        }
    }.flowOn(Dispatchers.IO).conflate()
}
