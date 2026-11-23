package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import shiguangschedule.shared.generated.resources.Res
import kotlin.coroutines.resume

@Serializable
internal data class ExamSemester(val id: String, val label: String)

@OptIn(ExperimentalResourceApi::class)
internal suspend fun WebViewController.readExamSemester(semester: ExamSemester?): String? {
    val sourceUrl = currentUrl
    if (!XatuImport.acceptsImportFrom(sourceUrl)) return null
    val parser = Res.readBytes("files/${XatuImport.examParserAsset}").decodeToString()
    val program = Res.readBytes("files/xatu_exam_semester.js").decodeToString()
    if (currentUrl != sourceUrl) return null
    return withTimeoutOrNull(25_000) {
        evaluateForResult(
            "window.__braveExamSemester = undefined; $program($parser, ${Json.encodeToString(semester)})" +
                ".then(data => { window.__braveExamSemester = data; }, " +
                "() => { window.__braveExamSemester = null; });"
        )
        while (currentUrl == sourceUrl) {
            if (evaluateForResult("typeof window.__braveExamSemester === 'undefined'") != "true") {
                return@withTimeoutOrNull evaluateForResult("window.__braveExamSemester")
                    ?.takeUnless { it == "null" }
            }
            delay(200)
        }
        null
    }
}

private suspend fun WebViewController.evaluateForResult(script: String): String? =
    suspendCancellableCoroutine { continuation ->
        evaluateJavascript(script) { continuation.resume(it) }
    }
