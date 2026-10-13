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
