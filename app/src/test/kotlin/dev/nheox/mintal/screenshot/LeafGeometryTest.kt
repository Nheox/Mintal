package dev.nheox.mintal.screenshot

import androidx.compose.ui.geometry.Offset
import dev.nheox.mintal.core.model.LeafMargin
import dev.nheox.mintal.core.model.LeafShape
import dev.nheox.mintal.ui.components.leafPath
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every leaf, in every direction, has to be a shape with actual area. A single NaN point
 * — from sin(PI) coming out faintly negative and then being raised to a power — collapses
 * the path silently, and the app just stops drawing leaves. That shipped once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LeafGeometryTest {

    private val directions = listOf(
        "up" to Offset(0f, -40f),
        "down" to Offset(0f, 40f),
        "right" to Offset(40f, 0f),
        "diagonal" to Offset(28f, -28f),
    )

    @Test
    fun `every shape and margin draws a blade with area`() {
        for (shape in LeafShape.entries) {
            for (margin in LeafMargin.entries) {
                for ((name, tip) in directions) {
                    val path = leafPath(
                        base = Offset(0f, 0f),
                        tip = tip,
                        halfWidth = 40f,
                        shape = shape,
                        margin = margin,
                    )
                    val bounds = path.getBounds()
                    val label = "$shape/$margin/$name"
                    assertTrue("$label produced no path", !path.isEmpty)
                    assertTrue(
                        "$label has NaN bounds: $bounds",
                        bounds.left.isFinite() && bounds.top.isFinite() &&
                            bounds.right.isFinite() && bounds.bottom.isFinite(),
                    )
                    assertTrue("$label is flat: $bounds", bounds.width > 1f && bounds.height > 1f)
                }
            }
        }
    }

    @Test
    fun `a zero length leaf is empty rather than broken`() {
        val path = leafPath(
            base = Offset(10f, 10f),
            tip = Offset(10f, 10f),
            halfWidth = 20f,
            shape = LeafShape.OVATE,
            margin = LeafMargin.ENTIRE,
        )
        assertTrue("a leaf with no length should draw nothing", path.isEmpty)
    }
}
