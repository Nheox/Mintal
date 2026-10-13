package dev.nheox.mintal.domain.stats

import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class StatsRange { DAY, WEEK, MONTH, YEAR }

data class StatsBucket(val label: String, val seconds: Int)

data class TagBreakdownRow(val tagId: Long?, val seconds: Int, val averageRating: Float?)

data class RatingPoint(val atMillis: Long, val rating: Int)

data class StatsResult(
    val range: StatsRange,
    val fromMillis: Long,
    val toMillis: Long,
    val totalFocusSeconds: Int,
    val sessionCount: Int,
    val successCount: Int,
    val successRate: Float,
    val averageSessionSeconds: Int,
    val longestSessionSeconds: Int,
    val averageRating: Float?,
    val buckets: List<StatsBucket>,
    val ratingDistribution: List<Int>,
    val ratingTrend: List<RatingPoint>,
    val tagRows: List<TagBreakdownRow>,
    val bestWeekday: Int?,
    val bestHour: Int?,
    val topSpeciesId: String?,
) {
    val isEmpty: Boolean get() = sessionCount == 0
}

/**
 * Everything on the Statistics screen (spec §5.7), computed as one pure function over the
 * session rows in range so it can be unit-tested without Android.
 */
object StatsCalculator {

    /** Day bucket the range is anchored on; ranges always align to calendar boundaries. */
    fun windowFor(
        range: StatsRange,
        anchorEpochDay: Long,
        dayStartMinutes: Int,
        firstWeekday: Int,
        zone: ZoneId = MintalTime.systemZone(),
    ): LongRange {
        val anchor = LocalDate.ofEpochDay(anchorEpochDay)
        val startDate = when (range) {
            StatsRange.DAY -> anchor
            StatsRange.WEEK -> {
                val shift = ((anchor.dayOfWeek.value - firstWeekday) + 7) % 7
                anchor.minusDays(shift.toLong())
            }
            StatsRange.MONTH -> anchor.withDayOfMonth(1)
            StatsRange.YEAR -> anchor.withDayOfYear(1)
        }
        val endDate = when (range) {
            StatsRange.DAY -> startDate.plusDays(1)
            StatsRange.WEEK -> startDate.plusWeeks(1)
            StatsRange.MONTH -> startDate.plusMonths(1)
            StatsRange.YEAR -> startDate.plusYears(1)
        }
        val from = MintalTime.startOfDayMillis(startDate.toEpochDay(), zone, dayStartMinutes)
        val to = MintalTime.startOfDayMillis(endDate.toEpochDay(), zone, dayStartMinutes)
        return from until to
    }

    fun compute(
        sessions: List<SessionEntity>,
        range: StatsRange,
        anchorEpochDay: Long,
        dayStartMinutes: Int,
        firstWeekday: Int,
        zone: ZoneId = MintalTime.systemZone(),
    ): StatsResult {
        val window = windowFor(range, anchorEpochDay, dayStartMinutes, firstWeekday, zone)
        val inRange = sessions.filter { it.startedAt >= window.first && it.startedAt <= window.last }

        val totalSeconds = inRange.sumOf { it.actualSeconds }
        val successes = inRange.count { it.outcome == SessionOutcome.SUCCESS }
        val ratings = inRange.mapNotNull { it.rating }
        val ratingDistribution = (1..5).map { star -> ratings.count { it == star } }

        val buckets = buildBuckets(inRange, range, window, dayStartMinutes, firstWeekday, zone)

        val weekdayTotals = HashMap<Int, Int>()
        val weekdayDays = HashMap<Int, MutableSet<Long>>()
        val hourTotals = HashMap<Int, Int>()
        val speciesTotals = HashMap<String, Int>()
        for (session in inRange) {
            val weekday = MintalTime.dayOfWeek(session.startedAt, zone)
            weekdayTotals[weekday] = (weekdayTotals[weekday] ?: 0) + session.actualSeconds
            weekdayDays.getOrPut(weekday) { HashSet() }
                .add(MintalTime.epochDayOf(session.startedAt, zone, dayStartMinutes))
            val hour = MintalTime.hourOfDay(session.startedAt, zone)
            hourTotals[hour] = (hourTotals[hour] ?: 0) + session.actualSeconds
            speciesTotals[session.speciesId] =
                (speciesTotals[session.speciesId] ?: 0) + session.actualSeconds
        }

        val tagRows = inRange
            .groupBy { it.tagId }
            .map { (tagId, rows) ->
                val tagRatings = rows.mapNotNull { it.rating }
                TagBreakdownRow(
                    tagId = tagId,
                    seconds = rows.sumOf { it.actualSeconds },
                    averageRating = if (tagRatings.isEmpty()) null else tagRatings.average().toFloat(),
                )
            }
            .sortedByDescending { it.seconds }

        return StatsResult(
            range = range,
            fromMillis = window.first,
            toMillis = window.last + 1,
            totalFocusSeconds = totalSeconds,
            sessionCount = inRange.size,
            successCount = successes,
            successRate = if (inRange.isEmpty()) 0f else successes.toFloat() / inRange.size,
            averageSessionSeconds = if (inRange.isEmpty()) 0 else totalSeconds / inRange.size,
            longestSessionSeconds = inRange.maxOfOrNull { it.actualSeconds } ?: 0,
            averageRating = if (ratings.isEmpty()) null else ratings.average().toFloat(),
            buckets = buckets,
            ratingDistribution = ratingDistribution,
            ratingTrend = inRange
                .filter { it.rating != null }
                .sortedBy { it.startedAt }
                .map { RatingPoint(it.startedAt, it.rating ?: 0) },
            tagRows = tagRows,
            // "Best focus day" is an average per day the user actually studied, not a raw total.
            bestWeekday = weekdayTotals
                .maxByOrNull { (day, total) -> total.toFloat() / (weekdayDays[day]?.size ?: 1) }
                ?.key,
            bestHour = hourTotals.maxByOrNull { it.value }?.key,
            topSpeciesId = speciesTotals.maxByOrNull { it.value }?.key,
        )
    }

