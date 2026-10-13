package dev.nheox.mintal.ui.grove

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.ui.components.EmptyState
import dev.nheox.mintal.ui.components.GroveCanvas
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.groveDaySummary
import dev.nheox.mintal.ui.theme.mintalPalette

/**
 * The Grove (spec §5.6): one spreading mint network, pannable and zoomable, with the days
 * also listed underneath so the growth is reachable without hunting for it on the canvas.
 */
@Composable
fun GroveScreen(
    onBack: () -> Unit,
    onOpenDay: (Long) -> Unit,
) {
    val viewModel: GroveViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = mintalPalette

    Scaffold(topBar = { MintalTopBar(title = "The Grove", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = MintalTime.formatDuration(state.totalSuccessfulSeconds),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${state.sessionCount} sessions · ${state.plantCount} plants · " +
                        "${state.days.size} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(palette.grovePaper),
                contentAlignment = Alignment.Center,
            ) {
                if (state.layout.isEmpty) {
                    EmptyState(
                        message = "Nothing has grown yet. Finish a session and the first " +
                            "mint goes in — every session after that adds a node to it, " +
                            "until it is full grown and the next one starts a new plant.",
                    )
                } else {
                    GroveCanvas(
                        layout = state.layout,
                        speciesById = state.speciesById,
                        onDayTap = onOpenDay,
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                items(items = state.days, key = { it.epochDay }) { day ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDay(day.epochDay) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = MintalTime.formatDayLabel(day.epochDay),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = groveDaySummary(day.seconds, day.sessionCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
