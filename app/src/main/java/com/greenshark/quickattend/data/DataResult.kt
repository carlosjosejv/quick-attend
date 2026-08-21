/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

/**
 * Result of a data layer call. Errors are typed instead of raw exceptions so the
 * backend can be swapped without leaking Firestore or HTTP types into the UI.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

enum class DataError {
    NETWORK,
    UNAUTHORIZED,
    NOT_FOUND,
    UNKNOWN
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(data)
    return this
}

inline fun <T> DataResult<T>.onFailure(action: (DataError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}
