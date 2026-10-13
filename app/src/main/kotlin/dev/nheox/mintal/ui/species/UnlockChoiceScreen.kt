package dev.nheox.mintal.ui.species

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.SpeciesEmblem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnlockChoiceUiState(
    val loading: Boolean = true,
    val pending: Int = 0,
    val locked: List<Species> = emptyList(),
    val unlockedCount: Int = 0,
    val catalogueSize: Int = 0,
    val totalSuccessfulSeconds: Int = 0,
) {
    val isFirstEver: Boolean get() = unlockedCount == 0
}

@HiltViewModel
class UnlockChoiceViewModel @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    sessionRepository: SessionRepository,
    private val preferences: MintalPreferences,
    private val clock: MintalClock,
) : ViewModel() {

    val state: StateFlow<UnlockChoiceUiState> = combine(
        speciesRepository.observeStatuses(),
        speciesRepository.observePendingChoices(),
        sessionRepository.observeSuccessfulSecondsTotal(),
    ) { statuses, pending, total ->
        UnlockChoiceUiState(
            loading = false,
            pending = pending,
            locked = statuses.filterNot { it.isUnlocked }.map { it.species },
            unlockedCount = statuses.count { it.isUnlocked },
            catalogueSize = statuses.size,
            totalSuccessfulSeconds = total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnlockChoiceUiState())

    fun choose(species: Species) = viewModelScope.launch {
        if (!speciesRepository.choose(species.id, clock.nowMillis())) return@launch
        // Planting it should be the obvious next move, so a mint chosen while the
        // selected one is locked — the first one, or after a wipe — becomes the selection.
        val unlocked = speciesRepository.unlockedIds()
        if (preferences.settings.first().selectedSpeciesId !in unlocked) {
            preferences.setSelectedSpecies(species.id)
        }
    }
}

/**
 * The milestone moment (spec §6.1, amended): the player picks which mint fills the slot
 * they just earned, from everything still locked — twelve candidates the first time, one
 * the last. Deferring is allowed; the slot stays pending and the Grove, Home and the
 * catalogue all keep offering it.
 */
@Composable
fun UnlockChoiceScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: UnlockChoiceViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The screen exists only while something is owed. Spending the last slot closes it,
    // and arriving with nothing to spend bounces straight back out.
    LaunchedEffect(state.loading, state.pending) {
        if (!state.loading && state.pending == 0) onDone()
    }

    Scaffold(
        topBar = {
            MintalTopBar(
                title = if (state.isFirstEver) "Choose your first mint" else "Choose your mint",
                onBack = if (state.isFirstEver) null else onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = if (state.pending > 1) "${state.pending} mints to choose" else null) {
                    Text(
                        text = if (state.isFirstEver) {
                            "Every mint in the catalogue is available, and you pick one to " +
                                "start with. The rest are earned one at a time, and each " +
                                "time it is you who decides which — so the order is yours, " +
                                "not the app's."
                        } else {
                            "${MintalTime.formatDuration(state.totalSuccessfulSeconds)} of " +
                                "successful focus has earned another mint. " +
                                "${state.locked.size} are still locked; one of them is " +
                                "yours now."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!state.isFirstEver) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${state.unlockedCount} of ${state.catalogueSize} unlocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(items = state.locked, key = { it.id }) { species ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Shown in full colour: you are choosing it, so you should be able
                        // to see what you are choosing.
                        SpeciesEmblem(species = species, size = 64.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = species.commonName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = species.binomial + if (species.hybrid) " · hybrid" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = species.note,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Button(
                                onClick = { viewModel.choose(species) },
                                modifier = Modifier.padding(top = 8.dp),
                            ) { Text("Choose ${species.commonName.lowercase()}") }
                        }
                    }
                }
            }

            if (!state.isFirstEver) {
                item {
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Decide later")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
