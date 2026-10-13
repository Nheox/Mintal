package dev.nheox.mintal.ui.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.db.entity.TagEntity
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.data.repository.TagRepository
import dev.nheox.mintal.di.ApplicationScope
import dev.nheox.mintal.domain.milestone.MilestoneEngine
import dev.nheox.mintal.domain.session.RunningSession
import dev.nheox.mintal.domain.session.SessionController
import dev.nheox.mintal.domain.session.SessionResult
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.service.FocusSessionService
import dev.nheox.mintal.ui.nav.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Session setup: the centre of the radial hub (spec §5.1). */
data class SetupState(
    val minutes: Int = SessionRules.DEFAULT_MINUTES,
    val mode: SessionMode = SessionMode.COUNTDOWN,
    val species: Species? = null,
    val tag: TagEntity? = null,
    val totalSuccessfulSeconds: Int = 0,
    val nextMilestone: Species? = null,
    val milestoneProgress: Float = 0f,
    val unlockedCount: Int = 0,
    val catalogueSize: Int = 0,
    val tags: List<TagEntity> = emptyList(),
)

@HiltViewModel
class SessionSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: MintalPreferences,
    private val speciesRepository: SpeciesRepository,
    private val tagRepository: TagRepository,
    private val sessionRepository: SessionRepository,
    private val controller: SessionController,
) : ViewModel() {

    val running: StateFlow<RunningSession?> = controller.running

    val state: StateFlow<SetupState> = combine(
        preferences.settings,
        speciesRepository.observeStatuses(),
        tagRepository.observeAll(),
        sessionRepository.observeSuccessfulSecondsTotal(),
    ) { settings, statuses, tags, totalSeconds ->
        val catalogue = statuses.map { it.species }
        val unlocked = statuses.filter {
            it.isUnlocked || MilestoneEngine.isUnlocked(it.species, totalSeconds)
        }
        val selected = unlocked.firstOrNull { it.species.id == settings.selectedSpeciesId }?.species
            ?: unlocked.firstOrNull()?.species
        SetupState(
            minutes = settings.lastDurationMinutes,
            mode = settings.mode,
            species = selected,
            tag = tags.firstOrNull { it.id == settings.selectedTagId },
            totalSuccessfulSeconds = totalSeconds,
            nextMilestone = MilestoneEngine.nextMilestone(catalogue, totalSeconds),
            milestoneProgress = MilestoneEngine.progressToNext(catalogue, totalSeconds),
            unlockedCount = unlocked.size,
            catalogueSize = catalogue.size,
            tags = tags,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupState())

    fun setMinutes(minutes: Int) = viewModelScope.launch {
        preferences.setLastDuration(SessionRules.clampMinutes(minutes))
    }

    fun setMode(mode: SessionMode) = viewModelScope.launch { preferences.setMode(mode) }

    fun selectTag(tagId: Long?) = viewModelScope.launch { preferences.setSelectedTag(tagId) }

    /** Starting a session is the service's job from here on (spec §5.2). */
    fun start() {
        val current = state.value
        FocusSessionService.start(
            context = context,
            mode = current.mode,
            plannedSeconds = current.minutes * 60,
            speciesId = current.species?.id ?: "spicata",
            tagId = current.tag?.id,
        )
    }
}

data class RunningUiState(
    val running: RunningSession? = null,
    val species: Species? = null,
    val tagName: String? = null,
    val keepScreenOn: Boolean = false,
)

@HiltViewModel
class SessionRunningViewModel @Inject constructor(
    private val controller: SessionController,
    private val speciesRepository: SpeciesRepository,
    private val tagRepository: TagRepository,
    preferences: MintalPreferences,
) : ViewModel() {

    val result: StateFlow<SessionResult?> = controller.result

    /** The controller's own state, for navigation decisions that must not race. */
    val live: StateFlow<RunningSession?> = controller.running

    private val speciesCache = MutableStateFlow<List<Species>>(emptyList())

    val state: StateFlow<RunningUiState> = combine(
        controller.running,
        speciesCache,
        tagRepository.observeAll(),
        preferences.settings,
    ) { running, catalogue, tags, settings ->
        RunningUiState(
            running = running,
            species = catalogue.firstOrNull { it.id == running?.speciesId },
            tagName = tags.firstOrNull { it.id == running?.tagId }?.name,
            keepScreenOn = settings.keepScreenOn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunningUiState())

    init {
        viewModelScope.launch { speciesCache.value = speciesRepository.catalogue() }
    }

    fun giveUp() = viewModelScope.launch { controller.giveUp() }

    fun stopCountUp() = viewModelScope.launch { controller.stopCountUp() }
}

data class ResultUiState(
    val loading: Boolean = true,
    val discarded: Boolean = false,
    val session: SessionEntity? = null,
    val species: Species? = null,
    val tagName: String? = null,
    val newlyUnlocked: List<Species> = emptyList(),
    val rating: Int? = null,
    val note: String = "",
)

@HiltViewModel
class SessionResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val speciesRepository: SpeciesRepository,
    private val tagRepository: TagRepository,
    private val controller: SessionController,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>(Routes.ARG_SESSION_ID) ?: -1L

    private val edits = MutableStateFlow<Pair<Int?, String>?>(null)

    private val sessionFlow: Flow<SessionEntity?> =
        if (sessionId >= 0) sessionRepository.observeById(sessionId) else flowOf(null)

    val state: StateFlow<ResultUiState> = combine(
        sessionFlow,
        tagRepository.observeAll(),
        controller.result,
        edits,
    ) { session, tags, result, edit ->
        val recorded = result as? SessionResult.Recorded
        val species = session?.speciesId?.let { id -> speciesRepository.byId(id) }
        ResultUiState(
            loading = false,
            discarded = sessionId < 0,
            session = session,
            species = species,
            tagName = tags.firstOrNull { it.id == session?.tagId }?.name,
            newlyUnlocked = if (recorded?.sessionId == sessionId) recorded.newlyUnlocked else emptyList(),
            rating = edit?.first ?: session?.rating,
            note = edit?.second ?: session?.note ?: "",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())

    fun setRating(rating: Int?) {
        val current = state.value
        edits.value = rating to (current.note)
    }

    fun setNote(note: String) {
        val current = state.value
        edits.value = current.rating to note.take(SessionRules.NOTE_MAX_LENGTH)
    }

    /**
     * Rating and note are offered even on a failed session (spec §5.4). The write runs on
     * the application scope because the caller navigates away in the same breath, which
     * cancels this ViewModel's own scope.
     */
    fun save() {
        val current = state.value
        val session = current.session ?: return
        val rating = current.rating
        val note = current.note.trim().ifEmpty { null }
        applicationScope.launch {
            sessionRepository.update(session.copy(rating = rating, note = note))
        }
        controller.clearResult()
    }

    fun dismiss() = controller.clearResult()

    val outcomeText: StateFlow<String> = state.map { ui ->
        when {
            ui.discarded -> "That one was under a minute, so it was not recorded."
            ui.session == null -> ""
            ui.session.outcome == SessionOutcome.SUCCESS -> "Session complete."
            ui.session.outcome == SessionOutcome.GAVE_UP -> "You gave this one up."
            else -> "You left the app, so this session was failed."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
}
