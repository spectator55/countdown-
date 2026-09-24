package com.spectator.countdown.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
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
    return produceState(initialValue = Instant.now(), intervalMillis, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val clock = MonotonicWallClock()
            while (isActive) {
                value = clock.now()
                if (intervalMillis <= 16) withFrameNanos { } else delay(intervalMillis)
            }
        }
    }
}
