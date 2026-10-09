package ru.sferadevelop.weighly.ui.history

import java.time.LocalDate

private const val CSV_HEADER = "date,weight_kg"

/**
 * Renders [rows] as CSV, oldest Record Date first: a file read for a trend over time, the
 * reverse of how the History displays (latest Record Date first).
 */
fun historyCsv(rows: List<RecordRow>): String {
    val lines = rows.asReversed().map { "${it.date},${it.weight}" }
    return (listOf(CSV_HEADER) + lines).joinToString("\n")
}

/** Names an export taken on [today], so exports on different days never collide. */
fun exportFileName(today: LocalDate): String = "weighly-export-$today.csv"
