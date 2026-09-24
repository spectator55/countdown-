package com.spectator.countdown.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_selections")
data class CalendarSelectionEntity(
    @PrimaryKey val calendarKey: String,
    val accountType: String,
    val accountName: String,
    val providerCalendarId: Long,
    val calendarSyncId: String?,
    val displayName: String,
    val selected: Boolean
)

/** Deleting an imported countdown hides it locally; it does not delete the Google event. */
@Entity(tableName = "hidden_imports")
data class HiddenImportEntity(@PrimaryKey val sourceKey: String)
