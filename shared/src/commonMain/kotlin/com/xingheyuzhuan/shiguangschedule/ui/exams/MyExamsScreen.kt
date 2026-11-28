package com.xingheyuzhuan.shiguangschedule.ui.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.ExamPlan
import com.xingheyuzhuan.shiguangschedule.data.repository.ExamRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolExamRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalScaffoldPadding
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.XatuImport
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import shiguangschedule.shared.generated.resources.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun MyExamsScreen(
    onNavigate: (Destination) -> Unit,
    repository: ExamRepository = koinInject(),
    schoolRepository: SchoolExamRepository = koinInject()
) {
    val exams by repository.exams.collectAsStateWithLifecycle(initialValue = emptyList())
    val schoolExams by schoolRepository.exams.collectAsStateWithLifecycle(initialValue = emptyList())
    val schoolZone = remember { TimeZone.of("Asia/Shanghai") }
    val now by produceState(Clock.System.now().toLocalDateTime(schoolZone), schoolZone) {
        while (true) {
            value = Clock.System.now().toLocalDateTime(schoolZone)
            delay((60 - value.second) * 1000L)
        }
    }
    val examSchedule = remember(schoolExams) { schoolExams.map { it to it.timing() } }
    val orderedExamSchedule = remember(examSchedule, now) {
        orderSchoolExams(examSchedule, now)
    }
    val bottomPadding = LocalScaffoldPadding.current.calculateBottomPadding()
    val scope = rememberCoroutineScope()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var examToDelete by remember { mutableStateOf<ExamPlan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.title_my_exams)) })
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.padding(insets),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(onClick = {
                    onNavigate(Destination.WebView(initialUrl = XatuImport.examHomeUrl, xatuExamImport = true))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.action_import_school_exams))
                }
                if (schoolExams.isEmpty() && exams.isEmpty()) {
                    Text(
                        stringResource(Res.string.exam_no_imported_schedule),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(orderedExamSchedule) { (exam, timing) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExamCardHeading(exam.course, timing, now)
                        val schedule = listOf(exam.date, exam.time).filter { it.isNotBlank() }.joinToString("  ")
                        if (schedule.isNotBlank()) Text(schedule, color = MaterialTheme.colorScheme.primary)
                        if (exam.room.isNotBlank()) Text(exam.room)
                        val seatNumber = exam.seatNumber
                        Text(
                            text = if (seatNumber == null) stringResource(Res.string.exam_seat_unavailable)
                                else stringResource(Res.string.exam_seat_number, seatNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (seatNumber == null) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary
                        )
                        for (detail in exam.details.filterNot { it.isSeatNumber }) {
                            Text("${detail.label}  ${detail.value}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (exams.isNotEmpty()) {
                item {
                    Text(stringResource(Res.string.exam_local_schedule), style = MaterialTheme.typography.titleMedium)
                }
            }
            items(exams, key = { it.id }) { exam ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExamCardHeading(exam.course, ExamTiming(exam.date, exam.start, exam.end), now)
                        val endTime = exam.end?.let { " – $it" }.orEmpty()
                        Text("${exam.date}  ${exam.start}$endTime", color = MaterialTheme.colorScheme.primary)
                        if (exam.room.isNotBlank()) {
                            Text(exam.room, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                editingId = exam.id
                                showEditor = true
                            }) {
                                Text(stringResource(Res.string.action_edit_exam))
                            }
                            TextButton(onClick = { examToDelete = exam }) {
                                Text(stringResource(Res.string.action_delete_exam))
                            }
                        }
                    }
                }
            }
        }
    }

    val editingExam = exams.firstOrNull { it.id == editingId }
    if (showEditor && (editingId == null || editingExam != null)) {
        ExamEditor(
            exam = editingExam,
            onDismiss = { showEditor = false },
            onSave = { exam ->
                scope.launch {
                    repository.save(exam)
                    showEditor = false
                }
            }
        )
    }
    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text(stringResource(Res.string.action_delete_exam)) },
            text = { Text(exam.course) },
            confirmButton = {
                TextButton(onClick = {
                    examToDelete = null
                    scope.launch { repository.delete(exam.id) }
                }) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) { Text(stringResource(Res.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun ExamCardHeading(course: String, timing: ExamTiming, now: LocalDateTime) {
    val status = timing.status(now)
    val text = when (status) {
        ExamStatus.Upcoming -> stringResource(Res.string.exam_countdown_days, timing.daysRemaining(now))
        ExamStatus.Today -> stringResource(Res.string.exam_countdown_today)
        ExamStatus.Started -> stringResource(Res.string.exam_countdown_started)
        ExamStatus.Pending -> stringResource(Res.string.exam_school_time_pending)
        ExamStatus.Ended -> stringResource(Res.string.exam_countdown_ended)
    }
    val active = status != ExamStatus.Pending && status != ExamStatus.Ended
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(course, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(
            text = text,
            modifier = Modifier.background(
                if (active) colors.primaryContainer else colors.surfaceContainerHighest,
                RoundedCornerShape(12.dp)
            ).padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ExamEditor(exam: ExamPlan?, onDismiss: () -> Unit, onSave: (ExamPlan) -> Unit) {
    val examId = rememberSaveable(exam?.id) { exam?.id ?: Uuid.random().toString() }
    var course by rememberSaveable(exam?.id) { mutableStateOf(exam?.course.orEmpty()) }
    var date by rememberSaveable(exam?.id) { mutableStateOf(exam?.date?.toString().orEmpty()) }
    var start by rememberSaveable(exam?.id) { mutableStateOf(exam?.start?.toString().orEmpty()) }
    var end by rememberSaveable(exam?.id) { mutableStateOf(exam?.end?.toString().orEmpty()) }
    var room by rememberSaveable(exam?.id) { mutableStateOf(exam?.room.orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val dateValue = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    val startValue = runCatching { LocalTime.parse(start.trim()) }.getOrNull()
    val endValue = runCatching { LocalTime.parse(end.trim()) }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (exam == null) Res.string.action_add_exam else Res.string.action_edit_exam)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(course, { course = it }, label = { Text(stringResource(Res.string.exam_course_label)) }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text(stringResource(Res.string.exam_date_label)) }, singleLine = true)
                OutlinedTextField(start, { start = it }, label = { Text(stringResource(Res.string.exam_start_label)) }, singleLine = true)
                OutlinedTextField(end, { end = it }, label = { Text(stringResource(Res.string.exam_end_label)) }, singleLine = true)
                OutlinedTextField(room, { room = it }, label = { Text(stringResource(Res.string.exam_room_label)) }, singleLine = true)
                if (submitted) {
                    Text(stringResource(Res.string.exam_invalid_time), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (course.isBlank() || dateValue == null || startValue == null ||
                    (end.isNotBlank() && (endValue == null || endValue <= startValue))
                ) {
                    submitted = true
                } else {
                    onSave(ExamPlan(examId, course.trim(), dateValue, startValue, endValue, room.trim()))
                }
            }) { Text(stringResource(Res.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}
