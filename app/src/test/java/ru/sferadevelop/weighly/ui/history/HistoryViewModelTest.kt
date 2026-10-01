package ru.sferadevelop.weighly.ui.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.sferadevelop.weighly.FakeWeightRepository
import ru.sferadevelop.weighly.MainDispatcherRule
import ru.sferadevelop.weighly.collectedState
import ru.sferadevelop.weighly.domain.Record
import ru.sferadevelop.weighly.domain.WeightRepository
import java.time.LocalDate

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `History lists Records from the latest Record Date to the earliest`() = runTest {
        val repository = FakeWeightRepository(
            listOf(
                record("2026-09-28", grams = 72_400),
                record("2026-09-30", grams = 71_900),
                record("2026-09-29", grams = 72_100)
            )
        )
        val viewModel = HistoryViewModel(repository)

        val state = collectedState(viewModel.uiState)

        assertEquals(
            listOf("2026-09-30", "2026-09-29", "2026-09-28"),
            (state as HistoryUiState.History).rows.map(RecordRow::date)
        )
    }

    @Test
    fun `a Record dated after today is listed first rather than hidden`() = runTest {
        val repository = FakeWeightRepository(
            listOf(
                record("2026-09-30", grams = 71_900),
                record("2099-01-01", grams = 70_000)
            )
        )
        val viewModel = HistoryViewModel(repository)

        val state = collectedState(viewModel.uiState)

        assertEquals(
            listOf("2099-01-01", "2026-09-30"),
            (state as HistoryUiState.History).rows.map(RecordRow::date)
        )
    }

    @Test
    fun `a History is loading until storage answers`() = runTest {
        val viewModel = HistoryViewModel(SilentWeightRepository())

        assertEquals(HistoryUiState.Loading, collectedState(viewModel.uiState))
    }

    @Test
    fun `a History with no Records is empty rather than still loading`() = runTest {
        val viewModel = HistoryViewModel(FakeWeightRepository())

        assertEquals(HistoryUiState.Empty, collectedState(viewModel.uiState))
    }

    @Test
    fun `a row shows the Record Date and the Weight to one decimal place`() = runTest {
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)

        val row = (collectedState(viewModel.uiState) as HistoryUiState.History).rows.single()

        assertEquals("2026-09-30", row.date)
        assertEquals("72.4", row.weight)
    }

    @Test
    fun `a Weight with no decimal part still shows one decimal place`() = runTest {
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 80_000)))
        val viewModel = HistoryViewModel(repository)

        val row = (collectedState(viewModel.uiState) as HistoryUiState.History).rows.single()

        assertEquals("80.0", row.weight)
    }

    @Test
    fun `a Weight between two tenths of a kilogram is shown rounded`() = runTest {
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_450)))
        val viewModel = HistoryViewModel(repository)

        val row = (collectedState(viewModel.uiState) as HistoryUiState.History).rows.single()

        assertEquals("72.5", row.weight)
    }

    @Test
    fun `a Weight under ten kilograms keeps its leading digit`() = runTest {
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 9_500)))
        val viewModel = HistoryViewModel(repository)

        val row = (collectedState(viewModel.uiState) as HistoryUiState.History).rows.single()

        assertEquals("9.5", row.weight)
    }

    private fun record(date: String, grams: Int) = Record(LocalDate.parse(date), grams)

    /** Storage that never answers, so screen state stays on its loading value. */
    private class SilentWeightRepository(
        private val delegate: WeightRepository = FakeWeightRepository()
    ) : WeightRepository by delegate {
        override fun records(): Flow<List<Record>> = MutableSharedFlow()
    }
}
