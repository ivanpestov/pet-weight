package ru.sferadevelop.weighly.ui.form

/**
 * What the entry form shows: [weight] as typed, [date] as `2026-09-30`. Both are display-ready;
 * the form does no formatting of its own. [saved] turns true once the Record is written, which is
 * what sends the screen back to the History. [error], when set, is shown as a modal message.
 */
data class FormUiState(
    val weight: String,
    val date: String,
    val saved: Boolean = false,
    val error: FormError? = null
)

/** Why a Weight was refused on Save. The screen names each one from resources. */
enum class FormError {
    /** Nothing was typed. */
    EMPTY_WEIGHT,

    /** A Weight no bathroom scale could produce — usually a typo such as an extra zero. */
    WEIGHT_OUT_OF_RANGE
}
