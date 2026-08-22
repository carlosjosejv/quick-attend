/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import android.graphics.Bitmap
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.attendanceHistory
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.data.signatureFor
import com.greenshark.quickattend.data.summarize
import com.greenshark.quickattend.ui.component.signature.SignatureBitmaps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything the attendance detail screen draws. */
@Stable
data class AttendanceDetailState(
    val event: Event? = null,
    /** Every past session of the event, oldest first, absences included. */
    val records: List<AttendanceRecord> = emptyList(),
    val summary: AttendanceSummary = AttendanceSummary(),
    /** [AttendanceRecord.key] of the session shown in the card at the top. */
    val selectedKey: String? = null,
    val profile: UserProfile? = null,
    val isSigning: Boolean = false,
    val isSavingSignature: Boolean = false,
    val isLoading: Boolean = false,
    val error: DataError? = null
) {
    val selectedRecord: AttendanceRecord?
        get() = records.firstOrNull { it.key == selectedKey } ?: records.lastOrNull()

    /** Newest first, which is the order the history list reads in. */
    val recentFirst: List<AttendanceRecord> get() = records.asReversed()

    /** What the featured session is signed with: the drawing, or a code. */
    val selectedSignature: AttendanceSignature
        get() {
            val record = selectedRecord ?: return AttendanceSignature.None
            val signer = profile ?: return AttendanceSignature.None
            return signatureFor(record = record, profile = signer)
        }

    val signatureImage: String get() = profile?.signatureImage.orEmpty()
}

sealed interface AttendanceDetailAction {
    data class RecordSelected(val key: String) : AttendanceDetailAction
    data object SignatureRequested : AttendanceDetailAction
    data object SignatureDismissed : AttendanceDetailAction
    data class SignatureDrawn(val bitmap: Bitmap) : AttendanceDetailAction
    data object SignatureRemoved : AttendanceDetailAction
    data object Refresh : AttendanceDetailAction
}

/**
 * Attendance for one event and the signed in user. The backend only stores the
 * check-ins, so the absences and the score are worked out here against the
 * event's own schedule.
 */
class AttendanceDetailViewModel(
    private val eventId: String,
    /** The session the user tapped; null falls back to the most recent one. */
    private val date: LocalDate? = null,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val attendanceRepository: AttendanceRepository = ServiceLocator.attendanceRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceDetailState(isLoading = true))
    val state: StateFlow<AttendanceDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: AttendanceDetailAction) {
        when (action) {
            is AttendanceDetailAction.RecordSelected ->
                _state.update { it.copy(selectedKey = action.key) }

            AttendanceDetailAction.SignatureRequested ->
                _state.update { it.copy(isSigning = true) }

            AttendanceDetailAction.SignatureDismissed ->
                _state.update { it.copy(isSigning = false) }

            is AttendanceDetailAction.SignatureDrawn ->
                saveSignature(SignatureBitmaps.encode(action.bitmap))

            AttendanceDetailAction.SignatureRemoved -> saveSignature("")

            AttendanceDetailAction.Refresh -> load()
        }
    }

    /** A blank image clears the signature, which is what "remove" sends. */
    private fun saveSignature(image: String) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _state.update { it.copy(isSigning = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isSavingSignature = true) }
        viewModelScope.launch {
            when (val result = userRepository.saveSignature(userId, image)) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        isSavingSignature = false,
                        isSigning = false,
                        // Updated locally too, so the signature on screen changes
                        // without waiting for the profile to be read back.
                        profile = it.profile?.copy(signatureImage = image)
                            ?: UserProfile(id = userId, signatureImage = image)
                    )
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isSavingSignature = false, error = result.error)
                }
            }
        }
    }

    private fun load() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val event = when (val result = eventRepository.getEvent(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }

            val checkIns = when (
                val result = attendanceRepository.getCheckIns(eventId, userId)
            ) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, event = event, error = result.error)
                    }
                    return@launch
                }
            }

            // A missing profile is not worth failing the screen over: without it
            // the records still read, they just sign with a code.
            val profile = (userRepository.getProfile(userId) as? DataResult.Success)?.data

            val records = event.attendanceHistory(
                checkIns = checkIns,
                now = LocalDateTime.now()
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    event = event,
                    profile = profile,
                    records = records,
                    summary = records.summarize(),
                    selectedKey = records.firstOrNull { record -> record.date == date }?.key
                        ?: records.lastOrNull()?.key
                )
            }
        }
    }

    companion object {
        fun factory(eventId: String, date: LocalDate?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AttendanceDetailViewModel(eventId = eventId, date = date) }
            }
    }
}
