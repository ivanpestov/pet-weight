package ru.sferadevelop.weighly.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.sferadevelop.weighly.R

@Composable
fun HistoryScreen(
    onAddRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onAddRecord = onAddRecord,
        onEditRecord = onEditRecord,
        modifier = modifier
    )
}

@Composable
private fun HistoryScreen(
    uiState: HistoryUiState,
    onAddRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecord) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.history_add_record)
                )
            }
        }
    ) { innerPadding ->
        when (uiState) {
            HistoryUiState.Loading -> Unit
            HistoryUiState.Empty -> EmptyHistory(modifier = Modifier.padding(innerPadding))
            is HistoryUiState.History -> RecordList(
                rows = uiState.rows,
                onEditRecord = onEditRecord,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(HORIZONTAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.history_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecordList(
    rows: List<RecordRow>,
    onEditRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Scaffold's inner padding covers the bars but not the floating button, which would
        // otherwise sit on top of the last Record.
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE)
    ) {
        items(items = rows, key = RecordRow::date) { row ->
            RecordListItem(row = row, onEdit = { onEditRecord(row.epochDay) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecordListItem(row: RecordRow, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = row.date, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.weight_with_unit, row.weight),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private val FAB_CLEARANCE = 88.dp
private val HORIZONTAL_PADDING = 16.dp
private val VERTICAL_PADDING = 12.dp
