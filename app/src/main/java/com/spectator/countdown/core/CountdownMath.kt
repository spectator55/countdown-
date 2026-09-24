package com.spectator.countdown.core

import java.math.BigInteger
import java.time.Instant

/** Pure calculations. An upcoming value is rounded UP at the smallest displayed unit, so it
 * never reaches zero before the target; an elapsed value is rounded DOWN. No device clock is
 * involved here, which also makes boundary behavior deterministic in unit tests. */
object CountdownMath {
    private val billion = BigInteger.valueOf(1_000_000_000L)
    private val million = BigInteger.valueOf(1_000_000L)
    private val second = billion
    private val minute = second * BigInteger.valueOf(60)
    private val hour = minute * BigInteger.valueOf(60)
    private val day = hour * BigInteger.valueOf(24)

    data class Reading(val upcoming: Boolean, val text: String)

    fun read(targetEpochMillis: Long, now: Instant, mode: CountdownMode, digits: Int = 3): Reading {
        val targetNanos = BigInteger.valueOf(targetEpochMillis) * million
        val nowNanos = BigInteger.valueOf(now.epochSecond) * billion + BigInteger.valueOf(now.nano.toLong())
        val delta = targetNanos - nowNanos
        val upcoming = delta.signum() > 0
        val magnitude = delta.abs()
        val precision = digits.coerceIn(1, 15)
        val step = when (mode) {
            CountdownMode.DAYS -> day
            CountdownMode.DAYS_HOURS -> hour
            CountdownMode.HOURS_MINUTES_SECONDS -> second
            CountdownMode.FULL -> BigInteger.TEN.pow((9 - precision).coerceAtLeast(0))
        }
        val rounded = if (upcoming) (magnitude + step - BigInteger.ONE) / step * step
        else magnitude / step * step

        val days = rounded / day
        val hours = rounded % day / hour
        val minutes = rounded % hour / minute
        val seconds = rounded % minute / second
        val text = when (mode) {
            CountdownMode.DAYS -> "${days}d"
            CountdownMode.DAYS_HOURS -> "${days}d ${hours.two()}h"
            CountdownMode.HOURS_MINUTES_SECONDS -> "${rounded / hour}h ${minutes.two()}m ${seconds.two()}s"
            CountdownMode.FULL -> {
                val nanos = (rounded % second).toString().padStart(9, '0')
                val fraction = if (precision <= 9) nanos.take(precision)
                else nanos + "0".repeat(precision - 9)
                "${days}d ${hours.two()}h ${minutes.two()}m ${seconds.two()}.$fraction" + "s"
            }
        }
        return Reading(upcoming, text)
    }

    /** 0–100 for [start, target]. A target before creation has no interval and is complete. */
    fun progress(now: Instant, startEpochMillis: Long, targetEpochMillis: Long): Double {
        if (targetEpochMillis <= startEpochMillis) {
            return if (now.toEpochMilli() >= targetEpochMillis) 100.0 else 0.0
        }
        val nowMillis = now.epochSecond * 1_000.0 + now.nano / 1_000_000.0
        return ((nowMillis - startEpochMillis) / (targetEpochMillis - startEpochMillis) * 100.0)
            .coerceIn(0.0, 100.0)
    }

    private fun BigInteger.two(): String = toString().padStart(2, '0')
}
