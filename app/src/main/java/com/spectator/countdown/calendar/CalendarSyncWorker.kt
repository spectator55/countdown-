package com.spectator.countdown.calendar

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.spectator.countdown.CountdownApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CalendarSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CountdownApplication
        if (!app.preferences.autoSyncEnabled.first() || !CalendarPermission.granted(applicationContext)) {
            return Result.success()
        }
        return try {
            app.synchronizer.synchronize()
            Result.success()
        } catch (_: SecurityException) {
            // Runtime permission may have been revoked while the provider was being read.
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}

object CalendarSyncScheduler {
    private const val UNIQUE_NAME = "selected-google-calendars"

    fun setEnabled(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (enabled) {
            manager.enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<CalendarSyncWorker>(6, TimeUnit.HOURS).build()
            )
        } else manager.cancelUniqueWork(UNIQUE_NAME)
    }
}
