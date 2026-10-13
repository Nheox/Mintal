package dev.nheox.mintal.ui.session

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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.SpeciesEmblem
import dev.nheox.mintal.ui.components.StatLine

/**
 * Session result (spec §5.4, §5.5): what happened, stated plainly and without moralising,
 * plus the rating and note — both offered even when the session failed, because a failed
 * session is often the one worth annotating.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionResultScreen(
    onDone: () -> Unit,
    onStartAnother: () -> Unit,
    onChooseMint: () -> Unit,
) {
    val viewModel: SessionResultViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val outcomeText by viewModel.outcomeText.collectAsStateWithLifecycle()

    Scaffold(topBar = { MintalTopBar(title = "Session result") }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = outcomeText, style = MaterialTheme.typography.headlineSmall)

            val session = state.session
            if (state.discarded || session == null) {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Home") }
                return@Column
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.species?.let { species ->
                    SpeciesEmblem(
                        species = species,
                        size = 72.dp,
                        unlocked = session.outcome == SessionOutcome.SUCCESS,
                    )
                    Column {
                        Text(
                            text = species.commonName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = species.binomial,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionCard {
                StatLine("Focused", MintalTime.formatDuration(session.actualSeconds))
                if (session.plannedSeconds > 0) {
                    StatLine("Planned", MintalTime.formatDuration(session.plannedSeconds))
                }
                StatLine("Started", MintalTime.formatTimeOfDay(session.startedAt))
                StatLine("Tag", state.tagName ?: "none")
            }

            if (state.pendingUnlockChoices > 0) {
                SectionCard(
                    title = if (state.pendingUnlockChoices == 1) {
                        "A new mint is yours to choose"
                    } else {
                        "${state.pendingUnlockChoices} mints are yours to choose"
                    },
                ) {
                    Text(
                        text = "A milestone has been reached. Which mint fills it is your " +
                            "call — every one still locked is on the table.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.save()
                            onChooseMint()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Choose now") }
                }
            }

            SectionCard(title = "How well did that go?") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionRules.RATING_RANGE.forEach { star ->
                        FilterChip(
                            selected = state.rating == star,
                            onClick = { viewModel.setRating(star) },
                            label = { Text(star.toString()) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Always skippable — an unrated session stays unrated, never a zero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.rating != null) {
                        TextButton(onClick = { viewModel.setRating(null) }) { Text("Clear") }
                    }
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note (optional)") },
                supportingText = {
                    Text("${state.note.length}/${SessionRules.NOTE_MAX_LENGTH}")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Button(
                onClick = {
                    viewModel.save()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }

            OutlinedButton(
                onClick = {
                    viewModel.save()
                    onStartAnother()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save and start another") }

            TextButton(
                onClick = {
                    viewModel.dismiss()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Discard changes and go home") }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
