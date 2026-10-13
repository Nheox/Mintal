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
    val unlockMinutes: Int,
    val hybrid: Boolean,
    val note: String,
    val art: SpeciesArt,
)

/**
 * Drawing parameters for a species. Mintal has no commissioned artwork yet (spec §8.3),
 * so every mint is drawn procedurally from real botanical traits — leaf shape, leaf
 * margin, growth habit, size — rather than being tinted copies of one leaf.
 */
data class SpeciesArt(
    val leafShape: LeafShape,
    val margin: LeafMargin,
    val habit: GrowthHabit,
    val leafScale: Float,
    val leafHue: LeafHue,
    val stemHue: StemHue,
    val attachment: LeafAttachment,
    val flower: FlowerForm,
)

enum class LeafShape { SPEAR, OVATE, ROUND, LANCEOLATE, NEEDLE }

/** Whether a leaf sits straight on the stem or on a short stalk — a real field mark. */
enum class LeafAttachment { SESSILE, STALKED }

/**
 * How a species flowers, which separates the mints more sharply than leaf shape does: a
 * slender terminal spike, a blunt one, a round head at the tip, whorls in the leaf axils
 * with the stem carrying on above them, whorls spaced up the stem, or dense globes.
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

enum class LeafMargin { SERRATE, CRENATE, ENTIRE }

enum class GrowthHabit { UPRIGHT, CREEPING, TALL }

enum class LeafHue { LEAF_PALE, LEAF_MID, LEAF_DARK, LEAF_BLUE, LEAF_GOLD, LEAF_GREY }

enum class StemHue { STEM_GREEN, STEM_PURPLE, STEM_GREY, STEM_BROWN }

/** A catalogue entry plus the device's unlock state for it. */
data class SpeciesStatus(
    val species: Species,
    val unlockedAt: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}
