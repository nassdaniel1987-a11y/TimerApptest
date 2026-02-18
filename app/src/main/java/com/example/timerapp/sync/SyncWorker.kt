package com.example.timerapp.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "🔄 Periodischer Sync gestartet")
            syncManager.processPendingSync()
            Log.d("SyncWorker", "✅ Periodischer Sync abgeschlossen")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Sync fehlgeschlagen: ${e.message}")
            Result.retry()
        }
    }
}
