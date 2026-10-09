package ru.sferadevelop.weighly

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import java.time.LocalDate

/**
 * In-memory [WeightRepository] with the same date-uniqueness semantics as the Room-backed one.
 * Shared by every ViewModel test.
 */
class FakeWeightRepository(initialRecords: List<Record> = emptyList()) : WeightRepository {

    private val stored = MutableStateFlow(initialRecords.associateBy(Record::date))

    override fun records(): Flow<List<Record>> =
        stored.map { records -> records.values.sortedByDescending(Record::date) }

    override suspend fun recordOn(date: LocalDate): Record? = stored.value[date]

    override suspend fun latestRecordNotAfter(date: LocalDate): Record? =
        stored.value.values.filter { !it.date.isAfter(date) }.maxByOrNull(Record::date)

    override suspend fun save(record: Record) {
        stored.update { records -> records + (record.date to record) }
    }

    override suspend fun saveAll(records: List<Record>) {
        stored.update { stored -> stored + records.associateBy(Record::date) }
    }

    override suspend fun move(from: LocalDate, record: Record) {
        stored.update { records -> records - from + (record.date to record) }
    }

    override suspend fun deleteOn(date: LocalDate) {
        stored.update { records -> records - date }
    }
}
