/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.attendance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.ON_TIME_THRESHOLD_MINUTES
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme

/**
 * One bar per past session, oldest on the left, as tall as the attendant was
 * late. Bars under the dashed grace line were on time; a missed session is the
 * hollow full height bar, since there is no lateness to measure for it.
 */
@Composable
fun AttendanceHistoryChart(
    records: List<AttendanceRecord>,
    modifier: Modifier = Modifier,
    maxBars: Int = DEFAULT_BARS
) {
    if (records.isEmpty()) return

    val bars = records.takeLast(maxBars)
    val measurer = rememberTextMeasurer()
    val graceLabel = stringResource(R.string.grace_threshold_line, ON_TIME_THRESHOLD_MINUTES)
    val minutesShort = stringResource(R.string.minutes_short)
    val description = stringResource(R.string.attendance_history_chart_description)

    // Never scale to less than three grace periods, or a run of punctual weeks
    // would blow one stray minute up into a full height bar.
    val ceiling = maxOf(
        bars.maxOf { it.minutesLate },
        ON_TIME_THRESHOLD_MINUTES * MIN_CEILING_FACTOR
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = CHART_HEIGHT)
                .semantics { contentDescription = description }
        ) {
            val baseline = size.height - 1.dp.toPx()
            val plotHeight = baseline - TOP_INSET.toPx()
            val slot = size.width / bars.size
            val barWidth = minOf(slot * BAR_SLOT_RATIO, MAX_BAR_WIDTH.toPx())
            val corner = CornerRadius(x = 6.dp.toPx(), y = 6.dp.toPx())

            fun yOf(minutes: Long): Float =
                baseline - plotHeight * (minutes / ceiling.toFloat()).coerceIn(0f, 1f)

            bars.forEachIndexed { index, record ->
                val left = slot * index + (slot - barWidth) / 2f
                val absent = record.status == AttendanceStatus.ABSENT
                val top = if (absent) {
                    baseline - plotHeight
                } else {
                    minOf(yOf(record.minutesLate), baseline - MIN_BAR_HEIGHT.toPx())
                }
                val rect = Size(width = barWidth, height = baseline - top)

                if (absent) {
                    // Hollow, because an absence has no lateness to read off the axis.
                    drawRoundRect(
                        color = record.status.color.copy(alpha = 0.12f),
                        topLeft = Offset(x = left, y = top),
                        size = rect,
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = record.status.color,
                        topLeft = Offset(x = left, y = top),
                        size = rect,
                        cornerRadius = corner,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                            )
                        )
                    )
                } else {
                    drawRoundRect(
                        color = record.status.color,
                        topLeft = Offset(x = left, y = top),
                        size = rect,
                        cornerRadius = corner
                    )
                }
            }

            val graceY = yOf(ON_TIME_THRESHOLD_MINUTES)
            drawLine(
                color = Gray,
                start = Offset(x = 0f, y = graceY),
                end = Offset(x = size.width, y = graceY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8.dp.toPx(), 6.dp.toPx())
                )
            )

            val axisStyle = TextStyle(fontSize = 10.sp)
            val grace = measurer.measure(text = graceLabel, style = axisStyle)
            drawText(
                textLayoutResult = grace,
                color = Gray,
                topLeft = Offset(
                    x = size.width - grace.size.width,
                    y = graceY - grace.size.height - 2.dp.toPx()
                )
            )

            val ceilingLabel = measurer.measure("$ceiling $minutesShort", axisStyle)
            drawText(
                textLayoutResult = ceilingLabel,
                color = Gray,
                topLeft = Offset(
                    x = 0f,
                    y = baseline - plotHeight - ceilingLabel.size.height - 2.dp.toPx()
                )
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { record ->
                Text(
                    text = "${record.date.dayOfMonth}/${record.date.monthValue}",
                    color = Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(weight = 1f)
                )
            }
        }
    }
}

/** How many sessions fit before the labels under the bars stop being readable. */
const val DEFAULT_BARS = 10

private val CHART_HEIGHT = 150.dp

/** Room above the tallest bar for the scale label. */
private val TOP_INSET = 16.dp

private val MAX_BAR_WIDTH = 26.dp

/** So a check-in exactly on the hour still draws something. */
private val MIN_BAR_HEIGHT = 4.dp

private const val BAR_SLOT_RATIO = 0.55f

private const val MIN_CEILING_FACTOR = 3L

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceHistoryChartPreview() {
    QuickAttendTheme {
        AttendanceHistoryChart(
            records = previewRecords(),
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceHistoryChartPunctualPreview() {
    QuickAttendTheme {
        AttendanceHistoryChart(
            records = previewRecords().map { it.copy(checkInAt = it.scheduledStart) },
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}
