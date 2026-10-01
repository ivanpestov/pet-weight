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
import ru.sferadevelop.weighly.ui.FormRoute
import ru.sferadevelop.weighly.ui.filterTypedWeight
import ru.sferadevelop.weighly.ui.weightFromDisplay
import ru.sferadevelop.weighly.ui.weightToDisplay
import ru.sferadevelop.weighly.weighlyApplication
import java.time.LocalDate

class FormViewModel(
    private val repository: WeightRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * The Record Date being written against. The route's date when it carries one, today
     * otherwise, and whatever the calendar picks afterwards.
     */
    private val recordDate: StateFlow<Long> = savedStateHandle.getStateFlow(
        RECORD_DATE_KEY,
        savedStateHandle.get<Long>(EPOCH_DAY_KEY) ?: LocalDate.now().toEpochDay()
    )

    private val typedWeight: StateFlow<String> = savedStateHandle.getStateFlow(WEIGHT_KEY, "")

    private val saved = MutableStateFlow(false)

    private val error = MutableStateFlow<FormError?>(null)

    private val replacement = MutableStateFlow<Replacement?>(null)

    /** The Record this Form was opened on, which saving over is not a replacement. */
    private val editedRecordDate: LocalDate? =
        savedStateHandle.get<Long>(EPOCH_DAY_KEY)?.let(LocalDate::ofEpochDay)

    /** The Weight the raised warning would write, already read and judged. */
    private var warnedGrams: Int? = null

    /** Guards a second tap on Save while the first write is still in flight. */
    private var writing = false

    /** Set once the Form has been typed into, so a late prefill cannot land on top. */
    private var typedInto = false

    val uiState: StateFlow<FormUiState> =
        combine(
            typedWeight,
            recordDate,
            saved,
            error,
            replacement
        ) { weight, date, saved, error, replacement ->
            FormUiState(
                weight = weight,
                date = LocalDate.ofEpochDay(date).toString(),
                epochDay = date,
                editing = editedRecordDate != null,
                saved = saved,
                error = error,
                replacement = replacement
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FormUiState(
                weight = typedWeight.value,
                date = recordDateValue().toString(),
                epochDay = recordDate.value,
                editing = editedRecordDate != null
            )
        )

    init {
        prefillLastWeight()
    }

    private fun recordDateValue(): LocalDate = LocalDate.ofEpochDay(recordDate.value)

    /**
     * Opens the field on the Weight the Form is about: the Record being edited, or - when one is
     * being created - the Record with the greatest Record Date not after today, so that daily
     * entry is a correction of a digit or two. A Record dated in the future is passed over: an
     * accidental one would otherwise poison entry every day.
     *
     * Runs once per Form rather than once per ViewModel: after recreation the field carries
     * whatever was typed, which may deliberately be nothing.
     */
    private fun prefillLastWeight() {
        if (savedStateHandle.get<Boolean>(PREFILLED_KEY) == true) return
        viewModelScope.launch {
            val openedOn = editedRecordDate
            val record = if (openedOn == null) {
                repository.latestRecordNotAfter(LocalDate.now())
            } else {
                repository.recordOn(openedOn)
            }
            if (record != null && !typedInto && typedWeight.value.isEmpty()) {
                savedStateHandle[WEIGHT_KEY] = weightToDisplay(record.grams)
            }
            // Marked only once the answer is in: a process death while waiting would otherwise
            // leave a Form that never prefills again.
            savedStateHandle[PREFILLED_KEY] = true
        }
    }

    /** Keeps only what may be a Weight; judging it waits for Save. */
    fun onWeightTyped(weight: String) {
        typedInto = true
        savedStateHandle[WEIGHT_KEY] = filterTypedWeight(weight)
    }

    /** Moves the Form onto [date]: the one gesture that enters a backdated Record. */
    fun onDatePicked(date: LocalDate) {
        savedStateHandle[RECORD_DATE_KEY] = date.toEpochDay()
    }

    /**
     * Writes the typed Weight against the Record Date, or reports why it will not: an impossible
     * Weight, or a Record already stored on that date, which ADR-0001 says is replaced only after
     * the warning is confirmed.
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
                    val occupant = repository.recordOn(recordDateValue())
                    if (occupant == null || recordDateValue() == editedRecordDate) {
                        write(grams)
                    } else {
                        warnedGrams = grams
                        replacement.value = Replacement(weight = weightToDisplay(occupant.grams))
                    }
                }
            }
        }
    }

    /** Replaces the Record on that Record Date: the deliberate way to correct an old Weight. */
    fun confirmReplacement() {
        val grams = warnedGrams ?: return declineReplacement()
        replacement.value = null
        viewModelScope.launch { write(grams) }
    }

    /** Leaves the Form as it was, so a different Record Date can be picked. */
    fun declineReplacement() {
        warnedGrams = null
        replacement.value = null
        writing = false
    }

    private suspend fun write(grams: Int) {
        writing = true
        val record = Record(date = recordDateValue(), grams = grams)
        val movedFrom = editedRecordDate?.takeIf { it != record.date }
        if (movedFrom == null) {
            repository.save(record)
        } else {
            repository.move(from = movedFrom, record = record)
        }
        saved.value = true
    }

    /**
     * Takes the message off the screen, leaving the typed text and the Record Date as they were.
     */
    fun dismissError() {
        error.value = null
    }

    companion object {
        /** The optional Record Date in the Form's route, as an epoch day. */
        private val EPOCH_DAY_KEY = FormRoute::epochDay.name

        private const val WEIGHT_KEY = "weight"
        private const val RECORD_DATE_KEY = "recordEpochDay"
        private const val PREFILLED_KEY = "prefilled"

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
