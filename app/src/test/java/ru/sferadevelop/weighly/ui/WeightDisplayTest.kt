package ru.sferadevelop.weighly.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightDisplayTest {

    @Test
    fun `grams are shown as kilograms to one decimal place`() {
        assertEquals("72.4", weightToDisplay(72_400))
    }

    @Test
    fun `a Weight with no decimal part still shows one decimal place`() {
        assertEquals("80.0", weightToDisplay(80_000))
    }

    @Test
    fun `a Weight under ten kilograms keeps its leading digit`() {
        assertEquals("9.5", weightToDisplay(9_500))
    }

    @Test
    fun `a Weight between two tenths is shown rounded`() {
        assertEquals("72.5", weightToDisplay(72_450))
    }

    @Test
    fun `a typed Weight is read as grams`() {
        assertEquals(72_400, weightFromDisplay("72.4"))
    }

    @Test
    fun `a typed Weight with no decimal part is read as whole kilograms`() {
        assertEquals(72_000, weightFromDisplay("72"))
    }

    @Test
    fun `a typed Weight with a zero decimal is read as whole kilograms too`() {
        assertEquals(72_000, weightFromDisplay("72.0"))
    }

    @Test
    fun `a typed comma reads like a typed period`() {
        assertEquals(72_400, weightFromDisplay("72,4"))
    }

    @Test
    fun `what a shown Weight reads back as is what it was`() {
        for (grams in listOf(1_000, 9_500, 72_400, 80_000, 500_000)) {
            assertEquals(grams, weightFromDisplay(weightToDisplay(grams)))
        }
    }

    @Test
    fun `a Weight still carrying the separator just typed reads as whole kilograms`() {
        assertEquals(72_000, weightFromDisplay("72."))
    }

    @Test
    fun `a Weight no scale could produce still reads, since judging it belongs to the Form`() {
        assertEquals(0, weightFromDisplay("0"))
        assertEquals(724_000, weightFromDisplay("724"))
    }

    @Test
    fun `text that is not a Weight reads as nothing`() {
        val notWeights =
            listOf("", " ", "kg", "7.2.4", "72.46", "-72.4", "Infinity", "NaN", "1e3", "5000000")
        for (text in notWeights) {
            assertNull("\"$text\" should not read as a Weight", weightFromDisplay(text))
        }
    }
}
