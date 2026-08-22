/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.ApprovalState
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.RecordedAttendance
import com.greenshark.quickattend.data.formatDistance
import com.greenshark.quickattend.ui.component.attendance.AttendanceStatusChip
import com.greenshark.quickattend.ui.theme.Amber
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * What the attendant sees the moment their attendance lands: which event and
 * session it was filed under, the exact minute recorded, whether that counts as
 * on time, and — when they were not on the roll — that the host now has to say
 * yes before it counts for anything.
 *
 * The verdict is read off [RecordedAttendance.status], which is the same
 * calculation the history screen and every export use, so nobody is ever told
 * "on time" here and shown "late" later.
 */
@Composable
fun AttendanceConfirmationCard(
    recorded: RecordedAttendance,
    onDone: () -> Unit,
    onScanAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val sessionLabel = remember(recorded.date, recorded.scheduledStart, locale) {
        val date = recorded.date.format(DateTimeFormatter.ofPattern(DATE_PATTERN, locale))
        "$date · ${recorded.record.scheduledStartLabel}"
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White),
        shape = RoundedCornerShape(size = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(all = 20.dp),
            verticalArrangement = Arrangement.spacedBy(space = 14.dp)
        ) {
            ConfirmationHeader(recorded = recorded)

            Text(
                text = recorded.event.title,
                color = Black,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            AttendanceStatusChip(status = recorded.status)

            HorizontalDivider(color = Ghost)

            ScanDetailRow(
                label = stringResource(R.string.scan_session),
                value = sessionLabel,
                icon = R.drawable.calendar_today
            )
            ScanDetailRow(
                label = stringResource(R.string.scan_checked_in),
                value = recorded.record.checkInLabel.orEmpty(),
                icon = R.drawable.schedule
            )
            recorded.event.hostLine()?.let { host ->
                ScanDetailRow(
                    label = stringResource(R.string.scan_event_host),
                    value = host,
                    icon = R.drawable.group
                )
            }
            recorded.event.locationLine()?.let { place ->
                ScanDetailRow(
                    label = stringResource(R.string.location),
                    value = place,
                    icon = R.drawable.location_on
                )
            }
            recorded.distanceMeters?.let { meters ->
                ScanDetailRow(
                    label = stringResource(R.string.scan_location_verified),
                    value = stringResource(
                        R.string.scan_distance_from_venue,
                        formatDistance(meters)
                    ),
                    icon = R.drawable.check_circle
                )
            }

            if (recorded.alreadyRecorded) {
                ScanNotice(
                    text = stringResource(
                        R.string.scan_already_recorded_description,
                        recorded.record.checkInLabel.orEmpty()
                    ),
                    icon = R.drawable.timer,
                    tint = Gray
                )
            }

            recorded.approvalNotice()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                OutlinedButton(
                    onClick = onScanAnother,
                    modifier = Modifier.weight(weight = 1f),
                    shape = RoundedCornerShape(size = 14.dp)
                ) {
                    Text(text = stringResource(R.string.scan_again), color = Black)
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(weight = 1f),
                    shape = RoundedCornerShape(size = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise)
                ) {
                    Text(
                        text = stringResource(R.string.action_done),
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Icon and headline, which say at a glance whether this is settled or not. */
@Composable
private fun ConfirmationHeader(recorded: RecordedAttendance) {
    val settled = recorded.approval == ApprovalState.ENROLLED
    val tint = when {
        recorded.approval == ApprovalState.REJECTED -> Red
        !settled -> Amber
        recorded.status == AttendanceStatus.LATE -> Amber
        else -> Turquoise
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Box(
            modifier = Modifier.background(
                color = tint.copy(alpha = 0.15f),
                shape = CircleShape
            ).padding(all = 8.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (settled) R.drawable.check_circle else R.drawable.pending
                ),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size = 28.dp)
            )
        }
        Text(
            text = stringResource(
                when {
                    !settled -> R.string.scan_pending_title
                    recorded.alreadyRecorded -> R.string.scan_already_recorded_title
                    else -> R.string.scan_recorded_title
                }
            ),
            color = Black,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

/** The line that explains why an off-roll check-in is not counting yet. */
@Composable
private fun RecordedAttendance.approvalNotice() {
    when (approval) {
        ApprovalState.ENROLLED -> Unit

        ApprovalState.PENDING_APPROVAL -> ScanNotice(
            text = stringResource(
                R.string.scan_pending_description,
                event.hostName.ifBlank {
                    event.organizerName.ifBlank { stringResource(R.string.scan_the_host) }
                }
            ),
            icon = R.drawable.pending,
            tint = Amber
        )

        ApprovalState.REJECTED -> ScanNotice(
            text = stringResource(R.string.scan_rejected_description),
            icon = R.drawable.error,
            tint = Red
        )
    }
}

/** Tinted strip for anything the attendant has to read rather than glance at. */
@Composable
private fun ScanNotice(
    text: String,
    icon: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = tint.copy(alpha = 0.12f), shape = RoundedCornerShape(size = 14.dp))
            .padding(all = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size = 20.dp)
        )
        Text(text = text, color = Black, fontSize = 14.sp)
    }
}

/** "Carlos Jiménez", falling back to the organizer, and null when neither is set. */
private fun Event.hostLine(): String? =
    hostName.ifBlank { organizerName }.takeIf { it.isNotBlank() }

/** "Salon C201 · Av. Máximo Gómez 72", with whichever half exists. */
private fun Event.locationLine(): String? =
    listOf(location, address)
        .filter { it.isNotBlank() }
        .joinToString(separator = " · ")
        .takeIf { it.isNotBlank() }

private const val DATE_PATTERN = "EEEE, d MMM"

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendanceConfirmationCardPreview() {
    QuickAttendTheme {
        AttendanceConfirmationCard(
            recorded = previewRecordedAttendance(),
            onDone = {},
            onScanAnother = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 760)
@Composable
fun AttendanceConfirmationCardPendingPreview() {
    QuickAttendTheme {
        AttendanceConfirmationCard(
            recorded = previewRecordedAttendance(
                approval = ApprovalState.PENDING_APPROVAL,
                minutesLate = 9,
                distanceMeters = 18.0
            ),
            onDone = {},
            onScanAnother = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 760)
@Composable
fun AttendanceConfirmationCardRejectedPreview() {
    QuickAttendTheme {
        AttendanceConfirmationCard(
            recorded = previewRecordedAttendance(
                approval = ApprovalState.REJECTED,
                alreadyRecorded = true
            ),
            onDone = {},
            onScanAnother = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

internal fun previewRecordedAttendance(
    approval: ApprovalState = ApprovalState.ENROLLED,
    minutesLate: Long = 2,
    alreadyRecorded: Boolean = false,
    distanceMeters: Double? = null
): RecordedAttendance {
    val date = LocalDate.of(2026, 9, 22)
    val start = LocalDateTime.of(date, LocalTime.of(13, 0))
    return RecordedAttendance(
        event = Event(
            id = "SBcH2mA1xQpLd0Rt",
            title = "Clase de Yoga",
            hostName = "Carlos Jiménez",
            location = "Salon C201",
            address = "Av. Máximo Gómez 72"
        ),
        date = date,
        scheduledStart = start,
        checkInAt = start.plusMinutes(minutesLate),
        alreadyRecorded = alreadyRecorded,
        approval = approval,
        distanceMeters = distanceMeters
    )
}
