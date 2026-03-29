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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timers ADD COLUMN source_device_id TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Timer Template Umstellungen
                // Normalerweise müsste man hier die Spalte 'defaultTime' in 'default_time' umbenennen.
                // Aber da SQLite 'RENAME COLUMN' nicht in allen alten Versionen glatt unterstützt (und TimerTemplates lokal noch gar nicht produktiv genutzt wurden),
                // machen wir einen Drop/Create, oder sicherer: einfach komplett neu anlegen (die Tabelle war ja vorher lokal aber ungenutzt und leer).
                // Hier versuchen wir den sauberen SQLite 3.25+ 'RENAME COLUMN' (Android API 30+) bzw. Workaround.
                // Sicherster Weg für lokale ungenutzte Tabellen: recreate
                db.execSQL("DROP TABLE IF EXISTS timer_templates")
                db.execSQL("CREATE TABLE timer_templates (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, default_time TEXT NOT NULL, category TEXT NOT NULL, note TEXT, created_at TEXT NOT NULL DEFAULT '', klasse TEXT, source_device_id TEXT)")
            }
        }
    }
    abstract fun timerDao(): TimerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timerTemplateDao(): TimerTemplateDao
    abstract fun qrCodeDao(): QRCodeDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
