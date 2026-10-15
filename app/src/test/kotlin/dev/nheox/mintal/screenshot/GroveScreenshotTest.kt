package dev.nheox.mintal.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.domain.grove.GroveLayoutBuilder
import dev.nheox.mintal.domain.grove.PlantBuilder
import dev.nheox.mintal.ui.components.GroveCanvas
import dev.nheox.mintal.ui.theme.MintalTheme
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.grove.FailureDisplay
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicLong

/**
 * The Grove, rendered. There is no emulator here and there cannot be, so this is how the
 * bed is actually looked at: a plant per species at different ages, a mature one in
 * flower, and the wilted stubs of two failed sessions.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class GroveScreenshotTest {

    private val nextId = AtomicLong(1L)

    private fun catalogue(): List<Species> = runBlocking {
        SpeciesCatalog(RuntimeEnvironment.getApplication()).all()
    }

    private fun sessions(
        speciesId: String,
        count: Int,
        outcome: SessionOutcome = SessionOutcome.SUCCESS,
    ): List<SessionEntity> = (0 until count).map {
        val id = nextId.getAndIncrement()
        SessionEntity(
            id = id,
            startedAt = 1_760_000_000_000L + id * 3_600_000L,
            endedAt = 1_760_000_000_000L + id * 3_600_000L + 1_500_000L,
            plannedSeconds = 1500,
            actualSeconds = 1500,
            mode = SessionMode.COUNTDOWN,
            outcome = outcome,
            speciesId = speciesId,
            tagId = null,
            rating = null,
            note = null,
            timezoneId = "UTC",
        )
    }

    @Composable
    private fun Bed(species: List<Species>, theme: ThemeChoice) {
        val byId = species.associateBy { it.id }
        val history = buildList {
            addAll(sessions("spicata", PlantBuilder.MATURE_NODES + 2))
            addAll(sessions("piperita", 7))
            addAll(sessions("suaveolens", PlantBuilder.MATURE_NODES))
            addAll(sessions("arvensis", 4))
            addAll(sessions("requienii", 9))
            addAll(sessions("cervina", PlantBuilder.MATURE_NODES))
            addAll(sessions("longifolia", 2))
            addAll(sessions("aquatica", 6, outcome = SessionOutcome.GAVE_UP))
        }
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                // Surface forces its child to fill, so the band goes inside a Column:
                // 420dp is roughly the height the Grove screen gives the canvas, and
                // rendering it any taller would flatter the layout.
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .background(mintalPalette.grovePaper),
                    ) {
                        GroveCanvas(
                            layout = GroveLayoutBuilder.build(
                                history,
                                byId,
                                failures = stubsShown(history),
                            ),
                            speciesById = byId,
                            onDayTap = {},
                        )
                    }
                }
            }
        }
    }

    /**
     * Failures are hidden in the shipped default, so a render that wants to show what a
     * wilted stub looks like has to ask for them. Anchored on the newest session rather
     * than on the wall clock, or the stubs would be years old and faded to nothing.
     */
    private fun stubsShown(history: List<SessionEntity>) = FailureDisplay.Fading(
        todayEpochDay = MintalTime.epochDayOf(history.maxOf { it.startedAt }),
    )

    @Composable
    private fun Stages(species: Species, theme: ThemeChoice) {
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    listOf(1, 3, 6, 9, PlantBuilder.MATURE_NODES).chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                            row.forEach { nodes ->
                                val history = sessions(species.id, nodes)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .background(mintalPalette.grovePaper),
                                    ) {
                                        GroveCanvas(
                                            layout = GroveLayoutBuilder.build(
                                                history,
                                                mapOf(species.id to species),
                                            ),
                                            speciesById = mapOf(species.id to species),
                                            onDayTap = {},
                                        )
                                    }
                                    Text(
                                        text = "$nodes",
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

    /**
     * The same four failures at four ages, so the fade can be looked at rather than
     * trusted. A stub two weeks old is not drawn at all, which is the point of the
     * window: a bad afternoon should not still be standing in the bed next month.
     */
    @Composable
    private fun FadingStubs(species: List<Species>, theme: ThemeChoice) {
        val byId = species.associateBy { it.id }
        val today = 20_000L
        val history = listOf(0, 4, 9, 13, FailureDisplay.DEFAULT_FADE_DAYS)
            .mapIndexed { index, age ->
                val id = nextId.getAndIncrement()
                SessionEntity(
                    id = id,
                    startedAt = MintalTime.startOfDayMillis(today - age) + index * 60_000L,
                    endedAt = MintalTime.startOfDayMillis(today - age) + index * 60_000L,
                    plannedSeconds = 1500,
                    actualSeconds = 600,
                    mode = SessionMode.COUNTDOWN,
                    outcome = SessionOutcome.LEFT_APP,
                    speciesId = "spicata",
                    tagId = null,
                    rating = null,
                    note = null,
                    timezoneId = "UTC",
                )
            } + sessions("spicata", 6)

        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .background(mintalPalette.grovePaper),
                    ) {
                        GroveCanvas(
                            layout = GroveLayoutBuilder.build(
                                history,
                                byId,
                                failures = FailureDisplay.Fading(todayEpochDay = today),
                            ),
                            speciesById = byId,
                            onDayTap = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `renders wilted stubs fading out with age`() {
        val shot = ScreenshotRenderer.render("grove-stubs-fading") {
            FadingStubs(catalogue(), ThemeChoice.LIGHT)
        }
        assertTrue(shot.length() > 0)
    }

    @Test
    fun `renders a mixed bed, light and dark`() {
        val species = catalogue()
        val light = ScreenshotRenderer.render("grove-bed-light") { Bed(species, ThemeChoice.LIGHT) }
        val dark = ScreenshotRenderer.render("grove-bed-dark") { Bed(species, ThemeChoice.DARK) }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }

    @Composable
    private fun YoungBed(species: List<Species>, theme: ThemeChoice) {
        val byId = species.associateBy { it.id }
        // Three sessions, three days old. The framing should suit what is there rather
        // than the mature plant it has not grown into.
        val history = sessions("spicata", 2) + sessions("piperita", 1)
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .background(mintalPalette.grovePaper),
                    ) {
                        GroveCanvas(
                            layout = GroveLayoutBuilder.build(
                                history,
                                byId,
                                failures = stubsShown(history),
                            ),
                            speciesById = byId,
                            onDayTap = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `renders a young bed close enough to see`() {
        val shot = ScreenshotRenderer.render("grove-young") {
            YoungBed(catalogue(), ThemeChoice.LIGHT)
        }
        assertTrue(shot.length() > 0)
    }

    @Test
    fun `renders one mint at every growth stage`() {
        val spearmint = catalogue().first { it.id == "spicata" }
        val sheet = ScreenshotRenderer.render("grove-stages") {
            Stages(spearmint, ThemeChoice.LIGHT)
        }
        assertTrue(sheet.length() > 0)
    }
}
