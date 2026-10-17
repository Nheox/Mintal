package dev.nheox.mintal.domain.session

/** The numbers in spec §6, in one place. */
object SessionRules {
    const val MIN_COUNTDOWN_MINUTES = 5
    const val MAX_MINUTES = 180
    const val STEP_MINUTES = 5
    const val DEFAULT_MINUTES = 25
    const val MAX_SECONDS = MAX_MINUTES * 60

    /** Grace period after leaving the app, with a visible warning (spec §5.3). */
    const val GRACE_SECONDS = 15

    /**
     * A session can be paused for this long (spec §5.2.1). While it is paused the clock
     * is stopped and leaving the app does nothing at all — that is what the pause is for.
     */
    const val PAUSE_SECONDS = 120

    /**
     * After a pause ends, the next one waits this long (spec §5.2.1), with the wait counted
     * down on the button. Back-to-back pauses would make the grace period of §5.3 optional;
     * a gap keeps a pause something you take, not a state you live in. Paused time never
     * counts as focus, so pausing can only make a session longer, never easier.
     */
    const val PAUSE_COOLDOWN_SECONDS = 30

    /**
     * A session is armed, not started, for this long after START is pressed. Nothing is
     * written and no service runs during it, so backing out leaves no row, no plant and
     * no wilted stub — the point being that a mistyped duration or a wrong tag should not
     * cost a mint.
     */
    const val PLANTING_WINDOW_SECONDS = 5

    /** A count-up session under a minute is discarded entirely (spec §5.4). */
    const val COUNT_UP_MIN_SECONDS = 60

    const val NOTE_MAX_LENGTH = 200

    val RATING_RANGE = 1..5

    val durationChoices: List<Int> =
        (MIN_COUNTDOWN_MINUTES..MAX_MINUTES step STEP_MINUTES).toList()

    fun clampMinutes(minutes: Int): Int =
        minutes.coerceIn(MIN_COUNTDOWN_MINUTES, MAX_MINUTES).let { value ->
            val steps = Math.round(value.toFloat() / STEP_MINUTES)
            (steps * STEP_MINUTES).coerceIn(MIN_COUNTDOWN_MINUTES, MAX_MINUTES)
        }
}
