package dev.nheox.mintal.domain.session

/**
 * What the device can tell Mintal, on one tick, about where the user is. The foreground
 * service gathers these from Android; the decision is made here, where it can be tested.
 */
data class AwaySignals(
    val detectionOn: Boolean,
    val mintalInFront: Boolean,
    val screenOn: Boolean,
    val deviceLocked: Boolean,
    val inCall: Boolean,
    /**
     * True only when layer B is switched on *and* Usage Access is granted (spec §5.3).
     * Without it, Android tells Mintal that it lost the foreground and nothing about what
     * took it.
     */
    val canSeeAppInFront: Boolean,
    /** The last app the system recorded coming to the front, if [canSeeAppInFront]. */
    val appInFront: String?,
)

/** Whether the user has gone somewhere that should start the grace period (spec §5.3). */
object DistractionPolicy {

    fun isAway(signals: AwaySignals, permitted: Set<String>): Boolean {
        if (!signals.detectionOn || signals.mintalInFront) return false
        // Nothing can be done on a dark or locked phone except wake and unlock it, so
        // neither is someone being distracted by something else.
        if (!signals.screenOn || signals.deviceLocked) return false
        if (signals.inCall) return false
        // Layer A alone: Mintal knows only that it is not in front. The home screen and
        // any other app look identical from here, so both have to count.
        if (!signals.canSeeAppInFront) return true
        // Layer B with nothing recorded gets the benefit of the doubt.
        val app = signals.appInFront ?: return false
        return app !in permitted
    }
}
