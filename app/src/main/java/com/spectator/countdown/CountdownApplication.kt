package com.spectator.countdown

import android.app.Application
import androidx.room.Room
import com.spectator.countdown.calendar.CalendarAutoSync
import com.spectator.countdown.calendar.CalendarProvider
import com.spectator.countdown.calendar.CalendarSynchronizer
import com.spectator.countdown.data.AppDatabase
import com.spectator.countdown.data.AppPreferences
import com.spectator.countdown.data.CountdownRepository
import com.spectator.countdown.widget.CountdownWidgetUpdater

class CountdownApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "countdown.db").build()
    }
    val preferences: AppPreferences by lazy { AppPreferences(this) }
    val widgetUpdater: CountdownWidgetUpdater by lazy { CountdownWidgetUpdater(this, database) }
    val events: CountdownRepository by lazy { CountdownRepository(database, widgetUpdater) }
    val synchronizer: CalendarSynchronizer by lazy {
        CalendarSynchronizer(this, database, CalendarProvider(contentResolver), widgetUpdater)
    }
    private val autoSync: CalendarAutoSync by lazy { CalendarAutoSync(this, preferences, synchronizer) }

    override fun onCreate() {
        super.onCreate()
        autoSync // Restore the observer and periodic work from persistent preferences.
    }

    fun onCalendarPermissionChanged() = autoSync.permissionChanged()
}
