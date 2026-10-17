package dev.nheox.mintal.ui.species

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.model.SpeciesStatus
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.prefs.SpeciesViewMode
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
    val viewMode: SpeciesViewMode = SpeciesViewMode.LIST,
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
            viewMode = settings.speciesViewMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeciesUiState())

    fun select(id: String) = viewModelScope.launch { preferences.setSelectedSpecies(id) }

    fun setViewMode(mode: SpeciesViewMode) =
        viewModelScope.launch { preferences.setSpeciesViewMode(mode) }
}

/**
 * The species catalogue (spec §5.1, §6.1): twelve real mints, locked ones visible with the
 * milestone that earns them.
 *
 * Two layouts of the same data. The list is the index — twelve mints down the page, each
 * a thumbnail and a line. The cards are the herbarium sheet: one mint filling the window,
 * drawn large, with everything the catalogue knows about it, swiped through sideways.
 * Tapping a row in the list opens that mint's card, so the two are one gesture apart.
 */
@Composable
fun SpeciesScreen(onBack: () -> Unit, onChooseMint: () -> Unit) {
    val viewModel: SpeciesViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Kept across a switch of layout, so coming back from the list lands on the mint the
    // card view was last showing rather than back at spearmint. Unlike the layout itself,
    // this is state about one visit and is deliberately not persisted.
    var focused by rememberSaveable { mutableStateOf(0) }

    SpeciesContent(
        state = state,
        mode = state.viewMode,
        focused = focused,
        onBack = onBack,
        onChooseMint = onChooseMint,
        onSelect = viewModel::select,
        onModeChange = viewModel::setViewMode,
        onFocusedChange = { focused = it },
    )
}

/**
 * The screen as a pure function of its state, so the screenshot renderer can draw it
 * without a database, a ViewModel or Hilt. Internal rather than copied into the test:
 * a render of a copy only ever proves the copy right.
 */
@Composable
internal fun SpeciesContent(
    state: SpeciesUiState,
    mode: SpeciesViewMode,
    focused: Int,
    onBack: () -> Unit,
    onChooseMint: () -> Unit,
    onSelect: (String) -> Unit,
    onModeChange: (SpeciesViewMode) -> Unit,
    onFocusedChange: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            MintalTopBar(
                title = "Mints",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = {
                            onModeChange(
                                if (mode == SpeciesViewMode.LIST) {
                                    SpeciesViewMode.CARDS
                                } else {
                                    SpeciesViewMode.LIST
                                },
                            )
                        },
                    ) {
                        Text(if (mode == SpeciesViewMode.LIST) "Cards" else "List")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (mode) {
                SpeciesViewMode.LIST -> SpeciesList(
                    state = state,
                    onChooseMint = onChooseMint,
                    onSelect = onSelect,
                    onOpenCard = { index ->
                        onFocusedChange(index)
                        onModeChange(SpeciesViewMode.CARDS)
                    },
                )

                SpeciesViewMode.CARDS -> SpeciesCards(
                    state = state,
                    startIndex = focused,
                    onSelect = onSelect,
                    onFocusedChange = onFocusedChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesList(
    state: SpeciesUiState,
    onChooseMint: () -> Unit,
    onSelect: (String) -> Unit,
    onOpenCard: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProgressCard(state = state, onChooseMint = onChooseMint) }

        itemsIndexed(items = state.statuses, key = { _, it -> it.species.id }) { index, status ->
            val unlocked = status.isUnlocked
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenCard(index) },
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
                                TextButton(onClick = { onSelect(status.species.id) }) {
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

@Composable
private fun SpeciesCards(
    state: SpeciesUiState,
    startIndex: Int,
    onSelect: (String) -> Unit,
    onFocusedChange: (Int) -> Unit,
) {
    val statuses = state.statuses
    if (statuses.isEmpty()) return

    // Composed only once the catalogue has arrived, so initialPage is a real index
    // rather than a zero clamped out of an empty list.
    val pager = rememberPagerState(
        initialPage = startIndex.coerceIn(0, statuses.lastIndex),
        pageCount = { statuses.size },
    )
    LaunchedEffect(pager.currentPage) { onFocusedChange(pager.currentPage) }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { page ->
            SpeciesCard(
                status = statuses[page],
                isSelected = statuses[page].species.id == state.selectedId,
                nextMilestoneMinutes = state.nextMilestoneMinutes,
                onSelect = onSelect,
            )
        }

        PagerDots(
            count = statuses.size,
            current = pager.currentPage,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
        Text(
            text = "${pager.currentPage + 1} of ${statuses.size} · swipe for the next mint",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
    }
}

/** One mint, filling the window: the plant first, then everything the catalogue knows. */
@Composable
private fun SpeciesCard(
    status: SpeciesStatus,
    isSelected: Boolean,
    nextMilestoneMinutes: Int?,
    onSelect: (String) -> Unit,
) {
    val species = status.species
    val unlocked = status.isUnlocked

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
    ) {
        SectionCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpeciesEmblem(species = species, size = 220.dp, unlocked = unlocked)
                Text(
                    text = species.commonName,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    // Not "· true species" for the rest: every such note already opens
                    // with the words, and the two lines sat one under the other.
                    text = species.binomial + if (species.hybrid) " · hybrid" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            Text(text = species.note, style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            Text(
                text = "Field marks",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            FieldMarks.of(species).forEach { mark ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = mark.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.26f),
                    )
                    Text(
                        text = mark.value,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            when {
                !unlocked -> Text(
                    text = if (nextMilestoneMinutes != null) {
                        "Still locked. Your next milestone is at " +
                            "${nextMilestoneMinutes / 60} h of successful focus, and this " +
                            "is one of the mints you can spend it on."
                    } else {
                        "Still locked — pick it at any milestone you reach from here."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                isSelected -> Text(
                    text = "You are growing this one. Your next finished session adds a " +
                        "node to it.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> {
                    Text(
                        text = "Unlocked. Switch to it and your next finished session " +
                            "grows this mint instead.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onSelect(species.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Grow this one") }
                }
            }
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val on = index == current
            Box(
                modifier = Modifier
                    .size(if (on) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (on) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun ProgressCard(state: SpeciesUiState, onChooseMint: () -> Unit) {
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
