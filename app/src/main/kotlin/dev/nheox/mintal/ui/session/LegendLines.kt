package dev.nheox.mintal.ui.session

/**
 * The rotating legend line under the session bar (spec §5.2). Two pools sit side by side
 * here, and the copy has to make clear which is which: in-world flavour is unlabelled,
 * and a real study claim is labelled and cited. Nothing in the tip pool is motivational
 * filler — these are the findings v2's Study guidance will expand on (spec §3.2).
 */
object LegendLines {

    val flavour: List<String> = listOf(
        "the roots are settling in",
        "a runner is reaching out under the soil",
        "new leaves are unfolding at the tip",
        "the stem is thickening where it branched",
        "the scent is strongest just before flowering",
        "mint spreads sideways before it grows tall",
    )

    val studyTips: List<String> = listOf(
        "Study tip · Testing yourself on material beats re-reading it, even when re-reading feels more productive (Roediger & Karpicke, 2006).",
        "Study tip · Spacing the same total study time across days beats massing it in one sitting (Cepeda et al., 2006).",
        "Study tip · Mixing problem types in one session — interleaving — improves later discrimination between them (Rohrer & Taylor, 2007).",
        "Study tip · Explaining a concept to yourself in your own words while reading improves retention (Chi et al., 1994).",
        "Study tip · Sleep after study consolidates what you learned; a late all-nighter trades retention for hours (Walker & Stickgold, 2006).",
        "Study tip · Writing a short summary from memory, then checking it, surfaces what you only thought you knew (Dunlosky et al., 2013).",
    )

    /** Alternates flavour and tip so the two never blur into one voice. */
    fun lineAt(index: Int): String {
        val slot = index / 2
        return if (index % 2 == 0) {
            flavour[slot % flavour.size]
        } else {
            studyTips[slot % studyTips.size]
        }
    }

    fun isTip(index: Int): Boolean = index % 2 == 1
}
