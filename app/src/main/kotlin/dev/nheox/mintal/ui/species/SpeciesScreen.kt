package dev.nheox.mintal.ui.species

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.model.SpeciesStatus
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.domain.milestone.MilestoneEngine
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.SpeciesEmblem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeciesUiState(
    val statuses: List<SpeciesStatus> = emptyList(),
    val totalSuccessfulSeconds: Int = 0,
    val selectedId: String = "",
    val nextMilestoneMinutes: Int? = null,
    val pendingChoices: Int = 0,
    val progress: Float = 0f,
)

@HiltViewModel
class SpeciesViewModel @Inject constructor(
    speciesRepository: SpeciesRepository,
    sessionRepository: SessionRepository,
    private val preferences: MintalPreferences,
) : ViewModel() {

    val state: StateFlow<SpeciesUiState> = combine(
        speciesRepository.observeStatuses(),
        sessionRepository.observeSuccessfulSecondsTotal(),
        preferences.settings,
        speciesRepository.observePendingChoices(),
    ) { statuses, total, settings, pending ->
        val catalogue = statuses.map { it.species }
        SpeciesUiState(
            statuses = statuses,
            totalSuccessfulSeconds = total,
            selectedId = settings.selectedSpeciesId,
            nextMilestoneMinutes = MilestoneEngine.nextThresholdMinutes(catalogue, total),
            pendingChoices = pending,
            progress = MilestoneEngine.progressToNext(catalogue, total),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeciesUiState())

    fun select(id: String) = viewModelScope.launch { preferences.setSelectedSpecies(id) }
}

/**
 * The species catalogue (spec §5.1, §6.1): twelve real mints, locked ones visible with the
 * milestone that earns them. v2 presents these as Herbarium cards; the data is the same.
 */
@Composable
fun SpeciesScreen(onBack: () -> Unit, onChooseMint: () -> Unit) {
    val viewModel: SpeciesViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { MintalTopBar(title = "Mints", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "Progress") {
                    Text(
                        text = "${MintalTime.formatDuration(state.totalSuccessfulSeconds)} of " +
                            "successful focus so far.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val next = state.nextMilestoneMinutes
                    if (next != null) {
                        Text(
                            text = "Next milestone at ${next / 60} h — and you pick which " +
                                "mint fills it.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            drawStopIndicator = {},
                        )
                    } else {
                        Text(
                            text = "Every milestone is behind you.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (state.pendingChoices > 0) {
                        Button(
                            onClick = onChooseMint,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        ) {
                            Text(
                                if (state.pendingChoices == 1) {
                                    "Choose your next mint"
                                } else {
                                    "Choose ${state.pendingChoices} mints"
                                },
                            )
                        }
                    }
                }
            }

            items(items = state.statuses, key = { it.species.id }) { status ->
                val unlocked = status.isUnlocked
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.species.id == state.selectedId) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SpeciesEmblem(
                            species = status.species,
                            size = 56.dp,
                            unlocked = unlocked,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.species.commonName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = status.species.binomial +
                                    if (status.species.hybrid) " · hybrid" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = status.species.note,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            if (unlocked) {
                                if (status.species.id != state.selectedId) {
                                    TextButton(onClick = { viewModel.select(status.species.id) }) {
                                        Text("Grow this one")
                                    }
                                } else {
                                    Text(
                                        text = "Selected",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            } else {
                                Text(
                                    text = "Still locked — pick it at any milestone you " +
                                        "reach from here.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
