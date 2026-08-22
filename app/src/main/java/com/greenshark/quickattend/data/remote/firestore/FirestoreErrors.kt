/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestoreException
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult

/** Runs a Firestore call and turns anything it throws into a typed failure. */
internal inline fun <T> runCatchingFirestore(block: () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: FirebaseFirestoreException) {
    DataResult.Failure(e.code.toDataError())
} catch (e: Exception) {
    DataResult.Failure(DataError.UNKNOWN)
}

internal fun FirebaseFirestoreException.Code.toDataError(): DataError = when (this) {
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DataError.NETWORK

    FirebaseFirestoreException.Code.PERMISSION_DENIED,
    FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.UNAUTHORIZED

    FirebaseFirestoreException.Code.NOT_FOUND -> DataError.NOT_FOUND
    else -> DataError.UNKNOWN
}
