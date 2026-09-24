package com.spectator.countdown.core

import android.os.SystemClock
import java.time.Instant

/** Interpolates the wall clock between samples with elapsedRealtimeNanos. Resampling every two
 * seconds corrects manual time changes, network time corrections, and sleep/wake transitions. */
class MonotonicWallClock {
    private var wall = Instant.now()
    private var elapsed = SystemClock.elapsedRealtimeNanos()

    fun now(): Instant {
        val tick = SystemClock.elapsedRealtimeNanos()
        val delta = tick - elapsed
        if (delta < 0 || delta >= 2_000_000_000L) {
            val before = SystemClock.elapsedRealtimeNanos()
            wall = Instant.now()
            val after = SystemClock.elapsedRealtimeNanos()
            elapsed = before + (after - before) / 2
            return wall.plusNanos((tick - elapsed).coerceAtLeast(0))
        }
        return wall.plusNanos(delta)
    }
}
