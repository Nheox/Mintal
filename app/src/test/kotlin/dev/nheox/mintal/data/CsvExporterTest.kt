package dev.nheox.mintal.data

import dev.nheox.mintal.core.model.SessionMode
import dev.nheox.mintal.core.model.SessionOutcome
import dev.nheox.mintal.data.db.entity.SessionEntity
import dev.nheox.mintal.data.transfer.CsvExporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CsvExporterTest {

    private fun session(rating: Int? = null, note: String? = null, tagId: Long? = null) =
        SessionEntity(
            id = 1,
            startedAt = 1_760_000_000_000L,
            endedAt = 1_760_001_500_000L,
            plannedSeconds = 1500,
            actualSeconds = 1500,
            mode = SessionMode.COUNTDOWN,
            outcome = SessionOutcome.SUCCESS,
            speciesId = "spicata",
            tagId = tagId,
            rating = rating,
            note = note,
            timezoneId = "UTC",
        )

    @Test
    fun `the header is the documented column order`() {
        val csv = CsvExporter.toCsv(emptyList(), emptyMap())
        assertEquals(CsvExporter.HEADER + "\r\n", csv)
    }

    @Test
    fun `an unrated session leaves the field empty rather than writing a zero`() {
        val csv = CsvExporter.toCsv(listOf(session(rating = null)), emptyMap())
        val row = csv.lines()[1]
        assertTrue(row.endsWith(",,"), "expected empty rating and note fields, got: $row")
        assertTrue(!row.contains(",0,"))
    }

    @Test
    fun `notes with commas, quotes and newlines are quoted per RFC 4180`() {
        assertEquals("plain", CsvExporter.field("plain"))
        assertEquals("\"with, comma\"", CsvExporter.field("with, comma"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.field("say \"hi\""))
        assertEquals("\"two\nlines\"", CsvExporter.field("two\nlines"))
    }

    @Test
    fun `the tag column carries the tag name, not its id`() {
        val csv = CsvExporter.toCsv(listOf(session(tagId = 7L)), mapOf(7L to "Study"))
        assertTrue(csv.lines()[1].contains(",Study,"))
    }

    @Test
    fun `timestamps are ISO-8601 with an offset`() {
        val csv = CsvExporter.toCsv(listOf(session()), emptyMap())
        val started = csv.lines()[1].substringBefore(',')
        assertTrue(started.startsWith("2025-10-09T"), "unexpected timestamp: $started")
        assertTrue(started.endsWith("Z") || started.contains('+') || started.contains('-'))
    }
}
