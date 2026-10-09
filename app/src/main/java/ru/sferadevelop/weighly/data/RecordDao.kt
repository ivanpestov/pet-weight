package ru.sferadevelop.weighly.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records ORDER BY epoch_day DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE epoch_day = :epochDay")
    suspend fun findOn(epochDay: Long): RecordEntity?

    @Query("SELECT * FROM records WHERE epoch_day <= :epochDay ORDER BY epoch_day DESC LIMIT 1")
    suspend fun findLatestNotAfter(epochDay: Long): RecordEntity?

    /** Replacing on conflict gives an upsert without branching in the repository. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RecordEntity)

    /** Room wraps a single multi-row @Insert in a transaction, which is what an Import needs. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<RecordEntity>)

    @Query("DELETE FROM records WHERE epoch_day = :epochDay")
    suspend fun deleteOn(epochDay: Long)

    /**
     * Moves a Record onto another day in one transaction: a half-applied move would leave the
     * same Record on two dates, or on none.
     */
    @Transaction
    suspend fun move(fromEpochDay: Long, record: RecordEntity) {
        deleteOn(fromEpochDay)
        insert(record)
    }
}
