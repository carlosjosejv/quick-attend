/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.repository

import com.greenshark.quickattend.data.DataResult

interface UserRepository {

    /** Pro accounts unlock location validation on their events. */
    suspend fun isProUser(userId: String): DataResult<Boolean>
}
