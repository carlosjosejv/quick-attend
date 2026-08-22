/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The generated code stands in for a handwritten signature on an exported
 * record, so the two things that matter are that the same check-in always
 * produces the same code and that a different one never does.
 */
class AttendanceSignatureTest {

    private val date = LocalDate.of(2026, 8, 24)

    private val scheduledStart = date.atTime(13, 0)

    @Test
    fun `same check-in always signs with the same code`() {
        val first = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 46_920)
        val second = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 46_920)

        assertEquals(first, second)
    }

    @Test
    fun `a different attendant signs differently`() {
        val mine = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 46_920)
        val theirs = signatureCodeFor("user-2", "event-1", date, checkInSecondOfDay = 46_920)

        assertNotEquals(mine, theirs)
    }

    @Test
    fun `a different session signs differently`() {
        val today = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 46_920)
        val nextWeek = signatureCodeFor(
            userId = "user-1",
            eventId = "event-1",
            date = date.plusWeeks(1),
            checkInSecondOfDay = 46_920
        )

        assertNotEquals(today, nextWeek)
    }

    @Test
    fun `a corrected check-in time produces a visibly different code`() {
        val original = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 46_920)
        val corrected = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 47_040)

        assertNotEquals(original, corrected)
    }

    @Test
    fun `the code reads as grouped hex a person can copy off a page`() {
        val code = signatureCodeFor("user-1", "event-1", date, checkInSecondOfDay = 46_920)

        assertTrue(code, Regex("""QA-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}""").matches(code))
    }

    @Test
    fun `an attendant with a drawing signs with it and keeps the code`() {
        val signature = signatureFor(
            record = record(checkInAt = scheduledStart.plusMinutes(2)),
            profile = UserProfile(id = "user-1", name = "Ana", signatureImage = "base64png")
        )

        assertTrue(signature is AttendanceSignature.Drawn)
        assertEquals("base64png", (signature as AttendanceSignature.Drawn).imageBase64)
        assertTrue(signature.displayCode?.startsWith("QA-") == true)
    }

    @Test
    fun `an attendant without a drawing signs with a code`() {
        val signature = signatureFor(
            record = record(checkInAt = scheduledStart.plusMinutes(2)),
            profile = UserProfile(id = "user-1", name = "Ana")
        )

        assertTrue(signature is AttendanceSignature.Code)
    }

    @Test
    fun `an absence signs with nothing at all`() {
        val signature = signatureFor(
            record = record(checkInAt = null),
            profile = UserProfile(id = "user-1", name = "Ana", signatureImage = "base64png")
        )

        assertEquals(AttendanceSignature.None, signature)
        assertEquals(null, signature.displayCode)
    }

    private fun record(checkInAt: LocalDateTime?) = AttendanceRecord(
        eventId = "event-1",
        eventTitle = "Clase de Yoga",
        date = date,
        scheduledStart = scheduledStart,
        checkInAt = checkInAt
    )
}
