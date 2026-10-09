package ru.sferadevelop.weighly.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistoryCsvTest {

    @Test
    fun `historyCsv is just the header when the History has no rows`() {
        assertEquals("date,weight_kg", historyCsv(emptyList()))
    }

    @Test
    fun `historyCsv renders a single row under the header`() {
        val rows = listOf(row("2026-09-30", "72.4"))

        assertEquals("date,weight_kg\n2026-09-30,72.4", historyCsv(rows))
    }

    @Test
    fun `historyCsv reverses the History's latest-first order to chronological`() {
        val rows = listOf(row("2026-09-30", "71.9"), row("2026-09-29", "72.1"))

        assertEquals(
            "date,weight_kg\n2026-09-29,72.1\n2026-09-30,71.9",
            historyCsv(rows)
        )
    }

    @Test
    fun `exportFileName names the file after the export date`() {
        assertEquals(
            "weighly-export-2026-10-09.csv",
            exportFileName(LocalDate.parse("2026-10-09"))
        )
    }

    private fun row(date: String, weight: String) =
        RecordRow(epochDay = LocalDate.parse(date).toEpochDay(), date = date, weight = weight)
}
