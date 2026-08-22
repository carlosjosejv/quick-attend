/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

/**
 * The bits of an account the rest of the app needs: who the person is and the
 * signature that goes next to their name on an exported record.
 *
 * [signatureImage] is a base64 PNG the attendant either drew in the app or
 * uploaded. It is blank for everyone who never set one, which is what makes the
 * export fall back to a generated code instead.
 */
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val signatureImage: String = "",
    val isPro: Boolean = false
) {
    /** Never blank, so a list never shows a nameless row. */
    val displayName: String
        get() = name.ifBlank { email.substringBefore('@').ifBlank { FALLBACK_NAME } }

    val hasSignatureImage: Boolean get() = signatureImage.isNotBlank()

    /** Two letters for the avatar bubble, e.g. "CJ" for "Carlos Jiménez". */
    val initials: String
        get() = displayName
            .split(' ')
            .filter { it.isNotBlank() }
            .take(n = 2)
            .joinToString(separator = "") { it.first().uppercase() }
            .ifBlank { FALLBACK_NAME.first().toString() }

    private companion object {
        const val FALLBACK_NAME = "Attendant"
    }
}

/** Placeholder for an attendee whose account document could not be read. */
fun unknownProfile(userId: String, name: String = "", email: String = ""): UserProfile =
    UserProfile(id = userId, name = name, email = email)
