package dev.nheox.mintal.core.model

/** How a session measures time (spec §3.1). */
enum class SessionMode { COUNTDOWN, COUNT_UP }

/**
 * How a session ended (spec §5.4). Only [SUCCESS] grows the Grove with its species and
 * counts toward unlock milestones; the other two are recorded honestly and still appear
 * in statistics, marked failed.
 */
enum class SessionOutcome {
    SUCCESS,
    GAVE_UP,
    LEFT_APP;

    val isSuccess: Boolean get() = this == SUCCESS
}

/** One mint from the bundled catalogue (spec §6.1, §7). */
data class Species(
    val id: String,
    val binomial: String,
    val commonName: String,
    /**
     * Contributes one milestone threshold to the catalogue — it is no longer *this*
     * mint's unlock. Which mint fills a slot is the player's choice (spec §6.1,
     * amended), so read these through [dev.nheox.mintal.domain.milestone.MilestoneEngine]
     * as a set of thresholds, never as "spearmint unlocks at zero".
     */
    val unlockMinutes: Int,
    val hybrid: Boolean,
    val note: String,
    val art: SpeciesArt,
)

/**
 * Drawing parameters for a species (spec §8.3).
 *
 * Mintal has no commissioned artwork, so every mint is drawn from the traits visible in
 * the author's photo reference sheet — one slide per species, each naming what can
 * actually be seen in the photograph. The fields are therefore the field marks a botanist
 * would use to key the plant out, not a palette: attachment and outline first, then
 * margin, then surface and hairiness, then stem colour, then how it flowers.
 *
 * The point is that two mints differ the way the real plants differ. Lavender mint drawn
 * with apple mint's sessile, puckered, crenate leaf is wrong even if it is a different
 * green — and it was wrong, until this reference was worked through.
 */
data class SpeciesArt(
    val leafShape: LeafShape,
    val margin: LeafMargin,
    val surface: LeafSurface,
    val pubescence: Pubescence,
    val flush: LeafFlush,
    val habit: GrowthHabit,
    val leafScale: Float,
    val leafHue: LeafHue,
    val stemHue: StemHue,
    val attachment: LeafAttachment,
    val stemHairy: Boolean,
    val variegated: Boolean,
    val flower: FlowerForm,
)

/**
 * Leaf outline. These are the botanical terms, and they are not interchangeable: a
 * lanceolate spearmint leaf and a rounded-ovate lavender mint leaf are the clearest
 * single difference between the two plants.
 */
enum class LeafShape {
    LANCEOLATE,
    OVATE_LANCEOLATE,
    OVATE,
    ROUNDED_OVATE,
    OBLONG,
    OVAL,
    ROUND,
    LINEAR,
}

/** Whether a leaf sits straight on the stem or on a short stalk — a real field mark. */
enum class LeafAttachment { SESSILE, STALKED }

/** Teeth, scallops, or a smooth edge. */
enum class LeafMargin { SERRATE, CRENATE, ENTIRE }

/** What the blade's surface does with light: flat, shiny, deeply veined, or quilted. */
enum class LeafSurface { SMOOTH, GLOSSY, VEINED, PUCKERED }

/** Hairiness, which is what makes horse mint silver and apple mint white. */
enum class Pubescence { NONE, DOWNY, WOOLLY, HAIRY }

/** A colour wash over the blade that is not the leaf's own green. */
enum class LeafFlush { NONE, PURPLE, BRONZE }

/**
 * How a species flowers, which separates the mints more sharply than leaf shape does: a
 * slender terminal spike, a blunt one, a round head at the tip, whorls in the leaf axils
 * with the stem carrying on above them, whorls spaced up the stem, or dense globes.
 * Only a mature plant flowers, so this is drawn at the last growth stage and not before.
 */
enum class FlowerForm {
    SLENDER_SPIKE,
    BLUNT_SPIKE,
    ROUND_HEAD,
    AXILLARY_WHORLS,
    SPACED_WHORLS,
    GLOBE,
    NONE,
}

/** Standing habit: how the plant holds itself, from a tall stand to a flat mat. */
enum class GrowthHabit { UPRIGHT, TALL, SPREADING, MAT }

enum class LeafHue {
    LEAF_PALE,
    LEAF_BRIGHT,
    LEAF_MID,
    LEAF_DARK,
    LEAF_BLUE,
    LEAF_GOLD,
    LEAF_GREY,
}

enum class StemHue {
    STEM_GREEN,
    STEM_PURPLE,
    STEM_RED_PURPLE,
    STEM_PURPLE_BROWN,
    STEM_RED_BROWN,
    STEM_GREY,
    STEM_BROWN,
}

/** A catalogue entry plus the device's unlock state for it. */
data class SpeciesStatus(
    val species: Species,
    val unlockedAt: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}
