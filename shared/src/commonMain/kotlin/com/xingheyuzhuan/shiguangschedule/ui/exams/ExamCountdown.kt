package com.xingheyuzhuan.shiguangschedule.ui.exams

import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolExam
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil

internal enum class ExamStatus { Upcoming, Today, Started, Pending, Ended }

internal data class ExamTiming(val date: LocalDate?, val start: LocalTime?, val end: LocalTime?) {
    fun status(now: LocalDateTime): ExamStatus = when {
        date == null -> ExamStatus.Pending
        date > now.date -> ExamStatus.Upcoming
        date < now.date -> ExamStatus.Ended
        end != null && now.time >= end -> ExamStatus.Ended
        start != null && now.time >= start -> ExamStatus.Started
        else -> ExamStatus.Today
    }

    fun daysRemaining(now: LocalDateTime): Int = date?.let { now.date.daysUntil(it) } ?: 0

    fun sortGroup(now: LocalDateTime): Int = when (status(now)) {
        ExamStatus.Upcoming, ExamStatus.Today, ExamStatus.Started -> 0
        ExamStatus.Pending -> 1
        ExamStatus.Ended -> 2
    }
}

private val datePattern = Regex("""(?<!\d)(\d{4})\s*[-/.年]\s*(\d{1,2})\s*[-/.月]\s*(\d{1,2})(?!\d)""")
private val timePattern = Regex("""(?<!\d)(\d{1,2})[:：](\d{2})(?!\d)""")

internal fun SchoolExam.timing(): ExamTiming {
    val dateMatch = datePattern.find(date) ?: datePattern.find(time)
    val examDate = dateMatch?.groupValues?.let { (_, year, month, day) ->
        runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    }
    val times = timePattern.findAll(time).take(2).map { match ->
        runCatching { LocalTime(match.groupValues[1].toInt(), match.groupValues[2].toInt()) }.getOrNull()
    }.toList()
    val start = times.getOrNull(0)
    val end = times.getOrNull(1)?.takeIf { start != null && it > start }
    return ExamTiming(examDate, start, end)
}

internal fun orderSchoolExams(exams: List<Pair<SchoolExam, ExamTiming>>, now: LocalDateTime) =
    exams.sortedWith(compareBy(
        { it.second.sortGroup(now) },
        { it.second.date },
        { it.second.start ?: LocalTime(23, 59, 59) },
        { it.first.course }
    ))
