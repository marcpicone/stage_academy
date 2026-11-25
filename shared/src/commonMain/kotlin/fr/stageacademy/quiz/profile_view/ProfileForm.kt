package fr.stageacademy.quiz.profile_view

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.ControlShape
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard
import fr.stageacademy.quiz.shared_view.QuizActionBar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileLayout(
    editor: ProfileContract<*, *, *>,
    cameraOpen: Boolean,
    pickerPending: Boolean,
    accountContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!cameraOpen && editor.cropImage == null) accountContent()
            content()
        }
        if (!cameraOpen && editor.cropImage == null) {
            QuizActionBar(
                label = stringResource(if (editor.saving) Res.string.profile_saving else Res.string.profile_save),
                onClick = editor::save,
                enabled = editor.ready && !editor.saving && !editor.importing && !pickerPending,
            ) {
                val message = if (editor.importing) Res.string.photo_importing else
                    editor.message ?: Res.string.profile_unsaved.takeIf { editor.hasChanges && !editor.nameInvalid }
                message?.let { PhotoMessage(stringResource(it)) }
            }
        }
    }
}

@Composable
internal fun ProfileForm(
    editor: ProfileContract<*, *, *>,
    preview: ImageBitmap?,
    status: StringResource?,
    mediaPending: Boolean,
    pickerPending: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    openPermissionSettings: (() -> Unit)? = null,
) {
    val nameRequester = remember { BringIntoViewRequester() }
    val previewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(editor.nameInvalid) {
        if (editor.nameInvalid) {
            withFrameNanos { }
            nameRequester.bringIntoView()
        }
    }
    MenuCard(title = stringResource(Res.string.profile_player)) {
        OutlinedTextField(
            value = editor.name,
            onValueChange = editor::editName,
            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(nameRequester),
            enabled = !editor.saving,
            label = { Text(stringResource(Res.string.profile_name)) },
            singleLine = true,
            isError = editor.nameInvalid,
            supportingText = if (editor.nameInvalid) {
                { Text(stringResource(Res.string.profile_name_required)) }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
    MenuCard(title = stringResource(Res.string.photo_title)) {
        Text(
            text = stringResource(Res.string.photo_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProfilePhoto(preview, previewRequester, status == Res.string.photo_captured)
        status?.let { PhotoMessage(stringResource(it)) }
        val enabled = !mediaPending && !pickerPending && !editor.saving && !editor.importing
        PhotoButton(
            label = stringResource(if (editor.photo == null) Res.string.photo_open_camera else Res.string.photo_retake),
            enabled = enabled,
            onClick = onCamera,
        )
        MenuButton(label = stringResource(Res.string.photo_gallery), enabled = enabled, onClick = onGallery)
        if (editor.photo != null) {
            MenuButton(
                label = stringResource(Res.string.profile_remove_photo),
                onClick = onRemovePhoto,
                enabled = !editor.saving && !editor.importing && !pickerPending,
            )
        }
        if (openPermissionSettings != null) {
            MenuButton(label = stringResource(Res.string.photo_open_settings), onClick = openPermissionSettings)
        }
    }
}

@Composable
internal fun CameraCard(
    ready: Boolean,
    saving: Boolean,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    preview: @Composable () -> Unit,
) {
    MenuCard(title = stringResource(Res.string.photo_title)) {
        preview()
        val message = when {
            saving -> Res.string.photo_saving
            !ready -> Res.string.photo_preparing
            else -> null
        }
        message?.let { PhotoMessage(stringResource(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton(
                label = stringResource(Res.string.photo_close_camera),
                onClick = onClose,
                modifier = Modifier.weight(1f),
            )
            PhotoButton(
                label = stringResource(Res.string.photo_take),
                enabled = ready && !saving,
                onClick = onCapture,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ProfilePhoto(bitmap: ImageBitmap?, requester: BringIntoViewRequester, newlyCaptured: Boolean) {
    if (bitmap != null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(Res.string.photo_preview),
                modifier = Modifier.size(128.dp).bringIntoViewRequester(requester).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        if (newlyCaptured) {
            LaunchedEffect(bitmap) {
                withFrameNanos { }
                requester.bringIntoView()
            }
        }
    } else {
        val tint = MaterialTheme.colorScheme.onPrimaryContainer
        val background = MaterialTheme.colorScheme.primaryContainer
        val description = stringResource(Res.string.profile_default_avatar)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.size(128.dp).clip(CircleShape).background(background)
                    .semantics { contentDescription = description },
            ) {
                drawCircle(tint, size.width * 0.15f, Offset(center.x, size.height * 0.37f))
                drawOval(tint, Offset(size.width * 0.22f, size.height * 0.56f), Size(size.width * 0.56f, size.height * 0.34f))
            }
        }
    }
}

@Composable
internal fun PhotoMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun PhotoButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ControlShape,
    ) {
        Text(label)
    }
}


