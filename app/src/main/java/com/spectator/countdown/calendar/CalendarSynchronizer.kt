package com.spectator.countdown.calendar

import android.content.Context
import androidx.room.withTransaction
import com.spectator.countdown.data.AppDatabase
import com.spectator.countdown.data.CalendarSelectionEntity
import com.spectator.countdown.data.CountdownEntity
import com.spectator.countdown.widget.CountdownWidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.UUID

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus
    data class Done(val added: Int, val changed: Int, val removed: Int) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

class CalendarSynchronizer(
    private val context: Context,
    private val db: AppDatabase,
    private val provider: CalendarProvider,
    private val widgets: CountdownWidgetUpdater
) {
    private val mutex = Mutex() // Serializes manual, observer and WorkManager syncs in this process.
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status = _status.asStateFlow()

    suspend fun calendarsWithSelections(): Pair<List<CalendarProvider.Calendar>, Set<String>> =
        withContext(Dispatchers.IO) {
            val calendars = provider.googleCalendars()
            val selected = db.selections().all().filter(CalendarSelectionEntity::selected)
                .map(CalendarSelectionEntity::calendarKey).toSet()
            calendars to selected
        }

    /** Save explicit choices, including unchecked calendars. Removed calendars stop showing
     * immediately. If a subsequent provider read fails, selected imports are NOT pruned. */
    suspend fun saveSelections(calendars: List<CalendarProvider.Calendar>, chosen: Set<String>) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val visibleKeys = calendars.map(CalendarProvider.Calendar::key).toSet()
                val selectedKeys = chosen.intersect(visibleKeys)
                db.withTransaction {
                    db.selections().putAll(calendars.map { calendar ->
                        CalendarSelectionEntity(
                            calendarKey = calendar.key,
                            accountType = calendar.identity.accountType,
                            accountName = calendar.identity.accountName,
                            providerCalendarId = calendar.identity.providerId,
                            calendarSyncId = calendar.identity.syncId,
                            displayName = calendar.displayName,
                            selected = calendar.key in selectedKeys
                        )
                    })
                    db.countdowns().imported().filter { it.sourceCalendarKey in visibleKeys &&
                        it.sourceCalendarKey !in selectedKeys }.forEach { db.countdowns().delete(it) }
                }
                widgets.updateAll()
            }
        }

    /** Fetch everything BEFORE entering the DB transaction. Provider errors, lost permission,
     * and partial reads can never turn into a successful empty import and erase good data. */
    suspend fun synchronize(): SyncStatus.Done = mutex.withLock {
        _status.value = SyncStatus.Running
        try {
            val result = withContext(Dispatchers.IO) {
                if (!CalendarPermission.granted(context)) throw SecurityException("Calendar access is required to sync.")
                val calendars = provider.googleCalendars().associateBy(CalendarProvider.Calendar::key)
                val selectedKeys = db.selections().all().filter(CalendarSelectionEntity::selected)
                    .map(CalendarSelectionEntity::calendarKey).toSet()
                val window = ZonedDateTime.now()
                val from = window.minusYears(2).toInstant().toEpochMilli()
                val until = window.plusYears(5).toInstant().toEpochMilli()
                val fetched = selectedKeys.mapNotNull(calendars::get).flatMap { calendar ->
                    provider.occurrences(calendar, from, until)
                }.associateBy(CalendarProvider.Occurrence::key)
                var added = 0
                var changed = 0
                var removed = 0
                db.withTransaction {
                    // Take the snapshot inside the transaction. A user may edit or hide an
                    // import while the provider query is running; never overwrite that edit.
                    val hidden = db.hiddenImports().allKeys().toSet()
                    val initial = db.countdowns().imported().associateBy { requireNotNull(it.sourceKey) }
                    fetched.forEach { (key, occurrence) ->
                        if (key in hidden) return@forEach
                        val previous = initial[key]
                        val event = if (previous == null) {
                            added++
                            CountdownEntity(
                                id = UUID.randomUUID().toString(),
                                name = occurrence.name,
                                targetEpochMillis = occurrence.targetEpochMillis,
                                zoneId = occurrence.zoneId,
                                createdEpochMillis = System.currentTimeMillis(),
                                sourceKey = key,
                                sourceCalendarKey = occurrence.calendar.key,
                                sourceAccountType = occurrence.calendar.identity.accountType,
                                sourceAccountName = occurrence.calendar.identity.accountName,
                                sourceCalendarId = occurrence.calendar.identity.providerId,
                                sourceCalendarSyncId = occurrence.calendar.identity.syncId,
                                sourceCalendarName = occurrence.calendar.displayName,
                                sourceEventId = occurrence.providerEventId,
                                sourceEventSyncId = occurrence.eventSyncId,
                                sourceOccurrenceMillis = occurrence.occurrenceAnchor
                            )
                        } else previous.copy(
                            name = occurrence.name,
                            targetEpochMillis = occurrence.targetEpochMillis,
                            zoneId = occurrence.zoneId,
                            sourceCalendarId = occurrence.calendar.identity.providerId,
                            sourceCalendarSyncId = occurrence.calendar.identity.syncId,
                            sourceCalendarName = occurrence.calendar.displayName,
                            sourceEventId = occurrence.providerEventId,
                            sourceEventSyncId = occurrence.eventSyncId,
                            sourceOccurrenceMillis = occurrence.occurrenceAnchor
                        ).also { if (it != previous) changed++ }
                        if (previous == null || event != previous) db.countdowns().put(event)
                    }
                    initial.forEach { (key, event) ->
                        if (key !in fetched || key in hidden) {
                            db.countdowns().delete(event)
                            removed++
                        }
                    }
                }
                widgets.updateAll()
                SyncStatus.Done(added, changed, removed)
            }
            _status.value = result
            result
        } catch (cancelled: CancellationException) {
            _status.value = SyncStatus.Idle
            throw cancelled
        } catch (error: Exception) {
            _status.value = SyncStatus.Failed(error.message ?: "The calendar could not be synchronized.")
            throw error
        }
    }
}
