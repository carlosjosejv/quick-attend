/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.ON_TIME_THRESHOLD_MINUTES
import com.greenshark.quickattend.ui.theme.Amber
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Where a check-in landed against the hour the session was meant to start: the
 * grace window is the turquoise band, everything past it is amber, and the dot
 * is the moment attendance was actually recorded. An absence draws the same
 * band dimmed with no dot on it, because nothing was ever scanned.
 */
@Composable
fun AttendanceTimeline(
    record: AttendanceRecord,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val status = record.status
    val absent = status == AttendanceStatus.ABSENT
    val statusLabel = stringResource(status.labelRes)
    val description = record.checkInLabel
        ?.let { time -> "$statusLabel, ${stringResource(R.string.checked_in_at)} $time" }
        ?: stringResource(R.string.not_checked_in)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            TimelineLegend(
                color = Turquoise,
                text = stringResource(R.string.on_time_until, record.onTimeUntilLabel)
            )
            TimelineLegend(
                color = Amber,
                text = stringResource(R.string.attendance_status_late)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = TIMELINE_HEIGHT)
                .semantics { contentDescription = description }
        ) {
            val spanStart = -EARLY_SPAN_MINUTES
            val spanEnd = maxOf(LATE_SPAN_MINUTES, record.minutesLate + EARLY_SPAN_MINUTES)
            val trackTop = 4.dp.toPx()
            val trackHeight = 22.dp.toPx()
            val alpha = if (absent) 0.4f else 1f

            fun xOf(minute: Long): Float =
                size.width * ((minute - spanStart) / (spanEnd - spanStart).toFloat())

            val track = Rect(
                offset = Offset(x = 0f, y = trackTop),
                size = Size(width = size.width, height = trackHeight)
            )
            val trackPath = Path().apply {
                addRoundRect(RoundRect(track, CornerRadius(trackHeight / 2f)))
            }

            clipPath(trackPath) {
                drawRect(color = Ghost, topLeft = track.topLeft, size = track.size)
                drawBand(
                    track = track,
                    from = xOf(0L),
                    to = xOf(ON_TIME_THRESHOLD_MINUTES),
                    color = Turquoise.copy(alpha = 0.35f * alpha)
                )
                drawBand(
                    track = track,
                    from = xOf(ON_TIME_THRESHOLD_MINUTES),
                    to = size.width,
                    color = Amber.copy(alpha = 0.25f * alpha)
                )
            }

            // The scheduled start, the only fixed reference on the track.
            val startX = xOf(0L)
            drawLine(
                color = Black.copy(alpha = 0.55f * alpha),
                start = Offset(x = startX, y = trackTop),
                end = Offset(x = startX, y = trackTop + trackHeight),
                strokeWidth = 2.dp.toPx()
            )

            val labelTop = trackTop + trackHeight + 6.dp.toPx()
            val startLabel = measurer.measure(
                text = record.scheduledStartLabel,
                style = TextStyle(fontSize = 11.sp)
            )
            val checkInLabel = record.checkInLabel?.let {
                measurer.measure(
                    text = it,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }

            val markerX = record.minutesFromStart
                ?.let { xOf(it).coerceIn(0f, size.width) }

            if (markerX != null) {
                drawCircle(
                    color = White,
                    radius = 9.dp.toPx(),
                    center = Offset(x = markerX, y = trackTop + trackHeight / 2f)
                )
                drawCircle(
                    color = status.color,
                    radius = 7.dp.toPx(),
                    center = Offset(x = markerX, y = trackTop + trackHeight / 2f)
                )
            }

            // Both labels sit under the track, so the start one is dropped when
            // the check-in is close enough to it that they would overlap.
            val checkInLeft = markerX?.let { x ->
                val width = checkInLabel?.size?.width ?: 0
                (x - width / 2f).coerceIn(0f, size.width - width)
            }
            val startLeft = (startX - startLabel.size.width / 2f)
                .coerceIn(0f, size.width - startLabel.size.width)
            val collides = checkInLeft != null &&
                    checkInLeft < startLeft + startLabel.size.width + 4.dp.toPx()

            if (!collides) {
                drawText(startLabel, color = Gray, topLeft = Offset(startLeft, labelTop))
            }
            if (checkInLabel != null && checkInLeft != null) {
                drawText(checkInLabel, color = status.color, topLeft = Offset(checkInLeft, labelTop))
            }
        }
    }
}

/** Fills the part of the track between two x positions with a flat colour. */
private fun DrawScope.drawBand(track: Rect, from: Float, to: Float, color: Color) {
    val left = from.coerceIn(0f, size.width)
    val right = to.coerceIn(0f, size.width)
    if (right <= left) return
    drawRect(
        color = color,
        topLeft = Offset(x = left, y = track.top),
        size = Size(width = right - left, height = track.height)
    )
}

@Composable
private fun TimelineLegend(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size = 8.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(text = text, color = Gray, fontSize = 12.sp, maxLines = 1)
    }
}

private val TIMELINE_HEIGHT = 52.dp

/** Minutes of track drawn before the scheduled start, so the tick is never flush left. */
private const val EARLY_SPAN_MINUTES = 10L

/** Minutes drawn after the start when the check-in was punctual. */
private const val LATE_SPAN_MINUTES = 25L

@Preview(showBackground = true, widthDp = 360)
@Composable
fun AttendanceTimelineOnTimePreview() {
    QuickAttendTheme {
        AttendanceTimeline(record = previewRecord(minutesFromStart = 3L))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun AttendanceTimelineLatePreview() {
    QuickAttendTheme {
        AttendanceTimeline(record = previewRecord(minutesFromStart = 18L))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun AttendanceTimelineAbsentPreview() {
    QuickAttendTheme {
        AttendanceTimeline(record = previewRecord(minutesFromStart = null))
    }
}
