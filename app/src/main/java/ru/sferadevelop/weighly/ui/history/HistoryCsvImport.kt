package ru.sferadevelop.weighly.ui.history

import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WEIGHT_RANGE_GRAMS
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Reads a CSV file as Records: the mirror of [historyCsv]. The app's own export round-trips, and
 * so does a foreign one whose lines carry a time of day and a comma for a decimal separator.
 *
 * Only the first two columns are read - the Record Date and the Weight - and a day holding
 * several lines collapses to its earliest one (ADR-0004).
 */

/**
 * How much of a picked file is read. The picker offers every file on the device, and the whole
 * text is held in memory for the counts shown before writing; 1 MB is some 40,000 lines, two
 * orders of magnitude past any real History.
 */
const val IMPORT_BYTE_LIMIT = 1_024 * 1_024

/** What a file turned out to hold: the Records it names, and how many lines were unreadable. */
data class ParsedHistory(
    /** One Record per calendar date the file names, from the earliest Record Date to the latest. */
    val records: List<Record>,
    /** Lines naming no readable Record Date and Weight. Blank lines are not among them. */
    val skipped: Int
)

private const val BOM = "\uFEFF"

/** Both separators a spreadsheet writes. A `;` in a line can only be one, so it wins. */
private const val SEMICOLON = ';'
private const val COMMA = ','

private const val QUOTE = '"'

/** `uuuu` rather than `yyyy`: strict resolution has no default era to read `yyyy` against. */
private val ISO_DATE: DateTimeFormatter = strict("uuuu-MM-dd")

/** Accepts `07.10.2026` and `7.10.2026`, but never a two-digit year. */
private val RUSSIAN_DATE: DateTimeFormatter = strict("d.M.uuuu")

private val TIME: DateTimeFormatter = strict("H:mm[:ss]")

/**
 * A Weight in kilograms: digits, optionally a decimal separator and more digits. Deliberately
 * narrow - no sign, no exponent, no thousands separator, no unit suffix - because reading
 * `118,1 kg` as kilograms invites reading `118,1 lb` as kilograms too. Six whole digits keep
 * grams inside an Int; the range check then rejects everything a scale cannot produce.
 */
private val WEIGHT = Regex("""(\d{1,6})(?:[.,](\d{1,9}))?""")

/** Weights are stored to the nearest 100 g, so that is what a file's extra digits round to. */
private const val GRAMS_PER_TENTH = 100

private fun strict(pattern: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)

/**
 * Reads [text] as CSV. The first line is dropped as a header only when it does not read as a
 * Record: a file whose first line is already data keeps it.
 */
fun parseHistoryCsv(text: String): ParsedHistory {
    val lines = text.removePrefix(BOM).split("\r\n", "\n", "\r")
    val separator = separatorOf(lines)
    val earliestPerDate = mutableMapOf<LocalDate, Line>()
    var skipped = 0
    var headerAllowed = true

    for (line in lines) {
        val fields = splitFields(line, separator)
        // A line of nothing but separators is as empty as a line of nothing, and neither is a
        // line the file got wrong.
        if (fields.all(String::isEmpty)) continue

        val read = readLine(fields, separator)
        if (read == null) {
            if (!headerAllowed) skipped++
            headerAllowed = false
            continue
        }
        headerAllowed = false

        // The earliest time of day wins the date, and an equal time leaves it with the line that
        // claimed it first.
        val held = earliestPerDate[read.date]
        if (held == null || read.time < held.time) earliestPerDate[read.date] = read
    }

    val records = earliestPerDate.values
        .sortedBy(Line::date)
        .map { Record(date = it.date, grams = it.grams) }
    return ParsedHistory(records = records, skipped = skipped)
}

/** One line's reading. The time of day only ever picks a winner; no Record holds it (ADR-0001). */
private class Line(val date: LocalDate, val time: LocalTime, val grams: Int)

/**
 * The separator the file uses. A `;` anywhere in a line settles it, since no Record Date or
 * Weight contains one, whereas a `,` may be either a separator or a decimal separator.
 */
private fun separatorOf(lines: List<String>): Char =
    if (lines.any { it.contains(SEMICOLON) }) SEMICOLON else COMMA

/**
 * Splits one line on [separator], honouring double quotes and `""` as an escaped quote
 * (RFC 4180). A quoted field spanning a line break is not supported: the file is split into
 * lines first, and a Record Date and a Weight never need one.
 */
private fun splitFields(line: String, separator: Char): List<String> {
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var index = 0

    while (index < line.length) {
        val character = line[index]
        when {
            quoted && character == QUOTE && line.getOrNull(index + 1) == QUOTE -> {
                field.append(QUOTE)
                index++
            }

            character == QUOTE -> quoted = !quoted
            character == separator && !quoted -> {
                fields.add(field.toString())
                field.clear()
            }

            else -> field.append(character)
        }
        index++
    }
    fields.add(field.toString())
    return fields.map(String::trim)
}

/** Reads a Record out of one line's [fields], or null when the line does not name one. */
private fun readLine(fields: List<String>, separator: Char): Line? {
    if (fields.size < 2) return null
    val stamp = readStamp(fields[0]) ?: return null
    if (separator == COMMA && splitDecimal(fields)) return null
    val grams = readGrams(fields[1]) ?: return null
    return Line(date = stamp.first, time = stamp.second, grams = grams)
}

/**
 * Whether the Weight was split across two fields by an unquoted decimal comma, as in
 * `07.10.2026,118,1`. Nothing can tell that apart from a third column holding `1`, so the line
 * is refused rather than read as one reading or the other.
 */
private fun splitDecimal(fields: List<String>): Boolean =
    fields.size > 2 &&
        fields[1].isNotEmpty() && fields[1].all(Char::isDigit) &&
        fields[2].length in 1..2 && fields[2].all(Char::isDigit)

/**
 * Reads a Record Date and the time of day beside it. Accepted: an ISO or Russian date, alone or
 * followed by a time after a space or a `T`. The date must be a real one - `31.02.2026` is
 * refused, not shifted into February - and a date with no time counts as the end of its day, so
 * that a line carrying a time always wins the date over one that does not.
 */
private fun readStamp(text: String): Pair<LocalDate, LocalTime>? {
    val separator = text.indexOfFirst { it == ' ' || it == 'T' }
    val datePart = if (separator < 0) text else text.take(separator)
    val timePart = if (separator < 0) "" else text.drop(separator + 1).trim()

    val date = readDate(datePart) ?: return null
    val time = if (timePart.isEmpty()) LocalTime.MAX else readTime(timePart) ?: return null
    return date to time
}

private fun readDate(text: String): LocalDate? =
    parsed(text, ISO_DATE, LocalDate::parse) ?: parsed(text, RUSSIAN_DATE, LocalDate::parse)

private fun readTime(text: String): LocalTime? = parsed(text, TIME, LocalTime::parse)

private fun <T> parsed(
    text: String,
    formatter: DateTimeFormatter,
    parse: (String, DateTimeFormatter) -> T
): T? = try {
    parse(text, formatter)
} catch (e: DateTimeParseException) {
    null
}

/**
 * Reads kilograms as grams, rounded to the nearest 100 g with halves going up, or null when the
 * text is not a Weight or is one no scale can produce.
 */
private fun readGrams(text: String): Int? {
    val match = WEIGHT.matchEntire(text.replace(COMMA, '.')) ?: return null
    val grams = match.value
        .toBigDecimal()
        .movePointRight(1)
        .setScale(0, RoundingMode.HALF_UP)
        .toInt() * GRAMS_PER_TENTH
    return grams.takeIf { it in WEIGHT_RANGE_GRAMS }
}
