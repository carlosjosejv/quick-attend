package com.greenshark.quickattend

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.greenshark.quickattend.data.remote.onesignal.OneSignalConfig

/**
 * Created by Carlos Jiménez on 21/08/26.
 */

/**
 * Brings OneSignal up and keeps the device tied to whoever is signed in, so a
 * push aimed at a Firebase uid reaches the right person's phone.
 */
class QuickAttendApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (!OneSignalConfig.isReceiveConfigured) return

        if (BuildConfig.DEBUG) OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, OneSignalConfig.appId)

        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.uid?.let(OneSignal::login)
        // Signing out has to clear the alias as well, otherwise the next person
        // to use the phone keeps receiving the previous account's pushes.
        auth.addAuthStateListener { state ->
            val uid = state.currentUser?.uid
            if (uid.isNullOrBlank()) OneSignal.logout() else OneSignal.login(uid)
        }
    }
}
