package com.example.timerapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.timerapp.sync.SyncManager
import com.example.timerapp.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TimerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Netzwerk-Monitoring starten
        syncManager.startMonitoring()
        Log.d("TimerApplication", "✅ SyncManager gestartet")

        // Periodischen Sync-Worker schedulen (alle 15 Minuten, nur mit Netzwerk)
        scheduleSyncWorker()
    }

    private fun scheduleSyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        Log.d("TimerApplication", "✅ SyncWorker scheduled (alle 15 Min)")
    }
}
