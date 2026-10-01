package ru.sferadevelop.weighly.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.sferadevelop.weighly.domain.Record
import java.time.LocalDate

/**
 * A Record as stored. The Record Date is the primary key, held as an epoch day: ordering and
 * range queries run on a numeric index and time zones play no part (ADR-0001).
 */
@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,
    @ColumnInfo(name = "grams")
    val grams: Int
)

fun RecordEntity.toRecord() = Record(date = LocalDate.ofEpochDay(epochDay), grams = grams)

fun Record.toEntity() = RecordEntity(epochDay = date.toEpochDay(), grams = grams)
