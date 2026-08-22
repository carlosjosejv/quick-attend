/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.repository

import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.UserProfile

interface UserRepository {

    /** Pro accounts unlock location validation on their events. */
    suspend fun isProUser(userId: String): DataResult<Boolean>

    suspend fun getProfile(userId: String): DataResult<UserProfile>

    /**
     * Profiles for a set of accounts. Ids with no document still come back, as
     * placeholders, so an attendee list never silently loses a row.
     */
    suspend fun getProfiles(userIds: List<String>): DataResult<List<UserProfile>>

    /** Stores the signature the attendant drew, as a base64 PNG. Blank clears it. */
    suspend fun saveSignature(userId: String, signatureImage: String): DataResult<Unit>
}
