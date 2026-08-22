package com.greenshark.quickattend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinRequestTest {

    @Test
    fun `the id is the same whichever way round it is built`() {
        assertEquals("u1_e1", joinRequestId(userId = "u1", eventId = "e1"))
        assertEquals(
            joinRequestId(userId = "u1", eventId = "e1"),
            request(userId = "u1", eventId = "e1").id
        )
    }

    @Test
    fun `only pending requests are waiting on the host`() {
        val requests = listOf(
            request(userId = "u1", status = JoinRequestStatus.PENDING),
            request(userId = "u2", status = JoinRequestStatus.APPROVED),
            request(userId = "u3", status = JoinRequestStatus.REJECTED)
        )
        assertEquals(listOf("u1"), requests.pending().map { it.userId })
        assertTrue(requests.first().isPending)
        assertFalse(requests.last().isPending)
    }

    @Test
    fun `counts are grouped by event and ignore answered requests`() {
        val requests = listOf(
            request(userId = "u1", eventId = "yoga"),
            request(userId = "u2", eventId = "yoga"),
            request(userId = "u3", eventId = "design"),
            request(userId = "u4", eventId = "design", status = JoinRequestStatus.APPROVED)
        )
        assertEquals(mapOf("yoga" to 2, "design" to 1), requests.pendingCountByEvent())
    }

    @Test
    fun `a nameless request falls back to the email and then to a placeholder`() {
        assertEquals(
            "ana",
            request(userId = "u1", name = "", email = "ana@mail.com").displayName
        )
        assertEquals("Attendant", request(userId = "u1", name = "", email = "").displayName)
    }

    @Test
    fun `epoch times are only turned into dates when they were actually set`() {
        assertEquals(null, request(userId = "u1").requestedAtDateTime)
        assertEquals(
            null,
            request(userId = "u1", requestedAt = 1_755_000_000_000L).firstCheckInDateTime
        )
    }

    private fun request(
        userId: String,
        eventId: String = "e1",
        name: String = "Ana Reyes",
        email: String = "ana@mail.com",
        status: JoinRequestStatus = JoinRequestStatus.PENDING,
        requestedAt: Long = 0L
    ) = JoinRequest(
        id = joinRequestId(userId = userId, eventId = eventId),
        eventId = eventId,
        eventTitle = "Clase de Yoga",
        userId = userId,
        userName = name,
        userEmail = email,
        hostId = "host",
        status = status,
        requestedAt = requestedAt
    )
}
