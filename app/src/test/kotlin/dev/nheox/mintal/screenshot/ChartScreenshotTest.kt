package dev.nheox.mintal.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.domain.stats.AxisTicks
import dev.nheox.mintal.ui.components.FocusBarChart
import dev.nheox.mintal.ui.components.RatingTrendChart
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.theme.MintalTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The statistics charts at the sizes that broke them on a real phone: twenty-four hour
 * labels on a phone-width axis, which used to truncate to "0 0 0 0 1 1 1 2", and a
 * fifteen-second day, which used to label its axis "15 s / 7 s / 0".
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ChartScreenshotTest {

    private val hourLabels = (0..23).map { it.toString().padStart(2, '0') }

    @Composable
    private fun Frame(theme: ThemeChoice, content: @Composable () -> Unit) {
        MintalTheme(theme = theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { content() }
            }
        }
    }

    @Composable
    private fun FocusSheet(theme: ThemeChoice) {
        // Exactly the day from the device screenshot: one short session at 10:00.
        val oneShortSession = List(24) { hour -> if (hour == 10) 15 else 0 }
        val realDay = List(24) { hour ->
            when (hour) {
                9 -> 50 * 60
                10 -> 25 * 60
                14 -> 75 * 60
                20 -> 40 * 60
                else -> 0
            }
        }
        Frame(theme) {
            SectionCard(title = "Focus time · one 15 second session") {
                FocusBarChart(
                    values = oneShortSession,
                    labels = hourLabels,
                    axis = AxisTicks.durations(oneShortSession.max()),
                    xAxisLabel = "hour of day",
                )
            }
            SectionCard(title = "Focus time · a real day") {
                FocusBarChart(
                    values = realDay,
                    labels = hourLabels,
                    axis = AxisTicks.durations(realDay.max()),
                    xAxisLabel = "hour of day",
                )
            }
            SectionCard(title = "Focus time · a month") {
                val month = List(31) { day -> if (day % 3 == 0) (day + 4) * 600 else 0 }
                FocusBarChart(
                    values = month,
                    labels = (1..31).map { it.toString() },
                    axis = AxisTicks.durations(month.max()),
                    xAxisLabel = "day of month",
                )
            }
        }
    }

    @Composable
    private fun RatingSheet(theme: ThemeChoice) {
        Frame(theme) {
            SectionCard(title = "Rating breakdown · one rated session") {
                val tally = listOf(0, 0, 1, 0, 0)
                FocusBarChart(
                    values = tally,
                    labels = listOf("1", "2", "3", "4", "5"),
                    height = 120.dp,
                    axis = AxisTicks.counts(tally.max()),
                    xAxisLabel = "rating",
                    describeValue = { "$it sessions" },
                )
            }
            SectionCard(title = "Rating breakdown · a busy month") {
                val tally = listOf(2, 5, 14, 23, 9)
                FocusBarChart(
                    values = tally,
                    labels = listOf("1", "2", "3", "4", "5"),
                    height = 150.dp,
                    axis = AxisTicks.counts(tally.max()),
                    xAxisLabel = "rating",
                    describeValue = { "$it sessions" },
                )
            }
            SectionCard(title = "Rating trend") {
                RatingTrendChart(ratings = listOf(3, 4, 2, 5, 4, 4, 5))
            }
        }
    }

    @Test
    fun `renders the focus charts, light and dark`() {
        val light = ScreenshotRenderer.render("charts-focus-light") {
            FocusSheet(ThemeChoice.LIGHT)
        }
        val dark = ScreenshotRenderer.render("charts-focus-dark") { FocusSheet(ThemeChoice.DARK) }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }

    @Test
    fun `renders the rating charts, light and dark`() {
        val light = ScreenshotRenderer.render("charts-rating-light") {
            RatingSheet(ThemeChoice.LIGHT)
        }
        val dark = ScreenshotRenderer.render("charts-rating-dark") { RatingSheet(ThemeChoice.DARK) }
        assertTrue(light.length() > 0 && dark.length() > 0)
    }
}
