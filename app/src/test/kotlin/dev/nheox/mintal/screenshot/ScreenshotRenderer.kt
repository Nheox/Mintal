package dev.nheox.mintal.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.graphics.Insets
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * Renders a composable to a PNG on the JVM, so the UI can be looked at without an
 * emulator or a device.
 *
 * Draws the decor view straight to a bitmap rather than going through the Compose test
 * rule, whose idling machinery times out under Robolectric. Screens are rendered as pure
 * content functions with hand-made state, so nothing here needs a database, Hilt or a
 * running service.
 */
object ScreenshotRenderer {

    val outputDirectory: File = File("build/screenshots")

    /**
     * [navigationBarPx] fakes a phone with on-screen back/home/recents buttons. Mintal
     * draws edge to edge, so anything anchored to the bottom has to keep itself clear of
     * them — and a render on a device with gesture navigation will never show that it
     * does not.
     */
    fun render(
        fileName: String,
        navigationBarPx: Int = 0,
        content: @Composable () -> Unit,
    ): File {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent { content() }
        shadowOf(Looper.getMainLooper()).idle()

        val view: View = activity.window.decorView
        if (navigationBarPx > 0) {
            val bars = Insets.of(0, 0, 0, navigationBarPx)
            view.dispatchApplyWindowInsets(
                WindowInsets.Builder()
                    .setInsets(WindowInsets.Type.navigationBars(), bars)
                    .setInsets(WindowInsets.Type.systemBars(), bars)
                    .build(),
            )
            shadowOf(Looper.getMainLooper()).idle()
        }
        var width = view.width
        var height = view.height
        if (width <= 0 || height <= 0) {
            width = 1080
            height = 1920
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
            shadowOf(Looper.getMainLooper()).idle()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        outputDirectory.mkdirs()
        val out = outputDirectory.resolve("$fileName.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        controller.pause().stop().destroy()
        println("SCREENSHOT ${out.absolutePath} ${width}x$height")
        return out
    }
}
