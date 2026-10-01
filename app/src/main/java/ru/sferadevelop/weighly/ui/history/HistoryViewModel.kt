package ru.sferadevelop.weighly.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.sferadevelop.weighly.weighlyApplication
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import ru.sferadevelop.weighly.ui.weightToDisplay

class HistoryViewModel(repository: WeightRepository) : ViewModel() {

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

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                HistoryViewModel(weighlyApplication().container.weightRepository)
            }
        }
    }
}

private fun Record.toRow() = RecordRow(
    date = date.toString(),
    weight = weightToDisplay(grams)
)
