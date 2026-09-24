package com.spectator.countdown.widget

import android.content.Context
import com.spectator.countdown.core.CountdownMode

/** Settings are per app-widget ID, independent of the countdown's in-app display settings. */
data class WidgetSettings(
    val eventId: String,
    val mode: CountdownMode,
    val precisionDigits: Int,
    val showProgress: Boolean
)

class WidgetPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("countdown_widgets", Context.MODE_PRIVATE)
    private fun key(id: Int, suffix: String) = "widget_${id}_$suffix"

    fun get(id: Int): WidgetSettings? {
        val eventId = prefs.getString(key(id, "event"), null) ?: return null
        return WidgetSettings(
            eventId = eventId,
            mode = CountdownMode.from(prefs.getString(key(id, "mode"), null).orEmpty()),
            precisionDigits = prefs.getInt(key(id, "digits"), 3).coerceIn(1, 15),
            showProgress = prefs.getBoolean(key(id, "progress"), false)
        )
    }

    fun save(id: Int, settings: WidgetSettings) {
        check(prefs.edit()
            .putString(key(id, "event"), settings.eventId)
            .putString(key(id, "mode"), settings.mode.name)
            .putInt(key(id, "digits"), settings.precisionDigits.coerceIn(1, 15))
            .putBoolean(key(id, "progress"), settings.showProgress)
            .commit()) { "Could not save widget settings." }
    }

    fun delete(id: Int) {
        prefs.edit().remove(key(id, "event")).remove(key(id, "mode"))
            .remove(key(id, "digits")).remove(key(id, "progress")).apply()
    }
}
