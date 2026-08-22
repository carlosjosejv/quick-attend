package com.greenshark.quickattend.ui.commons

import java.time.LocalDate

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

enum class Screen {
    HOME,
    LOGIN,
    SIGN_IN,
    SIGN_UP,
    WELCOME,
    SCAN,
    HISTORY,
    UPCOMING_EVENTS,
    EVENT_FORM,
    ATTENDANCE_DETAIL,
    MANAGED_EVENTS,
    HOST_EVENT_DETAIL,
    HOST_ATTENDEE_DETAIL,
    EXPORT_PREVIEW
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object Login : NavigationItem(Screen.LOGIN.name)
    object SignIn : NavigationItem(Screen.SIGN_IN.name)
    object SignUp : NavigationItem(Screen.SIGN_UP.name)
    object Welcome : NavigationItem(Screen.WELCOME.name)
    object Scan : NavigationItem(Screen.SCAN.name)
    object History : NavigationItem(Screen.HISTORY.name)
    object UpcomingEvents : NavigationItem(Screen.UPCOMING_EVENTS.name)

    /** Everything the signed in user hosts, which is the way into managing one. */
    object ManagedEvents : NavigationItem(Screen.MANAGED_EVENTS.name)

    /**
     * Create or edit an event. The optional eventId argument is empty when
     * creating, so the same destination covers both cases.
     */
    object EventForm : NavigationItem("${Screen.EVENT_FORM.name}?$ARG_EVENT_ID={$ARG_EVENT_ID}") {
        fun createRoute(eventId: String? = null): String =
            "${Screen.EVENT_FORM.name}?$ARG_EVENT_ID=${eventId.orEmpty()}"
    }

    /**
     * Attendance the user built up on one event. The optional date argument is
     * the session they tapped, so the screen can open on that one instead of
     * the most recent.
     */
    object AttendanceDetail : NavigationItem(
        "${Screen.ATTENDANCE_DETAIL.name}?$ARG_EVENT_ID={$ARG_EVENT_ID}&$ARG_DATE={$ARG_DATE}"
    ) {
        fun createRoute(eventId: String, date: LocalDate? = null): String =
            "${Screen.ATTENDANCE_DETAIL.name}?$ARG_EVENT_ID=$eventId&$ARG_DATE=${date?.toString().orEmpty()}"
    }

    /** One event as its host sees it: the roll, the actions, and the exports. */
    object HostEventDetail : NavigationItem(
        "${Screen.HOST_EVENT_DETAIL.name}/{$ARG_EVENT_ID}"
    ) {
        fun createRoute(eventId: String): String = "${Screen.HOST_EVENT_DETAIL.name}/$eventId"
    }

    /** One attendant's sessions on one event, as the host sees them. */
    object HostAttendeeDetail : NavigationItem(
        "${Screen.HOST_ATTENDEE_DETAIL.name}/{$ARG_EVENT_ID}/{$ARG_USER_ID}"
    ) {
        fun createRoute(eventId: String, userId: String): String =
            "${Screen.HOST_ATTENDEE_DETAIL.name}/$eventId/$userId"
    }

    /**
     * Preview of an attendance record before it is shared. The optional user
     * argument narrows it to one attendant; empty means the whole roll.
     */
    object ExportPreview : NavigationItem(
        "${Screen.EXPORT_PREVIEW.name}/{$ARG_EVENT_ID}?$ARG_USER_ID={$ARG_USER_ID}"
    ) {
        fun createRoute(eventId: String, userId: String? = null): String =
            "${Screen.EXPORT_PREVIEW.name}/$eventId?$ARG_USER_ID=${userId.orEmpty()}"
    }
}

const val ARG_EVENT_ID = "eventId"

/** ISO date of one run of an event, empty when the caller has no session in mind. */
const val ARG_DATE = "date"

/** Account the screen is about, which is not always the signed in one. */
const val ARG_USER_ID = "userId"
