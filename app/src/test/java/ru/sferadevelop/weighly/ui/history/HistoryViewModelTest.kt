package ru.sferadevelop.weighly.ui.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `a row carries its Record Date as a date, so the Record can be opened`() = runTest {
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)

        val row = (collectedState(viewModel.uiState) as HistoryUiState.History).rows.single()

        assertEquals(LocalDate.parse("2026-09-30").toEpochDay(), row.epochDay)
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

    @Test
    fun `a swipe removes the Record from the History`() = runTest {
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)
        collectedState(viewModel.uiState)

        viewModel.deleteRecord(LocalDate.parse("2026-09-30").toEpochDay())

        assertEquals(HistoryUiState.Empty, collectedState(viewModel.uiState))
    }

    @Test
    fun `undo restores the Record with the same Record Date and the same Weight`() = runTest {
        val date = LocalDate.parse("2026-09-30")
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(date.toEpochDay())

        viewModel.undoDeletion(date.toEpochDay())

        assertEquals(72_400, repository.recordOn(date)?.grams)
    }

    @Test
    fun `undo returns a deleted Record to its place in the History`() = runTest {
        val date = LocalDate.parse("2026-09-30")
        val repository = FakeWeightRepository(
            listOf(record("2026-09-30", grams = 72_400), record("2026-09-29", grams = 72_100))
        )
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(date.toEpochDay())

        viewModel.undoDeletion(date.toEpochDay())

        assertEquals(
            listOf("2026-09-30", "2026-09-29"),
            (collectedState(viewModel.uiState) as HistoryUiState.History).rows.map(RecordRow::date)
        )
    }

    @Test
    fun `once the undo window expires the deletion is final`() = runTest {
        val date = LocalDate.parse("2026-09-30")
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(date.toEpochDay())

        viewModel.finalizeDeletion(date.toEpochDay())

        assertNull(repository.recordOn(date))
    }

    @Test
    fun `undo does nothing once the undo window has expired`() = runTest {
        val date = LocalDate.parse("2026-09-30")
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(date.toEpochDay())
        viewModel.finalizeDeletion(date.toEpochDay())

        viewModel.undoDeletion(date.toEpochDay())

        assertNull(repository.recordOn(date))
    }

    @Test
    fun `deleting the only Record and undoing it brings the History back`() = runTest {
        val date = LocalDate.parse("2026-09-30")
        val repository = FakeWeightRepository(listOf(record("2026-09-30", grams = 72_400)))
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(date.toEpochDay())
        assertEquals(HistoryUiState.Empty, collectedState(viewModel.uiState))

        viewModel.undoDeletion(date.toEpochDay())

        assertEquals(
            listOf("2026-09-30"),
            (collectedState(viewModel.uiState) as HistoryUiState.History).rows.map(RecordRow::date)
        )
    }

    @Test
    fun `a second swipe before the first undo window closes keeps both undoable`() = runTest {
        val first = LocalDate.parse("2026-09-30")
        val second = LocalDate.parse("2026-09-29")
        val repository = FakeWeightRepository(
            listOf(record("2026-09-30", grams = 72_400), record("2026-09-29", grams = 72_100))
        )
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(first.toEpochDay())

        viewModel.deleteRecord(second.toEpochDay())
        viewModel.undoDeletion(first.toEpochDay())

        assertEquals(72_400, repository.recordOn(first)?.grams)
        assertNull(repository.recordOn(second))
    }

    @Test
    fun `finalizing one pending deletion leaves another still undoable`() = runTest {
        val first = LocalDate.parse("2026-09-30")
        val second = LocalDate.parse("2026-09-29")
        val repository = FakeWeightRepository(
            listOf(record("2026-09-30", grams = 72_400), record("2026-09-29", grams = 72_100))
        )
        val viewModel = HistoryViewModel(repository)
        viewModel.deleteRecord(first.toEpochDay())
        viewModel.deleteRecord(second.toEpochDay())

        viewModel.finalizeDeletion(first.toEpochDay())
        viewModel.undoDeletion(second.toEpochDay())

        assertNull(repository.recordOn(first))
        assertEquals(72_100, repository.recordOn(second)?.grams)
    }

    @Test
    fun `exportableCsv renders the History as CSV, oldest Record Date first`() = runTest {
        val repository = FakeWeightRepository(
            listOf(record("2026-09-30", grams = 71_900), record("2026-09-29", grams = 72_100))
        )
        val viewModel = HistoryViewModel(repository)
        collectedState(viewModel.uiState)

        assertEquals(
            "date,weight_kg\n2026-09-29,72.1\n2026-09-30,71.9",
            viewModel.exportableCsv()
        )
    }

    @Test
    fun `exportableCsv is null when the History is empty`() = runTest {
        val viewModel = HistoryViewModel(FakeWeightRepository())
        collectedState(viewModel.uiState)

        assertNull(viewModel.exportableCsv())
    }

    private fun record(date: String, grams: Int) = Record(LocalDate.parse(date), grams)

    /** Storage that never answers, so screen state stays on its loading value. */
    private class SilentWeightRepository(
        private val delegate: WeightRepository = FakeWeightRepository()
    ) : WeightRepository by delegate {
        override fun records(): Flow<List<Record>> = MutableSharedFlow()
    }
}
