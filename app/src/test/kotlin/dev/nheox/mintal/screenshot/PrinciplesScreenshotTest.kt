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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.onboarding.PrinciplesBody
import dev.nheox.mintal.ui.onboarding.PrinciplesFooter
import dev.nheox.mintal.ui.theme.MintalTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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
    private fun Gate(theme: ThemeChoice, dismissed: Boolean, beat: Float) {
        MintalTheme(theme = theme) {
            Scaffold(
                bottomBar = {
                    PrinciplesFooter(
                        dismissed = dismissed,
                        onDismissedChange = {},
                        actionLabel = "Start focusing",
                        onAction = {},
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mintal",
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrinciplesBody(tutorialSpecies = spearmint(), tutorialProgress = beat)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    private fun spearmint() = runBlocking {
        SpeciesCatalog(RuntimeEnvironment.getApplication()).all().first()
    }

    @Test
    fun `renders the principles gate, light and dark`() {
        // One render per beat, so all three frames of the tutorial get looked at.
        val light = ScreenshotRenderer.render("principles-light") {
            Gate(ThemeChoice.LIGHT, dismissed = false, beat = 0.6f)
        }
        val phone = ScreenshotRenderer.render("principles-phone-beat") {
            Gate(ThemeChoice.LIGHT, dismissed = false, beat = 1.6f)
        }
        val dark = ScreenshotRenderer.render("principles-dark") {
            Gate(ThemeChoice.DARK, dismissed = true, beat = 2.7f)
        }
        assertTrue(light.length() > 0 && phone.length() > 0 && dark.length() > 0)
    }
}
