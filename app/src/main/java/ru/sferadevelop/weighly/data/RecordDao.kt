package ru.sferadevelop.weighly.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM records WHERE epoch_day = :epochDay")
    suspend fun deleteOn(epochDay: Long)
}
