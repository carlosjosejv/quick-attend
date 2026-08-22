/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.time.LocalDateTime

/**
 * How early someone may scan. Doors usually open before the hour, so a scan
 * this far ahead of the start still belongs to that session.
 */
const val EARLY_CHECK_IN_MINUTES = 30L

/**
 * The run of this event that a scan right now belongs to, or null when the
 * event is not open for check-in.
 *
 * Only today is looked at, because [TimeRange] cannot end later than midnight:
 * a run always finishes on the calendar day it started, so a scan can never
 * belong to yesterday's.
 */
fun Event.checkInOccurrence(now: LocalDateTime): EventOccurrence? =
    occurrenceOn(date = now.toLocalDate(), now = now)?.takeIf { it.acceptsCheckInAt(now) }

/** Open from [EARLY_CHECK_IN_MINUTES] before the start until the session ends. */
fun EventOccurrence.acceptsCheckInAt(now: LocalDateTime): Boolean =
    !now.isBefore(start.minusMinutes(EARLY_CHECK_IN_MINUTES)) && now.isBefore(end)
