package ru.sferadevelop.weighly.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.sferadevelop.weighly.weighlyApplication
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import ru.sferadevelop.weighly.ui.weightToDisplay
import java.time.LocalDate

class HistoryViewModel(private val repository: WeightRepository) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.records()
        .map { records ->
            if (records.isEmpty()) {
                HistoryUiState.Empty
            } else {
                HistoryUiState.History(records.map(Record::toRow))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HistoryUiState.Loading
        )

    /**
     * Records deleted by a swipe, held by their Record Date until [undoDeletion] or
     * [finalizeDeletion] is called. Each swipe is tracked independently, since swiping a second
     * row before the first one's snackbar has expired must not cost the first its undo window.
     */
    private val pendingDeletions = mutableMapOf<Long, Record>()

    private val deletionsFlow = MutableSharedFlow<Long>(extraBufferCapacity = PENDING_CAPACITY)

    /** The Record Date of each Record a swipe just removed, one event per swipe. */
    val deletions: SharedFlow<Long> = deletionsFlow.asSharedFlow()

    /**
     * Deletes the Record on [epochDay] immediately and hard — no "deleted" flag enters the
     * schema. It is held until [undoDeletion] or [finalizeDeletion] is called for the same
     * [epochDay].
     */
    fun deleteRecord(epochDay: Long) {
        val date = LocalDate.ofEpochDay(epochDay)
        viewModelScope.launch {
            val record = repository.recordOn(date) ?: return@launch
            repository.deleteOn(date)
            pendingDeletions[epochDay] = record
            deletionsFlow.emit(epochDay)
        }
    }

    /** Re-inserts the Record held for [epochDay] with its original Weight: a genuine restore. */
    fun undoDeletion(epochDay: Long) {
        val record = pendingDeletions.remove(epochDay) ?: return
        viewModelScope.launch { repository.save(record) }
    }

    /** The undo window for [epochDay] has closed: the deletion stands, nothing is held for it. */
    fun finalizeDeletion(epochDay: Long) {
        pendingDeletions.remove(epochDay)
    }

    /** The current History as CSV, or null when there is none to export. */
    fun exportableCsv(): String? =
        (uiState.value as? HistoryUiState.History)?.rows?.let(::historyCsv)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** More swipes than fit here before a recomposition catches up would be unusual. */
        private const val PENDING_CAPACITY = 16

        val Factory = viewModelFactory {
            initializer {
                HistoryViewModel(weighlyApplication().container.weightRepository)
            }
        }
    }
}

private fun Record.toRow() = RecordRow(
    epochDay = date.toEpochDay(),
    date = date.toString(),
    weight = weightToDisplay(grams)
)
