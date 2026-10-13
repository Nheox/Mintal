package dev.nheox.mintal.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.stats.AxisTicks
import dev.nheox.mintal.domain.stats.StatsRange
import dev.nheox.mintal.ui.components.EmptyState
import dev.nheox.mintal.ui.components.FocusBarChart
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.RatingTrendChart
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.StatLine
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Statistics (spec §5.7), including the rating line chart §5.5 asks for. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val viewModel: StatsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { MintalTopBar(title = "Statistics", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = viewModel::previousPeriod) { Text("Earlier") }
                Text(
                    text = state.label,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::nextPeriod) { Text("Later") }
            }

            val result = state.result
            if (result == null || result.isEmpty) {
                EmptyState(
                    message = "No sessions in this range yet. Finish one and the totals, " +
                        "the chart and the rating trend fill in here.",
                )
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            SectionCard(title = "Summary") {
                StatLine("Total focus", MintalTime.formatDuration(result.totalFocusSeconds))
                StatLine("Sessions", result.sessionCount.toString())
                StatLine(
                    "Completed",
                    "${result.successCount} of ${result.sessionCount} " +
                        "(${(result.successRate * 100).toInt()}%)",
                )
                StatLine("Average length", MintalTime.formatDuration(result.averageSessionSeconds))
                StatLine("Longest session", MintalTime.formatDuration(result.longestSessionSeconds))
                StatLine(
                    "Average rating",
                    result.averageRating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "not rated",
                )
                StatLine(
                    "Best focus day",
                    result.bestWeekday?.let { weekday ->
                        DayOfWeek.of(weekday).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    } ?: "—",
                )
                StatLine(
                    "Best focus hour",
                    result.bestHour?.let { hour -> "${hour.toString().padStart(2, '0')}:00" } ?: "—",
                )
                StatLine("Most planted mint", state.topSpeciesName ?: "—")
            }

            SectionCard(title = "Focus time") {
                val bucketSeconds = result.buckets.map { it.seconds }
                FocusBarChart(
                    values = bucketSeconds,
                    labels = result.buckets.map { it.label },
                    axis = AxisTicks.durations(bucketSeconds.maxOrNull() ?: 0),
                    xAxisLabel = when (state.range) {
                        StatsRange.DAY -> "hour of day"
                        StatsRange.WEEK -> "day of month"
                        StatsRange.MONTH -> "day of month"
                        StatsRange.YEAR -> "month"
                    },
                )
            }

            SectionCard(title = "Rating trend") {
                if (result.ratingTrend.isEmpty()) {
                    Text(
                        text = "Nothing rated in this range yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    RatingTrendChart(ratings = result.ratingTrend.map { it.rating })
                }
            }

            SectionCard(title = "Rating breakdown") {
                FocusBarChart(
                    values = result.ratingDistribution,
                    labels = listOf("1", "2", "3", "4", "5"),
                    height = 120.dp,
                    axis = AxisTicks.counts(result.ratingDistribution.maxOrNull() ?: 0),
                    xAxisLabel = "rating",
                    describeValue = { count -> "$count sessions" },
                )
            }

            SectionCard(title = "Tags") {
                result.tagRows.forEach { row ->
                    StatLine(
                        label = row.tagId?.let { state.tagNames[it] } ?: "untagged",
                        value = buildString {
                            append(MintalTime.formatDuration(row.seconds))
                            row.averageRating?.let {
                                append(" · ")
                                append(String.format(Locale.ROOT, "%.1f", it))
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
