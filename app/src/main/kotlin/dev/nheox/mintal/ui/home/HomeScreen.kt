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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.repository.TagRepository
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.ui.components.GrowthTrack
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.SpeciesEmblem
import dev.nheox.mintal.ui.permissions.MintalPermission
import dev.nheox.mintal.ui.permissions.PermissionRationaleDialog
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenGrove: () -> Unit,
    onOpenSpecies: () -> Unit,
    onChooseMint: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onSessionRunning: () -> Unit,
) {
    val viewModel: SessionSetupViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val planting by viewModel.planting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTagPicker by remember { mutableStateOf(false) }
    var explainingNotifications by remember { mutableStateOf(false) }

    // Reopening the app mid-session lands back on the running screen.
    if (running != null) {
        androidx.compose.runtime.LaunchedEffect(running) { onSessionRunning() }
    }

    // Leaving the hub — to Settings, to the species list, or out of the app entirely —
    // is as clear a "not yet" as the Cancel button, and must not start a session behind
    // the user's back a moment later.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.cancelPlanting() }
    DisposableEffect(Unit) { onDispose { viewModel.cancelPlanting() } }

    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // Mintal starts the session either way: a refused permission costs the live
        // notification, not the feature (spec §2, principle 5).
        viewModel.start()
    }

    fun requestNotificationsThenStart() {
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun startSession() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        // Onboarding normally explains this one already; this covers the person who
        // skipped it there, or refused once and is trying again (spec §9).
        if (needsPermission) explainingNotifications = true else viewModel.start()
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
            if (state.pendingUnlockChoices > 0) {
                SectionCard(
                    title = if (state.unlockedCount == 0) {
                        "Choose your first mint"
                    } else {
                        "A mint is waiting"
                    },
                ) {
                    Text(
                        text = if (state.unlockedCount == 0) {
                            "Nothing is growing yet. Pick the mint you want to start with " +
                                "— all twelve are on the table."
                        } else {
                            "You have earned ${state.pendingUnlockChoices} more " +
                                if (state.pendingUnlockChoices == 1) "mint." else "mints."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onChooseMint, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose")
                    }
                }
            }

            val armed = planting
            if (armed != null) {
                SectionCard(title = "Planting in ${armed.secondsLeft}…") {
                    Text(
                        text = "Nothing has been planted yet. Cancel, or change the " +
                            "duration, mode or tag below, and this session never happened " +
                            "— no record, no mint, nothing wilted in the Grove.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.cancelPlanting() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancel") }
                }
            }

            HubRing(
                centreLabel = when {
                    armed != null -> armed.secondsLeft.toString()
                    state.mode == SessionMode.COUNTDOWN -> MintalTime.formatClock(state.minutes * 60)
                    else -> "Count up"
                },
                centreDetail = if (armed != null) {
                    "planting ${armed.speciesName}"
                } else {
                    state.species?.commonName ?: "no mint yet"
                },
                actionLabel = when {
                    armed != null -> "CANCEL"
                    state.species == null -> "CHOOSE"
                    else -> "START"
                },
                onStart = {
                    when {
                        armed != null -> viewModel.cancelPlanting()
                        state.species == null -> onChooseMint()
                        else -> startSession()
                    }
                },
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    // Wraps instead of pushing a button off the edge in a narrow window.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            modifier = Modifier.align(Alignment.CenterVertically),
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

            val nextMilestone = state.nextMilestoneMinutes
            if (nextMilestone != null) {
                SectionCard(title = "Next mint") {
                    Text(
                        text = "A milestone at ${nextMilestone / 60} h of successful focus. " +
                            "Which mint it unlocks is your choice when you get there.",
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

    if (explainingNotifications) {
        PermissionRationaleDialog(
            permission = MintalPermission.NOTIFICATIONS,
            onGrant = {
                explainingNotifications = false
                requestNotificationsThenStart()
            },
            onDismiss = {
                // Refusing the explanation is not refusing the session: it starts anyway,
                // without a live notification.
                explainingNotifications = false
                viewModel.start()
            },
            dismissLabel = "Start without it",
        )
    }

    if (showTagPicker) {
        var newTagName by remember { mutableStateOf("") }
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = {
                            if (it.length <= TagRepository.MAX_NAME_LENGTH) newTagName = it
                        },
                        label = { Text("New tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            enabled = newTagName.isNotBlank(),
                            onClick = {
                                viewModel.createTag(newTagName)
                                newTagName = ""
                                showTagPicker = false
                            },
                        ) { Text("Create and use") }
                        TextButton(
                            onClick = {
                                showTagPicker = false
                                onOpenTags()
                            },
                        ) { Text("Manage tags") }
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
    actionLabel: String = "START",
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
    ) {
        // Everything is sized from the space actually available. With a fixed radius and
        // fixed tiles, a narrow screen pushes the tiles through the centre button.
        val shortest = minOf(maxWidth, maxHeight)
        val tileSize = (shortest * 0.30f).coerceIn(80.dp, 112.dp)
        // Keeps the diagonal tiles inside the box: 0.707 * radius + half a tile + margin.
        val radius = ((shortest / 2f) - (tileSize / 2f) - 8.dp) * 1.414f
        val centreSize = (shortest * 0.42f)
            .coerceIn(96.dp, 160.dp)
            // ...and never closer than 10dp to the ring of tiles.
            .coerceAtMost((radius - tileSize / 2f - 10.dp) * 2f)
            .coerceAtLeast(72.dp)

        tiles.forEachIndexed { index, tile ->
            val angle = (-135f + index * 90f) * PI.toFloat() / 180f
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = radius * cos(angle), y = radius * sin(angle)),
            ) {
                HubTile(spec = tile, size = tileSize)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(centreSize),
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
                    maxLines = 1,
                )
                Text(
                    text = centreDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubTile(spec: HubTileSpec, size: Dp) {
    Surface(
        modifier = Modifier.size(size),
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
