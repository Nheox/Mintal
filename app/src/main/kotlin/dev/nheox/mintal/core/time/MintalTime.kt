package dev.nheox.mintal.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every time calculation in Mintal goes through here. Sessions are stored as UTC epoch
 * millis plus the IANA zone they were recorded in (spec §7); days are bucketed with the
 * user's configurable day start (spec §6), which is why "which day was that?" is never a
 * plain date conversion.
 */
object MintalTime {

    private val isoOffset: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val timeOfDay: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    // English, deliberately: every string in the app is English today, and a Spanish
    // weekday beside English copy reads as a bug. These follow the device locale again as
    // soon as the UI strings are translated.
    private val dayLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
    private val monthLabel: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    fun systemZone(): ZoneId = ZoneId.systemDefault()

    /** The day bucket a moment belongs to, honouring a day start offset in minutes. */
    fun epochDayOf(atMillis: Long, zone: ZoneId = systemZone(), dayStartMinutes: Int = 0): Long =
        Instant.ofEpochMilli(atMillis - dayStartMinutes * 60_000L)
            .atZone(zone)
            .toLocalDate()
            .toEpochDay()

    /** First millisecond of a day bucket. */
    fun startOfDayMillis(epochDay: Long, zone: ZoneId = systemZone(), dayStartMinutes: Int = 0): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli() +
            dayStartMinutes * 60_000L

    fun hourOfDay(atMillis: Long, zone: ZoneId = systemZone()): Int =
        Instant.ofEpochMilli(atMillis).atZone(zone).hour

    /** 1 = Monday .. 7 = Sunday, as in java.time. */
    fun dayOfWeek(atMillis: Long, zone: ZoneId = systemZone()): Int =
        Instant.ofEpochMilli(atMillis).atZone(zone).dayOfWeek.value

    /** mm:ss, or h:mm:ss once there is an hour to show. */
    fun formatClock(totalSeconds: Int): String {
        val safe = if (totalSeconds < 0) 0 else totalSeconds
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    /** Human duration for summaries: "1 h 25 min", "45 min", "40 s". */
    fun formatDuration(totalSeconds: Int): String {
        val safe = if (totalSeconds < 0) 0 else totalSeconds
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            minutes > 0 -> "$minutes min"
            else -> "$safe s"
        }
    }

    fun formatTimeOfDay(atMillis: Long, zone: ZoneId = systemZone()): String =
        Instant.ofEpochMilli(atMillis).atZone(zone).format(timeOfDay)

    fun formatDayLabel(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(dayLabel)

    fun formatMonthLabel(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(monthLabel)

    /** ISO-8601 local time with offset, as the CSV export requires (spec §7.1). */
    fun formatIsoWithOffset(atMillis: Long, zoneId: String): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrElse { systemZone() }
        return Instant.ofEpochMilli(atMillis).atZone(zone).format(isoOffset)
    }
}
