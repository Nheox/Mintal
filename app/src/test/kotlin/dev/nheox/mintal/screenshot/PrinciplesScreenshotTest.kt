package dev.nheox.mintal.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.ui.onboarding.PrinciplesBody
import dev.nheox.mintal.ui.theme.MintalTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The principles stand in front of the hub at every launch until they are dismissed, so
 * they have to fit on one screen and the tick-box has to be findable without scrolling
 * past the button.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class PrinciplesScreenshotTest {

    @Composable
    private fun Gate(theme: ThemeChoice, dismissed: Boolean) {
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Mintal", style = MaterialTheme.typography.displayLarge)
                    PrinciplesBody(dismissed = dismissed)
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text("Start focusing")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    @Test
    fun `renders the principles gate, light and dark`() {
        val light = ScreenshotRenderer.render("principles-light") {
            Gate(ThemeChoice.LIGHT, dismissed = false)
        }
        val dark = ScreenshotRenderer.render("principles-dark") {
            Gate(ThemeChoice.DARK, dismissed = true)
        }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }
}
