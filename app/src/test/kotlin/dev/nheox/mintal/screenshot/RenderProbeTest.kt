package dev.nheox.mintal.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Proves the rendering pipeline before anything is built on top of it: if Compose cannot
 * be drawn to a bitmap here, every screenshot test after this is wasted effort.
 *
 * Draws the view directly rather than going through the Compose test rule, whose
 * idling-resource machinery times out under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class RenderProbeTest {

    @Test
    fun `compose renders to a bitmap`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2F6D4F)))
        }
        shadowOf(Looper.getMainLooper()).idle()

        val view: View = activity.window.decorView
        val width = if (view.width > 0) view.width else 1080
        val height = if (view.height > 0) view.height else 1920
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
        shadowOf(Looper.getMainLooper()).idle()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val out = File("build/screenshots").apply { mkdirs() }.resolve("probe.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val centre = bitmap.getPixel(width / 2, height / 2)
        println("PROBE ${out.absolutePath} ${width}x$height centre=${Integer.toHexString(centre)}")
        assertTrue("nothing was drawn", out.length() > 0)
        assertTrue("expected the green fill at the centre", centre == 0xFF2F6D4F.toInt())
    }
}
