package ru.sferadevelop.weighly.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** The narrow seam a fake cannot reproduce: real SQLite behaviour behind [RecordDao]. */
@RunWith(AndroidJUnit4::class)
class RecordDaoTest {

    private lateinit var database: WeighlyDatabase
    private lateinit var dao: RecordDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WeighlyDatabase::class.java
        ).build()
        dao = database.recordDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertingOnAnOccupiedRecordDateReplacesRatherThanDuplicates() = runBlocking {
        dao.insert(entity("2026-09-30", grams = 72_400))
        dao.insert(entity("2026-09-30", grams = 71_900))

        val records = dao.observeAll().first()

        assertEquals(1, records.size)
        assertEquals(71_900, records.single().grams)
    }

    @Test
    fun recordsAreStreamedFromTheLatestRecordDateToTheEarliest() = runBlocking {
        dao.insert(entity("2026-09-28", grams = 72_400))
        dao.insert(entity("2026-09-30", grams = 71_900))
        dao.insert(entity("2026-09-29", grams = 72_100))

        val records = dao.observeAll().first()

        assertEquals(
            listOf("2026-09-30", "2026-09-29", "2026-09-28"),
            records.map { LocalDate.ofEpochDay(it.epochDay).toString() }
        )
    }

    @Test
    fun deletingByRecordDateRemovesExactlyOneRecord() = runBlocking {
        dao.insert(entity("2026-09-29", grams = 72_100))
        dao.insert(entity("2026-09-30", grams = 71_900))

        dao.deleteOn(LocalDate.parse("2026-09-30").toEpochDay())

        val records = dao.observeAll().first()
        assertEquals(1, records.size)
        assertEquals("2026-09-29", LocalDate.ofEpochDay(records.single().epochDay).toString())
        assertNull(dao.findOn(LocalDate.parse("2026-09-30").toEpochDay()))
    }

    @Test
    fun movingARecordOntoAnOccupiedRecordDateLeavesOneRecordThere() = runBlocking {
        dao.insert(entity("2026-09-30", grams = 71_900))
        dao.insert(entity("2026-09-25", grams = 72_100))

        dao.move(
            fromEpochDay = LocalDate.parse("2026-09-30").toEpochDay(),
            record = entity("2026-09-25", grams = 71_900)
        )

        val records = dao.observeAll().first()
        assertEquals(1, records.size)
        assertEquals("2026-09-25", LocalDate.ofEpochDay(records.single().epochDay).toString())
        assertEquals(71_900, records.single().grams)
        assertNull(dao.findOn(LocalDate.parse("2026-09-30").toEpochDay()))
    }

    @Test
    fun aReadAfterAWriteReturnsTheSameWeightInGrams() = runBlocking {
        dao.insert(entity("2026-09-30", grams = 72_450))

        val stored = dao.findOn(LocalDate.parse("2026-09-30").toEpochDay())

        assertEquals(72_450, stored?.grams)
    }

    private fun entity(date: String, grams: Int) =
        RecordEntity(epochDay = LocalDate.parse(date).toEpochDay(), grams = grams)
}
