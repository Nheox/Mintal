package dev.nheox.mintal.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.prefs.MintalPreferences
import dev.nheox.mintal.data.repository.SessionRepository
import dev.nheox.mintal.data.repository.SpeciesRepository
import dev.nheox.mintal.data.repository.TagRepository
import dev.nheox.mintal.domain.stats.StatsCalculator
import dev.nheox.mintal.domain.stats.StatsRange
import dev.nheox.mintal.domain.stats.StatsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class StatsUiState(
    val loading: Boolean = true,
    val range: StatsRange = StatsRange.WEEK,
    val label: String = "",
    val result: StatsResult? = null,
    val tagNames: Map<Long, String> = emptyMap(),
    val topSpeciesName: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    tagRepository: TagRepository,
    private val speciesRepository: SpeciesRepository,
    preferences: MintalPreferences,
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.WEEK)
    private val periodOffset = MutableStateFlow(0L)

    val state: StateFlow<StatsUiState> = combine(
        sessionRepository.observeAll(),
        tagRepository.observeAll(),
        preferences.settings,
        range,
        periodOffset,
    ) { sessions, tags, settings, selectedRange, offset ->
        val today = MintalTime.epochDayOf(
            System.currentTimeMillis(),
            dayStartMinutes = settings.dayStartMinutes,
        )
        val anchor = shiftAnchor(today, selectedRange, offset)
        val firstWeekday = if (settings.firstWeekdayOverride in 1..7) {
            settings.firstWeekdayOverride
        } else {
            WeekFields.of(Locale.getDefault()).firstDayOfWeek.value
        }
        val result = StatsCalculator.compute(
            sessions = sessions,
            range = selectedRange,
            anchorEpochDay = anchor,
            dayStartMinutes = settings.dayStartMinutes,
            firstWeekday = firstWeekday,
        )
        StatsUiState(
            loading = false,
            range = selectedRange,
            label = StatsCalculator.rangeLabel(
                selectedRange,
                result.fromMillis,
                settings.dayStartMinutes,
            ),
            result = result,
            tagNames = tags.associate { it.id to it.name },
            topSpeciesName = result.topSpeciesId?.let { speciesRepository.byId(it)?.commonName },
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsUiState(),
    )

    fun setRange(value: StatsRange) {
        range.value = value
        periodOffset.value = 0L
    }

    fun previousPeriod() {
        periodOffset.value -= 1L
    }

    fun nextPeriod() {
        if (periodOffset.value < 0L) periodOffset.value += 1L
    }

    private fun shiftAnchor(today: Long, range: StatsRange, offset: Long): Long = when (range) {
        StatsRange.DAY -> today + offset
        StatsRange.WEEK -> today + offset * 7L
        StatsRange.MONTH -> LocalDate.ofEpochDay(today).plusMonths(offset).toEpochDay()
        StatsRange.YEAR -> LocalDate.ofEpochDay(today).plusYears(offset).toEpochDay()
    }
}
