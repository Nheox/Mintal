package dev.nheox.mintal.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.species.SpeciesCatalog
import dev.nheox.mintal.ui.components.SectionCard
import dev.nheox.mintal.ui.components.SpeciesEmblem
import dev.nheox.mintal.ui.permissions.MintalPermission
import dev.nheox.mintal.ui.permissions.PermissionRationaleBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: MintalPreferences,
    speciesCatalog: SpeciesCatalog,
) : ViewModel() {

    val principlesDismissed: StateFlow<Boolean> = preferences.settings
        .map { it.principlesDismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _tutorialSpecies = MutableStateFlow<Species?>(null)

    /** The mint the tutorial grows: the catalogue's first, so it is never invented. */
    val tutorialSpecies: StateFlow<Species?> = _tutorialSpecies.asStateFlow()

    init {
        viewModelScope.launch { _tutorialSpecies.value = speciesCatalog.all().firstOrNull() }
    }

    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        preferences.setOnboardingComplete(true)
        onDone()
    }

    fun setPrinciplesDismissed(value: Boolean) = viewModelScope.launch {
        preferences.setPrinciplesDismissed(value)
    }
}

/**
 * First launch, as four slides (spec §4): the name, how it works, what Mintal promises,
 * and the one permission it needs — in that order, because it is the order the questions
 * arrive in. What is this, what does it do, what will it do to me, what does it want.
 *
 * Each slide carries one idea. The principles and the tutorial were on one page before,
 * and the page was a wall: a tutorial nobody reads sitting above three cards nobody
 * scrolls to.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current
    val principlesDismissed by viewModel.principlesDismissed.collectAsStateWithLifecycle()
    val species by viewModel.tutorialSpecies.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Below Android 13 the notification permission is granted at install time, so there
    // is nothing to explain and nothing to ask for: that slide does not exist.
    val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    val slides = if (needsNotificationPermission) 4 else 3
    val pager = rememberPagerState(pageCount = { slides })

    val requestNotifications = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // Either answer is fine; a refused permission costs the live notification, not
        // the app (spec §2, principle 5).
        viewModel.complete(onDone)
    }

    fun goTo(page: Int) = scope.launch { pager.animateScrollToPage(page) }

    BackHandler(enabled = pager.currentPage > 0) { goTo(pager.currentPage - 1) }

    Scaffold(
        bottomBar = {
            OnboardingFooter(
                page = pager.currentPage,
                slides = slides,
                principlesDismissed = principlesDismissed,
                onPrinciplesDismissedChange = viewModel::setPrinciplesDismissed,
                onNext = { goTo(pager.currentPage + 1) },
                onFinish = { viewModel.complete(onDone) },
                onAllowNotifications = {
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                lastSlideIsPermission = needsNotificationPermission,
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { page ->
            // A slide carrying one idea should sit in the middle of the screen, not at
            // the top of an empty one — but the principles still have to scroll on a
            // short phone, so the column is centred only while it fits.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    when (page) {
                        0 -> TitleSlide(species)
                        1 -> TutorialSlide(species)
                        2 -> PrinciplesSlide()
                        else -> NotificationSlide()
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleSlide(species: Species?) {
    Text(
        text = "Mintal",
        style = MaterialTheme.typography.displayLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = MINTAL_SLOGAN,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    if (species != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The thing itself, rather than a logo of the thing.
            SpeciesEmblem(species = species, size = 300.dp)
        }
    }
}

@Composable
private fun TutorialSlide(species: Species?) {
    Text(
        text = "How it works",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    FocusTutorial(species = species)
}

@Composable
private fun PrinciplesSlide() {
    Text(
        text = "What Mintal promises",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    PrinciplesBody()
}

@Composable
private fun NotificationSlide() {
    Text(text = "One permission", style = MaterialTheme.typography.headlineSmall)
    SectionCard(title = MintalPermission.NOTIFICATIONS.title) {
        PermissionRationaleBody(permission = MintalPermission.NOTIFICATIONS)
    }
}

/**
 * The way on, which differs per slide: next, next, next-with-the-tick, then allow or
 * skip. A thin progress bar rather than a row of dots — the tutorial has dots of its
 * own and two sets on one screen say nothing twice.
 */
@Composable
private fun OnboardingFooter(
    page: Int,
    slides: Int,
    principlesDismissed: Boolean,
    onPrinciplesDismissedChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onAllowNotifications: () -> Unit,
    lastSlideIsPermission: Boolean,
) {
    val onPrinciples = page == 2
    val onPermission = lastSlideIsPermission && page == 3

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            LinearProgressIndicator(
                progress = { (page + 1f) / slides },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            if (onPrinciples) {
                PrinciplesTick(
                    dismissed = principlesDismissed,
                    onDismissedChange = onPrinciplesDismissedChange,
                )
            }

            if (onPermission) {
                Button(onClick = onAllowNotifications, modifier = Modifier.fillMaxWidth()) {
                    Text(MintalPermission.NOTIFICATIONS.grantLabel)
                }
                OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip for now")
                }
            } else {
                Button(
                    onClick = if (page == slides - 1) onFinish else onNext,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (page == slides - 1) "Choose your first mint" else "Next")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
