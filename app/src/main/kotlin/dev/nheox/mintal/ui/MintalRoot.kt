package dev.nheox.mintal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.prefs.MintalSettings
import dev.nheox.mintal.data.prefs.ThemeChoice
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.nav.MintalNavHost
import dev.nheox.mintal.ui.nav.Routes
import dev.nheox.mintal.ui.theme.MintalTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MintalRootViewModel @Inject constructor(
    preferences: MintalPreferences,
    private val speciesCatalog: SpeciesCatalog,
) : ViewModel() {

    val settings: StateFlow<MintalSettings?> = preferences.settings
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            // Warms the catalogue parse so the first screen does not wait on it.
            speciesCatalog.all()
        }
    }
}

@Composable
fun MintalRoot() {
    val viewModel: MintalRootViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    MintalTheme(theme = settings?.theme ?: ThemeChoice.SYSTEM) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val current = settings
            if (current == null) {
                MintalSplash()
            } else {
                MintalNavHost(
                    startDestination = when {
                        !current.onboardingComplete -> Routes.ONBOARDING
                        // The principles stand in front of the hub until the reader says
                        // they have read them (spec §4, amended).
                        !current.principlesDismissed -> Routes.PRINCIPLES_GATE
                        else -> Routes.HOME
                    },
                )
            }
        }
    }
}

/** Shown only while preferences are read from disk — a frame or two in practice. */
@Composable
private fun MintalSplash() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Mintal", style = MaterialTheme.typography.displayLarge)
    }
}
