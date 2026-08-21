/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.repository.UserRepository
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun isProUser(userId: String): DataResult<Boolean> = try {
        val snapshot = firestore.collection(COLLECTION_USERS).document(userId).get().await()
        DataResult.Success(snapshot.getBoolean(FIELD_PRO) == true)
    } catch (e: Exception) {
        DataResult.Failure(DataError.UNKNOWN)
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val FIELD_PRO = "pro"
    }
}
