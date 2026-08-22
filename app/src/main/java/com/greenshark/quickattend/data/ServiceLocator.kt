/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import com.greenshark.quickattend.data.remote.firestore.FirestoreAttendanceRepository
import com.greenshark.quickattend.data.remote.firestore.FirestoreEventRepository
import com.greenshark.quickattend.data.remote.firestore.FirestoreUserRepository
import com.greenshark.quickattend.data.remote.onesignal.OneSignalNotificationSender
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.NotificationSender
import com.greenshark.quickattend.data.repository.UserRepository

/**
 * Single place where the backend is chosen. Swapping Firestore for a REST API
 * later is two assignments here, nothing else in the app changes.
 */
object ServiceLocator {

    var eventRepository: EventRepository = FirestoreEventRepository()

    var userRepository: UserRepository = FirestoreUserRepository()

    var attendanceRepository: AttendanceRepository = FirestoreAttendanceRepository()

    var notificationSender: NotificationSender = OneSignalNotificationSender()
}
