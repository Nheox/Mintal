package dev.nheox.mintal.domain.grove

import dev.nheox.mintal.core.model.GrowthHabit
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import java.time.ZoneId

/** One plant standing in the bed, with the sessions that grew it. */
data class GrovePlanting(
    val speciesId: String,
    val plantIndex: Int,
    val slot: Int,
    val baseX: Float,
    val plant: Plant,
    val firstStartedAt: Long,
)

/**
 * A session that failed, which leaves a wilted stub rather than growth. They are kept and
 * drawn, because a Grove that quietly hides the bad days is a Grove that flatters.
 */
data class GroveStub(
    val sessionId: Long,
    val speciesId: String,
    val slot: Int,
    val baseX: Float,
    val epochDay: Long,
)

/** Where a day's growth sits in the bed — the tap target for Day detail (spec §5.6). */
data class GroveDayAnchor(
    val epochDay: Long,
    val x: Float,
    val y: Float,
    val seconds: Int,
    val sessionCount: Int,
)

data class GroveBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(1f)
    val height: Float get() = (maxY - minY).coerceAtLeast(1f)
}

data class GroveLayout(
    val plantings: List<GrovePlanting>,
    val stubs: List<GroveStub>,
    val anchors: List<GroveDayAnchor>,
    val bounds: GroveBounds,
) {
    val isEmpty: Boolean get() = plantings.isEmpty() && stubs.isEmpty()

    val plantCount: Int get() = plantings.size
}

/**
 * The Grove is a rendering, not a table (spec §7): this turns the session list into a bed
 * of mints. Each successful session adds one node to that species' youngest unfinished
 * plant; when a plant matures it stops, and the next session of that species starts a new
 * one beside it. A species you use constantly becomes a clump of several plants, which is
 * both what mint does and what keeps one plant from scaling forever.
 *
 * Position is a pure function of the order plants first appeared, so a plant never moves
 * sideways as it grows and the bed never reshuffles between launches.
 */
object GroveLayoutBuilder {

    /** Horizontal units per plant. A mature plant is about two units across. */
    private const val SLOT_WIDTH = 3.0f

    /** A failed session is a stub, and takes a narrower place than a plant. */
    private const val STUB_WIDTH = 0.9f

    /** Something standing in the bed before it is given a place: a plant, or a stub. */
    private sealed interface Standing {
        val startedAt: Long

        data class Growth(
            val speciesId: String,
            val index: Int,
            val rows: List<SessionEntity>,
        ) : Standing {
            override val startedAt: Long get() = rows.first().startedAt
        }

        data class Failure(val session: SessionEntity) : Standing {
            override val startedAt: Long get() = session.startedAt
        }
    }

    private val EMPTY_BOUNDS = GroveBounds(-1f, -1f, 1f, 1f)

    fun build(
        sessions: List<SessionEntity>,
        speciesById: Map<String, Species> = emptyMap(),
        dayStartMinutes: Int = 0,
        zone: ZoneId = MintalTime.systemZone(),
    ): GroveLayout {
        if (sessions.isEmpty()) return GroveLayout(emptyList(), emptyList(), emptyList(), EMPTY_BOUNDS)

        val ordered = sessions.sortedBy { it.startedAt }
        val successes = ordered.filter { it.outcome == SessionOutcome.SUCCESS }
        val failures = ordered.filter { it.outcome != SessionOutcome.SUCCESS }

        // Chunking by species is what caps growth: the thirteenth session of a mint
        // starts its second plant rather than making the first one taller.
        val groups = LinkedHashMap<String, MutableList<SessionEntity>>()
        for (session in successes) {
            groups.getOrPut(session.speciesId) { ArrayList() } += session
        }

        val growths = ArrayList<Standing.Growth>()
        for ((speciesId, rows) in groups) {
            rows.chunked(PlantBuilder.MATURE_NODES).forEachIndexed { index, chunk ->
                growths += Standing.Growth(speciesId, index, chunk)
            }
        }

        // Everything standing in the bed, in the order it was planted, so the bed reads
        // left to right as a history. A stub takes a narrow place of its own: a bad week
        // should be visible without pushing a month of growth off the screen.
        val standing: List<Standing> = (growths + failures.map { Standing.Failure(it) })
            .sortedBy { it.startedAt }

        val plantings = ArrayList<GrovePlanting>(growths.size)
        val stubs = ArrayList<GroveStub>(failures.size)
        val nodePositions = HashMap<Long, Pair<Float, Float>>()
        var cursor = 0f

        standing.forEachIndexed { slot, item ->
            when (item) {
                is Standing.Growth -> {
                    cursor += SLOT_WIDTH / 2f
                    val habit = speciesById[item.speciesId]?.art?.habit ?: GrowthHabit.UPRIGHT
                    val plant = PlantBuilder.grow(
                        speciesId = item.speciesId,
                        plantIndex = item.index,
                        sessionIds = item.rows.map { it.id },
                        habit = habit,
                        seed = item.rows.first().id,
                    )
                    plantings += GrovePlanting(
                        speciesId = item.speciesId,
                        plantIndex = item.index,
                        slot = slot,
                        baseX = cursor,
                        plant = plant,
                        firstStartedAt = item.startedAt,
                    )
                    plant.nodes.forEach { node ->
                        nodePositions[node.sessionId] = (cursor + node.x) to node.y
                    }
                    cursor += SLOT_WIDTH / 2f
                }
                is Standing.Failure -> {
                    cursor += STUB_WIDTH / 2f
                    stubs += GroveStub(
                        sessionId = item.session.id,
                        speciesId = item.session.speciesId,
                        slot = slot,
                        baseX = cursor,
                        epochDay = MintalTime.epochDayOf(
                            item.session.startedAt,
                            zone,
                            dayStartMinutes,
                        ),
                    )
                    nodePositions[item.session.id] = cursor to -0.35f
                    cursor += STUB_WIDTH / 2f
                }
            }
        }

        val byDay = ordered.groupBy { MintalTime.epochDayOf(it.startedAt, zone, dayStartMinutes) }
        val anchors = byDay.map { (day, rows) ->
            val points = rows.mapNotNull { nodePositions[it.id] }
            GroveDayAnchor(
                epochDay = day,
                x = points.map { it.first }.averageOrZero(),
                y = points.map { it.second }.averageOrZero(),
                seconds = rows.sumOf { it.actualSeconds },
                sessionCount = rows.size,
            )
        }.sortedBy { it.epochDay }

        val minX = minOf(
            plantings.minOfOrNull { it.baseX + it.plant.minX } ?: 0f,
            stubs.minOfOrNull { it.baseX } ?: 0f,
        ) - 1f
        val maxX = maxOf(
            plantings.maxOfOrNull { it.baseX + it.plant.maxX } ?: 0f,
            stubs.maxOfOrNull { it.baseX } ?: 0f,
        ) + 1f
        val minY = (plantings.minOfOrNull { it.plant.minY } ?: -1f) - 1.2f

        return GroveLayout(
            plantings = plantings,
            stubs = stubs,
            anchors = anchors,
            // The ground line is y = 0 and everything grows up from it, so the bed always
            // has a floor even when the only thing in it is one seedling.
            bounds = GroveBounds(minX = minX, minY = minY, maxX = maxX, maxY = 0.6f),
        )
    }

    private fun List<Float>.averageOrZero(): Float =
        if (isEmpty()) 0f else sum() / size
}
