package dev.nheox.mintal.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.ui.components.GrowthTrack
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.SpeciesEmblem
import dev.nheox.mintal.ui.session.SessionSetupViewModel
import dev.nheox.mintal.ui.theme.MintalTextStyles
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home, laid out as a radial hub in the spirit of the PS Vita's LiveArea (spec §4, §8.2):
 * Session setup at the centre, the other destinations as large round tiles around it, each
 * carrying a live detail rather than just an icon. This is a navigation and motion
 * treatment, not a change to which screens exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenGrove: () -> Unit,
    onOpenSpecies: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionRunning: () -> Unit,
) {
    val viewModel: SessionSetupViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTagPicker by remember { mutableStateOf(false) }

    // Reopening the app mid-session lands back on the running screen.
    if (running != null) {
        androidx.compose.runtime.LaunchedEffect(running) { onSessionRunning() }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // Mintal starts the session either way: a refused permission costs the live
        // notification, not the feature (spec §2, principle 5).
        viewModel.start()
    }

    fun startSession() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }

    Scaffold(
        topBar = {
            MintalTopBar(
                title = "Mintal",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HubRing(
                centreLabel = if (state.mode == SessionMode.COUNTDOWN) {
                    MintalTime.formatClock(state.minutes * 60)
                } else {
                    "Count up"
                },
                centreDetail = state.species?.commonName ?: "—",
                onStart = { startSession() },
                tiles = listOf(
                    HubTileSpec("The Grove", "your mint network", onOpenGrove),
                    HubTileSpec(
                        "Species",
                        "${state.unlockedCount}/${state.catalogueSize} unlocked",
                        onOpenSpecies,
                    ),
                    HubTileSpec("Statistics", "totals and trends", onOpenStats),
                    HubTileSpec("Tags", "${state.tags.size} tags", onOpenTags),
                ),
            )

            SectionCard(title = "Session setup") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.mode == SessionMode.COUNTDOWN,
                        onClick = { viewModel.setMode(SessionMode.COUNTDOWN) },
                        label = { Text("Countdown") },
                    )
                    FilterChip(
                        selected = state.mode == SessionMode.COUNT_UP,
                        onClick = { viewModel.setMode(SessionMode.COUNT_UP) },
                        label = { Text("Count up") },
                    )
                }

                if (state.mode == SessionMode.COUNTDOWN) {
                    Spacer(modifier = Modifier.height(6.dp))
                    GrowthTrack(
                        minutes = state.minutes,
                        onMinutesChange = viewModel::setMinutes,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setMinutes(state.minutes - SessionRules.STEP_MINUTES)
                            },
                        ) { Text("−5 min") }
                        Text(
                            text = "${state.minutes} min",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.setMinutes(state.minutes + SessionRules.STEP_MINUTES)
                            },
                        ) { Text("+5 min") }
                    }
                } else {
                    Text(
                        text = "Count-up runs until you stop it, up to three hours. " +
                            "Anything under a minute is discarded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                onClick = onOpenSpecies,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.species?.let { species ->
                        SpeciesEmblem(species = species, size = 44.dp)
                        Column(modifier = Modifier.weight(1f)) {
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
                    Text(text = "Change", style = MaterialTheme.typography.labelLarge)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                onClick = { showTagPicker = true },
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tag", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = state.tag?.name ?: "No tag",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(text = "Change", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (state.nextMilestone != null) {
                SectionCard(title = "Next mint") {
                    Text(
                        text = "${state.nextMilestone?.commonName} unlocks at " +
                            "${(state.nextMilestone?.unlockMinutes ?: 0) / 60} h of successful focus.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.milestoneProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${MintalTime.formatDuration(state.totalSuccessfulSeconds)} focused so far",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                            viewModel.selectTag(null)
                            showTagPicker = false
                        },
                    ) { Text("No tag") }
                    state.tags.forEach { tag ->
                        TextButton(
                            onClick = {
                                viewModel.selectTag(tag.id)
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
}

private data class HubTileSpec(
    val label: String,
    val detail: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubRing(
    centreLabel: String,
    centreDetail: String,
    onStart: () -> Unit,
    tiles: List<HubTileSpec>,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
    ) {
        val radius = minOf(maxWidth, maxHeight) * 0.40f
        tiles.forEachIndexed { index, tile ->
            val angle = (-135f + index * 90f) * PI.toFloat() / 180f
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = radius * cos(angle), y = radius * sin(angle)),
            ) {
                HubTile(tile)
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(150.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            onClick = onStart,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = centreLabel,
                    style = MintalTextStyles.timerSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = centreDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "START",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubTile(spec: HubTileSpec) {
    Surface(
        modifier = Modifier.size(104.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = spec.onClick,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = spec.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = spec.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}
