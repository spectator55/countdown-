package com.spectator.countdown.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CountdownMathTest {
    private val target = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()

    @Test fun upcomingRoundsUpAtDisplayedBoundary() {
        val now = Instant.ofEpochMilli(target - 1)
        assertEquals("1d", CountdownMath.read(target, now, CountdownMode.DAYS).text)
        assertEquals("0d 01h", CountdownMath.read(target, now, CountdownMode.DAYS_HOURS).text)
        assertEquals("0h 00m 01s", CountdownMath.read(target, now, CountdownMode.HOURS_MINUTES_SECONDS).text)
        assertTrue(CountdownMath.read(target, now, CountdownMode.FULL).upcoming)
    }

    @Test fun elapsedAndExactTarget() {
        assertEquals("0d 00h", CountdownMath.read(target, Instant.ofEpochMilli(target), CountdownMode.DAYS_HOURS).text)
        assertFalse(CountdownMath.read(target, Instant.ofEpochMilli(target), CountdownMode.DAYS).upcoming)
        assertEquals("1h 01m 01s", CountdownMath.read(target, Instant.ofEpochMilli(target + 3_661_000), CountdownMode.HOURS_MINUTES_SECONDS).text)
    }

    @Test fun subSecondPrecisionUsesRealNanosAndPadsBeyondNine() {
        val now = Instant.ofEpochMilli(target - 1)
        assertEquals("0d 00h 00m 00.001s", CountdownMath.read(target, now, CountdownMode.FULL, 3).text)
        assertEquals("0d 00h 00m 00.001000000000000s", CountdownMath.read(target, now, CountdownMode.FULL, 15).text)
        assertEquals("0d 00h 00m 00.1s", CountdownMath.read(target, now, CountdownMode.FULL, 1).text)
    }

    @Test fun progressIsClampedAndHandlesPastCreation() {
        val start = target - 10_000
        assertEquals(0.0, CountdownMath.progress(Instant.ofEpochMilli(start - 1), start, target), 0.001)
        assertEquals(50.0, CountdownMath.progress(Instant.ofEpochMilli(start + 5_000), start, target), 0.001)
        assertEquals(100.0, CountdownMath.progress(Instant.ofEpochMilli(target + 1), start, target), 0.001)
        assertEquals(100.0, CountdownMath.progress(Instant.ofEpochMilli(target + 1), target + 1, target), 0.001)
    }
}
