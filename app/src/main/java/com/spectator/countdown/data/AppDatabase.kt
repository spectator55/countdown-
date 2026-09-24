package com.spectator.countdown.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetEpochMillis ASC")
    fun observeAll(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CountdownEntity?

    @Query("SELECT * FROM countdowns WHERE sourceKey IS NOT NULL")
    suspend fun imported(): List<CountdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(event: CountdownEntity)

    @Delete
    suspend fun delete(event: CountdownEntity)
}

@Dao
interface CalendarSelectionDao {
    @Query("SELECT * FROM calendar_selections")
    suspend fun all(): List<CalendarSelectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(selections: List<CalendarSelectionEntity>)
}

@Dao
interface HiddenImportDao {
    @Query("SELECT sourceKey FROM hidden_imports")
    suspend fun allKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(hidden: HiddenImportEntity)

    @Query("DELETE FROM hidden_imports")
    suspend fun clear()
}

@Database(
    entities = [CountdownEntity::class, CalendarSelectionEntity::class, HiddenImportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun countdowns(): CountdownDao
    abstract fun selections(): CalendarSelectionDao
    abstract fun hiddenImports(): HiddenImportDao
}
