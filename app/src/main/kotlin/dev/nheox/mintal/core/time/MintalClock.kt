package dev.nheox.mintal.core.time

import android.os.SystemClock
import javax.inject.Inject

/**
 * Sessions are measured against the monotonic clock, not wall time, so a timezone change
 * or a clock correction mid-session cannot stretch or shrink a block of focus.
 */
interface MintalClock {
    /** Wall clock, for the stored record. */
    fun nowMillis(): Long

    /** Monotonic, for measuring elapsed time. */
    fun elapsedRealtimeMillis(): Long
}

class SystemMintalClock @Inject constructor() : MintalClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
