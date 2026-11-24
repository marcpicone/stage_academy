package fr.stageacademy.quiz.profile_view

import android.Manifest
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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.stageacademy.quiz.R
import fr.stageacademy.quiz.shared_view.ControlShape
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard
import fr.stageacademy.quiz.shared_view.QuizActionBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private enum class PhotoStatus(val message: Int) {
    Captured(R.string.photo_captured),
    Denied(R.string.photo_permission_denied),
    Blocked(R.string.photo_permission_settings),
    Canceled(R.string.photo_canceled),
    Paused(R.string.photo_camera_paused),
    Unavailable(R.string.photo_camera_unavailable),
    Failed(R.string.photo_failed),
    Missing(R.string.photo_missing),
}

@Composable
internal fun ProfileScreen(editor: ProfileEditor, isProfileSelected: () -> Boolean) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val files = editor.photos
    var status by remember { mutableStateOf<PhotoStatus?>(null) }
    var cameraOpen by remember { mutableStateOf(false) }
    var permissionPending by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val photo = editor.photo
    var preview by remember(photo) { mutableStateOf<Bitmap?>(null) }
    val previewRequester = remember { BringIntoViewRequester() }
    val nameRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(editor) { editor.load() }
    LaunchedEffect(editor.nameInvalid) {
        if (editor.nameInvalid) {
            withFrameNanos { }
            nameRequester.bringIntoView()
        }
    }

    fun closeCamera(result: PhotoStatus) {
        cameraOpen = false
        capture = null
        pendingPhoto = null
        status = result
    }

    fun canUseCamera(): Boolean = cameraOpen && isProfileSelected() &&
        lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && context.hasCameraPermission()

    BackHandler(enabled = cameraOpen) { closeCamera(PhotoStatus.Canceled) }

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

    LaunchedEffect(files, photo) {
        try {
            preview = photo?.let { withContext(Dispatchers.IO) { files.loadPreview(it) } }
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

    fun takePhoto() {
        val imageCapture = capture ?: return
        if (!canUseCamera() || pendingPhoto != null) return

        val output = try {
            files.prepareCapture()
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
            if (saved && files.isValidCapture(output)) {
                editor.selectPhoto(output)
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

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                !editor.ready -> PhotoMessage(stringResource(R.string.profile_loading))
                cameraOpen -> MenuCard(title = stringResource(R.string.photo_title)) {
                    LiveCameraPreview(
                        canUseCamera = ::canUseCamera,
                        onReady = { capture = it },
                        onUnavailable = { closeCamera(PhotoStatus.Unavailable) },
                    )
                    val cameraMessage = when {
                        pendingPhoto != null -> R.string.photo_saving
                        capture == null -> R.string.photo_preparing
                        else -> null
                    }
                    if (cameraMessage != null) PhotoMessage(stringResource(cameraMessage))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MenuButton(
                            label = stringResource(R.string.photo_close_camera),
                            onClick = { closeCamera(PhotoStatus.Canceled) },
                            modifier = Modifier.weight(1f),
                        )
                        PhotoButton(
                            label = stringResource(R.string.photo_take),
                            enabled = capture != null && pendingPhoto == null,
                            onClick = ::takePhoto,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                else -> {
                    MenuCard(title = stringResource(R.string.profile_player)) {
                        OutlinedTextField(
                            value = editor.name,
                            onValueChange = editor::editName,
                            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(nameRequester),
                            enabled = !editor.saving,
                            label = { Text(stringResource(R.string.profile_name)) },
                            singleLine = true,
                            isError = editor.nameInvalid,
                            supportingText = if (editor.nameInvalid) {
                                { Text(stringResource(R.string.profile_name_required)) }
                            } else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        )
                    }
                    MenuCard(title = stringResource(R.string.photo_title)) {
                        Text(
                            text = stringResource(R.string.photo_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ProfilePhoto(preview, previewRequester, status == PhotoStatus.Captured)
                        status?.let { PhotoMessage(stringResource(it.message)) }
                        PhotoButton(
                            label = stringResource(if (photo == null) R.string.photo_open_camera else R.string.photo_retake),
                            enabled = !permissionPending && !editor.saving,
                            onClick = {
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
                        )
                        if (photo != null) {
                            MenuButton(
                                label = stringResource(R.string.profile_remove_photo),
                                onClick = {
                                    editor.selectPhoto(null)
                                    status = null
                                },
                                enabled = !editor.saving,
                            )
                        }
                        if (status == PhotoStatus.Blocked) {
                            MenuButton(label = stringResource(R.string.photo_open_settings), onClick = {
                                settingsLauncher.launch(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                                )
                            })
                        }
                    }
                }
            }
        }
        if (!cameraOpen) {
            QuizActionBar(
                label = stringResource(if (editor.saving) R.string.profile_saving else R.string.profile_save),
                onClick = editor::save,
                enabled = editor.ready && !editor.saving,
            ) {
                val message = editor.message ?: R.string.profile_unsaved.takeIf { editor.hasChanges && !editor.nameInvalid }
                message?.let { PhotoMessage(stringResource(it)) }
            }
        }
    }
}

@Composable
private fun ProfilePhoto(bitmap: Bitmap?, requester: BringIntoViewRequester, newlyCaptured: Boolean) {
    if (bitmap != null) {
        Image(
            bitmap = remember(bitmap) { bitmap.asImageBitmap() },
            contentDescription = stringResource(R.string.photo_preview),
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                .bringIntoViewRequester(requester).clip(ControlShape),
            contentScale = ContentScale.Fit,
        )
        if (newlyCaptured) {
            LaunchedEffect(bitmap) {
                withFrameNanos { }
                requester.bringIntoView()
            }
        }
    } else {
        val tint = MaterialTheme.colorScheme.onPrimaryContainer
        val background = MaterialTheme.colorScheme.primaryContainer
        val description = stringResource(R.string.profile_default_avatar)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.size(80.dp).clip(CircleShape).background(background)
                    .semantics { contentDescription = description },
            ) {
                drawCircle(tint, size.width * 0.15f, Offset(center.x, size.height * 0.37f))
                drawOval(tint, Offset(size.width * 0.22f, size.height * 0.56f), Size(size.width * 0.56f, size.height * 0.34f))
            }
        }
    }
}

@Composable
private fun PhotoMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun PhotoButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ControlShape,
    ) {
        Text(label)
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
