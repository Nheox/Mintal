package dev.nheox.mintal.domain.grove

import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** One session's worth of growth: a runner segment with leaves on it. */
data class GroveSegment(
    val sessionId: Long,
    val epochDay: Long,
    val speciesId: String,
    val wilted: Boolean,
    val actualSeconds: Int,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val leafCount: Int,
) {
    val midX: Float get() = (startX + endX) / 2f
    val midY: Float get() = (startY + endY) / 2f
}

/** Where a day's growth sits in the network — the tap target for Day detail (spec §5.6). */
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
    val segments: List<GroveSegment>,
    val anchors: List<GroveDayAnchor>,
    val bounds: GroveBounds,
) {
    val isEmpty: Boolean get() = segments.isEmpty()
}

/**
 * The Grove is a rendering, not a table (spec §7): this turns the session list into one
 * spreading mint network. Real mint travels by stolons that root and branch as they go,
 * so growth is laid out as runners forking off earlier runners rather than as separate
 * plants.
 *
 * The layout is a pure function of session ids, so the same history always draws the same
 * Grove — it must not shuffle between launches.
 */
object GroveLayoutBuilder {

    private const val BASE_LENGTH = 14f
    private const val LENGTH_PER_ROOT_MINUTE = 6f
    private const val MAX_TURN = 0.75f
    private const val FORK_TURN = 0.9f
    private const val OUTWARD_PULL = 0.22f

    fun build(
        sessions: List<SessionEntity>,
        dayStartMinutes: Int = 0,
        zone: ZoneId = MintalTime.systemZone(),
    ): GroveLayout {
        if (sessions.isEmpty()) {
            return GroveLayout(emptyList(), emptyList(), GroveBounds(-1f, -1f, 1f, 1f))
        }

        val ordered = sessions.sortedBy { it.startedAt }
        val segments = ArrayList<GroveSegment>(ordered.size)
        val endX = FloatArray(ordered.size)
        val endY = FloatArray(ordered.size)
        val endAngle = FloatArray(ordered.size)

        var minX = 0f
        var minY = 0f
        var maxX = 0f
        var maxY = 0f

        ordered.forEachIndexed { index, session ->
            // Fork off a slightly older runner every few sessions; otherwise continue the tip.
            val forking = index > 2 && index % 4 == 0
            val parent = when {
                index == 0 -> -1
                forking -> index - 3
                else -> index - 1
            }

            val startX = if (parent < 0) 0f else endX[parent]
            val startY = if (parent < 0) 0f else endY[parent]
            val parentAngle = if (parent < 0) -1.5707964f else endAngle[parent]

            val wobble = (noise(session.id, 1) - 0.5f) * 2f * MAX_TURN
            val fork = if (forking) {
                if (noise(session.id, 2) < 0.5f) -FORK_TURN else FORK_TURN
            } else {
                0f
            }
            var angle = parentAngle + wobble + fork

            // A weak outward pull keeps the network spreading instead of knotting up.
            if (abs(startX) > 0.01f || abs(startY) > 0.01f) {
                val outward = atan2(startY, startX)
                angle += normalise(outward - angle) * OUTWARD_PULL
            }

            val minutes = session.actualSeconds / 60f
            val length = BASE_LENGTH + sqrt(minutes.coerceAtLeast(0f)) * LENGTH_PER_ROOT_MINUTE
            val tipX = startX + cos(angle) * length
            val tipY = startY + sin(angle) * length

            endX[index] = tipX
            endY[index] = tipY
            endAngle[index] = angle

            segments += GroveSegment(
                sessionId = session.id,
                epochDay = MintalTime.epochDayOf(session.startedAt, zone, dayStartMinutes),
                speciesId = session.speciesId,
                wilted = session.outcome != SessionOutcome.SUCCESS,
                actualSeconds = session.actualSeconds,
                startX = startX,
                startY = startY,
                endX = tipX,
                endY = tipY,
                leafCount = (2 + (minutes / 8f).toInt()).coerceIn(2, 10),
            )

            minX = minOf(minX, startX, tipX)
            minY = minOf(minY, startY, tipY)
            maxX = maxOf(maxX, startX, tipX)
            maxY = maxOf(maxY, startY, tipY)
        }

        val anchors = segments
            .groupBy { it.epochDay }
            .map { (day, daySegments) ->
                GroveDayAnchor(
                    epochDay = day,
                    x = daySegments.map { it.midX }.average().toFloat(),
                    y = daySegments.map { it.midY }.average().toFloat(),
                    seconds = daySegments.sumOf { it.actualSeconds },
                    sessionCount = daySegments.size,
                )
            }
            .sortedBy { it.epochDay }

        return GroveLayout(
            segments = segments,
            anchors = anchors,
            bounds = GroveBounds(minX, minY, maxX, maxY),
        )
    }

    /** Deterministic [0f, 1f) from a session id, so the Grove never reshuffles. */
    fun noise(seed: Long, salt: Int): Float {
        var x = seed * -7046029254386353131L + salt * 1609587929392839161L
        x = (x xor (x ushr 30)) * -4658895280553007687L
        x = (x xor (x ushr 27)) * -7723592293110705685L
        x = x xor (x ushr 31)
        return ((x ushr 40).toInt() and 0xFFFF) / 65536f
    }

    /** Wrap an angle difference into (-PI, PI]. */
    private fun normalise(angle: Float): Float {
        var value = angle
        while (value > 3.1415927f) value -= 6.2831855f
        while (value <= -3.1415927f) value += 6.2831855f
        return value
    }
}
