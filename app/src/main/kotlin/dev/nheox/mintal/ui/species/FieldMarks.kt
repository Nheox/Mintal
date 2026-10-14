package dev.nheox.mintal.ui.species

import dev.nheox.mintal.core.model.FlowerForm
import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.LeafAttachment
import dev.nheox.mintal.core.model.LeafFlush
import dev.nheox.mintal.core.model.LeafHue
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.core.model.LeafSurface
import dev.nheox.mintal.core.model.Pubescence
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesArt
import dev.nheox.mintal.core.model.StemHue

/** One labelled line of the herbarium card: "Leaf", "Stem", "Flowers", "Habit". */
data class FieldMark(val label: String, val value: String)

/**
 * The drawing parameters, read back out in words.
 *
 * [SpeciesArt] is already a botanist's description of the plant — attachment, margin,
 * surface, hairs, stem colour, flower form — so the card does not need a second set of
 * prose written by hand and kept in step with it. It reads the same fields the drawing
 * reads, which means the words and the picture cannot disagree.
 *
 * Every `when` here is exhaustive on purpose: adding a trait to the model should fail to
 * compile rather than print an enum constant at someone.
 */
object FieldMarks {

    fun of(species: Species): List<FieldMark> {
        val art = species.art
        return listOf(
            FieldMark("Leaf", leafLine(art)),
            FieldMark("Stem", stemLine(art)),
            FieldMark("Flowers", flowerLine(art)),
            FieldMark("Habit", habitLine(art)),
        )
    }

    private fun leafLine(art: SpeciesArt): String {
        val parts = buildList {
            add("${hue(art.leafHue)}, ${shape(art.leafShape)}")
            add(attachment(art.attachment))
            add(margin(art.margin))
            surface(art.surface)?.let(::add)
            pubescence(art.pubescence)?.let(::add)
            flush(art.flush)?.let(::add)
            if (art.variegated) add("splashed with cream")
        }
        return sentence(parts)
    }

    private fun stemLine(art: SpeciesArt): String = sentence(
        listOf(
            stemHue(art.stemHue),
            if (art.stemHairy) "hairy" else "smooth",
        ),
    )

    private fun flowerLine(art: SpeciesArt): String = when (art.flower) {
        FlowerForm.SLENDER_SPIKE -> "A slender spike at the tip of the stem"
        FlowerForm.BLUNT_SPIKE -> "A short, blunt spike at the tip of the stem"
        FlowerForm.ROUND_HEAD -> "A single round head at the tip of the stem"
        FlowerForm.AXILLARY_WHORLS -> "Whorls in the leaf axils, the stem carrying on above them"
        FlowerForm.SPACED_WHORLS -> "Whorls spaced up the stem"
        FlowerForm.GLOBE -> "Dense globes"
        FlowerForm.NONE -> "Too small to flower at Grove scale"
    } + ". Only a full-grown plant flowers."

    private fun habitLine(art: SpeciesArt): String = when (art.habit) {
        GrowthHabit.UPRIGHT -> "Stands upright"
        GrowthHabit.TALL -> "Stands tall and upright"
        GrowthHabit.SPREADING -> "Spreads outward as it goes"
        GrowthHabit.MAT -> "Creeps flat, as a mat"
    } + "."

    private fun shape(shape: LeafShape): String = when (shape) {
        LeafShape.LANCEOLATE -> "lance-shaped"
        LeafShape.OVATE_LANCEOLATE -> "between egg- and lance-shaped"
        LeafShape.OVATE -> "egg-shaped"
        LeafShape.ROUNDED_OVATE -> "roundly egg-shaped"
        LeafShape.OBLONG -> "oblong"
        LeafShape.OVAL -> "oval"
        LeafShape.ROUND -> "round"
        LeafShape.LINEAR -> "narrow and strap-like"
    }

    private fun attachment(attachment: LeafAttachment): String = when (attachment) {
        LeafAttachment.SESSILE -> "sitting straight on the stem"
        LeafAttachment.STALKED -> "on a short stalk"
    }

    private fun margin(margin: LeafMargin): String = when (margin) {
        LeafMargin.SERRATE -> "sharply toothed"
        LeafMargin.CRENATE -> "scalloped at the edge"
        LeafMargin.ENTIRE -> "smooth-edged"
    }

    private fun surface(surface: LeafSurface): String? = when (surface) {
        LeafSurface.SMOOTH -> null
        LeafSurface.GLOSSY -> "glossy"
        LeafSurface.VEINED -> "with the veins pressed deep into the blade"
        LeafSurface.PUCKERED -> "puckered between the veins"
    }

    private fun pubescence(pubescence: Pubescence): String? = when (pubescence) {
        Pubescence.NONE -> null
        Pubescence.DOWNY -> "finely downy"
        Pubescence.WOOLLY -> "white-woolly all over"
        Pubescence.HAIRY -> "hairy"
    }

    private fun flush(flush: LeafFlush): String? = when (flush) {
        LeafFlush.NONE -> null
        LeafFlush.PURPLE -> "washed purple"
        LeafFlush.BRONZE -> "washed bronze"
    }

    private fun hue(hue: LeafHue): String = when (hue) {
        LeafHue.LEAF_PALE -> "Pale green"
        LeafHue.LEAF_BRIGHT -> "Bright green"
        LeafHue.LEAF_MID -> "Mid green"
        LeafHue.LEAF_DARK -> "Dark green"
        LeafHue.LEAF_BLUE -> "Blue-green"
        LeafHue.LEAF_GOLD -> "Gold-green"
        LeafHue.LEAF_GREY -> "Grey-green"
    }

    private fun stemHue(hue: StemHue): String = when (hue) {
        StemHue.STEM_GREEN -> "Green"
        StemHue.STEM_PURPLE -> "Purple"
        StemHue.STEM_RED_PURPLE -> "Red-purple"
        StemHue.STEM_PURPLE_BROWN -> "Purple-brown"
        StemHue.STEM_RED_BROWN -> "Red-brown"
        StemHue.STEM_GREY -> "Grey"
        StemHue.STEM_BROWN -> "Brown"
    }

    private fun sentence(parts: List<String>): String = parts.joinToString(", ") + "."
}
