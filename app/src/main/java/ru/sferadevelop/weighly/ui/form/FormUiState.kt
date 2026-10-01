package ru.sferadevelop.weighly.ui.form

/**
 * What the entry form shows: [weight] as typed, [date] as `2026-09-30`. Both are display-ready;
 * the form does no formatting of its own. [saved] turns true once the Record is written, which is
 * what sends the screen back to the History.
 */
data class FormUiState(
    val weight: String,
    val date: String,
    val saved: Boolean = false
)
