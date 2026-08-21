/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.ui.theme.Amber
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise

/**
 * Donut of the whole history with the score in the middle: each arc is the share
 * of sessions that ended on time, late or missed, so a low score always shows
 * which of the two is dragging it down.
 */
@Composable
fun AttendanceScoreRing(
    summary: AttendanceSummary,
    modifier: Modifier = Modifier,
    diameter: Dp = 150.dp
) {
    val empty = summary.total == 0
    val description = stringResource(R.string.attendance_score) + ": ${summary.score}"

    Box(
        modifier = modifier
            .size(size = diameter)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = RING_STROKE.toPx()
            val arcTopLeft = Offset(x = stroke / 2f, y = stroke / 2f)
            val arcSize = Size(
                width = size.width - stroke,
                height = size.height - stroke
            )

            drawArc(
                color = Ghost,
                startAngle = 0f,
                sweepAngle = FULL_TURN,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )

            if (empty) return@Canvas

            val segments = AttendanceStatus.entries
                .map { status -> status to summary.ratioOf(status) }
                .filter { (_, ratio) -> ratio > 0f }
            val gap = if (segments.size > 1) SEGMENT_GAP_DEGREES else 0f
            var angle = START_ANGLE

            segments.forEach { (status, ratio) ->
                val slice = ratio * FULL_TURN
                drawArc(
                    color = status.color,
                    startAngle = angle + gap / 2f,
                    sweepAngle = (slice - gap).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                angle += slice
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            Text(
                text = if (empty) NO_SCORE else summary.score.toString(),
                color = if (empty) Gray else summary.scoreColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.attendance_sessions, summary.total),
                color = Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

/** Green above [GOOD_SCORE], amber while it is recoverable, red below that. */
internal fun AttendanceSummary.scoreColor(): Color = when {
    score >= GOOD_SCORE -> Turquoise
    score >= FAIR_SCORE -> Amber
    else -> Red
}

private val RING_STROKE = 14.dp

private const val FULL_TURN = 360f

/** Twelve o'clock, so the first slice starts where a clock would. */
private const val START_ANGLE = -90f

private const val SEGMENT_GAP_DEGREES = 4f

private const val GOOD_SCORE = 85

private const val FAIR_SCORE = 60

private const val NO_SCORE = "--"

@Preview(showBackground = true, widthDp = 200, heightDp = 200)
@Composable
fun AttendanceScoreRingPreview() {
    QuickAttendTheme {
        AttendanceScoreRing(summary = AttendanceSummary(onTime = 12, late = 3, absent = 1))
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 200)
@Composable
fun AttendanceScoreRingPoorPreview() {
    QuickAttendTheme {
        AttendanceScoreRing(summary = AttendanceSummary(onTime = 2, late = 3, absent = 5))
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 200)
@Composable
fun AttendanceScoreRingEmptyPreview() {
    QuickAttendTheme {
        AttendanceScoreRing(summary = AttendanceSummary())
    }
}
