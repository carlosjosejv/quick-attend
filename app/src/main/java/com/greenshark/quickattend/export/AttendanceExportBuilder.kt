/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventAttendanceOverview
import com.greenshark.quickattend.data.ON_TIME_THRESHOLD_MINUTES
import com.greenshark.quickattend.data.daysSummary
import com.greenshark.quickattend.data.signatureFor
import com.greenshark.quickattend.data.timeSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns attendance into the two records a host can export: the roll of everyone
 * on the event, and one attendant's session by session sheet.
 *
 * Both are built from the same aggregates the screens read, so an exported file
 * can never disagree with what the host was looking at when they exported it.
 */
object AttendanceExportBuilder {

    /**
     * One row per attendant, summing up how they did across every session.
     * The signature is the one from their most recent attended session, which
     * is what a roll sheet is signed with.
     */
    fun general(
        event: Event,
        attendees: List<AttendeeAttendance>,
        overview: EventAttendanceOverview,
        generatedOn: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault()
    ): ExportDocument = ExportDocument(
        title = event.title.ifBlank { UNTITLED_EVENT },
        subtitle = "Attendance record · all attendants",
        metadata = event.metadata(generatedOn, locale) + listOf(
            "Attendants" to overview.attendees.toString(),
            "Sessions" to overview.sessions.toString(),
            "Average score" to overview.score.toString()
        ),
        columns = listOf(
            ExportColumn(title = "Attendant", weight = 2.4f),
            ExportColumn(title = "Email", weight = 2.6f),
            ExportColumn(title = "On time", weight = 0.9f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Late", weight = 0.8f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Absent", weight = 0.9f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Score", weight = 0.9f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Signature", weight = 2.2f, alignment = ColumnAlignment.CENTER)
        ),
        rows = attendees.map { attendee ->
            ExportRow(
                cells = listOf(
                    attendee.profile.displayName,
                    attendee.profile.email,
                    attendee.summary.onTime.toString(),
                    attendee.summary.late.toString(),
                    attendee.summary.absent.toString(),
                    attendee.summary.score.toString(),
                    "" // Filled from the signature by whichever writer runs.
                ),
                signature = attendee.rollSignature()
            )
        },
        notes = scoringNotes(),
        fileName = "${event.title.toFileNameSlug()}-attendance-$generatedOn"
    )

    /** One row per session for a single attendant, each signed on its own. */
    fun forAttendee(
        event: Event,
        attendee: AttendeeAttendance,
        generatedOn: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault()
    ): ExportDocument = ExportDocument(
        title = event.title.ifBlank { UNTITLED_EVENT },
        subtitle = "Attendance record · ${attendee.profile.displayName}",
        metadata = event.metadata(generatedOn, locale) + listOfNotNull(
            "Attendant" to attendee.profile.displayName,
            ("Email" to attendee.profile.email).takeIf { attendee.profile.email.isNotBlank() },
            "Sessions" to attendee.records.size.toString(),
            "Score" to attendee.summary.score.toString()
        ),
        columns = listOf(
            ExportColumn(title = "Date", weight = 1.6f),
            ExportColumn(title = "Scheduled", weight = 1.1f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Checked in", weight = 1.2f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Status", weight = 1.2f, alignment = ColumnAlignment.CENTER),
            ExportColumn(title = "Late by", weight = 1f, alignment = ColumnAlignment.END),
            ExportColumn(title = "Signature", weight = 2.4f, alignment = ColumnAlignment.CENTER)
        ),
        rows = attendee.recentFirst.map { record ->
            ExportRow(
                cells = listOf(
                    record.date.format(dateFormatter(locale)),
                    record.scheduledStartLabel,
                    record.checkInLabel ?: EMPTY_TIME,
                    record.status.label(),
                    record.lateLabel(),
                    ""
                ),
                signature = signatureFor(record = record, profile = attendee.profile)
            )
        },
        notes = scoringNotes(),
        fileName = "${event.title.toFileNameSlug()}-" +
                "${attendee.profile.displayName.toFileNameSlug("attendant")}-$generatedOn"
    )

    private fun Event.metadata(
        generatedOn: LocalDate,
        locale: Locale
    ): List<Pair<String, String>> = listOfNotNull(
        ("Host" to hostName).takeIf { hostName.isNotBlank() },
        ("Location" to location).takeIf { location.isNotBlank() },
        "Schedule" to "${schedule.daysSummary(locale)} · ${schedule.timeSummary()}",
        "Generated" to generatedOn.format(dateFormatter(locale))
    )

    private fun scoringNotes(): List<String> = listOf(
        "On time counts full, late counts half, an absence counts zero.",
        "Arriving within $ON_TIME_THRESHOLD_MINUTES min of the start still counts as on time.",
        "Attendants without a drawn signature sign with a unique code derived " +
                "from their account and the session they attended."
    )

    private const val UNTITLED_EVENT = "Untitled event"

    private const val EMPTY_TIME = "--:--"
}

/**
 * What an attendant signs a roll sheet with: their most recent attended session.
 * Someone who never attended has nothing to sign.
 */
private fun AttendeeAttendance.rollSignature(): AttendanceSignature =
    lastAttended?.let { signatureFor(record = it, profile = profile) }
        ?: AttendanceSignature.None

private fun AttendanceStatus.label(): String = when (this) {
    AttendanceStatus.ON_TIME -> "On time"
    AttendanceStatus.LATE -> "Late"
    AttendanceStatus.ABSENT -> "Absent"
}

private fun AttendanceRecord.lateLabel(): String = when {
    status == AttendanceStatus.ABSENT -> "--"
    minutesLate <= 0L -> "--"
    else -> "$minutesLate min"
}

private fun dateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(EXPORT_DATE_PATTERN, locale)

private const val EXPORT_DATE_PATTERN = "EEE, d MMM yyyy"
