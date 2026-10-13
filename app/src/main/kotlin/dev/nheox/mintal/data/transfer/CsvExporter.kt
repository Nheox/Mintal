package dev.nheox.mintal.data.transfer

import dev.nheox.mintal.core.time.MintalTime
import dev.nheox.mintal.data.db.entity.SessionEntity

/**
 * CSV export (spec §7.1). Timestamps are ISO-8601 local with the offset the session was
 * recorded in; an unrated session is an empty field, never a zero.
 */
object CsvExporter {

    const val HEADER =
        "started_at,ended_at,planned_seconds,actual_seconds,mode,outcome,species,tag,rating,note"

    fun toCsv(sessions: List<SessionEntity>, tagNames: Map<Long, String>): String {
        val builder = StringBuilder()
        builder.append(HEADER).append("\r\n")
        for (session in sessions) {
            builder.append(MintalTime.formatIsoWithOffset(session.startedAt, session.timezoneId))
                .append(',')
                .append(MintalTime.formatIsoWithOffset(session.endedAt, session.timezoneId))
                .append(',')
                .append(session.plannedSeconds)
                .append(',')
                .append(session.actualSeconds)
                .append(',')
                .append(session.mode.name)
                .append(',')
                .append(session.outcome.name)
                .append(',')
                .append(field(session.speciesId))
                .append(',')
                .append(field(session.tagId?.let { tagNames[it] } ?: ""))
                .append(',')
                .append(session.rating?.toString() ?: "")
                .append(',')
                .append(field(session.note ?: ""))
                .append("\r\n")
        }
        return builder.toString()
    }

    /** RFC 4180 quoting: quote when needed, double any embedded quote. */
    fun field(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
