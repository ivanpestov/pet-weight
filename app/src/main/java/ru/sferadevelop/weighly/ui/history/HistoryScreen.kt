package ru.sferadevelop.weighly.ui.history

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sferadevelop.weighly.R
import java.io.IOException
import java.time.LocalDate

@Composable
fun HistoryScreen(
    onAddRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.action_undo)
    val deletedMessage = stringResource(R.string.history_record_deleted)
    val exportFailedMessage = stringResource(R.string.history_export_failed)

    // CreateDocument hands back a null Uri when the user backs out of the picker, which is not
    // a failure: nothing was written, and nothing needs to be said.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = viewModel.exportableCsv()
        if (uri == null || csv == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val written = withContext(Dispatchers.IO) { writeCsv(context, uri, csv) }
            if (!written) snackbarHostState.showSnackbar(exportFailedMessage)
        }
    }

    // Each deletion gets its own snackbar coroutine rather than sharing one keyed on the latest
    // Record: SnackbarHostState already queues concurrent showSnackbar() calls, so a second swipe
    // before the first snackbar expires does not cost the first Record its undo window. The
    // Snackbar's own timing is that window, a second timer in the ViewModel would duplicate it.
    LaunchedEffect(Unit) {
        viewModel.deletions.collect { epochDay ->
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = deletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDeletion(epochDay)
                } else {
                    viewModel.finalizeDeletion(epochDay)
                }
            }
        }
    }

    HistoryScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddRecord = onAddRecord,
        onEditRecord = onEditRecord,
        onDeleteRecord = viewModel::deleteRecord,
        onExportHistory = { exportLauncher.launch(exportFileName(LocalDate.now())) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    uiState: HistoryUiState,
    snackbarHostState: SnackbarHostState,
    onAddRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onExportHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    HistoryOverflowMenu(
                        exportEnabled = uiState is HistoryUiState.History,
                        onExport = onExportHistory
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                onDeleteRecord = onDeleteRecord,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun HistoryOverflowMenu(exportEnabled: Boolean, onExport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.history_overflow_menu)
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.history_export)) },
            enabled = exportEnabled,
            onClick = {
                expanded = false
                onExport()
            }
        )
    }
}

/**
 * Writes [csv] to [uri], true on success. False on a thrown IOException, and also when
 * [uri] opens no stream at all - a legal [android.content.ContentResolver] answer that is
 * just as much a failure to export as an exception would be.
 */
private fun writeCsv(context: Context, uri: Uri, csv: String): Boolean =
    try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } != null
    } catch (e: IOException) {
        false
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
    onDeleteRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Scaffold's inner padding covers the bars but not the floating button, which would
        // otherwise sit on top of the last Record.
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE)
    ) {
        items(items = rows, key = RecordRow::date) { row ->
            RecordListItem(
                row = row,
                onEdit = { onEditRecord(row.epochDay) },
                onDelete = { onDeleteRecord(row.epochDay) },
                modifier = Modifier.animateItem()
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordListItem(
    row: RecordRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Any direction counts: the ticket deliberately skips a confirmation dialog, so a swipe
    // either way is the one gesture that deletes.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete()
            true
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
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
}

private val FAB_CLEARANCE = 88.dp
private val HORIZONTAL_PADDING = 16.dp
private val VERTICAL_PADDING = 12.dp
