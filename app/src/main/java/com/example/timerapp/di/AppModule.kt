package com.example.timerapp.di

import android.content.Context
import com.example.timerapp.SettingsManager
import com.example.timerapp.SupabaseClient
import com.example.timerapp.repository.TimerRepository
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
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTimerRepository(supabaseClient: SupabaseClientType): TimerRepository {
        return TimerRepository(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AlarmScheduler(context)
    }
}
