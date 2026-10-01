package ru.sferadevelop.weighly.ui

/**
 * Conversion between the Weight as stored (grams) and the Weight as typed and shown (kilograms to
 * one decimal place). Integer arithmetic throughout: 0.1 kg has no exact binary floating-point
 * representation, and Weights are compared for equality.
 */

/** A Weight as it may be typed: up to three digits of kilograms and at most one decimal digit. */
private val TYPED_WEIGHT = Regex("""(\d{1,3})(?:\.(\d))?""")

private const val GRAMS_PER_KILOGRAM = 1_000
private const val GRAMS_PER_TENTH = 100

/** Renders grams as kilograms to one decimal place, rounding to the nearest 100 g. */
fun weightToDisplay(grams: Int): String {
    val tenthsOfKilogram = (grams + GRAMS_PER_TENTH / 2) / GRAMS_PER_TENTH
    return "${tenthsOfKilogram / 10}.${tenthsOfKilogram % 10}"
}

/**
 * Reads typed kilograms as grams, or null when [text] is not a Weight. Both separators are
 * accepted: whichever one the keyboard offers just works. Anything else — letters, a second
 * separator, a second decimal digit, a zero Weight — yields null, and the caller writes nothing;
 * the input filter and the out-of-range message that replace this guard arrive with #6.
 */
fun weightFromDisplay(text: String): Int? {
    val match = TYPED_WEIGHT.matchEntire(text.trim().replace(',', '.')) ?: return null
    val (kilograms, tenths) = match.destructured
    val grams = kilograms.toInt() * GRAMS_PER_KILOGRAM +
        (tenths.takeIf(String::isNotEmpty)?.toInt() ?: 0) * GRAMS_PER_TENTH
    return grams.takeIf { it > 0 }
}
