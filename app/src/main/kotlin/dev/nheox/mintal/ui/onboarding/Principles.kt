package dev.nheox.mintal.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.components.SectionCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The line under the name, wherever the name appears. */
const val MINTAL_SLOGAN = "Stay focused, stay mintalized"

/**
 * What Mintal is and what it will not do. These three claims are the product, not a
 * welcome screen, so they are shown at **every** launch until the reader says they have
 * read them — and they stay in Settings afterwards, because a promise you can no longer
 * find is not much of a promise.
 */
@Composable
fun PrinciplesBody(
    tutorialSpecies: Species? = null,
    /** Pins the tutorial to one frame instead of animating it; used by the renders. */
    tutorialProgress: Float? = null,
) {
    Text(
        text = MINTAL_SLOGAN,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (tutorialProgress == null) {
        FocusTutorial(species = tutorialSpecies)
    } else {
        FocusTutorialFrame(species = tutorialSpecies, progress = tutorialProgress)
    }

    Text(
        text = "Choose how long you want to concentrate. Stay off the phone and the " +
            "session is planted in the Grove: a bed of real mints that grows with every " +
            "block of attention you finish.",
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
                "before it is requested — in Settings too, whenever you want to read " +
                "it again.",
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
}

/**
 * The tick and the way on, pinned to the bottom. With a tutorial and three cards above
 * them they would otherwise sit below the fold, and a dismissal you have to go looking
 * for is not much of a dismissal either.
 */
@Composable
fun PrinciplesFooter(
    dismissed: Boolean,
    onDismissedChange: (Boolean) -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = dismissed, onCheckedChange = onDismissedChange)
                Text(
                    text = "Don't show this again — it stays in Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@HiltViewModel
class PrinciplesViewModel @Inject constructor(
    private val preferences: MintalPreferences,
    speciesCatalog: SpeciesCatalog,
) : ViewModel() {

    val dismissed: StateFlow<Boolean> = preferences.settings
        .map { it.principlesDismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _tutorialSpecies = MutableStateFlow<Species?>(null)

    /** The mint the tutorial grows: the catalogue's first, so it is never invented. */
    val tutorialSpecies: StateFlow<Species?> = _tutorialSpecies.asStateFlow()

    init {
        viewModelScope.launch { _tutorialSpecies.value = speciesCatalog.all().firstOrNull() }
    }

    fun setDismissed(value: Boolean) = viewModelScope.launch {
        preferences.setPrinciplesDismissed(value)
    }
}

/**
 * The principles on their own: as the gate before the hub on every launch until it is
 * dismissed ([onContinue] carries on into the app), or opened from Settings to read
 * again ([onBack] pops).
 */
@Composable
fun PrinciplesScreen(
    onContinue: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: PrinciplesViewModel = hiltViewModel()
    val dismissed by viewModel.dismissed.collectAsStateWithLifecycle()
    val species by viewModel.tutorialSpecies.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (onBack != null) {
                MintalTopBar(title = "Mintal's principles", onBack = onBack)
            }
        },
        bottomBar = {
            if (onContinue != null) {
                PrinciplesFooter(
                    dismissed = dismissed,
                    onDismissedChange = viewModel::setDismissed,
                    actionLabel = "Start focusing",
                    onAction = onContinue,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (onBack == null) {
                Text(
                    text = "Mintal",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PrinciplesBody(tutorialSpecies = species)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