    private fun buildBuckets(
        sessions: List<SessionEntity>,
        range: StatsRange,
        window: LongRange,
        dayStartMinutes: Int,
        firstWeekday: Int,
        zone: ZoneId,
    ): List<StatsBucket> = when (range) {
        StatsRange.DAY -> (0 until 24).map { hour ->
            StatsBucket(
                label = hour.toString().padStart(2, '0'),
                seconds = sessions
                    .filter { MintalTime.hourOfDay(it.startedAt, zone) == hour }
                    .sumOf { it.actualSeconds },
            )
        }
        StatsRange.WEEK, StatsRange.MONTH -> {
            val startDay = MintalTime.epochDayOf(window.first, zone, dayStartMinutes)
            val endDay = MintalTime.epochDayOf(window.last, zone, dayStartMinutes)
            val dayCount = (endDay - startDay + 1).toInt().coerceAtLeast(1)
            (0 until dayCount).map { offset ->
                val day = startDay + offset
                StatsBucket(
                    label = LocalDate.ofEpochDay(day).dayOfMonth.toString(),
                    seconds = sessions
                        .filter { MintalTime.epochDayOf(it.startedAt, zone, dayStartMinutes) == day }
                        .sumOf { it.actualSeconds },
                )
            }
        }
        StatsRange.YEAR -> {
            val startDate = LocalDate.ofEpochDay(
                MintalTime.epochDayOf(window.first, zone, dayStartMinutes),
            )
            (0 until 12).map { offset ->
                val month = startDate.plusMonths(offset.toLong())
                val monthStart = month.withDayOfMonth(1).toEpochDay()
                val monthEnd = month.withDayOfMonth(1).plusMonths(1).toEpochDay()
                StatsBucket(
                    label = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    seconds = sessions
                        .filter {
                            val day = MintalTime.epochDayOf(it.startedAt, zone, dayStartMinutes)
                            day >= monthStart && day < monthEnd
                        }
                        .sumOf { it.actualSeconds },
                )
            }
        }
    }

    /** Range label for the header, e.g. "Mon 13 Oct 2026" or "Oct 2026". */
    fun rangeLabel(range: StatsRange, fromMillis: Long, dayStartMinutes: Int): String {
        val day = MintalTime.epochDayOf(fromMillis, dayStartMinutes = dayStartMinutes)
        return when (range) {
            StatsRange.DAY -> MintalTime.formatDayLabel(day)
            StatsRange.WEEK -> {
                val end = LocalDate.ofEpochDay(day).plus(6, ChronoUnit.DAYS).toEpochDay()
                MintalTime.formatDayLabel(day) + " — " + MintalTime.formatDayLabel(end)
            }
            StatsRange.MONTH -> MintalTime.formatMonthLabel(day)
            StatsRange.YEAR -> LocalDate.ofEpochDay(day).year.toString()
        }
    }
}
