package com.example.timerapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.timerapp.data.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Query("SELECT * FROM pending_sync ORDER BY created_at ASC")
    suspend fun getAllPending(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun getPendingCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: PendingSyncEntity)

    @Delete
    suspend fun delete(entity: PendingSyncEntity)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()
}
