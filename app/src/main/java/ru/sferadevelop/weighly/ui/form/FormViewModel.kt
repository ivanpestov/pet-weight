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
import ru.sferadevelop.weighly.ui.filterTypedWeight
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

    private val error = MutableStateFlow<FormError?>(null)

    /** Guards a second tap on Save while the first write is still in flight. */
    private var writing = false

    val uiState: StateFlow<FormUiState> =
        combine(typedWeight, saved, error) { weight, saved, error ->
            FormUiState(
                weight = weight,
                date = recordDate.toString(),
                saved = saved,
                error = error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FormUiState(weight = typedWeight.value, date = recordDate.toString())
        )

    /** Keeps only what may be a Weight; judging it waits for Save. */
    fun onWeightTyped(weight: String) {
        savedStateHandle[WEIGHT_KEY] = filterTypedWeight(weight)
    }

    /**
     * Writes the typed Weight against the Record Date, or reports why it will not. The write runs
     * on [viewModelScope], so leaving the screen cannot cancel it halfway.
     */
    fun save() {
        if (writing) return
        error.value = null

        val typed = typedWeight.value
        val grams = weightFromDisplay(typed)
        when {
            typed.none(Char::isDigit) -> error.value = FormError.EMPTY_WEIGHT
            grams == null || grams !in MIN_GRAMS..MAX_GRAMS ->
                error.value = FormError.WEIGHT_OUT_OF_RANGE

            else -> {
                writing = true
                viewModelScope.launch {
                    repository.save(Record(date = recordDate, grams = grams))
                    saved.value = true
                }
            }
        }
    }

    /**
     * Takes the message off the screen, leaving the typed text and the Record Date as they were.
     */
    fun dismissError() {
        error.value = null
    }

    companion object {
        /** The optional Record Date in the Form's route, as an epoch day. */
        private const val EPOCH_DAY_KEY = "epochDay"

        private const val WEIGHT_KEY = "weight"

        /** The Weights a bathroom scale can produce: 1.0 kg to 500.0 kg, boundaries included. */
        private const val MIN_GRAMS = 1_000
        private const val MAX_GRAMS = 500_000

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
