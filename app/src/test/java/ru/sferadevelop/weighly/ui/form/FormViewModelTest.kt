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
    fun `saving an empty field writes no Record and stays in the Form`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)

        viewModel.save()

        assertNull(repository.recordOn(LocalDate.now()))
        assertFalse(collectedState(viewModel.uiState).saved)
    }

    @Test
    fun `text that is not a Weight writes no Record`() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = createViewModel(repository)

        for (text in listOf("kg", "7.2.4", "72.46", "Infinity", "1e3", "5000000", "0")) {
            viewModel.onWeightTyped(text)
            viewModel.save()
        }

        assertNull(repository.recordOn(LocalDate.now()))
    }

    private fun createViewModel(
        repository: WeightRepository = FakeWeightRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = FormViewModel(repository, savedStateHandle)
}
