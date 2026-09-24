package com.spectator.countdown.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateTimesTest {
    private val newYork = ZoneId.of("America/New_York")

    @Test fun nonExistentDstWallTimeIsRejected() {
        assertNull(DateTimes.toEpochMillis(LocalDate.of(2026, 3, 8), 2, 30, 0, newYork))
    }

    @Test fun repeatedDstHourUsesEarlierOffsetAndKeepsSeconds() {
        val instant = DateTimes.toEpochMillis(LocalDate.of(2026, 11, 1), 1, 30, 42, newYork)
        assertEquals(Instant.parse("2026-11-01T05:30:42Z").toEpochMilli(), instant)
    }

    @Test fun invalidSecondsAreRejected() {
        assertNull(DateTimes.toEpochMillis(LocalDate.of(2026, 9, 24), 12, 30, 60, newYork))
    }
}
