package ru.sferadevelop.weighly.domain

import java.time.LocalDate

/**
 * A body-mass figure fixed against one calendar date. The date is the natural key: there is at
 * most one Record per date (ADR-0001).
 *
 * The Weight is held in grams so that one decimal place of a kilogram is exact and two Records
 * can be compared for equality.
 */
data class Record(
    val date: LocalDate,
    val grams: Int
)
