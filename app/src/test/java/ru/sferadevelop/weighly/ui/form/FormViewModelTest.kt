package ru.sferadevelop.weighly.ui.form

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.sferadevelop.weighly.FakeWeightRepository
import ru.sferadevelop.weighly.MainDispatcherRule
import ru.sferadevelop.weighly.collectedState
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import ru.sferadevelop.weighly.ui.FormRoute
import java.time.LocalDate

class FormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a Form opened without a Record Date is dated today`() = runTest {
        val viewModel = createViewModel()

        assertEquals(LocalDate.now().toString(), collectedState(viewModel.uiState).date)
    }

    @Test
    fun `the Weight field opens carrying the Weight of the last Record`() = runTest {
        val repository = FakeWeightRepository(
            listOf(
                Record(daysAgo(2), 72_100),
                Record(daysAgo(1), 71_900)
            )
        )

        val viewModel = createViewModel(repository)

        assertEquals("71.9", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a Record dated after today does not reach the prefill`() = runTest {
        val repository = FakeWeightRepository(
            listOf(
                Record(daysAgo(1), 71_900),
                Record(daysAhead(30), 60_000)
            )
        )

        val viewModel = createViewModel(repository)

        assertEquals("71.9", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a History with no Records opens an empty field`() = runTest {
        val viewModel = createViewModel(FakeWeightRepository())

        assertEquals("", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `the prefill does not overwrite a Weight already typed`() = runTest {
        val repository = FakeWeightRepository(listOf(Record(daysAgo(1), 71_900)))
        val savedStateHandle = SavedStateHandle()
        createViewModel(repository, savedStateHandle).onWeightTyped("68.2")

        val recreated = createViewModel(repository, savedStateHandle)

        assertEquals("68.2", collectedState(recreated.uiState).weight)
    }

    @Test
    fun `the chosen Record Date survives ViewModel recreation`() = runTest {
        val backdated = daysAgo(3)
        val savedStateHandle = SavedStateHandle()
        createViewModel(savedStateHandle = savedStateHandle).onDatePicked(backdated)

        val recreated = createViewModel(savedStateHandle = savedStateHandle)

        assertEquals(backdated.toString(), collectedState(recreated.uiState).date)
    }

    @Test
    fun `a backdated Record is written against the chosen Record Date`() = runTest {
        val backdated = daysAgo(3)
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onDatePicked(backdated)
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        assertEquals(72_400, repository.recordOn(backdated)?.grams)
        assertNull(repository.recordOn(LocalDate.now()))
    }

    @Test
    fun `the typed Weight survives ViewModel recreation`() = runTest {
        val savedStateHandle = SavedStateHandle()
        createViewModel(savedStateHandle = savedStateHandle).onWeightTyped("72.4")

        val recreated = createViewModel(savedStateHandle = savedStateHandle)

        assertEquals("72.4", collectedState(recreated.uiState).weight)
    }

    @Test
    fun `the typed Weight is shown as it is typed`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("71.")

        assertEquals("71.", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a saved Weight becomes a Record on today in grams`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        assertEquals(72_400, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `a Weight typed with no decimal part is saved as whole kilograms`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("72")

        viewModel.save()

        assertEquals(72_000, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `a Weight typed with a zero decimal is saved as whole kilograms too`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("72.0")

        viewModel.save()

        assertEquals(72_000, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `a comma typed as the decimal separator is saved like a period`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("72,4")

        viewModel.save()

        assertEquals(72_400, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `saving onto an occupied Record Date raises the replacement warning`() = runTest {
        val occupied = daysAgo(1)
        val repository = FakeWeightRepository(listOf(Record(occupied, 71_900)))
        val viewModel = createViewModel(repository)
        viewModel.onDatePicked(occupied)
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        val state = collectedState(viewModel.uiState)
        assertEquals("71.9", state.replacement?.weight)
        assertEquals(occupied.toString(), state.date)
        assertEquals(71_900, repository.recordOn(occupied)?.grams)
    }

    @Test
    fun `confirming the replacement writes the new Weight onto that Record Date`() = runTest {
        val occupied = daysAgo(1)
        val repository = FakeWeightRepository(listOf(Record(occupied, 71_900)))
        val viewModel = createViewModel(repository)
        viewModel.onDatePicked(occupied)
        viewModel.onWeightTyped("72.4")
        viewModel.save()

        viewModel.confirmReplacement()

        assertEquals(72_400, repository.recordOn(occupied)?.grams)
        assertTrue(collectedState(viewModel.uiState).saved)
    }

    @Test
    fun `declining the replacement leaves the Record and the Form as they were`() = runTest {
        val occupied = daysAgo(1)
        val repository = FakeWeightRepository(listOf(Record(occupied, 71_900)))
        val viewModel = createViewModel(repository)
        viewModel.onDatePicked(occupied)
        viewModel.onWeightTyped("72.4")
        viewModel.save()

        viewModel.declineReplacement()

        val state = collectedState(viewModel.uiState)
        assertNull(state.replacement)
        assertFalse(state.saved)
        assertEquals("72.4", state.weight)
        assertEquals(occupied.toString(), state.date)
        assertEquals(71_900, repository.recordOn(occupied)?.grams)
    }

    @Test
    fun `saving onto a free Record Date writes without a warning`() = runTest {
        val repository = FakeWeightRepository(listOf(Record(daysAgo(1), 71_900)))
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        assertNull(collectedState(viewModel.uiState).replacement)
        assertEquals(72_400, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `saving onto the Record being edited does not warn about replacing it`() = runTest {
        val edited = daysAgo(1)
        val repository = FakeWeightRepository(listOf(Record(edited, 71_900)))
        val viewModel = createViewModel(repository, editing = edited)
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        assertNull(collectedState(viewModel.uiState).replacement)
        assertEquals(72_400, repository.recordOn(edited)?.grams)
    }

    @Test
    fun `a Form opened on a Record carries its Weight and its Record Date`() = runTest {
        val edited = daysAgo(2)
        val repository = FakeWeightRepository(
            listOf(Record(edited, 71_900), Record(daysAgo(1), 72_400))
        )

        val viewModel = createViewModel(repository, editing = edited)

        val state = collectedState(viewModel.uiState)
        assertEquals("71.9", state.weight)
        assertEquals(edited.toString(), state.date)
        assertTrue(state.editing)
    }

    @Test
    fun `a Form opened without a Record is not in edit mode`() = runTest {
        val viewModel = createViewModel()

        assertFalse(collectedState(viewModel.uiState).editing)
    }

    @Test
    fun `correcting the Weight updates the Record in place`() = runTest {
        val edited = daysAgo(1)
        val repository = FakeWeightRepository(listOf(Record(edited, 71_900)))
        val viewModel = createViewModel(repository, editing = edited)
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        assertEquals(72_400, repository.recordOn(edited)?.grams)
    }

    @Test
    fun `moving a Record onto a free Record Date empties the old one`() = runTest {
        val edited = daysAgo(1)
        val moved = daysAgo(5)
        val repository = FakeWeightRepository(listOf(Record(edited, 71_900)))
        val viewModel = createViewModel(repository, editing = edited)
        viewModel.onWeightTyped("71.9")
        viewModel.onDatePicked(moved)

        viewModel.save()

        assertNull(repository.recordOn(edited))
        assertEquals(71_900, repository.recordOn(moved)?.grams)
    }

    @Test
    fun `confirming a move onto an occupied Record Date empties the old one`() = runTest {
        val edited = daysAgo(1)
        val occupied = daysAgo(2)
        val repository = FakeWeightRepository(
            listOf(Record(edited, 71_900), Record(occupied, 72_100))
        )
        val viewModel = createViewModel(repository, editing = edited)
        viewModel.onWeightTyped("71.9")
        viewModel.onDatePicked(occupied)
        viewModel.save()

        viewModel.confirmReplacement()

        assertNull(repository.recordOn(edited))
        assertEquals(71_900, repository.recordOn(occupied)?.grams)
    }

    @Test
    fun `a Record moved back onto its own Record Date stays where it was`() = runTest {
        val edited = daysAgo(1)
        val repository = FakeWeightRepository(listOf(Record(edited, 71_900)))
        val viewModel = createViewModel(repository, editing = edited)
        viewModel.onWeightTyped("72.4")
        viewModel.onDatePicked(daysAgo(5))
        viewModel.onDatePicked(edited)

        viewModel.save()

        assertEquals(72_400, repository.recordOn(edited)?.grams)
        assertNull(repository.recordOn(daysAgo(5)))
    }

    @Test
    fun `a Form opened on a Record that is no longer stored opens an empty field`() = runTest {
        val gone = daysAgo(1)

        val viewModel = createViewModel(FakeWeightRepository(), editing = gone)

        val state = collectedState(viewModel.uiState)
        assertEquals("", state.weight)
        assertEquals(gone.toString(), state.date)
        assertTrue(state.editing)
    }

    @Test
    fun `moving a Record onto an occupied Record Date raises the same warning`() = runTest {
        val edited = daysAgo(1)
        val occupied = daysAgo(2)
        val repository = FakeWeightRepository(
            listOf(Record(edited, 71_900), Record(occupied, 72_100))
        )
        val viewModel = createViewModel(repository, editing = edited)
        viewModel.onWeightTyped("72.4")
        viewModel.onDatePicked(occupied)

        viewModel.save()

        assertEquals("72.1", collectedState(viewModel.uiState).replacement?.weight)
        assertEquals(72_100, repository.recordOn(occupied)?.grams)
    }

    @Test
    fun `a saved Record sends the Form back to the History`() = runTest {
        val viewModel = createViewModel()
        viewModel.onWeightTyped("72.4")

        viewModel.save()

        assertTrue(collectedState(viewModel.uiState).saved)
    }

    @Test
    fun `a typed comma is shown back as a period`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("72,4")

        assertEquals("72.4", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `typed letters never reach the field`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("7a2kg")

        assertEquals("72", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a second separator never reaches the field`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("72.4.5")

        assertEquals("72.4", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a second digit after the separator is refused`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("72.45")

        assertEquals("72.4", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a separator typed before any digit gains a leading zero`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped(".")

        assertEquals("0.", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `digits typed after a leading separator are not stranded`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped(".7")
        viewModel.onWeightTyped("0.72")

        assertEquals("0.7", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a Weight still carrying the separator just typed saves as whole kilograms`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("72.")

        viewModel.save()

        assertEquals(72_000, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `digits no keyboard of this app produces never reach the field`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("٧٢")

        assertEquals("", collectedState(viewModel.uiState).weight)
    }

    @Test
    fun `a Weight is not judged while it is being typed`() = runTest {
        val viewModel = createViewModel()

        viewModel.onWeightTyped("0.9")

        assertNull(collectedState(viewModel.uiState).error)
    }

    @Test
    fun `saving with an empty field reports Enter a weight`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)

        viewModel.save()

        assertEquals(FormError.EMPTY_WEIGHT, collectedState(viewModel.uiState).error)
        assertFalse(collectedState(viewModel.uiState).saved)
        assertNull(repository.recordOn(LocalDate.now()))
    }

    @Test
    fun `a Weight under one kilogram is reported as out of range`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("0.9")

        viewModel.save()

        assertEquals(FormError.WEIGHT_OUT_OF_RANGE, collectedState(viewModel.uiState).error)
        assertNull(repository.recordOn(LocalDate.now()))
    }

    @Test
    fun `a Weight over five hundred kilograms is reported as out of range`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("500.1")

        viewModel.save()

        assertEquals(FormError.WEIGHT_OUT_OF_RANGE, collectedState(viewModel.uiState).error)
        assertNull(repository.recordOn(LocalDate.now()))
    }

    @Test
    fun `a typo that adds a zero is reported as out of range`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("724")

        viewModel.save()

        assertEquals(FormError.WEIGHT_OUT_OF_RANGE, collectedState(viewModel.uiState).error)
        assertNull(repository.recordOn(LocalDate.now()))
    }

    @Test
    fun `one kilogram is accepted`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("1")

        viewModel.save()

        assertEquals(1_000, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `five hundred kilograms is accepted`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)
        viewModel.onWeightTyped("500")

        viewModel.save()

        assertEquals(500_000, repository.recordOn(LocalDate.now())?.grams)
    }

    @Test
    fun `dismissing the message leaves the typed text and the Record Date untouched`() = runTest {
        val viewModel = createViewModel()
        viewModel.onWeightTyped("0.9")
        viewModel.save()
        val reported = collectedState(viewModel.uiState)

        viewModel.dismissError()

        val dismissed = collectedState(viewModel.uiState)
        assertNull(dismissed.error)
        assertEquals("0.9", dismissed.weight)
        assertEquals(reported.date, dismissed.date)
    }

    private fun daysAgo(days: Long) = LocalDate.now().minusDays(days)

    private fun daysAhead(days: Long) = LocalDate.now().plusDays(days)

    /** [editing] is the Record Date the Form was opened on, as the route passes it. */
    private fun createViewModel(
        repository: WeightRepository = FakeWeightRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        editing: LocalDate? = null
    ): FormViewModel {
        editing?.let { savedStateHandle[FormRoute::epochDay.name] = it.toEpochDay() }
        return FormViewModel(repository, savedStateHandle)
    }
}
