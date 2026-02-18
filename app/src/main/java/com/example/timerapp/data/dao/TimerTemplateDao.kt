package com.example.timerapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.timerapp.models.TimerTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerTemplateDao {

    @Query("SELECT * FROM timer_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TimerTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TimerTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TimerTemplate>)

    @Query("DELETE FROM timer_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("DELETE FROM timer_templates")
    suspend fun deleteAllTemplates()
}
