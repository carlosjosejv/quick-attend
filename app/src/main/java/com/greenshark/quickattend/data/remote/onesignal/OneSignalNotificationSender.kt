/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.onesignal

import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.data.daysSummary
import com.greenshark.quickattend.data.repository.NotificationSender
import com.greenshark.quickattend.data.timeSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends the schedule-change push through OneSignal's REST API.
 *
 * Attendants are addressed by their external id, which the app sets to the
 * Firebase uid when it logs the device in, so the host never needs to know
 * anything about the devices someone signed in on.
 */
class OneSignalNotificationSender(
    private val appId: String = OneSignalConfig.appId,
    private val restApiKey: String = OneSignalConfig.restApiKey,
    private val endpoint: String = OneSignalConfig.SendEndpoint
) : NotificationSender {

    override suspend fun notifyScheduleChanged(
        event: Event,
        attendeeIds: List<String>
    ): DataResult<Unit> {
        val recipients = attendeeIds.filter { it.isNotBlank() }.distinct()
        if (recipients.isEmpty()) return DataResult.Success(Unit)
        if (appId.isBlank() || restApiKey.isBlank()) {
            return DataResult.Failure(DataError.UNAUTHORIZED)
        }

        val body = JSONObject().apply {
            put("app_id", appId)
            put("target_channel", "push")
            put("include_aliases", JSONObject().apply {
                put("external_id", JSONArray(recipients))
            })
            put("headings", JSONObject().put("en", "${event.title} was rescheduled"))
            put("contents", JSONObject().put("en", event.scheduleChangeMessage()))
            // Carried through so tapping the push can open the event straight away.
            put("data", JSONObject().put("eventId", event.id))
        }

        return withContext(Dispatchers.IO) { post(body) }
    }

    override suspend fun notifyJoinRequested(request: JoinRequest): DataResult<Unit> {
        if (request.hostId.isBlank()) return DataResult.Failure(DataError.NOT_FOUND)
        if (appId.isBlank() || restApiKey.isBlank()) {
            return DataResult.Failure(DataError.UNAUTHORIZED)
        }

        val body = JSONObject().apply {
            put("app_id", appId)
            put("target_channel", "push")
            put("include_aliases", JSONObject().apply {
                put("external_id", JSONArray(listOf(request.hostId)))
            })
            put("headings", JSONObject().put("en", "New attendance request"))
            put("contents", JSONObject().put("en", request.joinRequestMessage()))
            // Carried through so tapping the push can open the event's requests.
            put("data", JSONObject().apply {
                put("eventId", request.eventId)
                put("joinRequestId", request.id)
            })
        }

        return withContext(Dispatchers.IO) { post(body) }
    }

    private fun post(body: JSONObject): DataResult<Unit> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Key $restApiKey")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            when (val code = connection.responseCode) {
                in SUCCESS_RANGE -> DataResult.Success(Unit)
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN -> DataResult.Failure(DataError.UNAUTHORIZED)

                HttpURLConnection.HTTP_NOT_FOUND -> DataResult.Failure(DataError.NOT_FOUND)
                else -> DataResult.Failure(
                    if (code >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
                        DataError.NETWORK
                    } else {
                        DataError.UNKNOWN
                    }
                )
            }
        } catch (e: IOException) {
            DataResult.Failure(DataError.NETWORK)
        } catch (e: Exception) {
            DataResult.Failure(DataError.UNKNOWN)
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
        val SUCCESS_RANGE = 200..299
    }
}

/** "Now Mon, Wed 13:00 - 17:00", short enough to read in a notification shade. */
internal fun Event.scheduleChangeMessage(): String =
    "Now ${schedule.daysSummary()} · ${schedule.timeSummary()}"

/**
 * "Ana Reyes checked into Clase de Yoga without being on the roll". The host
 * reads this on a lock screen, so it says who and where and nothing else.
 */
internal fun JoinRequest.joinRequestMessage(): String =
    "$displayName checked into ${eventTitle.ifBlank { "your event" }} " +
            "without being on the roll"
