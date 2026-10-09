package ru.sferadevelop.weighly.ui.history

/** What the History screen shows. Loading and Empty are deliberately distinct. */
sealed interface HistoryUiState {

    /** Storage has not answered yet. */
    data object Loading : HistoryUiState

    /** Storage answered, and it holds no Records. */
    data object Empty : HistoryUiState

    /** Storage answered with Records, latest Record Date first. */
    data class History(val rows: List<RecordRow>) : HistoryUiState
}

/**
 * One History row, display-ready: [date] as `2026-09-30` and [weight] in kilograms to one decimal
 * place, without a unit. The screen appends the unit from resources. [epochDay] is the same
 * Record Date as a date rather than a label, which is what opening the Record for editing needs.
 */
data class RecordRow(
    val epochDay: Long,
    val date: String,
    val weight: String
)

/**
 * What an Import would do, offered for confirmation before anything is written (ADR-0004):
 * [adding] dates the History does not hold yet, [replacing] dates it does, and [skipped] lines
 * the file got wrong.
 */
data class ImportPreview(
    val adding: Int,
    val replacing: Int,
    val skipped: Int
)

/** How an Import ended, for the message that reports it. */
sealed interface ImportResult {

    /** [count] Records were written. */
    data class Imported(val count: Int) : ImportResult

    /** Nothing was written: the file named no Record this app can read. */
    data object Failed : ImportResult
}
