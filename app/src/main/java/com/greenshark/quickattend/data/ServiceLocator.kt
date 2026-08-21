/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data

import com.greenshark.quickattend.data.remote.firestore.FirestoreEventRepository
import com.greenshark.quickattend.data.remote.firestore.FirestoreUserRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.UserRepository

/**
 * Single place where the backend is chosen. Swapping Firestore for a REST API
 * later is two assignments here, nothing else in the app changes.
 */
object ServiceLocator {

    var eventRepository: EventRepository = FirestoreEventRepository()

    var userRepository: UserRepository = FirestoreUserRepository()
}
