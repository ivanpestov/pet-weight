package ru.sferadevelop.weighly.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import java.time.LocalDate

class RoomWeightRepository(private val dao: RecordDao) : WeightRepository {

    override fun records(): Flow<List<Record>> =
        dao.observeAll().map { entities -> entities.map(RecordEntity::toRecord) }

    override suspend fun recordOn(date: LocalDate): Record? =
        dao.findOn(date.toEpochDay())?.toRecord()

    override suspend fun latestRecordNotAfter(date: LocalDate): Record? =
        dao.findLatestNotAfter(date.toEpochDay())?.toRecord()

    override suspend fun save(record: Record) = dao.insert(record.toEntity())

    override suspend fun deleteOn(date: LocalDate) = dao.deleteOn(date.toEpochDay())
}
