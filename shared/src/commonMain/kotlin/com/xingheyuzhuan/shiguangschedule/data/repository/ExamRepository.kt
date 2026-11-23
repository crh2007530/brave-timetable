package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Serializable
data class ExamPlan(
    val id: String,
    val course: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime?,
    val room: String
)

@Single
class ExamRepository(@Named("AppSettings") private val store: DataStore<Preferences>) {
    private val examsKey = stringPreferencesKey("brave_exam_plans")

    val exams = store.data.map { preferences ->
        readExams(preferences).sortedWith(compareBy({ it.date }, { it.start }, { it.course }))
    }.distinctUntilChanged()

    suspend fun save(exam: ExamPlan) {
        store.edit { preferences ->
            val exams = readExams(preferences).filterNot { it.id == exam.id } + exam
            preferences[examsKey] = Json.encodeToString(exams)
        }
    }

    suspend fun delete(id: String) {
        store.edit { preferences ->
            preferences[examsKey] = Json.encodeToString(readExams(preferences).filterNot { it.id == id })
        }
    }

    private fun readExams(preferences: Preferences): List<ExamPlan> =
        preferences[examsKey]?.let { Json.decodeFromString<List<ExamPlan>>(it) }.orEmpty()
}
