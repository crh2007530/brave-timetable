package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Serializable
data class SchoolExam(
    val course: String,
    val date: String,
    val time: String,
    val room: String,
    val details: List<ExamDetail>
)

@Serializable
data class ExamDetail(val label: String, val value: String)

@Single
class SchoolExamRepository(@Named("SchoolExamCache") private val store: DataStore<Preferences>) {
    private val examsKey = stringPreferencesKey("latest_school_exams")
    private val browserExamsKey = stringPreferencesKey("browser_school_exams")

    val exams = store.data.map { preferences ->
        val school = read(preferences[examsKey])
        val browser = read(preferences[browserExamsKey])
        (school + browser).distinctBy { it.identity() }.sortedWith(
            compareBy<SchoolExam>({ it.date.isBlank() }, { it.date }, { it.time }, { it.course })
        )
    }.distinctUntilChanged()

    suspend fun replace(exams: List<SchoolExam>) {
        val snapshot = Json.encodeToString(exams)
        store.edit { preferences ->
            if (preferences[examsKey] != snapshot) preferences[examsKey] = snapshot
        }
    }

    private fun read(json: String?): List<SchoolExam> =
        json?.let { Json.decodeFromString<List<SchoolExam>>(it) }.orEmpty()

    private fun SchoolExam.identity() = listOf(course, date, time.replace('~', '-').replace('～', '-'), room)
}

internal fun decodeSchoolExams(result: String?): List<SchoolExam>? =
    result?.takeUnless { it.isBlank() || it == "null" }
        ?.let { Json.decodeFromString<List<SchoolExam>>(it).distinct() }
