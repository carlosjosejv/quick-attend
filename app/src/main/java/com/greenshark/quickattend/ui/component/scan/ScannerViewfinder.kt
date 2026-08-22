/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.scan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * The dimmed frame drawn over the camera: everything outside the square is
 * shaded, the square itself is left clear with brackets on its corners, and a
 * line sweeps down it while a scan is live.
 *
 * Drawn rather than composed out of boxes, because the clear square is a hole
 * punched through one scrim — four separate shaded panels would show seams
 * wherever their edges met.
 */
@Composable
fun ScannerViewfinder(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    windowFraction: Float = DEFAULT_WINDOW_FRACTION,
    accent: Color = Turquoise
) {
    val sweep = rememberSweepFraction(isScanning = isScanning)

    Canvas(modifier = modifier.fillMaxSize()) {
        val window = windowRect(windowFraction = windowFraction)
        val radius = CORNER_RADIUS.toPx()

        drawScrim(window = window, radius = radius)
        drawCornerBrackets(window = window, radius = radius, color = accent)
        if (sweep != null) drawSweepLine(window = window, fraction = sweep, color = accent)
    }
}

/**
 * Where the sweep line sits, 0f at the top of the window and 1f at the bottom.
 * Null when nothing is being scanned, which is what stops the animation.
 */
@Composable
private fun rememberSweepFraction(isScanning: Boolean): Float? {
    // An endless animation never lets a preview or a screenshot test settle.
    if (!isScanning || LocalInspectionMode.current) return null

    val transition = rememberInfiniteTransition(label = "viewfinder-sweep")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_DURATION_MILLIS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "viewfinder-sweep-fraction"
    )
    return fraction
}

/** The clear square, centred and inset from the shorter edge of the screen. */
private fun DrawScope.windowRect(windowFraction: Float): Rect {
    val side = minOf(size.width, size.height) * windowFraction
    return Rect(
        offset = Offset(
            x = (size.width - side) / 2f,
            // Sits above centre, so the card below it covers nothing.
            y = (size.height - side) / 2f - size.height * WINDOW_RISE_FRACTION
        ),
        size = Size(width = side, height = side)
    )
}

/** One shade with the window cut out of it, so no two edges can show a seam. */
private fun DrawScope.drawScrim(window: Rect, radius: Float) {
    val scrim = Path().apply { addRect(Rect(offset = Offset.Zero, size = size)) }
    val hole = Path().apply {
        addRoundRect(
            RoundRect(rect = window, cornerRadius = CornerRadius(radius, radius))
        )
    }
    drawPath(
        path = Path.combine(operation = PathOperation.Difference, path1 = scrim, path2 = hole),
        color = Black.copy(alpha = SCRIM_ALPHA)
    )
}

/**
 * Four short elbows, one per corner, rather than a full outline: the eye reads
 * the square from its corners alone, and less ink means less of the code hidden.
 */
private fun DrawScope.drawCornerBrackets(window: Rect, radius: Float, color: Color) {
    val length = window.width * BRACKET_LENGTH_FRACTION
    val width = BRACKET_WIDTH.toPx()

    // Each corner with the direction its two arms run in.
    val corners = listOf(
        Corner(point = window.topLeft, xStep = 1f, yStep = 1f),
        Corner(point = window.topRight, xStep = -1f, yStep = 1f),
        Corner(point = window.bottomLeft, xStep = 1f, yStep = -1f),
        Corner(point = window.bottomRight, xStep = -1f, yStep = -1f)
    )

    corners.forEach { corner ->
        // Both arms start past the rounded corner, so neither cuts across it.
        drawLine(
            color = color,
            start = corner.point.copy(x = corner.point.x + corner.xStep * radius),
            end = corner.point.copy(x = corner.point.x + corner.xStep * (radius + length)),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = corner.point.copy(y = corner.point.y + corner.yStep * radius),
            end = corner.point.copy(y = corner.point.y + corner.yStep * (radius + length)),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}

/** The line that travels the window, so a live camera never looks frozen. */
private fun DrawScope.drawSweepLine(window: Rect, fraction: Float, color: Color) {
    val y = window.top + window.height * fraction
    drawLine(
        // Faded at both ends, so it reads as a sweep and not as a border.
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, color, Color.Transparent),
            startX = window.left,
            endX = window.right
        ),
        start = Offset(x = window.left, y = y),
        end = Offset(x = window.right, y = y),
        strokeWidth = SWEEP_WIDTH.toPx()
    )
}

private data class Corner(val point: Offset, val xStep: Float, val yStep: Float)

/** Share of the shorter screen edge the clear square takes up. */
const val DEFAULT_WINDOW_FRACTION = 0.68f

private const val WINDOW_RISE_FRACTION = 0.08f
private const val SCRIM_ALPHA = 0.62f
private const val BRACKET_LENGTH_FRACTION = 0.22f
private const val SWEEP_DURATION_MILLIS = 2_200
private val CORNER_RADIUS: Dp = 24.dp
private val BRACKET_WIDTH: Dp = 4.dp
private val SWEEP_WIDTH: Dp = 2.dp

@Preview(showBackground = true, backgroundColor = 0xFF3A3A3A, heightDp = 640)
@Composable
fun ScannerViewfinderPreview() {
    QuickAttendTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ScannerViewfinder(accent = Turquoise)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF3A3A3A, heightDp = 640)
@Composable
fun ScannerViewfinderPausedPreview() {
    QuickAttendTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ScannerViewfinder(isScanning = false, accent = White)
        }
    }
}
