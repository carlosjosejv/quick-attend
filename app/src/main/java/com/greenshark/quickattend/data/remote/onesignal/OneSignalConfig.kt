/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.data.remote.onesignal

import com.greenshark.quickattend.BuildConfig

/**
 * The two OneSignal credentials, read from the build so neither is typed into
 * source. Both come from gradle.properties (or CI environment variables) via
 * the buildConfigField entries in the app's build file.
 *
 * The app id is public by design; the REST key is not. A key shipped inside an
 * APK can be pulled back out of it, so [isSendConfigured] is false unless a key
 * was supplied at build time, and a release build should leave it empty and
 * point [SendEndpoint] at a server of your own that holds the key instead.
 */
object OneSignalConfig {

    val appId: String get() = BuildConfig.ONESIGNAL_APP_ID

    val restApiKey: String get() = BuildConfig.ONESIGNAL_REST_API_KEY

    /** True when the app can receive pushes, which only needs the public id. */
    val isReceiveConfigured: Boolean get() = appId.isNotBlank()

    /** True when this build can also send them, which needs the private key. */
    val isSendConfigured: Boolean get() = isReceiveConfigured && restApiKey.isNotBlank()

    const val SendEndpoint = "https://api.onesignal.com/notifications"
}
