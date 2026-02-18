package com.example.timerapp.di

import android.content.Context
import androidx.room.Room
import com.example.timerapp.SettingsManager
import com.example.timerapp.SupabaseClient
import com.example.timerapp.data.AppDatabase
import com.example.timerapp.data.dao.CategoryDao
import com.example.timerapp.data.dao.PendingSyncDao
import com.example.timerapp.data.dao.QRCodeDao
import com.example.timerapp.data.dao.TimerDao
import com.example.timerapp.data.dao.TimerTemplateDao
import com.example.timerapp.repository.TimerRepository
import com.example.timerapp.sync.SyncManager
import com.example.timerapp.utils.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import io.github.jan.supabase.SupabaseClient as SupabaseClientType

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClientType {
        return SupabaseClient.client
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "timer_database"
        ).build()
    }

    @Provides
    fun provideTimerDao(db: AppDatabase): TimerDao = db.timerDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTimerTemplateDao(db: AppDatabase): TimerTemplateDao = db.timerTemplateDao()

    @Provides
    fun provideQRCodeDao(db: AppDatabase): QRCodeDao = db.qrCodeDao()

    @Provides
    fun providePendingSyncDao(db: AppDatabase): PendingSyncDao = db.pendingSyncDao()

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTimerRepository(
        supabaseClient: SupabaseClientType,
        timerDao: TimerDao,
        categoryDao: CategoryDao,
        templateDao: TimerTemplateDao,
        qrCodeDao: QRCodeDao,
        pendingSyncDao: PendingSyncDao
    ): TimerRepository {
        return TimerRepository(supabaseClient, timerDao, categoryDao, templateDao, qrCodeDao, pendingSyncDao)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        supabaseClient: SupabaseClientType,
        pendingSyncDao: PendingSyncDao
    ): SyncManager {
        return SyncManager(context, supabaseClient, pendingSyncDao)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AlarmScheduler(context)
    }
}
