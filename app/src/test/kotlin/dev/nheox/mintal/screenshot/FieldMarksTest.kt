package dev.nheox.mintal.screenshot

import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.species.FieldMarks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The card describes each mint by reading its own drawing parameters back out in words,
 * which is only an improvement on hand-written prose if the words are actually words.
 * The failure this guards is an enum constant reaching the screen: a `when` that gains a
 * branch returning `hue.name` prints "LEAF_BLUE" at someone and nothing else notices.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FieldMarksTest {

    private val shouty = Regex("[A-Z]{2,}(_[A-Z]+)*")

    @Test
    fun `every mint in the catalogue describes itself in plain words`() {
        val catalogue = runBlocking { SpeciesCatalog(RuntimeEnvironment.getApplication()).all() }
        assertEquals("the catalogue should ship twelve mints", 12, catalogue.size)

        catalogue.forEach { species ->
            val marks = FieldMarks.of(species)
            assertEquals(
                "${species.id} should have a leaf, a stem, a flower and a habit line",
                listOf("Leaf", "Stem", "Flowers", "Habit"),
                marks.map { it.label },
            )
            marks.forEach { mark ->
                assertTrue(
                    "${species.id} ${mark.label} should say something",
                    mark.value.length > 8,
                )
                assertTrue(
                    "${species.id} ${mark.label} should be a sentence: '${mark.value}'",
                    mark.value.endsWith("."),
                )
                assertFalse(
                    "${species.id} ${mark.label} leaks an enum constant: '${mark.value}'",
                    shouty.containsMatchIn(mark.value),
                )
            }
        }
    }

    @Test
    fun `the traits that separate two mints separate their descriptions`() {
        val catalogue = runBlocking { SpeciesCatalog(RuntimeEnvironment.getApplication()).all() }
        val leafLines = catalogue.associate { it.id to FieldMarks.of(it).first().value }

        // Spearmint is the silhouette every other mint differs from; apple mint is the
        // one furthest from it. If these two read the same, the mapping is broken.
        assertTrue(leafLines.getValue("spicata") != leafLines.getValue("suaveolens"))
        assertTrue(leafLines.getValue("spicata").contains("lance-shaped"))
        assertTrue(leafLines.getValue("suaveolens").contains("woolly"))
    }
}
