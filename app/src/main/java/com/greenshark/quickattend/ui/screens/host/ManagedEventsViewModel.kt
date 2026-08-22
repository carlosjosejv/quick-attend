/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventStatus
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.occurrenceOn
import com.greenshark.quickattend.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** One row of the host's list: the event plus whether it is running right now. */
@Stable
data class ManagedEvent(
    val event: Event,
    val status: EventStatus? = null
)

/** Everything the managed events screen draws. */
@Stable
data class ManagedEventsState(
    val events: List<ManagedEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: DataError? = null
) {
    val isEmpty: Boolean get() = !isLoading && events.isEmpty()
}

/**
 * Every event the signed in user hosts. Attendee counts are deliberately not
 * loaded here: that would be one query per event just to fill a subtitle, and
 * the count is right there on the detail screen a tap away.
 */
class ManagedEventsViewModel(
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(ManagedEventsState(isLoading = true))
    val state: StateFlow<ManagedEventsState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        val organizerId = auth.currentUser?.uid
        if (organizerId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = eventRepository.getEventsByOrganizer(organizerId)) {
                is DataResult.Success -> _state.update {
                    it.copy(isLoading = false, events = result.data.toManagedEvents())
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ManagedEventsViewModel() }
        }
    }
}

/** Whatever is running now sorts to the top, then the rest alphabetically. */
private fun List<Event>.toManagedEvents(): List<ManagedEvent> {
    val now = LocalDateTime.now()
    return map { event ->
        ManagedEvent(
            event = event,
            status = event.occurrenceOn(date = now.toLocalDate(), now = now)?.status
        )
    }.sortedWith(
        compareByDescending<ManagedEvent> { it.status == EventStatus.IN_PROGRESS }
            .thenBy { it.event.title.lowercase() }
    )
}
