package com.xingheyuzhuan.shiguangschedule.ui.campus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseDeadline
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseDeadlineRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.DeadlinePlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import shiguangschedule.shared.generated.resources.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun CourseDeadlinesScreen(
    onBack: () -> Unit,
    repository: CourseDeadlineRepository = koinInject()
) {
    val deadlines by repository.deadlines.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CourseDeadline?>(null) }
    val zone = remember { TimeZone.of("Asia/Shanghai") }
    val now by produceState(Clock.System.now().toLocalDateTime(zone), zone) {
        while (true) {
            value = Clock.System.now().toLocalDateTime(zone)
            delay((60 - value.second) * 1_000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.campus_deadlines)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(vectorResource(Res.drawable.arrow_back_24px), stringResource(Res.string.a11y_back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        adding = true
                        editingId = Uuid.random().toString()
                    }) {
                        Text(stringResource(Res.string.action_add))
                    }
                }
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (deadlines.isEmpty()) {
                item { Text(stringResource(Res.string.campus_deadlines_empty)) }
            }
            items(deadlines, key = { it.id }) { deadline ->
                Card(onClick = {
                    adding = false
                    editingId = deadline.id
                }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(deadline.course, style = MaterialTheme.typography.titleMedium)
                        val deadlineTime = deadline.due.time.toString().take(5)
                        Text("${deadline.due.date}  $deadlineTime  ·  ${deadline.platform.label()}")
                        if (deadline.due <= now) {
                            Text(
                                stringResource(Res.string.campus_deadline_elapsed),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    editingId?.let { id ->
        val deadline = deadlines.firstOrNull { it.id == id }
        if (!adding && deadline == null) return@let
        DeadlineEditor(
            id = id,
            deadline = deadline,
            onDismiss = { editingId = null },
            onDelete = {
                editingId = null
                pendingDelete = deadline
            },
            onSave = {
                scope.launch {
                    repository.save(it)
                    editingId = null
                }
            }
        )
    }
    pendingDelete?.let { deadline ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(Res.string.campus_deadline_delete)) },
            text = { Text(deadline.course) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.delete(deadline.id)
                        pendingDelete = null
                    }
                }) { Text(stringResource(Res.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(Res.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun DeadlineEditor(
    id: String,
    deadline: CourseDeadline?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (CourseDeadline) -> Unit
) {
    var course by rememberSaveable(id) { mutableStateOf(deadline?.course.orEmpty()) }
    var date by rememberSaveable(id) { mutableStateOf(deadline?.due?.date?.toString().orEmpty()) }
    var time by rememberSaveable(id) { mutableStateOf(deadline?.due?.time?.toString()?.take(5).orEmpty()) }
    var platform by rememberSaveable(id) { mutableStateOf(deadline?.platform ?: DeadlinePlatform.Chaoxing) }
    val parsedDate = remember(date) { runCatching { LocalDate.parse(date.trim()) }.getOrNull() }
    val parsedTime = remember(time) { runCatching { LocalTime.parse(time.trim()) }.getOrNull() }
    val due = if (parsedDate != null && parsedTime != null) LocalDateTime(parsedDate, parsedTime) else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.campus_deadlines)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeadlinePlatform.entries.forEach { option ->
                        FilterChip(
                            selected = platform == option,
                            onClick = { platform = option },
                            label = { Text(option.label()) }
                        )
                    }
                }
                OutlinedTextField(
                    course, { course = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.exam_course_label)) }, singleLine = true
                )
                OutlinedTextField(
                    date, { date = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.campus_deadline_date)) }, singleLine = true,
                    isError = date.length >= 10 && parsedDate == null
                )
                OutlinedTextField(
                    time, { time = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.campus_deadline_time)) }, singleLine = true,
                    isError = time.length >= 5 && parsedTime == null
                )
                if (deadline != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(Res.string.campus_deadline_delete)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = course.isNotBlank() && due != null,
                onClick = { due?.let { onSave(CourseDeadline(id, platform, course.trim(), it)) } }
            ) { Text(stringResource(Res.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}

@Composable
private fun DeadlinePlatform.label(): String = stringResource(
    when (this) {
        DeadlinePlatform.Chaoxing -> Res.string.campus_chaoxing
        DeadlinePlatform.Zhidao -> Res.string.campus_zhidao
        DeadlinePlatform.Other -> Res.string.campus_deadline_other
    }
)
