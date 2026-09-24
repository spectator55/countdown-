package com.spectator.countdown.core

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimes {
    private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
    private val instantFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy • h:mm:ss a z", Locale.getDefault())

    fun date(date: LocalDate): String = date.format(dateFormat)

    fun instant(epochMillis: Long, zoneId: String): String = Instant.ofEpochMilli(epochMillis)
        .atZone(zone(zoneId)).format(instantFormat)

    fun zone(id: String): ZoneId = try {
        ZoneId.of(id)
    } catch (_: DateTimeException) {
        ZoneId.systemDefault()
    }

    /** Returns null for a non-existent wall time (spring DST transition). For repeated local
     * times (autumn transition) the earlier offset is consistently selected. */
    fun toEpochMillis(date: LocalDate, hour: Int, minute: Int, second: Int, zone: ZoneId): Long? {
        if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
        val local = LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute, second)
        val offset = zone.rules.getValidOffsets(local).firstOrNull() ?: return null
        return local.atOffset(offset).toInstant().toEpochMilli()
    }
}
