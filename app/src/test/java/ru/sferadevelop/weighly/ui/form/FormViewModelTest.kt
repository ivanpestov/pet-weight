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
import ru.sferadevelop.weighly.domain.WeightRepository
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

    private fun createViewModel(
        repository: WeightRepository = FakeWeightRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = FormViewModel(repository, savedStateHandle)
}
