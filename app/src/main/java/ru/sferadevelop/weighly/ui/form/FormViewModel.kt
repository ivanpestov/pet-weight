package ru.sferadevelop.weighly.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import ru.sferadevelop.weighly.ui.weightFromDisplay
import ru.sferadevelop.weighly.weighlyApplication
import java.time.LocalDate

class FormViewModel(
    private val repository: WeightRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** The Record Date being written to. Absent in the route means a Record for today. */
    private val recordDate: LocalDate =
        savedStateHandle.get<Long>(EPOCH_DAY_KEY)?.let(LocalDate::ofEpochDay) ?: LocalDate.now()

    private val typedWeight: StateFlow<String> = savedStateHandle.getStateFlow(WEIGHT_KEY, "")

    private val saved = MutableStateFlow(false)

    /** Guards a second tap on Save while the first write is still in flight. */
    private var writing = false

    val uiState: StateFlow<FormUiState> = combine(typedWeight, saved) { weight, saved ->
        FormUiState(weight = weight, date = recordDate.toString(), saved = saved)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FormUiState(weight = typedWeight.value, date = recordDate.toString())
    )

    fun onWeightTyped(weight: String) {
        savedStateHandle[WEIGHT_KEY] = weight
    }

    /**
     * Writes the typed Weight against the Record Date. Text that is not a Weight writes nothing.
     * The write runs on [viewModelScope], so leaving the screen cannot cancel it halfway.
     */
    fun save() {
        if (writing) return
        val grams = weightFromDisplay(typedWeight.value) ?: return
        writing = true
        viewModelScope.launch {
            repository.save(Record(date = recordDate, grams = grams))
            saved.value = true
        }
    }

    companion object {
        /** The optional Record Date in the Form's route, as an epoch day. */
        private const val EPOCH_DAY_KEY = "epochDay"

        private const val WEIGHT_KEY = "weight"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                FormViewModel(
                    repository = weighlyApplication().container.weightRepository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
