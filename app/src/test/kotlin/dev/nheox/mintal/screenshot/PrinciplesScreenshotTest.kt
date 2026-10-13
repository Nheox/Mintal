package dev.nheox.mintal.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.components.SpeciesEmblem
import dev.nheox.mintal.ui.onboarding.FocusTutorialFrame
import dev.nheox.mintal.ui.onboarding.MINTAL_SLOGAN
import dev.nheox.mintal.ui.onboarding.PrinciplesBody
import dev.nheox.mintal.ui.onboarding.PrinciplesFooter
import dev.nheox.mintal.ui.onboarding.PrinciplesTick
import dev.nheox.mintal.ui.theme.MintalTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * First launch, slide by slide, and the principles gate that stands in front of the hub
 * afterwards. Each slide has to carry one idea and fit a phone: the version before this
 * put the tutorial and all three principle cards on one page, which fit nothing.
 *
 * The tutorial is pinned to a frame rather than animated — an infinite animation never
 * lets Robolectric's looper go idle, and the renderer hangs on it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class PrinciplesScreenshotTest {

    private fun spearmint(): Species = runBlocking {
        SpeciesCatalog(RuntimeEnvironment.getApplication()).all().first()
    }

    @Composable
    private fun Slide(
        theme: ThemeChoice,
        page: Int,
        slides: Int = 4,
        tick: Boolean = false,
        actionLabel: String = "Next",
        content: @Composable () -> Unit,
    ) {
        MintalTheme(theme = theme) {
            Scaffold(
                bottomBar = {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            LinearProgressIndicator(
                                progress = { (page + 1f) / slides },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            )
                            if (tick) PrinciplesTick(dismissed = false, onDismissedChange = {})
                            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                                Text(actionLabel)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
            ) { padding ->
                BoxWithConstraints(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            14.dp,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        content()
                    }
                }
            }
        }
    }

    @Composable
    private fun Title(species: Species) {
        Text(
            text = "Mintal",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = MINTAL_SLOGAN,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeciesEmblem(species = species, size = 300.dp)
        }
    }

    @Test
    fun `renders the first-run slides`() {
        val species = spearmint()
        val title = ScreenshotRenderer.render("onboarding-1-title") {
            Slide(ThemeChoice.LIGHT, page = 0) { Title(species) }
        }
        val tutorial = ScreenshotRenderer.render("onboarding-2-tutorial") {
            Slide(ThemeChoice.LIGHT, page = 1) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                FocusTutorialFrame(species = species, progress = 0.6f)
            }
        }
        val principles = ScreenshotRenderer.render("onboarding-3-principles") {
            Slide(ThemeChoice.LIGHT, page = 2, tick = true) {
                Text(
                    text = "What Mintal promises",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrinciplesBody()
            }
        }
        assertTrue(title.length() > 0 && tutorial.length() > 0 && principles.length() > 0)
    }

    @Test
    fun `the way forward clears a three-button navigation bar`() {
        // The reported bug: on a phone with on-screen back/home/recents, the Next button
        // was underneath them.
        val species = spearmint()
        val slide = ScreenshotRenderer.render("onboarding-navbar", navigationBarPx = 132) {
            Slide(ThemeChoice.LIGHT, page = 1) {
                FocusTutorialFrame(species = species, progress = 0.6f)
            }
        }
        // And the gate, which is the screen seen at every launch.
        val gate = ScreenshotRenderer.render("principles-navbar", navigationBarPx = 132) {
            MintalTheme(theme = ThemeChoice.LIGHT) {
                Scaffold(
                    bottomBar = {
                        PrinciplesFooter(
                            dismissed = false,
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
                    ) { PrinciplesBody() }
                }
            }
        }
        assertTrue(slide.length() > 0 && gate.length() > 0)
    }

    @Test
    fun `renders the second and third tutorial beats`() {
        val species = spearmint()
        val phone = ScreenshotRenderer.render("onboarding-tutorial-phone") {
            Slide(ThemeChoice.LIGHT, page = 1) {
                FocusTutorialFrame(species = species, progress = 1.6f)
            }
        }
        val mint = ScreenshotRenderer.render("onboarding-tutorial-mint") {
            Slide(ThemeChoice.DARK, page = 1) {
                FocusTutorialFrame(species = species, progress = 2.7f)
            }
        }
        assertTrue(phone.length() > 0 && mint.length() > 0)
    }

    @Test
    fun `renders the principles gate, light and dark`() {
        val gate: @Composable (ThemeChoice, Boolean) -> Unit = { theme, dismissed ->
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
                        Text(
                            text = MINTAL_SLOGAN,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PrinciplesBody()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        val light = ScreenshotRenderer.render("principles-light") {
            gate(ThemeChoice.LIGHT, false)
        }
        val dark = ScreenshotRenderer.render("principles-dark") { gate(ThemeChoice.DARK, true) }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }
}
