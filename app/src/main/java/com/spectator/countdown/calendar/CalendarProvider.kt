package com.spectator.countdown.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.CalendarContract
import com.spectator.countdown.core.CalendarIdentity
import com.spectator.countdown.core.DateTimes
import com.spectator.countdown.core.eventSourceKey
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** READ_CALENDAR only. Google's accounts are read from the on-device Calendar Provider; neither
 * email credentials nor Google network APIs are used. Calls must be made on Dispatchers.IO. */
class CalendarProvider(private val resolver: ContentResolver) {
    data class Calendar(val identity: CalendarIdentity, val displayName: String) {
        val key: String get() = identity.key
    }

    data class Occurrence(
        val key: String,
        val calendar: Calendar,
        val providerEventId: Long,
        val eventSyncId: String?,
        val occurrenceAnchor: Long,
        val name: String,
        val targetEpochMillis: Long,
        val zoneId: String
    )

    fun googleCalendars(): List<Calendar> {
        val columns = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars._SYNC_ID
        )
        val cursor = resolver.query(
            CalendarContract.Calendars.CONTENT_URI, columns,
            "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?", arrayOf("com.google"), null
        ) ?: throw IOException("The device Calendar Provider is unavailable.")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val identity = CalendarIdentity(
                        accountType = it.getString(1) ?: continue,
                        accountName = it.getString(2) ?: continue,
                        providerId = it.getLong(0),
                        syncId = it.optionalString(4)
                    )
                    add(Calendar(identity, it.getString(3)?.takeIf(String::isNotBlank) ?: "Unnamed calendar"))
                }
            }.distinctBy(Calendar::key).sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { c: Calendar -> c.identity.accountName }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { c -> c.displayName }
                    .thenBy { c -> c.identity.providerId }
            )
        }
    }

    /** Instances, rather than Events DTSTART, expands recurring events and their exceptions. */
    fun occurrences(calendar: Calendar, fromMillis: Long, untilMillis: Long): List<Occurrence> {
        val metadata = eventMetadata(calendar.identity.providerId)
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, fromMillis)
            ContentUris.appendId(it, untilMillis)
        }.build()
        val columns = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.ALL_DAY
        )
        val cursor = resolver.query(
            uri, columns, "${CalendarContract.Instances.CALENDAR_ID} = ?",
            arrayOf(calendar.identity.providerId.toString()), "${CalendarContract.Instances.BEGIN} ASC"
        ) ?: throw IOException("Could not read ${calendar.displayName} in ${calendar.identity.accountName}.")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val eventId = it.getLong(0)
                    val begin = it.getLong(1)
                    val event = metadata[eventId]
                    // 0 is a stable occurrence anchor for one-off events, so changing their
                    // time updates the existing countdown rather than creating a duplicate.
                    val anchor = event?.originalInstanceTime ?: if (event?.recurring == false) 0L else begin
                    val allDay = it.getInt(3) != 0
                    // All-day BEGIN is UTC midnight by Calendar Provider convention. Interpret
                    // its UTC *date* at midnight in the device zone, not as a UTC wall time.
                    val deviceZone = ZoneId.systemDefault()
                    val target = if (allDay) {
                        Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                            .atStartOfDay(deviceZone).toInstant().toEpochMilli()
                    } else begin
                    val zone = if (allDay) deviceZone else DateTimes.zone(event?.timeZone ?: deviceZone.id)
                    add(Occurrence(
                        key = eventSourceKey(calendar.key, eventId, event?.syncId, anchor),
                        calendar = calendar,
                        providerEventId = eventId,
                        eventSyncId = event?.syncId,
                        occurrenceAnchor = anchor,
                        name = it.getString(2)?.takeIf(String::isNotBlank) ?: "Untitled event",
                        targetEpochMillis = target,
                        zoneId = zone.id
                    ))
                }
            }.distinctBy(Occurrence::key)
        }
    }

    private data class EventMetadata(
        val syncId: String?,
        val recurring: Boolean,
        val originalInstanceTime: Long?,
        val timeZone: String?
    )

    private fun eventMetadata(calendarId: Long): Map<Long, EventMetadata> {
        val columns = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events._SYNC_ID,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.RDATE,
            CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
            CalendarContract.Events.EVENT_TIMEZONE
        )
        val cursor = resolver.query(
            CalendarContract.Events.CONTENT_URI, columns,
            "${CalendarContract.Events.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString()), null
        ) ?: throw IOException("The device Calendar Provider could not read event details.")
        return cursor.use {
            buildMap {
                while (it.moveToNext()) {
                    put(it.getLong(0), EventMetadata(
                        syncId = it.optionalString(1),
                        recurring = !it.optionalString(2).isNullOrBlank() || !it.optionalString(3).isNullOrBlank(),
                        originalInstanceTime = if (it.isNull(4)) null else it.getLong(4),
                        timeZone = it.optionalString(5)
                    ))
                }
            }
        }
    }

    private fun Cursor.optionalString(index: Int): String? = if (isNull(index)) null else getString(index)
}
