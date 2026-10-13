package dev.nheox.mintal.ui.grove

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.ui.components.EmptyState
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SpeciesEmblem

/** Day detail (spec §5.6): a day's sessions, with outcome, tag, rating and note. */
@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val viewModel: DayDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MintalTopBar(
                title = state.label.ifEmpty { "Day" },
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "${MintalTime.formatDuration(state.totalSeconds)} · " +
                    "${state.successCount}/${state.rows.size} completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.rows.isEmpty()) {
                EmptyState(message = "No sessions on this day.")
                return@Column
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = state.rows, key = { it.session.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSession(row.session.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.species?.let { species ->
                            SpeciesEmblem(
                                species = species,
                                size = 40.dp,
                                unlocked = row.session.outcome == SessionOutcome.SUCCESS,
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                text = "${MintalTime.formatTimeOfDay(row.session.startedAt)} · " +
                                    MintalTime.formatDuration(row.session.actualSeconds),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = buildString {
                                    append(
                                        when (row.session.outcome) {
                                            SessionOutcome.SUCCESS -> "completed"
                                            SessionOutcome.GAVE_UP -> "gave up"
                                            SessionOutcome.LEFT_APP -> "left the app"
                                        },
                                    )
                                    row.tagName?.let { append(" · ").append(it) }
                                    row.session.rating?.let { append(" · rated ").append(it) }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            row.session.note?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
