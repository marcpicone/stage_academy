package fr.stageacademy.quiz.profile_view

import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.shared_view.ControlShape

@Composable
internal fun LiveCameraPreview(
    canUseCamera: () -> Boolean,
    onReady: (ImageCapture?) -> Unit,
    onUnavailable: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val canUse by rememberUpdatedState(canUseCamera)
    val ready by rememberUpdatedState(onReady)
    val unavailable by rememberUpdatedState(onUnavailable)
    val description = stringResource(R.string.photo_live_preview)
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxWidth().height(280.dp).clip(ControlShape)
            .semantics { contentDescription = description },
    )

    DisposableEffect(previewView, owner) {
        var disposed = false
        var cameraProvider: ProcessCameraProvider? = null
        var cameraState: LiveData<CameraState>? = null
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val streamObserver = Observer<PreviewView.StreamState> { stream ->
            if (!disposed && canUse()) ready(imageCapture.takeIf { stream == PreviewView.StreamState.STREAMING })
        }
        val cameraObserver = Observer<CameraState> { state ->
            if (!disposed && canUse() && state.error != null) unavailable()
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            if (!disposed && canUse()) {
                try {
                    val provider = providerFuture.get()
                    val selector = when {
                        provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> null
                    }
                    if (selector == null) {
                        unavailable()
                    } else {
                        cameraProvider = provider
                        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                        preview.targetRotation = rotation
                        imageCapture.targetRotation = rotation
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        val camera = provider.bindToLifecycle(owner, selector, preview, imageCapture)
                        cameraState = camera.cameraInfo.cameraState
                        cameraState?.observe(owner, cameraObserver)
                        previewView.previewStreamState.observe(owner, streamObserver)
                    }
                } catch (_: Exception) {
                    unavailable()
                }
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            previewView.previewStreamState.removeObserver(streamObserver)
            cameraState?.removeObserver(cameraObserver)
            cameraProvider?.unbind(preview, imageCapture)
        }
    }
}
