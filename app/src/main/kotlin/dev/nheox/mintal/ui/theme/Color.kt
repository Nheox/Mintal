package dev.nheox.mintal.ui.theme

import androidx.compose.ui.graphics.Color
import dev.nheox.mintal.core.model.LeafHue
import dev.nheox.mintal.core.model.StemHue

/**
 * Mintal's palette: light green and light blue as the two principal colours (spec §8.2),
 * declared once here and mapped onto Material 3 roles in [MintalTheme] rather than
 * inherited from Material's defaults.
 */
internal object MintalPaletteValues {
    // Greens
    val Mint10 = Color(0xFF07271A)
    val Mint20 = Color(0xFF123A28)
    val Mint30 = Color(0xFF1F5138)
    val Mint40 = Color(0xFF2F6D4F)
    val Mint60 = Color(0xFF62A883)
    val Mint80 = Color(0xFFA9DCC2)
    val Mint90 = Color(0xFFD7F2E3)
    val Mint95 = Color(0xFFEAF8F0)
    val Mint99 = Color(0xFFF2FBF6)

    // Blues
    val Sky20 = Color(0xFF10303D)
    val Sky30 = Color(0xFF1B4756)
    val Sky40 = Color(0xFF2B6E82)
    val Sky60 = Color(0xFF68A9BC)
    val Sky80 = Color(0xFFA9DCEA)
    val Sky90 = Color(0xFFD5EEF6)

    // Neutrals
    val Ink = Color(0xFF14201A)
    val InkMuted = Color(0xFF4C5C54)
    val Paper = Color(0xFFFBFEFC)
    val Night = Color(0xFF0C1511)
    val NightRaised = Color(0xFF16231D)
    val NightInk = Color(0xFFE4F0E9)

    // Botanical hues, used by the procedural mint art and the Grove
    val LeafPale = Color(0xFFB8DEBD)
    val LeafMid = Color(0xFF66A86F)
    val LeafDark = Color(0xFF2F6D4F)
    val LeafBlue = Color(0xFF6FA8A6)
    val LeafGold = Color(0xFFBFC06A)
    val LeafGrey = Color(0xFF9FB3A4)
    val StemGreen = Color(0xFF4C8A5E)
    val StemPurple = Color(0xFF6E5E8A)
    val StemGrey = Color(0xFF8C9A90)
    val StemBrown = Color(0xFF6E5243)
    val Wilted = Color(0xFFB08A5E)
}

/**
 * Semantic roles Mintal owns, for the things Material 3 has no name for: the Grove, the
 * session bar, leaf and stem colour, tag swatches. Built as a first-class token layer so
 * dark mode is a palette swap rather than a rewrite (spec §8.2, §11.8).
 */
data class MintalPalette(
    val grovePaper: Color,
    val groveRunner: Color,
    val groveGlow: Color,
    val wilted: Color,
    val barTrack: Color,
    val barFill: Color,
    val barFillSpent: Color,
    val chartGrid: Color,
    val chartBar: Color,
    val chartLine: Color,
    val leafHues: Map<LeafHue, Color>,
    val stemHues: Map<StemHue, Color>,
    val tagSwatches: List<Color>,
) {
    fun leaf(hue: LeafHue): Color = leafHues[hue] ?: MintalPaletteValues.LeafMid

    fun stem(hue: StemHue): Color = stemHues[hue] ?: MintalPaletteValues.StemGreen

    /** Tag colours come from the token layer, not a free colour picker (spec §5.8). */
    fun tagColour(index: Int): Color = tagSwatches[((index % tagSwatches.size) + tagSwatches.size) % tagSwatches.size]
}

private val sharedLeafHues = mapOf(
    LeafHue.LEAF_PALE to MintalPaletteValues.LeafPale,
    LeafHue.LEAF_MID to MintalPaletteValues.LeafMid,
    LeafHue.LEAF_DARK to MintalPaletteValues.LeafDark,
    LeafHue.LEAF_BLUE to MintalPaletteValues.LeafBlue,
    LeafHue.LEAF_GOLD to MintalPaletteValues.LeafGold,
    LeafHue.LEAF_GREY to MintalPaletteValues.LeafGrey,
)

private val sharedStemHues = mapOf(
    StemHue.STEM_GREEN to MintalPaletteValues.StemGreen,
    StemHue.STEM_PURPLE to MintalPaletteValues.StemPurple,
    StemHue.STEM_GREY to MintalPaletteValues.StemGrey,
    StemHue.STEM_BROWN to MintalPaletteValues.StemBrown,
)

private val tagSwatchesLight = listOf(
    Color(0xFF2F6D4F), Color(0xFF2B6E82), Color(0xFF7A5EA8), Color(0xFFB4593F),
    Color(0xFF8A7A2E), Color(0xFF3F6FB4), Color(0xFF9A4B6E), Color(0xFF4C8A5E),
    Color(0xFF6E5243), Color(0xFF53606E), Color(0xFF2E8A7E), Color(0xFF8A2E4C),
)

val MintalLightPalette = MintalPalette(
    grovePaper = MintalPaletteValues.Mint95,
    groveRunner = MintalPaletteValues.StemGreen,
    groveGlow = MintalPaletteValues.Mint80,
    wilted = MintalPaletteValues.Wilted,
    barTrack = MintalPaletteValues.Mint90,
    barFill = MintalPaletteValues.Mint40,
    barFillSpent = MintalPaletteValues.Mint60,
    chartGrid = MintalPaletteValues.Mint80,
    chartBar = MintalPaletteValues.Mint40,
    chartLine = MintalPaletteValues.Sky40,
    leafHues = sharedLeafHues,
    stemHues = sharedStemHues,
    tagSwatches = tagSwatchesLight,
)

val MintalDarkPalette = MintalPalette(
    grovePaper = MintalPaletteValues.NightRaised,
    groveRunner = MintalPaletteValues.Mint60,
    groveGlow = MintalPaletteValues.Mint30,
    wilted = MintalPaletteValues.Wilted,
    barTrack = MintalPaletteValues.Mint20,
    barFill = MintalPaletteValues.Mint60,
    barFillSpent = MintalPaletteValues.Mint40,
    chartGrid = MintalPaletteValues.Mint30,
    chartBar = MintalPaletteValues.Mint60,
    chartLine = MintalPaletteValues.Sky60,
    leafHues = sharedLeafHues,
    stemHues = sharedStemHues,
    tagSwatches = tagSwatchesLight,
)
