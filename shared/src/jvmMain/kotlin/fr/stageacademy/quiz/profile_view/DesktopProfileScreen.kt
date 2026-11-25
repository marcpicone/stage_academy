package fr.stageacademy.quiz.profile_view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import fr.stageacademy.quiz.LocalQuizWindow
import fr.stageacademy.quiz.LocalQuizWindowFocused
import fr.stageacademy.quiz.PlatformBackHandler
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.PanelShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
internal fun DesktopProfileScreen(
    editor: ProfileContract<File, BufferedImage, File>,
    isProfileSelected: () -> Boolean,
    accountContent: @Composable () -> Unit,
) {
    val window = LocalQuizWindow.current
    val focused = LocalQuizWindowFocused.current
    var status by remember { mutableStateOf<StringResource?>(null) }
    var cameraOpen by remember { mutableStateOf(false) }
    var frame by remember { mutableStateOf<BufferedImage?>(null) }
    var capture by remember { mutableStateOf<BufferedImage?>(null) }
    val photo = editor.photo
    var preview by remember(photo) { mutableStateOf<BufferedImage?>(null) }
    val cropImage = editor.cropImage

    fun closeCamera(result: StringResource) {
        cameraOpen = false
        frame = null
        capture = null
        status = result
    }

    LaunchedEffect(editor) { editor.load() }
    LaunchedEffect(photo) {
        try {
            preview = photo?.let { editor.loadPreview(it) }
        } catch (_: FileNotFoundException) {
            status = Res.string.photo_missing
        } catch (_: IOException) {
            status = Res.string.photo_failed
        }
    }
    LaunchedEffect(cameraOpen, focused) {
        if (!cameraOpen) return@LaunchedEffect
        if (!focused || !isProfileSelected()) {
            closeCamera(Res.string.photo_camera_paused)
            return@LaunchedEffect
        }
        DesktopCamera.frames()
            .catch { failure ->
                if (failure is Exception || failure is LinkageError) {
                    closeCamera(Res.string.photo_camera_unavailable)
                } else {
                    throw failure
                }
            }
            .collect { frame = it }
    }
    LaunchedEffect(capture) {
        val image = capture ?: return@LaunchedEffect
        var output: File? = null
        var accepted = false
        try {
            val file = withContext(Dispatchers.IO) {
                editor.prepareCapture().also {
                    output = it
                    if (!ImageIO.write(image, "jpg", it)) throw IOException("Capture encoding failed")
                }
            }
            if (cameraOpen && isProfileSelected() && window.isFocused) {
                accepted = editor.acceptCapture(file)
                closeCamera(if (accepted) Res.string.photo_captured else Res.string.photo_failed)
            }
        } catch (_: IOException) {
            closeCamera(Res.string.photo_failed)
        } finally {
            if (!accepted) output?.delete()
        }
    }
    PlatformBackHandler(cameraOpen || cropImage != null) {
        if (cameraOpen) closeCamera(Res.string.photo_canceled) else editor.cancelCrop()
    }

    val galleryTitle = stringResource(Res.string.photo_gallery)
    ProfileLayout(editor, cameraOpen, pickerPending = false, accountContent) {
        when {
            !editor.ready -> PhotoMessage(stringResource(Res.string.profile_loading))
            cropImage != null -> PhotoCropPanel(
                image = remember(cropImage) { cropImage.toComposeImageBitmap() },
                busy = editor.importing,
                message = editor.message,
                onCancel = editor::cancelCrop,
                onConfirm = editor::confirmCrop,
            )
            cameraOpen -> CameraCard(
                ready = frame != null,
                saving = capture != null,
                onClose = { closeCamera(Res.string.photo_canceled) },
                onCapture = { capture = frame },
            ) {
                frame?.let { current ->
                    Image(
                        bitmap = remember(current) { current.toComposeImageBitmap() },
                        contentDescription = stringResource(Res.string.photo_live_preview),
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(PanelShape),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            else -> ProfileForm(
                editor = editor,
                preview = remember(preview) { preview?.toComposeImageBitmap() },
                status = status,
                mediaPending = false,
                pickerPending = false,
                onCamera = {
                    if (isProfileSelected() && window.isFocused) {
                        status = null
                        cameraOpen = true
                    }
                },
                onGallery = {
                    val chooser = JFileChooser().apply {
                        dialogTitle = galleryTitle
                        isAcceptAllFileFilterUsed = false
                        fileFilter = FileNameExtensionFilter("JPEG, PNG, GIF, BMP, TIFF", "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff")
                    }
                    if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION && isProfileSelected()) {
                        status = null
                        editor.importPhoto(chooser.selectedFile)
                    }
                },
                onRemovePhoto = {
                    editor.removePhoto()
                    status = null
                },
            )
        }
    }
}
