/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.greenshark.quickattend.data.ApprovalState
import com.greenshark.quickattend.data.AttendanceCheckIn
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.data.JoinRequestStatus
import com.greenshark.quickattend.data.RecordedAttendance
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.data.accepts
import com.greenshark.quickattend.data.checkInOccurrence
import com.greenshark.quickattend.data.distanceMetersTo
import com.greenshark.quickattend.data.joinRequestId
import com.greenshark.quickattend.data.location.AndroidLocationProvider
import com.greenshark.quickattend.data.location.LocationError
import com.greenshark.quickattend.data.location.LocationProvider
import com.greenshark.quickattend.data.location.LocationResult
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.JoinRequestRepository
import com.greenshark.quickattend.data.repository.NotificationSender
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.qr.QrCodeGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Why a scan did not turn into an attendance record. */
enum class ScanErrorReason {
    /** The code is a QR, just not one of this app's event codes. */
    INVALID_CODE,
    EVENT_NOT_FOUND,

    /** The event is real, but it is not running and does not start soon. */
    NOT_IN_SESSION,
    NOT_SIGNED_IN,
    LOCATION_PERMISSION_DENIED,
    LOCATION_DISABLED,
    LOCATION_UNAVAILABLE,
    TOO_FAR,
    NETWORK,
    UNKNOWN
}

/**
 * A refused scan, with the numbers behind it when there are any. Telling someone
 * they are 300 m away is actionable in a way that "too far" is not.
 */
@Stable
data class ScanError(
    val reason: ScanErrorReason,
    val eventTitle: String? = null,
    val distanceMeters: Double? = null,
    val allowedMeters: Int? = null
)

/** What the scan screen is doing right now. */
enum class ScanStage {
    /** Camera is live and looking for a code. */
    SCANNING,

    /** A code was read and the event behind it is being looked up. */
    RESOLVING,

    /** Waiting on a location fix, which is the slow step worth naming. */
    LOCATING,

    /** A result is on screen and the camera is paused behind it. */
    RESULT
}

@Stable
data class ScanState(
    val stage: ScanStage = ScanStage.SCANNING,
    val recorded: RecordedAttendance? = null,
    val error: ScanError? = null,
    val isTorchOn: Boolean = false
) {
    /** The camera only decodes while nothing else is going on. */
    val isCameraActive: Boolean get() = stage == ScanStage.SCANNING

    val isWorking: Boolean get() = stage == ScanStage.RESOLVING || stage == ScanStage.LOCATING
}

sealed interface ScanAction {
    /** Raw payload straight off the camera. */
    data class QrScanned(val payload: String) : ScanAction

    /** Answer to the permission dialog the screen put up on the model's behalf. */
    data class LocationPermissionResult(val granted: Boolean) : ScanAction

    /** Close the result and go back to scanning. */
    data object Dismissed : ScanAction
    data object TorchToggled : ScanAction
}

sealed interface ScanEvent {
    /** Asks the screen to put up the system location dialog. */
    data object RequestLocationPermission : ScanEvent

    /** A record was written, so the screen can let the rest of the app know. */
    data class AttendanceRecorded(val recorded: RecordedAttendance) : ScanEvent
}

/**
 * Turning a scanned QR into an attendance record.
 *
 * Two rules shape the whole flow. Location validation is a gate: an event that
 * asks for it records nothing until the phone proves it is at the venue. Being
 * on the roll is not a gate: someone who scans an event they never signed up for
 * still gets their attendance recorded, and what the host decides afterwards is
 * only whether that record joins the roll and the exports.
 */
