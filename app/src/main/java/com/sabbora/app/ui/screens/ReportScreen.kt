package com.sabbora.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sabbora.app.R
import com.sabbora.app.data.repository.SabboraRepository
import com.sabbora.app.domain.report.StudentReport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReportViewModel(
    repository: SabboraRepository,
    classId: String,
) : ViewModel() {

    val rows: StateFlow<List<StudentReport>> = repository.observeReport(classId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(repository: SabboraRepository, classId: String) = viewModelFactory {
            initializer { ReportViewModel(repository, classId) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    repository: SabboraRepository,
    classId: String,
    onBack: () -> Unit,
    viewModel: ReportViewModel = viewModel(
        factory = ReportViewModel.factory(repository, classId),
        key = classId,
    ),
) {
    val rows by viewModel.rows.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { insets ->
        if (rows.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.no_students_yet),
                modifier = Modifier.fillMaxSize().padding(insets),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.studentId }) { row -> ReportRow(row) }
            }
        }
    }
}

@Composable
private fun ReportRow(row: StudentReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = row.studentName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Figure(
                    label = stringResource(R.string.attendance_rate),
                    value = row.attendanceRate.asPercent(),
                )
                Figure(
                    label = stringResource(R.string.average),
                    value = row.scoreAverage.asPercent(),
                )
                Figure(
                    label = stringResource(R.string.days_marked),
                    value = row.daysMarked.toString(),
                )
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * A `null` figure means the app has nothing to base it on, and prints an em dash.
 * Rendering it as "0%" would be a claim the data does not support.
 */
private fun Double?.asPercent(): String =
    if (this == null) "—" else "${(this * 100).toInt()}%"
