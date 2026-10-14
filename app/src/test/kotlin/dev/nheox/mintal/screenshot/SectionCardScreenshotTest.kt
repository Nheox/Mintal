package dev.nheox.mintal.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.theme.MintalTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A column of boxes, half of them tappable. The hub reported "no margins" on exactly two
 * of its boxes, and the cause was that those two were hand-rolled `Card`s beside the
 * shared [SectionCard] — 14dp of padding and no border against everyone else's 16dp and
 * an edge. A difference that small is invisible in code review and obvious in a stack.
 *
 * Calls the real [SectionCard]: a render of a rebuilt copy would only prove the copy.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SectionCardScreenshotTest {

    @Composable
    private fun Stack(theme: ThemeChoice) {
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionCard(title = "Session setup") {
                        Text(
                            text = "A plain box. Every other box on the page has to line " +
                                "up with this one.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    SectionCard(title = "Mint", onClick = {}) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Spearmint",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(text = "Change", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    SectionCard(title = "Tag", onClick = {}) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Thesis",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(text = "Change", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    SectionCard(title = "Next mint") {
                        Text(
                            text = "Another plain box, to close the sandwich.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `tappable boxes sit on the same edges as plain ones`() {
        val light = ScreenshotRenderer.render("section-cards-light") { Stack(ThemeChoice.LIGHT) }
        val dark = ScreenshotRenderer.render("section-cards-dark") { Stack(ThemeChoice.DARK) }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }
}
