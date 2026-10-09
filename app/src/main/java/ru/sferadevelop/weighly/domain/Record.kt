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

/**
 * The Weights a bathroom scale can produce, in grams: 1.0 kg to 500.0 kg, boundaries included.
 * A property of a Weight rather than of any one screen, so the entry Form and an Import judge a
 * figure by the same rule.
 */
val WEIGHT_RANGE_GRAMS: IntRange = 1_000..500_000
