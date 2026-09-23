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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sabbora.app.R
import com.sabbora.app.data.local.StudentEntity
import com.sabbora.app.data.repository.SabboraRepository
import com.sabbora.app.domain.model.AttendanceStatus
import com.sabbora.app.ui.theme.AttendanceColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class RollCallViewModel(
    private val repository: SabboraRepository,
    private val classId: String,
) : ViewModel() {

    private val _day = MutableStateFlow(LocalDate.now())

    /** The day being marked. Teachers routinely fill in a day they missed. */
    val day: StateFlow<LocalDate> = _day.asStateFlow()

    val students: StateFlow<List<StudentEntity>> = repository.observeStudents(classId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Status by student id for the selected day; a missing key means "not marked yet". */
    val marks: StateFlow<Map<String, AttendanceStatus>> = _day
        .flatMapLatest { selected -> repository.observeAttendance(classId, selected) }
        .map { records -> records.associate { it.studentId to it.status } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun shiftDay(days: Long) {
        val candidate = _day.value.plusDays(days)
        // A roll call cannot be taken for a day that has not happened.
        if (!candidate.isAfter(LocalDate.now())) _day.value = candidate
    }

    fun mark(studentId: String, status: AttendanceStatus) {
        viewModelScope.launch { repository.mark(studentId, _day.value, status) }
    }

    fun addStudent(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addStudent(classId, name) }
    }

    companion object {
        fun factory(repository: SabboraRepository, classId: String) = viewModelFactory {
            initializer { RollCallViewModel(repository, classId) }
        }
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollCallScreen(
    repository: SabboraRepository,
    classId: String,
    onBack: () -> Unit,
    onOpenReport: () -> Unit,
    viewModel: RollCallViewModel = viewModel(
        factory = RollCallViewModel.factory(repository, classId),
        key = classId,
    ),
) {
    val students by viewModel.students.collectAsState()
    val marks by viewModel.marks.collectAsState()
    val day by viewModel.day.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.roll_call)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (pending > 0) PendingBadge(pending)
                    IconButton(onClick = onOpenReport) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.report),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_student))
            }
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {

            DayPicker(
                label = day.format(dayFormat),
                onPrevious = { viewModel.shiftDay(-1) },
                onNext = { viewModel.shiftDay(1) },
                canGoForward = day.isBefore(LocalDate.now()),
            )

            if (students.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.no_students_yet),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentRow(
                            name = student.name,
                            current = marks[student.id],
                            onMark = { status -> viewModel.mark(student.id, status) },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(
            title = stringResource(R.string.add_student),
            label = stringResource(R.string.student_name),
            onDismiss = { showAdd = false },
            onConfirm = { name ->
                viewModel.addStudent(name)
                showAdd = false
            },
        )
    }
}

@Composable
private fun DayPicker(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoForward: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.previous_day),
            )
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.next_day),
            )
        }
    }
}

@Composable
private fun StudentRow(
    name: String,
    current: AttendanceStatus?,
    onMark: (AttendanceStatus) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusChip(R.string.present, AttendanceStatus.PRESENT, current, AttendanceColors.present, onMark)
                StatusChip(R.string.late, AttendanceStatus.LATE, current, AttendanceColors.late, onMark)
                StatusChip(R.string.absent, AttendanceStatus.ABSENT, current, AttendanceColors.absent, onMark)
                StatusChip(R.string.excused, AttendanceStatus.EXCUSED, current, AttendanceColors.excused, onMark)
            }
        }
    }
}

@Composable
private fun StatusChip(
    labelRes: Int,
    status: AttendanceStatus,
    current: AttendanceStatus?,
    color: androidx.compose.ui.graphics.Color,
    onMark: (AttendanceStatus) -> Unit,
) {
    val selected = current == status
    FilterChip(
        selected = selected,
        onClick = { onMark(status) },
        label = { Text(stringResource(labelRes)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
