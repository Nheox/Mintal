package dev.nheox.mintal.ui.onboarding

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.ui.components.SectionCard
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
}

/** First launch only (spec §4). Says plainly what the app asks for, before it asks. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()

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
            Text(text = "Mintal", style = MaterialTheme.typography.displayLarge)
            Text(
                text = "Choose how long you want to concentrate. Stay off the phone and the " +
                    "session is written into the Grove: one mint network that grows with " +
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
                        "left. Nothing else is required. An optional setting can check which " +
                        "app is in front so an allowlisted app does not fail a session — " +
                        "that one needs Usage Access, it is off by default, and Mintal works " +
                        "fully without it.",
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
                onClick = { viewModel.complete(onDone) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Plant the first mint") }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
