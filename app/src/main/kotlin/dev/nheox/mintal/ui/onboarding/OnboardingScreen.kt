package dev.nheox.mintal.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.permissions.MintalPermission
import dev.nheox.mintal.ui.permissions.PermissionRationaleBody
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: MintalPreferences,
) : ViewModel() {
    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        preferences.setOnboardingComplete(true)
        onDone()
    }

    fun setRationaleHidden(permission: MintalPermission, hidden: Boolean) = viewModelScope.launch {
        preferences.setPermissionNoteDismissed(permission.key, hidden)
    }
}

/**
 * First launch only (spec §4). Says plainly what the app asks for, and then — on the
 * second page — says why the one permission it needs is needed, immediately before
 * Android's own prompt appears. An explanation after the fact is no explanation.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current
    var explainingNotifications by remember { mutableStateOf(false) }
    var hideRationale by remember { mutableStateOf(false) }

    // Below Android 13 the notification permission is granted at install time, so there is
    // nothing to explain and nothing to ask for.
    val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

    val requestNotifications = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // Either answer is fine; a refused permission costs the live notification, not the
        // app (spec §2, principle 5).
        viewModel.complete(onDone)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            if (explainingNotifications) {
                Text(text = "One permission", style = MaterialTheme.typography.displaySmall)
                SectionCard(title = MintalPermission.NOTIFICATIONS.title) {
                    PermissionRationaleBody(
                        permission = MintalPermission.NOTIFICATIONS,
                        hideInFuture = hideRationale,
                        onHideInFutureChange = { hidden ->
                            hideRationale = hidden
                            viewModel.setRationaleHidden(MintalPermission.NOTIFICATIONS, hidden)
                        },
                    )
                }
                Button(
                    onClick = {
                        requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(MintalPermission.NOTIFICATIONS.grantLabel) }
                OutlinedButton(
                    onClick = { viewModel.complete(onDone) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip for now") }
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            Text(text = "Mintal", style = MaterialTheme.typography.displayLarge)
            Text(
                text = "Choose how long you want to concentrate. Stay off the phone and the " +
                    "session is written into the Grove: a bed of real mints that grows with " +
                    "every block of attention you finish.",
                style = MaterialTheme.typography.bodyLarge,
            )

            SectionCard(title = "It stays on this device") {
                Text(
                    text = "No account, no server, no analytics, no ads, nothing for sale. " +
                        "Mintal has no internet permission at all. You can export or delete " +
                        "everything whenever you want.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "What it will ask for") {
                Text(
                    text = "A notification, so the timer can keep running and show the time " +
                        "left. Nothing else is required, and each permission is explained " +
                        "before it is requested — in Settings too, whenever you want to " +
                        "read it again.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionCard(title = "If you leave mid-session") {
                Text(
                    text = "You get a 15 second warning first, and the session is only then " +
                        "recorded as failed — with the time you did manage. A dark screen or " +
                        "a phone call never counts as leaving. Nothing here takes over your " +
                        "phone, and you can always exit.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = {
                    if (needsNotificationPermission) {
                        explainingNotifications = true
                    } else {
                        viewModel.complete(onDone)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (needsNotificationPermission) "Continue" else "Choose your first mint") }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
