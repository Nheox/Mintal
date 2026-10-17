package dev.nheox.mintal.screenshot

import androidx.compose.runtime.Composable
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesStatus
import dev.nheox.mintal.data.prefs.SpeciesViewMode
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.species.SpeciesContent
import dev.nheox.mintal.ui.species.SpeciesUiState
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
 * The herbarium card: one mint filling the window, with everything the catalogue knows
 * about it. It carries the largest drawing of a plant anywhere in the app — four times
 * the emblem size — so it is the render that shows whether the procedural art holds up
 * when it is not a thumbnail.
 *
 * Renders [SpeciesContent] itself rather than a rebuilt copy of it, so what is checked
 * is the screen the app actually shows.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SpeciesCardScreenshotTest {

    private fun catalogue(): List<Species> = runBlocking {
        SpeciesCatalog(RuntimeEnvironment.getApplication()).all()
    }

    /** Four unlocked, the rest still locked: the state a player is in for a long while. */
    private fun state(catalogue: List<Species>): SpeciesUiState = SpeciesUiState(
        statuses = catalogue.mapIndexed { index, species ->
            SpeciesStatus(species = species, unlockedAt = if (index < 4) 1_700_000_000_000L else null)
        },
        totalSuccessfulSeconds = 7 * 3600 + 20 * 60,
        selectedId = catalogue.first().id,
        nextMilestoneMinutes = 600,
        pendingChoices = 0,
        progress = 0.73f,
    )

    @Composable
    private fun Screen(
        state: SpeciesUiState,
        mode: SpeciesViewMode,
        focused: Int,
        theme: ThemeChoice,
    ) {
        MintalTheme(theme = theme) {
            SpeciesContent(
                state = state,
                mode = mode,
                focused = focused,
                onBack = {},
                onChooseMint = {},
                onSelect = {},
                onModeChange = {},
                onFocusedChange = {},
            )
        }
    }

    @Test
    fun `renders the card view, light and dark`() {
        val state = state(catalogue())

        // Spearmint, which is selected, and apple mint, which is unlocked but not.
        val light = ScreenshotRenderer.render("species-card-light") {
            Screen(state, SpeciesViewMode.CARDS, focused = 0, theme = ThemeChoice.LIGHT)
        }
        val dark = ScreenshotRenderer.render("species-card-dark") {
            Screen(state, SpeciesViewMode.CARDS, focused = 2, theme = ThemeChoice.DARK)
        }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }

    @Test
    fun `renders a locked mint's card`() {
        val state = state(catalogue())
        val locked = ScreenshotRenderer.render("species-card-locked") {
            Screen(state, SpeciesViewMode.CARDS, focused = 8, theme = ThemeChoice.LIGHT)
        }
        assertTrue(locked.length() > 0)
    }

    @Test
    fun `renders the list view it toggles from`() {
        val state = state(catalogue())
        val list = ScreenshotRenderer.render("species-list-light") {
            Screen(state, SpeciesViewMode.LIST, focused = 0, theme = ThemeChoice.LIGHT)
        }
        assertTrue(list.length() > 0)
    }
}
