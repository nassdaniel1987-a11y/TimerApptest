package com.example.timerapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.timerapp.models.Timer
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers ORDER BY target_time ASC")
    fun getAllTimers(): Flow<List<Timer>>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getTimerById(id: String): Timer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: Timer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimers(timers: List<Timer>)

    @Update
    suspend fun updateTimer(timer: Timer)

    @Query("UPDATE timers SET is_completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimer(id: String)

    @Query("DELETE FROM timers WHERE is_completed = 1 AND target_time < :cutoffTimeStr")
    suspend fun deleteOldCompletedTimers(cutoffTimeStr: String): Int

    @Query("SELECT id FROM timers WHERE is_completed = 1 AND target_time < :cutoffTimeStr")
    suspend fun getOldCompletedTimerIds(cutoffTimeStr: String): List<String>

    @Query("DELETE FROM timers")
    suspend fun deleteAllTimers()

    @Query("SELECT * FROM timers WHERE is_completed = 0 ORDER BY target_time ASC LIMIT 10")
    suspend fun getActiveTimersForWidget(): List<Timer>

    @Transaction
    suspend fun replaceAllTimers(timers: List<Timer>) {
        deleteAllTimers()
        insertTimers(timers)
    }
}
