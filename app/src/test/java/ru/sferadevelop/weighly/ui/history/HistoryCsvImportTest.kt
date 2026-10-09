package ru.sferadevelop.weighly.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.sferadevelop.weighly.domain.Record
import java.time.LocalDate

class HistoryCsvImportTest {

    @Test
    fun `an ISO date and a period read as a Record`() {
        val parsed = parseHistoryCsv("date,weight_kg\n2026-09-30,72.4")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `the app's own export round-trips`() {
        val exported = historyCsv(
            listOf(row("2026-09-30", "71.9"), row("2026-09-29", "72.1"))
        )

        assertEquals(
            listOf(record("2026-09-29", 72_100), record("2026-09-30", 71_900)),
            parseHistoryCsv(exported).records
        )
    }

    @Test
    fun `a Russian date with a time and a comma reads as a Record`() {
        val parsed = parseHistoryCsv("\"time\",\"value\"\n\"07.10.2026 08:16:18\",\"118,1\"")

        assertEquals(listOf(record("2026-10-07", 118_100)), parsed.records)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `a Russian date with single-digit parts reads as a Record`() {
        val parsed = parseHistoryCsv("date,weight\n7.1.2026,80.0")

        assertEquals(listOf(record("2026-01-07", 80_000)), parsed.records)
    }

    @Test
    fun `an ISO date with a T before the time reads as a Record`() {
        val parsed = parseHistoryCsv("date,weight\n2026-10-07T08:16:18,118.1")

        assertEquals(listOf(record("2026-10-07", 118_100)), parsed.records)
    }

    @Test
    fun `semicolons separate fields when a line holds one`() {
        val parsed = parseHistoryCsv("date;weight\n07.10.2026;118,1")

        assertEquals(listOf(record("2026-10-07", 118_100)), parsed.records)
    }

    @Test
    fun `a doubled quote inside a quoted field is read as one quote`() {
        val parsed = parseHistoryCsv("date,weight,note\n\"2026-09-30\",\"72.4\",\"a\"\"b\"")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `a byte order mark does not hide the first line`() {
        val parsed = parseHistoryCsv("\uFEFF2026-09-30,72.4")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
    }

    @Test
    fun `carriage returns separate lines as newlines do`() {
        val parsed = parseHistoryCsv("date,weight_kg\r\n2026-09-29,72.1\r\n2026-09-30,71.9\r\n")

        assertEquals(
            listOf(record("2026-09-29", 72_100), record("2026-09-30", 71_900)),
            parsed.records
        )
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `a first line that already reads as a Record is kept`() {
        val parsed = parseHistoryCsv("2026-09-30,72.4")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
    }

    @Test
    fun `only the first unreadable line is taken for a header`() {
        val parsed = parseHistoryCsv("time,value\n2026-09-30,72.4\nrubbish,here")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(1, parsed.skipped)
    }

    @Test
    fun `blank lines are not counted as skipped`() {
        val parsed = parseHistoryCsv("date,weight\n2026-09-30,72.4\n\n   \n,,\n")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `the earliest time of day wins a date`() {
        val parsed = parseHistoryCsv(
            "date,weight\n" +
                "07.10.2026 20:15:00,119.0\n" +
                "07.10.2026 06:30:00,118.1\n" +
                "07.10.2026 12:00:00,118.6"
        )

        assertEquals(listOf(record("2026-10-07", 118_100)), parsed.records)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `a line with a time wins the date over a line with none`() {
        val parsed = parseHistoryCsv("date,weight\n07.10.2026,119.0\n07.10.2026 23:59:00,118.1")

        assertEquals(listOf(record("2026-10-07", 118_100)), parsed.records)
    }

    @Test
    fun `two lines on one date with the same time leave the first in place`() {
        val parsed = parseHistoryCsv("date,weight\n07.10.2026 08:00,118.1\n07.10.2026 08:00,119.0")

        assertEquals(listOf(record("2026-10-07", 118_100)), parsed.records)
    }

    @Test
    fun `a Weight is rounded to the nearest hundred grams`() {
        val parsed = parseHistoryCsv("date,weight\n2026-09-30,118.15\n2026-09-29,118.14")

        assertEquals(
            listOf(record("2026-09-29", 118_100), record("2026-09-30", 118_200)),
            parsed.records
        )
    }

    @Test
    fun `a Weight no scale can produce is skipped`() {
        val parsed = parseHistoryCsv(
            "date,weight\n2026-09-28,0.5\n2026-09-29,500.1\n2026-09-30,72.4"
        )

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(2, parsed.skipped)
    }

    @Test
    fun `a Weight carrying a sign, a unit or a thousands separator is skipped`() {
        val parsed = parseHistoryCsv(
            "date,weight\n" +
                "2026-09-27,+118\n" +
                "2026-09-28,118.1 kg\n" +
                "2026-09-29,1 118.1\n" +
                "2026-09-30,72.4"
        )

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(3, parsed.skipped)
    }

    @Test
    fun `an unquoted decimal comma beside a comma separator is refused, not guessed at`() {
        val parsed = parseHistoryCsv("date,weight\n2026-09-30,72.4\n07.10.2026,118,1")

        assertEquals(listOf(record("2026-09-30", 72_400)), parsed.records)
        assertEquals(1, parsed.skipped)
    }

    @Test
    fun `a date that no calendar holds is skipped rather than shifted`() {
        val parsed = parseHistoryCsv("date,weight\n31.02.2026,72.4\n2026-13-01,72.4")

        assertEquals(emptyList<Record>(), parsed.records)
        assertEquals(2, parsed.skipped)
    }

    @Test
    fun `a two-digit year is skipped`() {
        val parsed = parseHistoryCsv("date,weight\n07.10.26,118.1")

        assertEquals(emptyList<Record>(), parsed.records)
        assertEquals(1, parsed.skipped)
    }

    @Test
    fun `a line of fewer than two fields is skipped`() {
        val parsed = parseHistoryCsv("date,weight\n2026-09-30\n2026-09-29,72.1")

        assertEquals(listOf(record("2026-09-29", 72_100)), parsed.records)
        assertEquals(1, parsed.skipped)
    }

    @Test
    fun `an empty file names no Record and nothing skipped`() {
        val parsed = parseHistoryCsv("")

        assertEquals(emptyList<Record>(), parsed.records)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `a date in the future is read like any other`() {
        val parsed = parseHistoryCsv("date,weight\n2099-01-01,70.0")

        assertEquals(listOf(record("2099-01-01", 70_000)), parsed.records)
    }

    @Test
    fun `a foreign export reads whole, latest Record Date last`() {
        val parsed = parseHistoryCsv(FOREIGN_EXPORT)

        assertEquals(9, parsed.records.size)
        assertEquals(0, parsed.skipped)
        assertEquals(record("2026-09-09", 117_400), parsed.records.first())
        assertEquals(record("2026-10-07", 118_100), parsed.records.last())
    }

    private fun record(date: String, grams: Int) = Record(LocalDate.parse(date), grams)

    private fun row(date: String, weight: String) =
        RecordRow(epochDay = LocalDate.parse(date).toEpochDay(), date = date, weight = weight)

    private companion object {
        /** A weight tracker's own export: quoted fields, Russian dates with a time, newest first. */
        const val FOREIGN_EXPORT = """"time","value"
"07.10.2026 08:16:18","118,1"
"06.10.2026 07:52:28","119,2"
"30.09.2026 07:13:26","119,3"
"29.09.2026 07:50:06","119,7"
"24.09.2026 08:12:25","117,3"
"15.09.2026 06:48:14","118,1"
"12.09.2026 09:25:53","118,0"
"11.09.2026 10:42:07","117,5"
"09.09.2026 09:16:41","117,4""""
    }
}
