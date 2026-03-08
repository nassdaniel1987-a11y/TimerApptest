package com.example.timerapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.timerapp.data.dao.CategoryDao
import com.example.timerapp.data.dao.PendingSyncDao
import com.example.timerapp.data.dao.QRCodeDao
import com.example.timerapp.data.dao.TimerDao
import com.example.timerapp.data.dao.TimerTemplateDao
import com.example.timerapp.data.entity.PendingSyncEntity
import com.example.timerapp.models.Category
import com.example.timerapp.models.QRCodeData
import com.example.timerapp.models.Timer
import com.example.timerapp.models.TimerTemplate

@Database(
    entities = [
        Timer::class,
        Category::class,
        TimerTemplate::class,
        QRCodeData::class,
        PendingSyncEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timers ADD COLUMN source_device_id TEXT DEFAULT NULL")
            }
        }
    }
    abstract fun timerDao(): TimerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timerTemplateDao(): TimerTemplateDao
    abstract fun qrCodeDao(): QRCodeDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
