package dev.nheox.mintal.ui.grove

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.model.Species
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.db.entity.TagEntity
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.data.repository.TagRepository
import dev.nheox.mintal.domain.grove.GroveBounds
import dev.nheox.mintal.domain.grove.GroveDayAnchor
import dev.nheox.mintal.domain.grove.GroveLayout
import dev.nheox.mintal.domain.grove.GroveLayoutBuilder
import dev.nheox.mintal.domain.session.SessionRules
import dev.nheox.mintal.di.ApplicationScope
import dev.nheox.mintal.ui.nav.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroveUiState(
    val loading: Boolean = true,
    val layout: GroveLayout = GroveLayout(emptyList(), emptyList(), GroveBounds(-1f, -1f, 1f, 1f)),
    val speciesById: Map<String, Species> = emptyMap(),
    val days: List<GroveDayAnchor> = emptyList(),
    val totalSuccessfulSeconds: Int = 0,
    val sessionCount: Int = 0,
)

@HiltViewModel
class GroveViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val speciesRepository: SpeciesRepository,
    preferences: MintalPreferences,
) : ViewModel() {

    val state: StateFlow<GroveUiState> = combine(
        sessionRepository.observeAll(),
        preferences.settings,
    ) { sessions, settings ->
        val layout = GroveLayoutBuilder.build(sessions, settings.dayStartMinutes)
        GroveUiState(
            loading = false,
            layout = layout,
            speciesById = speciesRepository.catalogue().associateBy { it.id },
            days = layout.anchors.sortedByDescending { it.epochDay },
            totalSuccessfulSeconds = sessions
                .filter { it.outcome == SessionOutcome.SUCCESS }
                .sumOf { it.actualSeconds },
            sessionCount = sessions.size,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GroveUiState(),
    )
}

data class DaySessionRow(
    val session: SessionEntity,
    val species: Species?,
    val tagName: String?,
)

data class DayDetailUiState(
    val epochDay: Long = 0L,
    val label: String = "",
    val rows: List<DaySessionRow> = emptyList(),
    val totalSeconds: Int = 0,
    val successCount: Int = 0,
)

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
    private val speciesRepository: SpeciesRepository,
    tagRepository: TagRepository,
    preferences: MintalPreferences,
) : ViewModel() {

    private val epochDay: Long = savedStateHandle.get<Long>(Routes.ARG_EPOCH_DAY) ?: 0L

    val state: StateFlow<DayDetailUiState> = combine(
        sessionRepository.observeAll(),
        tagRepository.observeAll(),
        preferences.settings,
    ) { sessions, tags, settings ->
        val catalogue = speciesRepository.catalogue().associateBy { it.id }
        val tagsById = tags.associateBy { it.id }
        val ofDay = sessions.filter {
            MintalTime.epochDayOf(
                it.startedAt,
                dayStartMinutes = settings.dayStartMinutes,
            ) == epochDay
        }
        DayDetailUiState(
            epochDay = epochDay,
            label = MintalTime.formatDayLabel(epochDay),
            rows = ofDay.sortedBy { it.startedAt }.map { session ->
                DaySessionRow(
                    session = session,
                    species = catalogue[session.speciesId],
                    tagName = session.tagId?.let { tagsById[it]?.name },
                )
            },
            totalSeconds = ofDay.sumOf { it.actualSeconds },
            successCount = ofDay.count { it.outcome == SessionOutcome.SUCCESS },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayDetailUiState())
}

data class SessionDetailUiState(
    val session: SessionEntity? = null,
    val species: Species? = null,
    val tags: List<TagEntity> = emptyList(),
    val tagName: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val speciesRepository: SpeciesRepository,
    tagRepository: TagRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>(Routes.ARG_SESSION_ID) ?: -1L

    val state: StateFlow<SessionDetailUiState> = combine(
        sessionRepository.observeById(sessionId),
        tagRepository.observeAll(),
    ) { session, tags ->
        SessionDetailUiState(
            session = session,
            species = session?.speciesId?.let { speciesRepository.byId(it) },
            tags = tags,
            tagName = session?.tagId?.let { id -> tags.firstOrNull { it.id == id }?.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    /** Note, tag and rating stay editable afterwards (spec §5.5, §5.6). */
    fun setRating(rating: Int?) = update { it.copy(rating = rating) }

    fun setNote(note: String) = update {
        it.copy(note = note.take(SessionRules.NOTE_MAX_LENGTH).ifBlank { null })
    }

    fun setTag(tagId: Long?) = update { it.copy(tagId = tagId) }

    fun delete(onDeleted: () -> Unit) {
        val session = state.value.session
        applicationScope.launch { session?.let { sessionRepository.delete(it) } }
        onDeleted()
    }

    /** Edits outlive this screen: the user may well change a note and immediately leave. */
    private fun update(transform: (SessionEntity) -> SessionEntity) = applicationScope.launch {
        val current = sessionRepository.getById(sessionId) ?: return@launch
        sessionRepository.update(transform(current))
    }
}
