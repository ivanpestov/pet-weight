package ru.sferadevelop.weighly.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecordEntity::class], version = 1, exportSchema = true)
abstract class WeighlyDatabase : RoomDatabase() {

    abstract fun recordDao(): RecordDao

    companion object {
        private const val NAME = "weighly.db"

        fun create(context: Context): WeighlyDatabase =
            Room.databaseBuilder(context.applicationContext, WeighlyDatabase::class.java, NAME)
                .build()
    }
}
