package dev.nheox.mintal.domain

import androidx.compose.ui.geometry.Offset
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.domain.grove.GroveLayoutBuilder
import dev.nheox.mintal.ui.components.clampPan
import dev.nheox.mintal.ui.components.groveContentRect
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The Grove could be dragged anywhere: fling it far enough and the bed left the screen
 * for good, with nothing on screen to drag back. Panning is clamped now, and this is the
 * invariant that clamp exists for — wherever you drag, some of the bed is still visible.
 */
class GrovePanTest {

    private val canvasWidth = 1080f
    private val canvasHeight = 900f

    private fun layout(sessions: Int) = GroveLayoutBuilder.build(
        (1L..sessions.toLong()).map { id ->
            SessionEntity(
                id = id,
                startedAt = 1_760_000_000_000L + id * 3_600_000L,
                endedAt = 1_760_000_000_000L + id * 3_600_000L + 1_500_000L,
                plannedSeconds = 1500,
                actualSeconds = 1500,
                mode = SessionMode.COUNTDOWN,
                outcome = SessionOutcome.SUCCESS,
                speciesId = if (id % 3 == 0L) "piperita" else "spicata",
                tagId = null,
                rating = null,
                note = null,
                timezoneId = "UTC",
            )
        },
    )

    private val wildPans = listOf(
        Offset(50_000f, 0f),
        Offset(-50_000f, 0f),
        Offset(0f, 90_000f),
        Offset(0f, -90_000f),
        Offset(12_345f, -6_789f),
        Offset(-999_999f, 999_999f),
    )

    @Test
    fun `no fling can push the bed off the screen`() {
        for (sessions in listOf(1, 5, 14, 40, 120)) {
            val bed = layout(sessions)
            for (scale in listOf(0.5f, 1f, 3f, 8f)) {
                for (pan in wildPans) {
                    val clamped = clampPan(pan, canvasWidth, canvasHeight, bed, scale)
                    val rect = groveContentRect(canvasWidth, canvasHeight, bed, scale, clamped)
                    val visibleX = minOf(rect.right, canvasWidth) - maxOf(rect.left, 0f)
                    val visibleY = minOf(rect.bottom, canvasHeight) - maxOf(rect.top, 0f)
                    val where = "$sessions sessions at ${scale}x panned $pan"
                    assertTrue(visibleX > 0f, "$where left nothing across: $rect")
                    assertTrue(visibleY > 0f, "$where left nothing down: $rect")
                }
            }
        }
    }

    @Test
    fun `an ordinary nudge is not clamped at all`() {
        val bed = layout(40)
        val nudge = Offset(60f, -20f)
        assertTrue(
            clampPan(nudge, canvasWidth, canvasHeight, bed, 1f) == nudge,
            "a small pan should pass through untouched",
        )
    }

    @Test
    fun `clamping settles rather than oscillating`() {
        val bed = layout(9)
        val once = clampPan(Offset(9_000f, 9_000f), canvasWidth, canvasHeight, bed, 2f)
        val twice = clampPan(once, canvasWidth, canvasHeight, bed, 2f)
        assertTrue(once == twice, "clamping an already clamped pan moved it: $once -> $twice")
    }
}
