package fr.stageacademy.quiz.profile_view

import org.jetbrains.compose.resources.StringResource

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private enum class PhotoStatus(val message: StringResource) {
    Captured(Res.string.photo_captured),
    Denied(Res.string.photo_permission_denied),
    Blocked(Res.string.photo_permission_settings),
    Canceled(Res.string.photo_canceled),
    Paused(Res.string.photo_camera_paused),
    Unavailable(Res.string.photo_camera_unavailable),
    Failed(Res.string.photo_failed),
    Missing(Res.string.photo_missing),
    GalleryUnavailable(Res.string.photo_gallery_unavailable),
}

@Composable
internal fun ProfileScreen(
    editor: ProfileContract<Uri, Bitmap, File>, isProfileSelected: () -> Boolean, accountContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember { mutableStateOf<PhotoStatus?>(null) }
    var cameraOpen by remember { mutableStateOf(false) }
    var permissionPending by remember { mutableStateOf(false) }
    var pickerPending by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val photo = editor.photo
    var preview by remember(photo) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(editor) { editor.load() }

    fun closeCamera(result: PhotoStatus) {
        cameraOpen = false
        capture = null
        pendingPhoto = null
        status = result
    }

    fun canUseCamera(): Boolean = cameraOpen && isProfileSelected() &&
        lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && context.hasCameraPermission()

    val cropImage = editor.cropImage
    BackHandler(enabled = cameraOpen || cropImage != null) {
        if (cameraOpen) closeCamera(PhotoStatus.Canceled) else editor.cancelCrop()
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && cameraOpen) closeCamera(PhotoStatus.Paused)
            if (event == Lifecycle.Event.ON_START && cameraOpen && !context.hasCameraPermission()) {
                closeCamera(PhotoStatus.Denied)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            cameraOpen = false
            permissionPending = false
            pendingPhoto = null
        }
    }

    LaunchedEffect(editor, photo) {
        try {
            preview = photo?.let { editor.loadPreview(it) }
        } catch (_: FileNotFoundException) {
            preview = null
            if (editor.photo == photo) status = PhotoStatus.Missing
        } catch (_: IOException) {
            preview = null
            if (editor.photo == photo) status = PhotoStatus.Failed
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (permissionPending && isProfileSelected()) {
            permissionPending = false
            if (granted) {
                cameraOpen = true
            } else {
                val canAskAgain = activity == null ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                status = if (canAskAgain) PhotoStatus.Denied else PhotoStatus.Blocked
            }
        }
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isProfileSelected() && context.hasCameraPermission()) status = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickerPending = false
        if (uri != null && isProfileSelected()) {
            status = null
            editor.importPhoto(uri)
        }
    }

    fun takePhoto() {
        val imageCapture = capture ?: return
        if (!canUseCamera() || pendingPhoto != null) return

        val output = try {
            editor.prepareCapture()
        } catch (_: IOException) {
            closeCamera(PhotoStatus.Failed)
            return
        }
        pendingPhoto = output

        fun handleCaptureResult(saved: Boolean) {
            if (pendingPhoto != output || !canUseCamera()) {
                output.delete()
                return
            }
            if (saved && editor.acceptCapture(output)) {
                closeCamera(PhotoStatus.Captured)
            } else {
                output.delete()
                closeCamera(PhotoStatus.Failed)
            }
        }

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(output).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = handleCaptureResult(true)
                override fun onError(exception: ImageCaptureException) = handleCaptureResult(false)
            },
        )
    }

    ProfileLayout(editor, cameraOpen, pickerPending, accountContent) {
        when {
            !editor.ready -> PhotoMessage(stringResource(Res.string.profile_loading))
            cropImage != null -> PhotoCropPanel(
                image = remember(cropImage) { cropImage.asImageBitmap() },
                busy = editor.importing,
                message = editor.message,
                onCancel = editor::cancelCrop,
                onConfirm = editor::confirmCrop,
            )
            cameraOpen -> CameraCard(
                ready = capture != null,
                saving = pendingPhoto != null,
                onClose = { closeCamera(PhotoStatus.Canceled) },
                onCapture = ::takePhoto,
            ) {
                LiveCameraPreview(
                    canUseCamera = ::canUseCamera,
                    onReady = { capture = it },
                    onUnavailable = { closeCamera(PhotoStatus.Unavailable) },
                )
            }
            else -> ProfileForm(
                editor = editor,
                preview = remember(preview) { preview?.asImageBitmap() },
                status = status?.message,
                mediaPending = permissionPending,
                pickerPending = pickerPending,
                onCamera = {
                    if (!permissionPending && !editor.saving && isProfileSelected()) {
                        status = null
                        if (context.hasCameraPermission()) {
                            cameraOpen = true
                        } else {
                            permissionPending = true
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                onGallery = {
                    pickerPending = true
                    try {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } catch (_: ActivityNotFoundException) {
                        pickerPending = false
                        status = PhotoStatus.GalleryUnavailable
                    }
                },
                onRemovePhoto = {
                    editor.removePhoto()
                    status = null
                },
                openPermissionSettings = if (status == PhotoStatus.Blocked) {
                    {
                        settingsLauncher.launch(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    }
                } else null,
            )
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
