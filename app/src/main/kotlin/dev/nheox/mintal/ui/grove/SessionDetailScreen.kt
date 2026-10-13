package dev.nheox.mintal.ui.grove

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.ui.components.EmptyState
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.StatLine

/** Session detail (spec §5.6): note, tag and rating are editable; the session can be deleted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionDetailScreen(onBack: () -> Unit) {
    val viewModel: SessionDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTagPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { MintalTopBar(title = "Session", onBack = onBack) }) { padding ->
        val session = state.session
        if (session == null) {
            EmptyState(message = "This session is no longer here.", modifier = Modifier.padding(padding))
            return@Scaffold
        }
        // The note field edits a local draft and writes through, so the round trip to the
        // database cannot move the cursor while someone is typing.
        var noteDraft by remember(session.id) { mutableStateOf(session.note ?: "") }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard {
                StatLine("Started", MintalTime.formatTimeOfDay(session.startedAt))
                StatLine("Focused", MintalTime.formatDuration(session.actualSeconds))
                if (session.plannedSeconds > 0) {
                    StatLine("Planned", MintalTime.formatDuration(session.plannedSeconds))
                }
                StatLine(
                    "Outcome",
                    when (session.outcome) {
                        SessionOutcome.SUCCESS -> "completed"
                        SessionOutcome.GAVE_UP -> "gave up"
                        SessionOutcome.LEFT_APP -> "left the app"
                    },
                )
                StatLine("Mint", state.species?.commonName ?: session.speciesId)
                StatLine("Mode", session.mode.name.lowercase().replace('_', ' '))
            }

            SectionCard(title = "Rating") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionRules.RATING_RANGE.forEach { star ->
                        FilterChip(
                            selected = session.rating == star,
                            onClick = { viewModel.setRating(star) },
                            label = { Text(star.toString()) },
                        )
                    }
                }
                if (session.rating != null) {
                    TextButton(onClick = { viewModel.setRating(null) }) { Text("Clear rating") }
                }
            }

            SectionCard(title = "Tag") {
                Text(
                    text = state.tagName ?: "none",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { showTagPicker = true }) { Text("Change tag") }
            }

            OutlinedTextField(
                value = noteDraft,
                onValueChange = { typed ->
                    noteDraft = typed.take(SessionRules.NOTE_MAX_LENGTH)
                    viewModel.setNote(noteDraft)
                },
                label = { Text("Note") },
                supportingText = {
                    Text("${noteDraft.length}/${SessionRules.NOTE_MAX_LENGTH}")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete this session") }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showTagPicker) {
        AlertDialog(
            onDismissRequest = { showTagPicker = false },
            title = { Text("Choose a tag") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(
                        onClick = {
                            viewModel.setTag(null)
                            showTagPicker = false
                        },
                    ) { Text("No tag") }
                    state.tags.forEach { tag ->
                        TextButton(
                            onClick = {
                                viewModel.setTag(tag.id)
                                showTagPicker = false
                            },
                        ) { Text(tag.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagPicker = false }) { Text("Close") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this session?") },
            text = {
                Text(
                    "The time it recorded leaves your statistics and the Grove. " +
                        "Unlocks are recomputed, so this cannot strand a mint.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onBack)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Keep") }
            },
        )
    }
}
