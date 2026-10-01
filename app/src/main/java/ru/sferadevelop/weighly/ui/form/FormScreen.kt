package ru.sferadevelop.weighly.ui.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.sferadevelop.weighly.R
import ru.sferadevelop.weighly.ui.filterTypedWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun FormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormViewModel = viewModel(factory = FormViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The write runs on the ViewModel's scope; the screen leaves once it reports the Record saved.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    FormScreen(
        uiState = uiState,
        onWeightTyped = viewModel::onWeightTyped,
        onDatePicked = viewModel::onDatePicked,
        onSave = viewModel::save,
        onCancel = onDone,
        onDismissError = viewModel::dismissError,
        onConfirmReplacement = viewModel::confirmReplacement,
        onDeclineReplacement = viewModel::declineReplacement,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormScreen(
    uiState: FormUiState,
    onWeightTyped: (String) -> Unit,
    onDatePicked: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDismissError: () -> Unit,
    onConfirmReplacement: () -> Unit,
    onDeclineReplacement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var calendarOpen by rememberSaveable { mutableStateOf(false) }

    // The field owns the caret; the Weight itself comes from the ViewModel. When the Weight
    // changes under the field — the prefill arriving — the caret goes to its end, ready for a
    // correction.
    var field by remember {
        mutableStateOf(TextFieldValue(uiState.weight, TextRange(uiState.weight.length)))
    }
    LaunchedEffect(uiState.weight) {
        if (uiState.weight != field.text) {
            field = TextFieldValue(uiState.weight, TextRange(uiState.weight.length))
        }
    }

    // The keyboard is up and the caret is in the field before the first tap: entry happens while
    // standing on the scale.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.form_title_new)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(CONTENT_PADDING)
        ) {
            OutlinedButton(onClick = { calendarOpen = true }) {
                Text(uiState.date)
            }
            TextField(
                value = field,
                onValueChange = { typed ->
                    // The field has to apply the filter itself, not wait to be corrected: a
                    // refused character leaves the ViewModel's Weight unchanged, and an unchanged
                    // Weight cannot push anything back. The caret steps back over what was
                    // refused instead of jumping to the end of the line.
                    val kept = filterTypedWeight(typed.text)
                    val refused = typed.text.length - kept.length
                    val caret = (typed.selection.end - refused).coerceIn(0, kept.length)
                    field = TextFieldValue(text = kept, selection = TextRange(caret))
                    onWeightTyped(typed.text)
                },
                label = { Text(stringResource(R.string.form_weight_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = onSave) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }

        // The message lives in screen state, so dismissing it leaves the form exactly as it was.
        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = onDismissError,
                text = { Text(stringResource(error.messageId)) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }

        // ADR-0001: a Record on an occupied Record Date is replaced only after this is confirmed.
        uiState.replacement?.let { replacement ->
            AlertDialog(
                onDismissRequest = onDeclineReplacement,
                text = {
                    Text(
                        stringResource(
                            R.string.replace_record_message,
                            uiState.date,
                            stringResource(R.string.weight_with_unit, replacement.weight)
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmReplacement) {
                        Text(stringResource(R.string.action_replace))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDeclineReplacement) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (calendarOpen) {
            RecordDateCalendar(
                epochDay = uiState.epochDay,
                onPicked = {
                    onDatePicked(it)
                    calendarOpen = false
                },
                onDismiss = { calendarOpen = false }
            )
        }
    }
}

/** A calendar open on the Record Date being saved against, with no date it refuses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDateCalendar(
    epochDay: Long,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = rememberDatePickerState(
        initialSelectedDateMillis = epochDay * MILLIS_PER_DAY
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = calendar.selectedDateMillis?.toLocalDate()
                    if (picked == null) onDismiss() else onPicked(picked)
                }
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = calendar)
    }
}

/** The calendar answers in UTC milliseconds; a Record Date is a day, with no time of day. */
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@get:StringRes
private val FormError.messageId: Int
    get() = when (this) {
        FormError.EMPTY_WEIGHT -> R.string.error_enter_weight
        FormError.WEIGHT_OUT_OF_RANGE -> R.string.error_weight_out_of_range
    }

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
private val CONTENT_PADDING = 16.dp
