package dev.nheox.mintal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.ui.components.MintalTopBar

/**
 * The allowlist (spec §5.9): visible but inert until the foreground-app check is on, with
 * the reason stated inline rather than hidden. It starts empty, and the system dialler and
 * settings are always permitted whether or not they are listed here.
 */
@Composable
fun AllowlistScreen(onBack: () -> Unit) {
    val viewModel: AllowlistViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSystem by remember { mutableStateOf(false) }

    Scaffold(topBar = { MintalTopBar(title = "Allowlist", onBack = onBack) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    text = if (state.layerEnabled) {
                        "Apps you allow here will not fail a session while they are in front."
                    } else {
                        "The allowlist is inert right now: it only does anything when " +
                            "“Also check which app is in front” is switched on in Settings. " +
                            "You can still prepare it."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()
            }

            items(items = state.userApps, key = { it.packageName }) { row ->
                AllowlistRowItem(
                    label = row.label,
                    packageName = row.packageName,
                    allowed = row.allowed,
                    onToggle = { viewModel.setAllowed(row.packageName, it) },
                )
                HorizontalDivider()
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSystem = !showSystem }
                        .padding(16.dp),
                ) {
                    Text(
                        text = if (showSystem) {
                            "Hide system apps (${state.systemApps.size})"
                        } else {
                            "Show system apps (${state.systemApps.size})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider()
            }

            if (showSystem) {
                items(items = state.systemApps, key = { it.packageName }) { row ->
                    AllowlistRowItem(
                        label = row.label,
                        packageName = row.packageName,
                        allowed = row.allowed,
                        onToggle = { viewModel.setAllowed(row.packageName, it) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AllowlistRowItem(
    label: String,
    packageName: String,
    allowed: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!allowed) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(checked = allowed, onCheckedChange = onToggle)
    }
}
