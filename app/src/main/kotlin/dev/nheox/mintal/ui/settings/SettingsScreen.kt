package dev.nheox.mintal.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.service.UsageAccess
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard

/** Settings (spec §4). Every permission is explained here before it is ever requested. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAllowlist: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDayStartDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val exportCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::exportCsv) }

    val exportBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportBackup) }

    val pickBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingRestore = uri }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { MintalTopBar(title = "Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(title = "Timer") {
                SettingSwitch(
                    label = "Keep the screen on during a session",
                    checked = settings.keepScreenOn,
                    onCheckedChange = viewModel::setKeepScreenOn,
                )
                SettingSwitch(
                    label = "Vibrate when a session ends",
                    checked = settings.vibrateOnFinish,
                    onCheckedChange = viewModel::setVibrate,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "A new day begins at",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${(settings.dayStartMinutes / 60).toString().padStart(2, '0')}:00" +
                                " — a session finished after midnight but before this hour " +
                                "still counts as the previous day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showDayStartDialog = true }) { Text("Change") }
                }

                Text(
                    text = "Week starts on",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = "Used by the weekly statistics range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0 to "Locale default", 1 to "Monday", 7 to "Sunday").forEach { choice ->
                        FilterChip(
                            selected = settings.firstWeekdayOverride == choice.first,
                            onClick = { viewModel.setFirstWeekday(choice.first) },
                            label = { Text(choice.second) },
                        )
                    }
                }
            }

            SectionCard(title = "Blocking and allowlist") {
                SettingSwitch(
                    label = "Fail a session if I leave the app",
                    description = "Needs no permission. Leaving Mintal starts a 15 second " +
                        "warning; a dark screen or a phone call never counts as leaving.",
                    checked = settings.detectLeavingApp,
                    onCheckedChange = viewModel::setDetectLeavingApp,
                )
                SettingSwitch(
                    label = "Also check which app is in front",
                    description = "Optional. Uses Android's Usage Access so an allowlisted " +
                        "app — a dictionary, say — does not fail a session. Mintal works " +
                        "fully without it, and never asks for it to start a session.",
                    checked = settings.detectForegroundApp,
                    onCheckedChange = viewModel::setDetectForegroundApp,
                )
                if (settings.detectForegroundApp && !UsageAccess.isGranted(context)) {
                    OutlinedButton(
                        onClick = { context.startActivity(UsageAccess.settingsIntent()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("Grant Usage Access in system settings") }
                }
                OutlinedButton(
                    onClick = onOpenAllowlist,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Edit the allowlist") }
            }

            SectionCard(title = "Appearance") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = settings.theme == choice,
                            onClick = { viewModel.setTheme(choice) },
                            label = {
                                Text(choice.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                        )
                    }
                }
            }

            SectionCard(title = "Data") {
                OutlinedButton(
                    onClick = { exportCsv.launch(viewModel.csvFileName()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Export sessions as CSV") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { exportBackup.launch(viewModel.backupFileName()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save a full backup") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { pickBackup.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Restore from a backup") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete all sessions") }
                Text(
                    text = "Everything lives on this device. Exports go wherever you point " +
                        "the system file picker, which is the same mechanism a later cloud " +
                        "backup will use — Mintal runs no server and holds no account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            SectionCard(title = "Device") {
                OutlinedButton(
                    onClick = {
                        runCatching { context.startActivity(viewModel.batteryOptimisationIntent()) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ask Android not to kill running sessions") }
                Text(
                    text = "Some phones stop background timers aggressively. This is optional " +
                        "and only ever opened by you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
                Text("About Mintal")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDayStartDialog) {
        AlertDialog(
            onDismissRequest = { showDayStartDialog = false },
            title = { Text("Day starts at") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    (0..23).forEach { hour ->
                        TextButton(
                            onClick = {
                                viewModel.setDayStartHour(hour)
                                showDayStartDialog = false
                            },
                        ) { Text("${hour.toString().padStart(2, '0')}:00") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDayStartDialog = false }) { Text("Close") }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete all sessions?") },
            text = {
                Text(
                    "Every session, the Grove and all unlock stamps go. Tags stay. " +
                        "This cannot be undone — save a backup first if you are unsure.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllData()
                    },
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace everything with this backup?") },
            text = {
                Text(
                    "A restore replaces the database wholesale: sessions, tags, unlocks and " +
                        "settings. Unlock state is recomputed from the restored sessions.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackup(uri)
                        pendingRestore = null
                    },
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
