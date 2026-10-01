package ru.sferadevelop.weighly.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The single seam between the domain and storage. */
interface WeightRepository {

    /** The History: every Record from the latest Record Date to the earliest. */
    fun records(): Flow<List<Record>>

    /** The Record on [date], or null when that date holds none. */
    suspend fun recordOn(date: LocalDate): Record?

    /** The Record with the greatest Record Date not after [date], or null when there is none. */
    suspend fun latestRecordNotAfter(date: LocalDate): Record?

    /** Stores [record], replacing whatever Record its date already held. */
    suspend fun save(record: Record)

    /**
     * Moves a Record from [from] onto [record]'s own date as one operation: a Record edited onto
     * another day must not leave a copy of itself behind on the old one.
     */
    suspend fun move(from: LocalDate, record: Record)

    /** Removes the Record on [date]; does nothing when that date holds none. */
    suspend fun deleteOn(date: LocalDate)
}
