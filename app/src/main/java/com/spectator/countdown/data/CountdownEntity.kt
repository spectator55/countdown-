package com.spectator.countdown.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spectator.countdown.core.CountdownMode

@Entity(
    tableName = "countdowns",
    indices = [Index(value = ["sourceKey"], unique = true), Index(value = ["sourceCalendarKey"])]
)
data class CountdownEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetEpochMillis: Long,
    val zoneId: String,
    val createdEpochMillis: Long,
    val mode: String = CountdownMode.DAYS_HOURS.name,
    val precisionDigits: Int = 3,
    val showProgress: Boolean = false,
    val customStartEpochMillis: Long? = null,
    // Null for manually created events. The source key includes the account and server calendar
    // ID (or provider ID fallback), server event ID (or provider ID fallback), and occurrence.
    val sourceKey: String? = null,
    val sourceCalendarKey: String? = null,
    val sourceAccountType: String? = null,
    val sourceAccountName: String? = null,
    val sourceCalendarId: Long? = null,
    val sourceCalendarSyncId: String? = null,
    val sourceCalendarName: String? = null,
    val sourceEventId: Long? = null,
    val sourceEventSyncId: String? = null,
    val sourceOccurrenceMillis: Long? = null
) {
    val imported: Boolean get() = sourceKey != null
    val progressStartEpochMillis: Long get() = customStartEpochMillis ?: createdEpochMillis
}
