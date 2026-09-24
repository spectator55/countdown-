package com.spectator.countdown.core

enum class CountdownMode(val label: String, val description: String, val refreshMillis: Long) {
    DAYS("Days", "Whole days", 60_000),
    DAYS_HOURS("Days + Hours", "Days and remaining hours", 10_000),
    HOURS_MINUTES_SECONDS("Hours + Minutes + Seconds", "Total hours, minutes and seconds", 100),
    FULL("Days + Hours + Minutes + Seconds + Milliseconds", "Fractional seconds (1–15 digits)", 16);

    companion object {
        fun from(value: String): CountdownMode = entries.firstOrNull { it.name == value } ?: DAYS_HOURS
    }
}
