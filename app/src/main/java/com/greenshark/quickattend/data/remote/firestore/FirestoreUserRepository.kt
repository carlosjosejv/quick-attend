/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.data.unknownProfile
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val users get() = firestore.collection(COLLECTION_USERS)

    override suspend fun isProUser(userId: String): DataResult<Boolean> = try {
        val snapshot = users.document(userId).get().await()
        DataResult.Success(snapshot.getBoolean(FIELD_PRO) == true)
    } catch (e: Exception) {
        DataResult.Failure(DataError.UNKNOWN)
    }

    override suspend fun getProfile(userId: String): DataResult<UserProfile> = try {
        val snapshot = users.document(userId).get().await()
        val profile = snapshot.toObject(UserDto::class.java)?.toProfile(fallbackId = userId)
        DataResult.Success(profile ?: unknownProfile(userId))
    } catch (e: Exception) {
        DataResult.Failure(DataError.UNKNOWN)
    }

    override suspend fun getProfiles(userIds: List<String>): DataResult<List<UserProfile>> {
        val wanted = userIds.filter { it.isNotBlank() }.distinct()
        if (wanted.isEmpty()) return DataResult.Success(emptyList())

        return try {
            // A document id query only takes so many values at a time, so the
            // ids are asked for in batches and stitched back together.
            val found = wanted.chunked(MAX_IDS_PER_QUERY).flatMap { batch ->
                users.whereIn(FieldPath.documentId(), batch).get().await()
                    .documents
                    .mapNotNull { document ->
                        document.toObject(UserDto::class.java)?.toProfile(document.id)
                    }
            }.associateBy { it.id }

            // Ids with no document become placeholders, so the caller's list
            // keeps its length and nobody drops off an attendee list.
            DataResult.Success(wanted.map { id -> found[id] ?: unknownProfile(id) })
        } catch (e: Exception) {
            DataResult.Failure(DataError.UNKNOWN)
        }
    }

    override suspend fun saveSignature(
        userId: String,
        signatureImage: String
    ): DataResult<Unit> = try {
        // Merged rather than set, so saving a signature never wipes the pro flag
        // or anything else the account document holds.
        users.document(userId)
            .set(mapOf(FIELD_SIGNATURE to signatureImage), SetOptions.merge())
            .await()
        DataResult.Success(Unit)
    } catch (e: Exception) {
        DataResult.Failure(DataError.UNKNOWN)
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val FIELD_PRO = "pro"
        const val FIELD_SIGNATURE = "signatureImage"

        /** Firestore's cap on the values a single "in" filter accepts. */
        const val MAX_IDS_PER_QUERY = 30
    }
}
