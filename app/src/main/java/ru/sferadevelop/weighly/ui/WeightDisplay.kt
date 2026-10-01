package ru.sferadevelop.weighly.ui

/**
 * Conversion between the Weight as stored (grams) and the Weight as typed and shown (kilograms to
 * one decimal place). Integer arithmetic throughout: 0.1 kg has no exact binary floating-point
 * representation, and Weights are compared for equality.
 */

private const val GRAMS_PER_KILOGRAM = 1_000
private const val GRAMS_PER_TENTH = 100

/** Keeps a typed Weight short enough that grams always fit an Int. */
private const val MAX_KILOGRAM_DIGITS = 6

private const val DECIMAL_SEPARATOR = '.'
private const val TYPED_SEPARATOR = ','

/**
 * A Weight as it may be typed, the trailing separator of one still being typed included.
 * Deliberately wider than the 1-500 kg a scale can produce, so that a typo such as an extra zero
 * reaches validation and is reported, rather than being swallowed here.
 */
private val TYPED_WEIGHT = Regex("""(\d{1,$MAX_KILOGRAM_DIGITS})(?:\.(\d)?)?""")

/**
 * Keeps what may be typed into the Weight field: digits and a single decimal separator, a comma
 * normalised to a period, at most one digit after it. Everything else never reaches the field.
 * A separator typed before any digit gains a leading zero, so that the digits after it are not
 * stranded in a field with no whole part.
 */
fun filterTypedWeight(text: String): String {
    val typed = text
        .replace(TYPED_SEPARATOR, DECIMAL_SEPARATOR)
        .filter { it in '0'..'9' || it == DECIMAL_SEPARATOR }
    val separator = typed.indexOf(DECIMAL_SEPARATOR)
    if (separator < 0) return typed.take(MAX_KILOGRAM_DIGITS)

    val kilograms = typed.take(separator).take(MAX_KILOGRAM_DIGITS).ifEmpty { "0" }
    val tenths = typed.drop(separator + 1).filter { it != DECIMAL_SEPARATOR }.take(1)
    return kilograms + DECIMAL_SEPARATOR + tenths
}

/** Renders grams as kilograms to one decimal place, rounding to the nearest 100 g. */
fun weightToDisplay(grams: Int): String {
    val tenthsOfKilogram = (grams + GRAMS_PER_TENTH / 2) / GRAMS_PER_TENTH
    return "${tenthsOfKilogram / 10}.${tenthsOfKilogram % 10}"
}

/**
 * Reads typed kilograms as grams, or null when [text] is not a Weight at all. Whether the Weight
 * is one a scale could produce is the caller's question, not this one's.
 */
fun weightFromDisplay(text: String): Int? {
    val typed = text.trim().replace(TYPED_SEPARATOR, DECIMAL_SEPARATOR)
    val match = TYPED_WEIGHT.matchEntire(typed) ?: return null
    val (kilograms, tenths) = match.destructured
    return kilograms.toInt() * GRAMS_PER_KILOGRAM +
        (tenths.takeIf(String::isNotEmpty)?.toInt() ?: 0) * GRAMS_PER_TENTH
}
