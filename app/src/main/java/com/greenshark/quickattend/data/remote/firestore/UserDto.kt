/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.greenshark.quickattend.data.UserProfile

/**
 * Wire shape of an account. The signature travels as a base64 PNG rather than a
 * storage path so a profile is one read, and stays inside the document size
 * limit because the pad writes a small monochrome bitmap.
 */
data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val signatureImage: String = "",
    val pro: Boolean = false
)

fun UserDto.toProfile(fallbackId: String = ""): UserProfile = UserProfile(
    id = id.ifBlank { fallbackId },
    name = name,
    email = email,
    signatureImage = signatureImage,
    isPro = pro
)
