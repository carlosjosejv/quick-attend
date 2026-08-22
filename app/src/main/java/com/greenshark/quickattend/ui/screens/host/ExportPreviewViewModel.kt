/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.attendanceHistory
import com.greenshark.quickattend.data.attendeeAttendance
import com.greenshark.quickattend.data.overview
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.data.summarize
import com.greenshark.quickattend.export.AttendanceExportBuilder
import com.greenshark.quickattend.export.AttendanceExporter
import com.greenshark.quickattend.export.ExportDocument
import com.greenshark.quickattend.export.ExportFormat
import com.greenshark.quickattend.export.ExportedFile
import com.greenshark.quickattend.export.PdfPageRenderer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Everything the export preview screen draws. */
@Stable
data class ExportPreviewState(
    val event: Event? = null,
    /** Set when the export covers one attendant rather than the whole roll. */
    val attendee: AttendeeAttendance? = null,
    val format: ExportFormat = ExportFormat.PDF,
    val document: ExportDocument? = null,
    val exported: ExportedFile? = null,
    /** Rendered pages of the written PDF; empty for a spreadsheet. */
    val pages: List<Bitmap> = emptyList(),
    val isLoading: Boolean = true,
    val isRendering: Boolean = false,
    val error: DataError? = null,
    val writeFailed: Boolean = false
) {
    val isSingleAttendee: Boolean get() = attendee != null

    val canShare: Boolean get() = exported != null && !isRendering
}

sealed interface ExportPreviewAction {
    data class FormatChanged(val value: ExportFormat) : ExportPreviewAction
    data object Retry : ExportPreviewAction
}

sealed interface ExportPreviewEvent {
    /** The file is written and ready to hand to another app. */
    data class Share(val exported: ExportedFile) : ExportPreviewEvent
    data class Open(val exported: ExportedFile) : ExportPreviewEvent
}

/**
 * Builds an attendance record, writes it, and renders it back for preview.
 *
 * The preview is rasterised from the written file rather than drawn a second
 * time, so what the host approves is exactly the bytes they go on to share.
 */
class ExportPreviewViewModel(
    application: Application,
    private val eventId: String,
    /** Blank exports the whole roll; an id narrows it to one attendant. */
    private val userId: String,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val attendanceRepository: AttendanceRepository = ServiceLocator.attendanceRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ExportPreviewState())
    val state: StateFlow<ExportPreviewState> = _state.asStateFlow()

    private val _events = Channel<ExportPreviewEvent>(Channel.BUFFERED)
    val events: Flow<ExportPreviewEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ExportPreviewAction) {
        when (action) {
            is ExportPreviewAction.FormatChanged -> {
                if (action.value == _state.value.format) return
                _state.update { it.copy(format = action.value) }
                writeAndRender()
            }

            ExportPreviewAction.Retry -> load()
        }
    }

    fun share() {
        _state.value.exported?.let { exported ->
            viewModelScope.launch { _events.send(ExportPreviewEvent.Share(exported)) }
        }
    }

    fun open() {
        _state.value.exported?.let { exported ->
            viewModelScope.launch { _events.send(ExportPreviewEvent.Open(exported)) }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null, writeFailed = false) }
        viewModelScope.launch {
            val event = when (val result = eventRepository.getEvent(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }

            val document = if (userId.isBlank()) {
                buildGeneralDocument(event) ?: return@launch
            } else {
                buildAttendeeDocument(event) ?: return@launch
            }

            _state.update { it.copy(isLoading = false, event = event, document = document) }
            writeAndRender()
        }
    }

    /** Null when a call failed; the state already carries the reason by then. */
    private suspend fun buildGeneralDocument(event: Event): ExportDocument? {
        val attendeeIds = when (val result = eventRepository.getAttendeeIds(eventId)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> {
                failWith(result.error)
                return null
            }
        }

        val profiles = if (attendeeIds.isEmpty()) {
            emptyList()
        } else {
            when (val result = userRepository.getProfiles(attendeeIds)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                failWith(result.error)
                return null
            }
            }
        }

        val checkIns = when (val result = attendanceRepository.getEventCheckIns(eventId)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> {
                failWith(result.error)
                return null
            }
        }

        val attendees = event.attendeeAttendance(
            profiles = profiles,
            checkIns = checkIns,
            now = LocalDateTime.now()
        )
        return AttendanceExportBuilder.general(
            event = event,
            attendees = attendees,
            overview = attendees.overview()
        )
    }

    private suspend fun buildAttendeeDocument(event: Event): ExportDocument? {
        val profile = when (val result = userRepository.getProfile(userId)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> {
                failWith(result.error)
                return null
            }
        }

        val checkIns = when (
            val result = attendanceRepository.getCheckIns(eventId = eventId, userId = userId)
        ) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> {
                failWith(result.error)
                return null
            }
        }

        val records = event.attendanceHistory(checkIns = checkIns, now = LocalDateTime.now())
        val attendee = AttendeeAttendance(
            profile = profile,
            records = records,
            summary = records.summarize()
        )
        _state.update { it.copy(attendee = attendee) }
        return AttendanceExportBuilder.forAttendee(event = event, attendee = attendee)
    }

    private fun failWith(error: DataError) {
        _state.update { it.copy(isLoading = false, error = error) }
    }

    private fun writeAndRender() {
        val document = _state.value.document ?: return
        val format = _state.value.format

        _state.update { it.copy(isRendering = true, writeFailed = false, pages = emptyList()) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Only the newest export is ever shared, so older ones are cleared
            // rather than left piling up in the cache.
            AttendanceExporter.clearPrevious(context)

            val exported = AttendanceExporter.export(
                context = context,
                document = document,
                format = format
            )
            if (exported == null) {
                _state.update { it.copy(isRendering = false, writeFailed = true) }
                return@launch
            }

            val pages = if (format == ExportFormat.PDF) {
                PdfPageRenderer.render(exported.file)
            } else {
                emptyList()
            }
            _state.update { it.copy(isRendering = false, exported = exported, pages = pages) }
        }
    }

    companion object {
        fun factory(eventId: String, userId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ExportPreviewViewModel(
                        application = checkNotNull(get(APPLICATION_KEY)),
                        eventId = eventId,
                        userId = userId
                    )
                }
            }
    }
}
