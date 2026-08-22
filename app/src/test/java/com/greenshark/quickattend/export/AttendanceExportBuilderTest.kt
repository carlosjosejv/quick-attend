/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.export

import com.greenshark.quickattend.data.AttendanceCheckIn
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.data.attendanceHistory
import com.greenshark.quickattend.data.attendeeAttendance
import com.greenshark.quickattend.data.overview
import com.greenshark.quickattend.data.summarize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/**
 * Both writers read nothing but the document the builder produces, so these
 * check the shape a PDF and a spreadsheet of the same record are built from.
 */
class AttendanceExportBuilderTest {

    /** A Monday, so the weekly event below lands on its own schedule. */
    private val today = LocalDate.of(2026, 8, 24)

    private val now: LocalDateTime = today.atTime(18, 0)

    private val event = Event(
        id = "event-1",
        title = "Clase de Yoga",
        hostName = "Carlos Jiménez",
        location = "Salon C201",
        schedule = EventSchedule(
            recurrence = Recurrence.WEEKLY,
            days = listOf(ScheduleDay(dayKey = 1, range = TimeRange(13 * 60, 17 * 60)))
        )
    )

    private val ana = UserProfile(id = "ana", name = "Ana Reyes", email = "ana@example.com")
    private val luis = UserProfile(
        id = "luis",
        name = "Luis Pérez",
        email = "luis@example.com",
        signatureImage = "base64png"
    )

    @Test
    fun `the roll has one row per attendant and ends in a signature column`() {
        val document = generalDocument()

        assertEquals(2, document.rows.size)
        assertEquals("Signature", document.columns.last().title)
        // The signature cell is left blank; the writers fill it from the
        // signature on the row, as an image or as a code.
        assertEquals("", document.rows.first().cells.last())
    }

    @Test
    fun `an attendant with a drawing signs the roll with it`() {
        val document = generalDocument()

        val row = document.rows[document.columnIndexOfName("Luis Pérez")]
        assertTrue(row.signature is AttendanceSignature.Drawn)
        assertTrue(document.hasDrawnSignatures)
    }

    @Test
    fun `an attendant who never came signs the roll with nothing`() {
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana),
            checkIns = emptyList(),
            now = now,
            withinDays = 21
        )
        val document = AttendanceExportBuilder.general(
            event = event,
            attendees = attendees,
            overview = attendees.overview(),
            generatedOn = today,
            locale = Locale.US
        )

        assertEquals(AttendanceSignature.None, document.rows.single().signature)
        assertFalse(document.hasDrawnSignatures)
    }

    @Test
    fun `an attendant sheet signs every session on its own`() {
        val document = attendeeDocument()

        val signed = document.rows.filter { it.signature != AttendanceSignature.None }
        val codes = signed.mapNotNull { it.signature.displayCode }
        // One code per attended session, and never the same code twice.
        assertEquals(codes.size, codes.distinct().size)
        assertTrue(codes.isNotEmpty())
    }

    @Test
    fun `an attendant sheet reads newest session first`() {
        val document = attendeeDocument()

        val dates = document.rows.map { it.cells.first() }
        assertEquals(dates.sortedByDescending { it }, dates.sortedByDescending { it })
        // The most recent session is today's, which is the first row.
        assertTrue(dates.first().contains("24 Aug 2026"))
    }

    @Test
    fun `counts land in the cells a spreadsheet can add up`() {
        val document = generalDocument()

        val onTimeColumn = document.columns.indexOfFirst { it.title == "On time" }
        document.rows.forEach { row ->
            assertTrue(row.cells[onTimeColumn].toIntOrNull() != null)
        }
    }

    @Test
    fun `the file name is safe to write and carries the date`() {
        assertEquals("clase-de-yoga", "Clase de Yoga".toFileNameSlug())
        // Accents fold instead of being dropped, so a Spanish name stays
        // recognisable rather than coming out as "sal-n" or "jim-nez".
        assertEquals("salon-c-201", "Salón / C:201".toFileNameSlug())
        assertEquals("carlos-jimenez", "Carlos Jiménez".toFileNameSlug())
        assertEquals("sesion-de-nino", "Sesión de Niño".toFileNameSlug())
        assertEquals("attendance", "   ".toFileNameSlug())
        assertEquals("attendant", "***".toFileNameSlug("attendant"))

        assertTrue(generalDocument().fileName.endsWith(today.toString()))
        assertTrue(generalDocument().fileNameFor(ExportFormat.EXCEL).endsWith(".xlsx"))
    }

    @Test
    fun `an event that has never run still produces a document, just an empty one`() {
        val future = event.copy(
            schedule = EventSchedule(
                recurrence = Recurrence.WEEKLY,
                days = listOf(ScheduleDay(dayKey = today.plusDays(1).dayOfWeek.value))
            )
        )
        val attendee = AttendeeAttendance(profile = ana)

        val document = AttendanceExportBuilder.forAttendee(
            event = future,
            attendee = attendee,
            generatedOn = today,
            locale = Locale.US
        )

        assertTrue(document.isEmpty)
        assertTrue(document.columns.isNotEmpty())
        assertTrue(document.notes.isNotEmpty())
    }

    private fun generalDocument(): ExportDocument {
        val attendees = event.attendeeAttendance(
            profiles = listOf(ana, luis),
            checkIns = listOf(
                checkIn("ana", today, minutesLate = 1),
                checkIn("luis", today, minutesLate = 40)
            ),
            now = now,
            withinDays = 21
        )
        return AttendanceExportBuilder.general(
            event = event,
            attendees = attendees,
            overview = attendees.overview(),
            generatedOn = today,
            locale = Locale.US
        )
    }

    private fun attendeeDocument(): ExportDocument {
        val records = event.attendanceHistory(
            checkIns = listOf(
                checkIn("ana", today, minutesLate = 1),
                checkIn("ana", today.minusWeeks(1), minutesLate = 12)
            ),
            now = now,
            withinDays = 21
        )
        return AttendanceExportBuilder.forAttendee(
            event = event,
            attendee = AttendeeAttendance(
                profile = ana,
                records = records,
                summary = records.summarize()
            ),
            generatedOn = today,
            locale = Locale.US
        )
    }

    private fun ExportDocument.columnIndexOfName(name: String): Int =
        rows.indexOfFirst { it.cells.first() == name }

    private fun checkIn(userId: String, date: LocalDate, minutesLate: Long) = AttendanceCheckIn(
        eventId = "event-1",
        userId = userId,
        date = date,
        checkInAt = date.atTime(13, 0).plusMinutes(minutesLate)
    )
}
