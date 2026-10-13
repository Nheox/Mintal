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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.permissions.MintalPermission
import dev.nheox.mintal.ui.permissions.PermissionRationaleBody
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val principlesDismissed: StateFlow<Boolean> = preferences.settings
        .map { it.principlesDismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPrinciplesDismissed(value: Boolean) = viewModelScope.launch {
        preferences.setPrinciplesDismissed(value)
    }
}

/**
 * First launch (spec §4): the principles, and then — on the second page — why the one
 * permission Mintal needs is needed, immediately before Android's own prompt appears. An
 * explanation after the fact is no explanation.
 *
 * The principles themselves live in [PrinciplesBody] and are shown at every launch until
 * dismissed, so this screen is only their first appearance, not their only one.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current
    val principlesDismissed by viewModel.principlesDismissed.collectAsStateWithLifecycle()
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

            PrinciplesBody(
                dismissed = principlesDismissed,
                onDismissedChange = viewModel::setPrinciplesDismissed,
            )

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
