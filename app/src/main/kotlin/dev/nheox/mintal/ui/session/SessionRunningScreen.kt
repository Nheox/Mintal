package dev.nheox.mintal.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.domain.session.SessionResult
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SpeciesEmblem
import dev.nheox.mintal.ui.components.SessionBar
import dev.nheox.mintal.ui.theme.MintalTextStyles
import kotlinx.coroutines.delay

/**
 * The live session (spec §5.2): a chunky horizontal bar, the time, and one rotating legend
 * line that alternates in-world flavour with a cited study tip. Nothing else to watch.
 */
@Composable
fun SessionRunningScreen(
    onFinished: (Long) -> Unit,
    onAbandoned: () -> Unit,
) {
    val viewModel: SessionRunningViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val live by viewModel.live.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    var showGiveUpDialog by remember { mutableStateOf(false) }
    var legendIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(14_000)
            legendIndex += 1
        }
    }

    // The session is over the moment the controller drops it; where we go next depends on
    // whether anything was recorded.
    LaunchedEffect(live, result) {
        if (live != null) return@LaunchedEffect
        when (val outcome = result) {
            is SessionResult.Recorded -> onFinished(outcome.sessionId)
            SessionResult.Discarded -> onFinished(-1L)
            // The controller now clears a session only once its result exists, so this is
            // a session that genuinely went away — a restarted process, say. Wait a beat
            // before concluding that, rather than racing a write we cannot see.
            null -> {
                delay(500)
                if (viewModel.live.value == null && viewModel.result.value == null) {
                    onAbandoned()
                }
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(topBar = { MintalTopBar(title = "Focusing") }) { padding ->
        val running = state.running
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            state.species?.let { species ->
                SpeciesEmblem(species = species, size = 96.dp)
                Text(text = species.commonName, style = MaterialTheme.typography.titleMedium)
            }

            // In a small multi-window pane the full-size numerals wrap and collide, so
            // the clock is sized from the width it actually has.
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        running == null -> "—"
                        running.mode == SessionMode.COUNTDOWN ->
                            MintalTime.formatClock(running.remainingSeconds)
                        else -> MintalTime.formatClock(running.elapsedSeconds)
                    },
                    style = if (maxWidth < 340.dp) {
                        MintalTextStyles.timerSmall
                    } else {
                        MintalTextStyles.timer
                    },
                    maxLines = 1,
                )
            }

            SessionBar(
                progress = running?.progress ?: 0f,
                label = if (running?.mode == SessionMode.COUNT_UP) {
                    "counting up"
                } else {
                    "${((running?.progress ?: 0f) * 100).toInt()}%"
                },
                inGracePeriod = running?.inGracePeriod == true,
            )

            if (state.tagName != null) {
                AssistChip(onClick = {}, label = { Text(text = state.tagName ?: "") })
            }

            val grace = running?.graceRemainingSeconds
            if (grace != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Come back within ${grace}s",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onError,
                        )
                        Text(
                            text = "You left Mintal. The session fails if you stay away.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }

            Text(
                text = LegendLines.lineAt(legendIndex),
                style = MintalTextStyles.legend,
                fontStyle = if (LegendLines.isTip(legendIndex)) FontStyle.Normal else FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (running?.mode == SessionMode.COUNT_UP) {
                Button(
                    onClick = { viewModel.stopCountUp() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Stop and keep it") }
            }

            OutlinedButton(
                onClick = { showGiveUpDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Give up") }
        }
    }

    if (showGiveUpDialog) {
        AlertDialog(
            onDismissRequest = { showGiveUpDialog = false },
            title = { Text("Give up this session?") },
            text = {
                Text(
                    "It will be recorded with the time you managed so far, and grafted onto " +
                        "the Grove as a wilted sprig. No species is earned.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGiveUpDialog = false
                        viewModel.giveUp()
                    },
                ) { Text("Give up") }
            },
            dismissButton = {
                TextButton(onClick = { showGiveUpDialog = false }) { Text("Keep going") }
            },
        )
    }
}
