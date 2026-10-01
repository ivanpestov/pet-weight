package ru.sferadevelop.weighly.ui.form

/**
 * What the entry form shows: [weight] as typed, [date] as `2026-09-30`. Both are display-ready;
 * the form does no formatting of its own. [epochDay] is the same Record Date as a date rather
 * than a label, which is what the calendar opens on. [saved] turns true once the Record is
 * written, which is what sends the screen back to the History. [error], when set, is shown as a
 * modal message, and so is [replacement].
 */
data class FormUiState(
    val weight: String,
    val date: String,
    val epochDay: Long,
    val saved: Boolean = false,
    val error: FormError? = null,
    val replacement: Replacement? = null
)

/**
 * The Record that Save would overwrite, carrying its [weight] in kilograms to one decimal place
 * so the warning can name what is at stake. Its Record Date is the Form's own: a Record is only
 * ever looked up on the date being saved against.
 */
data class Replacement(
    val weight: String
)

/** Why a Weight was refused on Save. The screen names each one from resources. */
enum class FormError {
    /** Nothing was typed. */
    EMPTY_WEIGHT,

    /** A Weight no bathroom scale could produce — usually a typo such as an extra zero. */
    WEIGHT_OUT_OF_RANGE
}
