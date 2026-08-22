/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.repository

import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event

/**
 * Push notifications the app sends to other people. OneSignal is today's
 * implementation; the interface keeps the SDK out of the presentation layer and
 * lets tests assert what would have been sent.
 */
interface NotificationSender {

    /**
     * Tells everyone signed up that the hours of an event moved. Called after
     * the change is saved, so a failure to notify never blocks the edit.
     */
    suspend fun notifyScheduleChanged(
        event: Event,
        attendeeIds: List<String>
    ): DataResult<Unit>
}