class ScanViewModel(
    private val locationProvider: LocationProvider,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val attendanceRepository: AttendanceRepository = ServiceLocator.attendanceRepository,
    private val joinRequestRepository: JoinRequestRepository = ServiceLocator.joinRequestRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val notificationSender: NotificationSender = ServiceLocator.notificationSender,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val now: () -> LocalDateTime = LocalDateTime::now
) : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events: Flow<ScanEvent> = _events.receiveAsFlow()

    /** Held across the permission dialog, so the scan resumes where it stopped. */
    private var pending: PendingCheckIn? = null

    fun onAction(action: ScanAction) {
        when (action) {
            is ScanAction.QrScanned -> resolve(action.payload)
            is ScanAction.LocationPermissionResult -> onLocationPermissionResult(action.granted)
            ScanAction.Dismissed -> reset()
            ScanAction.TorchToggled -> _state.update { it.copy(isTorchOn = !it.isTorchOn) }
        }
    }

    private fun reset() {
        pending = null
        _state.update {
            it.copy(stage = ScanStage.SCANNING, recorded = null, error = null)
        }
    }

    private fun resolve(payload: String) {
        if (_state.value.stage != ScanStage.SCANNING) return

        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            fail(ScanErrorReason.NOT_SIGNED_IN)
            return
        }

        val eventId = QrCodeGenerator.eventIdFrom(payload)
        if (eventId.isNullOrBlank()) {
            fail(ScanErrorReason.INVALID_CODE)
            return
        }

        _state.update { it.copy(stage = ScanStage.RESOLVING, error = null, recorded = null) }
        viewModelScope.launch {
            val event = when (val result = eventRepository.getEvent(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    fail(result.error.toScanReason(notFound = ScanErrorReason.EVENT_NOT_FOUND))
                    return@launch
                }
            }

            val occurrence = event.checkInOccurrence(now())
            if (occurrence == null) {
                fail(ScanErrorReason.NOT_IN_SESSION, eventTitle = event.title)
                return@launch
            }

            val checkIn = PendingCheckIn(userId = userId, event = event, occurrence = occurrence)
            if (!event.validatesLocation()) {
                record(checkIn, distanceMeters = null)
                return@launch
            }

            if (!locationProvider.hasPermission()) {
                // Only ever asked for at the point an event actually needs it,
                // rather than up front for every scan.
                pending = checkIn
                _events.send(ScanEvent.RequestLocationPermission)
                return@launch
            }
            validateLocationThenRecord(checkIn)
        }
    }

    private fun onLocationPermissionResult(granted: Boolean) {
        val checkIn = pending ?: return
        pending = null

        if (!granted) {
            fail(
                reason = ScanErrorReason.LOCATION_PERMISSION_DENIED,
                eventTitle = checkIn.event.title
            )
            return
        }
        viewModelScope.launch { validateLocationThenRecord(checkIn) }
    }

    /**
     * The gate. Nothing is written unless the phone reports a position inside
     * the venue's radius, because an event that turned this on is relying on it.
     */
    private suspend fun validateLocationThenRecord(checkIn: PendingCheckIn) {
        val mapLocation = checkIn.event.mapLocation ?: run {
            // Validation was switched on but no point was ever picked, so there
            // is nothing to check against and the scan is taken at face value.
            record(checkIn, distanceMeters = null)
            return
        }

        _state.update { it.copy(stage = ScanStage.LOCATING) }
        val point = when (val result = locationProvider.currentLocation()) {
            is LocationResult.Success -> result.point
            is LocationResult.Failure -> {
                fail(result.error.toScanReason(), eventTitle = checkIn.event.title)
                return
            }
        }

        val distance = mapLocation.distanceMetersTo(point)
        if (!mapLocation.accepts(point)) {
            fail(
                reason = ScanErrorReason.TOO_FAR,
                eventTitle = checkIn.event.title,
                distanceMeters = distance,
                allowedMeters = mapLocation.radiusMeters
            )
            return
        }
        record(checkIn, distanceMeters = distance)
    }

    private suspend fun record(checkIn: PendingCheckIn, distanceMeters: Double?) {
        val moment = now()
        val attendance = AttendanceCheckIn(
            eventId = checkIn.event.id,
            userId = checkIn.userId,
            date = checkIn.occurrence.date,
            checkInAt = moment
        )

        val stored = when (val result = attendanceRepository.recordCheckIn(attendance)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> {
                fail(result.error.toScanReason(), eventTitle = checkIn.event.title)
                return
            }
        }

        val enrolled = when (val result = eventRepository.isAttendee(
            eventId = checkIn.event.id,
            attendeeId = checkIn.userId
        )) {
            is DataResult.Success -> result.data
            // The record is already safe in the backend, so a failed lookup
            // must not throw the confirmation away. Treat it as on the roll and
            // let the host's own screen be the source of truth.
            is DataResult.Failure -> true
        }

        val approval = if (enrolled) {
            ApprovalState.ENROLLED
        } else {
            fileJoinRequest(checkIn = checkIn, checkInAt = stored.checkInAt)
        }

        val recorded = RecordedAttendance(
            event = checkIn.event,
            date = stored.date,
            scheduledStart = checkIn.occurrence.start,
            checkInAt = stored.checkInAt,
            alreadyRecorded = stored.checkInAt != moment,
            approval = approval,
            distanceMeters = distanceMeters
        )
        _state.update {
            it.copy(stage = ScanStage.RESULT, recorded = recorded, error = null)
        }
        _events.send(ScanEvent.AttendanceRecorded(recorded))
    }

    /**
     * Asks the host to let this person onto the roll. Their attendance is
     * already stored by the time this runs, so neither a failed write nor a
     * failed push can cost them the record; the worst case is a host who has to
     * spot the request on their dashboard instead of being nudged to it.
     */
    private suspend fun fileJoinRequest(
        checkIn: PendingCheckIn,
        checkInAt: LocalDateTime
    ): ApprovalState {
        val profile = userRepository.profileOrNull(checkIn.userId)
        val request = JoinRequest(
            id = joinRequestId(userId = checkIn.userId, eventId = checkIn.event.id),
            eventId = checkIn.event.id,
            eventTitle = checkIn.event.title,
            userId = checkIn.userId,
            userName = profile?.displayName.orEmpty()
                .ifBlank { auth.currentUser?.displayName.orEmpty() },
            userEmail = profile?.email.orEmpty()
                .ifBlank { auth.currentUser?.email.orEmpty() },
            hostId = checkIn.event.organizerId,
            requestedAt = System.currentTimeMillis(),
            firstCheckInAt = System.currentTimeMillis()
        )

        val filed = when (val result = joinRequestRepository.requestJoin(request)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return ApprovalState.PENDING_APPROVAL
        }

        return when (filed.status) {
            // Already answered on an earlier scan, so the host is not asked twice.
            JoinRequestStatus.APPROVED -> ApprovalState.ENROLLED
            JoinRequestStatus.REJECTED -> ApprovalState.REJECTED
            JoinRequestStatus.PENDING -> {
                if (filed.requestedAt == request.requestedAt) {
                    notificationSender.notifyJoinRequested(filed)
                }
                ApprovalState.PENDING_APPROVAL
            }
        }
    }

    private fun fail(
        reason: ScanErrorReason,
        eventTitle: String? = null,
        distanceMeters: Double? = null,
        allowedMeters: Int? = null
    ) {
        pending = null
        _state.update {
            it.copy(
                stage = ScanStage.RESULT,
                recorded = null,
                error = ScanError(
                    reason = reason,
                    eventTitle = eventTitle,
                    distanceMeters = distanceMeters,
                    allowedMeters = allowedMeters
                )
            )
        }
    }

    /** One scan in flight: everything the steps after the gate still need. */
    private data class PendingCheckIn(
        val userId: String,
        val event: Event,
        val occurrence: EventOccurrence
    )

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScanViewModel(locationProvider = AndroidLocationProvider(context))
            }
        }
    }
}

/** A point is only worth asking for when the host both turned it on and set one. */
private fun Event.validatesLocation(): Boolean =
    locationValidationEnabled && mapLocation != null

private suspend fun UserRepository.profileOrNull(userId: String): UserProfile? =
    when (val result = getProfile(userId)) {
        is DataResult.Success -> result.data
        is DataResult.Failure -> null
    }

private fun DataError.toScanReason(
    notFound: ScanErrorReason = ScanErrorReason.UNKNOWN
): ScanErrorReason = when (this) {
    DataError.NETWORK -> ScanErrorReason.NETWORK
    DataError.UNAUTHORIZED -> ScanErrorReason.NOT_SIGNED_IN
    DataError.NOT_FOUND -> notFound
    DataError.UNKNOWN -> ScanErrorReason.UNKNOWN
}

private fun LocationError.toScanReason(): ScanErrorReason = when (this) {
    LocationError.PERMISSION_DENIED -> ScanErrorReason.LOCATION_PERMISSION_DENIED
    LocationError.DISABLED -> ScanErrorReason.LOCATION_DISABLED
    LocationError.UNAVAILABLE -> ScanErrorReason.LOCATION_UNAVAILABLE
}
