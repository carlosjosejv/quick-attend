/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.signature

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/** One continuous stroke: the points a finger passed through without lifting. */
private typealias PenStroke = SnapshotStateList<Offset>

/**
 * Pad an attendant signs on with their finger.
 *
 * The strokes are kept as points rather than as a bitmap, so the drawing stays
 * crisp at any size and is rasterised once, at export resolution, when the user
 * commits. [onSigned] never fires for an empty pad.
 */
@Composable
fun SignaturePad(
    onSigned: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    onCleared: (() -> Unit)? = null
) {
    val strokes = remember { mutableListOf<PenStroke>().toMutableStateList() }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    val hasDrawing = strokes.any { it.size > 1 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = height)
                .clip(shape = RoundedCornerShape(size = 16.dp))
                .background(color = Ghost)
                .border(
                    width = 1.dp,
                    color = if (hasDrawing) Turquoise else Gray.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(size = 16.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { padSize = it }
                    .pointerInput(Unit) {
                        // A drag is single-pointer, so the stroke being drawn is
                        // always the one most recently started.
                        detectDragGestures(
                            onDragStart = { offset ->
                                strokes.add(mutableListOf(offset).toMutableStateList())
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                strokes.lastOrNull()?.add(change.position)
                            }
                        )
                    }
            ) {
                strokes.forEach { stroke ->
                    drawPath(
                        path = stroke.toComposePath(),
                        color = Black,
                        style = Stroke(
                            width = STROKE_WIDTH_PX,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (!hasDrawing) {
                Text(
                    text = stringResource(R.string.signature_pad_hint),
                    color = Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.align(alignment = Alignment.Center)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    strokes.clear()
                    onCleared?.invoke()
                },
                enabled = hasDrawing
            ) {
                Text(text = stringResource(R.string.signature_clear), color = Gray)
            }
            TextButton(
                onClick = {
                    strokes.rasterize(width = padSize.width, height = padSize.height)
                        ?.let(onSigned)
                },
                enabled = hasDrawing
            ) {
                Text(
                    text = stringResource(R.string.signature_save),
                    color = Turquoise,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun List<Offset>.toComposePath(): Path = Path().apply {
    val first = firstOrNull() ?: return@apply
    moveTo(first.x, first.y)
    // Straight segments between sampled points; the pointer samples often
    // enough that fitting a curve would not read any smoother.
    drop(n = 1).forEach { point -> lineTo(point.x, point.y) }
}

/**
 * Draws the strokes onto a white page at export resolution. Null when nothing
 * was drawn or the pad was never laid out.
 */
private fun List<List<Offset>>.rasterize(width: Int, height: Int): Bitmap? {
    if (width <= 0 || height <= 0 || none { it.size > 1 }) return null

    val scale = (EXPORT_WIDTH_PX.toFloat() / width).coerceAtMost(MAX_SCALE)
    val bitmap = Bitmap.createBitmap(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        strokeWidth = STROKE_WIDTH_PX * scale
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    forEach { stroke ->
        if (stroke.size < 2) return@forEach
        val path = AndroidPath().apply {
            moveTo(stroke.first().x * scale, stroke.first().y * scale)
            stroke.drop(n = 1).forEach { point -> lineTo(point.x * scale, point.y * scale) }
        }
        canvas.drawPath(path, paint)
    }
    return bitmap
}

private const val STROKE_WIDTH_PX = 6f

/** Wide enough to stay sharp in a printed PDF without bloating the profile. */
private const val EXPORT_WIDTH_PX = 900

private const val MAX_SCALE = 3f

@Preview(showBackground = true, widthDp = 380)
@Composable
fun SignaturePadPreview() {
    QuickAttendTheme {
        Box(modifier = Modifier.background(color = White)) {
            SignaturePad(onSigned = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
