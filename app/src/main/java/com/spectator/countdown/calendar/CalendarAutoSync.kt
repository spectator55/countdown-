package com.spectator.countdown.calendar

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.spectator.countdown.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Observes provider changes while this process is alive. WorkManager covers background time;
 * on resume, the UI also calls permissionChanged() to handle a permission granted in Settings. */
class CalendarAutoSync(
    private val context: Context,
    private val preferences: AppPreferences,
    private val synchronizer: CalendarSynchronizer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observer: ContentObserver? = null
    private var pending: Job? = null

    init {
        scope.launch {
            preferences.autoSyncEnabled.collectLatest { enabled ->
                CalendarSyncScheduler.setEnabled(context, enabled)
                if (enabled) {
                    ensureObserver()
                    if (CalendarPermission.granted(context)) safelySync()
                } else {
                    pending?.cancel()
                    observer?.let(context.contentResolver::unregisterContentObserver)
                    observer = null
                }
            }
        }
    }

    fun permissionChanged() {
        scope.launch {
            if (preferences.autoSyncEnabled.first() && CalendarPermission.granted(context)) {
                ensureObserver()
                safelySync()
            }
        }
    }

    private fun ensureObserver() {
        if (observer != null || !CalendarPermission.granted(context)) return
        val watcher = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                pending?.cancel()
                pending = scope.launch {
                    delay(1_000) // Coalesce multi-row provider updates.
                    safelySync()
                }
            }
        }
        try {
            context.contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, watcher)
            context.contentResolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, watcher)
            observer = watcher
        } catch (_: SecurityException) {
            context.contentResolver.unregisterContentObserver(watcher)
        }
    }

    private suspend fun safelySync() {
        if (CalendarPermission.granted(context)) {
            try {
                synchronizer.synchronize()
            } catch (_: Exception) {
                // The UI has the failure state; WorkManager retries later.
            }
        }
    }
}
