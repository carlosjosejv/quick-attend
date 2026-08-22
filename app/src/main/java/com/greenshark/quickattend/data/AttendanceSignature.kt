/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import java.security.MessageDigest
import java.time.LocalDate

/**
 * What gets printed in the signature column of an exported record.
 *
 * An attendant who drew or uploaded a signature signs with it; everyone else
 * signs with a [Code], a short value derived from who they are and which
 * session they attended. The code is not a picture of a signature, it is a
 * receipt: the same check-in always produces the same code, and a different
 * check-in never does, so an exported sheet can be checked against the data it
 * came from.
 */
sealed interface AttendanceSignature {

    /** Base64 PNG the attendant drew in the app or uploaded to their profile. */
    data class Drawn(val imageBase64: String, val code: String) : AttendanceSignature

    /** Generated stand-in, e.g. "QA-7F3A-91C2-88DE". */
    data class Code(val value: String) : AttendanceSignature

    /** Nothing to sign: the attendant never showed up for this session. */
    data object None : AttendanceSignature

    /** The code either kind carries, used as the caption under a drawn image. */
    val displayCode: String?
        get() = when (this) {
            is Drawn -> code
            is Code -> value
            None -> null
        }
}

/**
 * Deterministic code for one attendant on one session of one event. Built from
 * the check-in time as well, so re-signing after a corrected check-in produces
 * a visibly different code instead of silently reusing the old one.
 *
 * [checkInSecondOfDay] is the clock time alone, because [date] already carries
 * which day it was; seeding with both would say the same thing twice.
 */
fun signatureCodeFor(
    userId: String,
    eventId: String,
    date: LocalDate,
    checkInSecondOfDay: Long
): String {
    val seed = "$userId|$eventId|$date|$checkInSecondOfDay"
    val digest = MessageDigest.getInstance(DIGEST_ALGORITHM).digest(seed.toByteArray())
    val hex = digest.take(n = CODE_BYTES).joinToString(separator = "") { "%02X".format(it) }
    return hex.chunked(size = GROUP_SIZE).joinToString(separator = "-", prefix = CODE_PREFIX)
}

/**
 * The signature to print for [record], which is [profile]'s drawn one when they
 * have one and a generated code otherwise. Absences sign with nothing.
 */
fun signatureFor(record: AttendanceRecord, profile: UserProfile): AttendanceSignature {
    val checkInAt = record.checkInAt ?: return AttendanceSignature.None
    val code = signatureCodeFor(
        userId = profile.id,
        eventId = record.eventId,
        date = record.date,
        checkInSecondOfDay = checkInAt.toLocalTime().toSecondOfDay().toLong()
    )
    return if (profile.hasSignatureImage) {
        AttendanceSignature.Drawn(imageBase64 = profile.signatureImage, code = code)
    } else {
        AttendanceSignature.Code(code)
    }
}

private const val DIGEST_ALGORITHM = "SHA-256"

/** Six bytes is twelve hex characters, short enough to read off a printed page. */
private const val CODE_BYTES = 6

private const val GROUP_SIZE = 4

private const val CODE_PREFIX = "QA-"
