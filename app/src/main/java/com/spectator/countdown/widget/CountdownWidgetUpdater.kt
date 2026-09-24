package com.spectator.countdown.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.spectator.countdown.MainActivity
import com.spectator.countdown.R
import com.spectator.countdown.core.CountdownMath
import com.spectator.countdown.core.CountdownMode
import com.spectator.countdown.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale

class CountdownWidgetUpdater(private val context: Context, private val db: AppDatabase) {
    private val settings = WidgetPreferences(context)

    suspend fun updateAll() {
        val manager = AppWidgetManager.getInstance(context)
        update(manager.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java)))
    }

    suspend fun update(ids: IntArray) = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context)
        ids.forEach { id ->
            // A deleted widget or a temporarily unavailable host must not abort an event save.
            try {
                val preference = settings.get(id)
                val event = preference?.let { db.countdowns().get(it.eventId) }
                val views = RemoteViews(context.packageName, R.layout.widget_countdown)
                if (event == null || preference == null) {
                    views.setTextViewText(R.id.widget_name, "Countdown")
                    views.setTextViewText(R.id.widget_status, "TAP TO CHOOSE AN EVENT")
                    views.setTextViewText(R.id.widget_value, "Your next moment")
                    views.setViewVisibility(R.id.widget_value, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                    views.setViewVisibility(R.id.widget_live_label, View.GONE)
                    views.setViewVisibility(R.id.widget_progress_row, View.GONE)
                    val config = Intent(context, WidgetConfigActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent(config, id))
                } else {
                    val now = Instant.now()
                    val reading = CountdownMath.read(
                        event.targetEpochMillis, now, preference.mode, preference.precisionDigits
                    )
                    views.setTextViewText(R.id.widget_name, event.name)
                    views.setTextViewText(R.id.widget_status, if (reading.upcoming) "ARRIVES IN" else "ALREADY")
                    views.setTextViewText(R.id.widget_value, reading.text)
                    val ticking = preference.mode == CountdownMode.HOURS_MINUTES_SECONDS ||
                        preference.mode == CountdownMode.FULL
                    views.setViewVisibility(R.id.widget_value,
                        if (preference.mode == CountdownMode.HOURS_MINUTES_SECONDS) View.GONE else View.VISIBLE)
                    views.setViewVisibility(R.id.widget_chronometer, if (ticking) View.VISIBLE else View.GONE)
                    views.setViewVisibility(R.id.widget_live_label,
                        if (preference.mode == CountdownMode.FULL) View.VISIBLE else View.GONE)
                    if (ticking) {
                        val diff = event.targetEpochMillis - System.currentTimeMillis()
                        val base = SystemClock.elapsedRealtime() + diff
                        views.setChronometer(R.id.widget_chronometer, base, null, true)
                        views.setChronometerCountDown(R.id.widget_chronometer, reading.upcoming)
                    }
                    views.setViewVisibility(R.id.widget_progress_row,
                        if (preference.showProgress) View.VISIBLE else View.GONE)
                    if (preference.showProgress) {
                        val progress = CountdownMath.progress(
                            now, event.progressStartEpochMillis, event.targetEpochMillis
                        )
                        views.setProgressBar(R.id.widget_progress, 1_000, (progress * 10).toInt(), false)
                        views.setTextViewText(R.id.widget_percent, String.format(Locale.getDefault(), "%.1f%%", progress))
                    }
                    val open = Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_EVENT_ID, event.id)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent(open, id))
                }
                manager.updateAppWidget(id, views)
            } catch (_: Exception) {
                // Keep updating other widgets, and never fail an otherwise successful save/sync.
            }
        }
    }

    private fun pendingIntent(intent: Intent, id: Int): PendingIntent = PendingIntent.getActivity(
        context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
