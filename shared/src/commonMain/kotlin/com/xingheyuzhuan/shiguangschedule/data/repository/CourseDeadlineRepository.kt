package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Serializable
internal enum class DeadlinePlatform { Chaoxing, Zhidao, Other }

@Serializable
internal data class CourseDeadline(
    val id: String,
    val platform: DeadlinePlatform,
    val course: String,
    val due: LocalDateTime
)

@Single
internal class CourseDeadlineRepository(@Named("AppSettings") private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("brave_course_deadlines")

    val deadlines = store.data.map { preferences ->
        read(preferences).sortedWith(compareBy({ it.due }, { it.course }, { it.id }))
    }.distinctUntilChanged()

    suspend fun save(deadline: CourseDeadline) {
        store.edit { preferences ->
            preferences[key] = Json.encodeToString(read(preferences).filterNot { it.id == deadline.id } + deadline)
        }
    }

    suspend fun delete(id: String) {
        store.edit { preferences ->
            preferences[key] = Json.encodeToString(read(preferences).filterNot { it.id == id })
        }
    }

    private fun read(preferences: Preferences): List<CourseDeadline> =
        preferences[key]?.let { Json.decodeFromString<List<CourseDeadline>>(it) }.orEmpty()
}
