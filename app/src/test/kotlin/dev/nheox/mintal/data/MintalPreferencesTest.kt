package dev.nheox.mintal.data

import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.prefs.SpeciesViewMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The catalogue layout is a preference, not screen state: choosing Cards once should
 * still mean Cards the next time Species is opened, including after the process has died.
 * It used to be a `rememberSaveable` inside the screen, so it survived rotation and was
 * forgotten the moment the screen left the back stack.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MintalPreferencesTest {

    private fun preferences() = MintalPreferences(RuntimeEnvironment.getApplication())

    @Test
    fun `the catalogue layout defaults to the list`() = runBlocking {
        assertEquals(SpeciesViewMode.LIST, preferences().settings.first().speciesViewMode)
    }

    @Test
    fun `choosing cards outlives the screen that chose it`() = runBlocking {
        val prefs = preferences()
        prefs.setSpeciesViewMode(SpeciesViewMode.CARDS)

        // A second instance reads the same store, which is what a later visit to the
        // screen — or a later launch of the app — actually does.
        assertEquals(SpeciesViewMode.CARDS, preferences().settings.first().speciesViewMode)
    }

    @Test
    fun `switching back to the list is remembered too`() = runBlocking {
        val prefs = preferences()
        prefs.setSpeciesViewMode(SpeciesViewMode.CARDS)
        prefs.setSpeciesViewMode(SpeciesViewMode.LIST)

        assertEquals(SpeciesViewMode.LIST, preferences().settings.first().speciesViewMode)
    }

    @Test
    fun `a restored backup carries the layout with it`() = runBlocking {
        val prefs = preferences()
        prefs.setSpeciesViewMode(SpeciesViewMode.LIST)
        // replaceAll is the path a backup restore takes (spec §7.1); a field missing from
        // it restores as whatever was already on the device, which is the bug this guards.
        prefs.replaceAll(prefs.settings.first().copy(speciesViewMode = SpeciesViewMode.CARDS))

        assertEquals(SpeciesViewMode.CARDS, preferences().settings.first().speciesViewMode)
    }
}
