package com.spectator.countdown.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.spectator.countdown.core.MonotonicWallClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant

/** Only ticks while the screen is started. The fastest visible mode sets the refresh interval. */
@Composable
fun rememberLiveTime(intervalMillis: Long): State<Instant> {
    val owner = LocalLifecycleOwner.current
    val time = remember(intervalMillis, owner) { mutableStateOf(Instant.now()) }
    LaunchedEffect(intervalMillis, owner) {
        val clock = MonotonicWallClock()
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                time.value = clock.now()
                if (intervalMillis <= 16) withFrameNanos { } else delay(intervalMillis)
            }
        }
    }
    return time
}
