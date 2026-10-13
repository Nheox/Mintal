package dev.nheox.mintal.ui.nav

/** The screen map of spec §4, as navigation routes. */
object Routes {
    const val ONBOARDING = "onboarding"

    /** The principles, kept readable from Settings after the first-run deck. */
    const val PRINCIPLES = "principles"
    const val HOME = "home"
    const val RUNNING = "running"
    const val GROVE = "grove"
    const val STATS = "stats"
    const val SPECIES = "species"
    const val UNLOCK = "unlock"
    const val TAGS = "tags"
    const val SETTINGS = "settings"
    const val ALLOWLIST = "allowlist"
    const val ABOUT = "about"

    const val ARG_SESSION_ID = "sessionId"
    const val ARG_EPOCH_DAY = "epochDay"

    const val RESULT_PATTERN = "result/{$ARG_SESSION_ID}"
    const val DAY_PATTERN = "day/{$ARG_EPOCH_DAY}"
    const val SESSION_DETAIL_PATTERN = "session/{$ARG_SESSION_ID}"

    fun result(sessionId: Long): String = "result/$sessionId"

    fun day(epochDay: Long): String = "day/$epochDay"

    fun sessionDetail(sessionId: Long): String = "session/$sessionId"
}
