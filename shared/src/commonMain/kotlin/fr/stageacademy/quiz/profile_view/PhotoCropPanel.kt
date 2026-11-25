package fr.stageacademy.quiz.profile_view

import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.stageacademy.quiz.resources.Res
import fr.stageacademy.quiz.resources.*
import fr.stageacademy.quiz.shared_view.MenuButton
import fr.stageacademy.quiz.shared_view.MenuCard

@Composable
internal fun PhotoCropPanel(
    image: ImageBitmap,
    busy: Boolean,
    message: StringResource?,
    onCancel: () -> Unit,
    onConfirm: (PhotoCrop) -> Unit,
) {
    var crop by remember(image) { mutableStateOf(PhotoCrop()) }
    val frameDescription = stringResource(Res.string.photo_crop_preview)
    val zoomDescription = stringResource(Res.string.photo_crop_zoom)

    MenuCard(title = stringResource(Res.string.photo_crop_title)) {
        Text(stringResource(Res.string.photo_crop_instruction))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                    .semantics { contentDescription = frameDescription }
                    .pointerInput(image, busy) {
                        if (!busy) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                val width = size.width.toFloat()
                                crop = crop.adjust(image.width, image.height, crop.zoom * zoomChange, pan.x / width, pan.y / width)
                            }
                        }
                    },
            ) {
                val bounds = crop.bounds(image.width, image.height)
                val scale = size.width / bounds.width
                withTransform({
                    translate(-bounds.left * scale, -bounds.top * scale)
                    scale(scale, scale, Offset.Zero)
                }) {
                    drawImage(image)
                }
            }
        }
        Slider(
            value = crop.zoom,
            onValueChange = { crop = crop.adjust(image.width, image.height, it) },
            valueRange = 1f..PhotoCrop.MaxZoom,
            enabled = !busy,
            modifier = Modifier.semantics { contentDescription = zoomDescription },
        )
        message?.let { Text(stringResource(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton(stringResource(Res.string.cancel), onCancel, Modifier.weight(1f), enabled = !busy)
            Button(onClick = { onConfirm(crop) }, modifier = Modifier.weight(1f), enabled = !busy) {
                Text(stringResource(Res.string.photo_crop_confirm))
            }
        }
    }
}
