package dev.nheox.mintal.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import dev.nheox.mintal.ui.theme.MintalTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every mint on one sheet. The whole point of drawing species from botanical traits is
 * that they should be tellable apart at a glance — this is how that claim gets checked.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SpeciesSheetScreenshotTest {

    private fun catalogue(): List<Species> = runBlocking {
        SpeciesCatalog(RuntimeEnvironment.getApplication()).all()
    }

    @Composable
    private fun Sheet(species: List<Species>, theme: ThemeChoice) {
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    species.chunked(3).forEach { rowSpecies ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowSpecies.forEach { one ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    SpeciesEmblem(species = one, size = 104.dp)
                                    Text(
                                        text = one.commonName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `renders every mint, light and dark`() {
        val species = catalogue()
        assertEquals("the catalogue should ship twelve mints", 12, species.size)

        val light = ScreenshotRenderer.render("species-sheet-light") {
            Sheet(species, ThemeChoice.LIGHT)
        }
        val dark = ScreenshotRenderer.render("species-sheet-dark") {
            Sheet(species, ThemeChoice.DARK)
        }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }
}
