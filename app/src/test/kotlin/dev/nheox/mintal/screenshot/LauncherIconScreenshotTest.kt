package dev.nheox.mintal.screenshot

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import dev.nheox.mintal.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The store icon, drawn from the launcher icon the app actually ships.
 *
 * F-Droid cannot render an adaptive icon, and with minSdk 26 the APK carries no PNG to
 * fall back on, so the listing needs `fastlane/metadata/android/en-US/images/icon.png`.
 * Drawing it from `R.mipmap.ic_launcher` rather than redrawing it by hand means the two
 * cannot drift: after any change to the launcher icon, run this and copy
 * `build/screenshots/store-icon.png` over the fastlane file.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class LauncherIconScreenshotTest {

    @Test
    fun `store icon is the launcher leaf, centred in a circle`() {
        val icon = RuntimeEnvironment.getApplication()
            .getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable

        // Framed on the leaf as drawn, not on the 72dp a launcher shows: the leaf sits low
        // in the layer and its base touches that window's edge, so a circle there cuts it
        // flat. Measured rather than hard-coded so a redrawn leaf is framed the same way.
        val probe = Bitmap.createBitmap(PROBE_PX, PROBE_PX, Bitmap.Config.ARGB_8888)
        icon.foreground.setBounds(0, 0, PROBE_PX, PROBE_PX)
        icon.foreground.draw(Canvas(probe))
        val pixels = IntArray(PROBE_PX * PROBE_PX)
        probe.getPixels(pixels, 0, PROBE_PX, 0, 0, PROBE_PX, PROBE_PX)
        var left = PROBE_PX
        var top = PROBE_PX
        var right = 0
        var bottom = 0
        for (y in 0 until PROBE_PX) {
            for (x in 0 until PROBE_PX) {
                if (Color.alpha(pixels[y * PROBE_PX + x]) > 0) {
                    left = min(left, x)
                    top = min(top, y)
                    right = max(right, x)
                    bottom = max(bottom, y)
                }
            }
        }
        assertTrue("the foreground drew nothing", right > left && bottom > top)

        val layerPx = GLYPH_SHARE * SIZE_PX * PROBE_PX / max(right - left, bottom - top)
        val scale = layerPx / PROBE_PX
        val layerLeft = (SIZE_PX / 2f - (left + right) / 2f * scale).roundToInt()
        val layerTop = (SIZE_PX / 2f - (top + bottom) / 2f * scale).roundToInt()
        val layers = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        Canvas(layers).let { canvas ->
            for (layer in listOf(icon.background, icon.foreground)) {
                layer.setBounds(
                    layerLeft,
                    layerTop,
                    layerLeft + layerPx.roundToInt(),
                    layerTop + layerPx.roundToInt(),
                )
                layer.draw(canvas)
            }
        }

        // Masked by painting through a shader, because a clip path is not anti-aliased and
        // leaves a stepped rim.
        val masked = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(layers, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(masked).drawCircle(SIZE_PX / 2f, SIZE_PX / 2f, SIZE_PX / 2f, paint)

        ScreenshotRenderer.outputDirectory.mkdirs()
        val out = ScreenshotRenderer.outputDirectory.resolve("store-icon.png")
        out.outputStream().use { masked.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SCREENSHOT ${out.absolutePath} ${SIZE_PX}x$SIZE_PX")
        assertTrue(out.length() > 0)
    }

    private companion object {
        /** What F-Droid and most stores ask for. */
        const val SIZE_PX = 512

        /** The 108dp layer at ten pixels per dp, fine enough to find the leaf's edges. */
        const val PROBE_PX = 1080

        /** The leaf's longer side as a share of the circle's diameter. */
        const val GLYPH_SHARE = 0.74f
    }
}
