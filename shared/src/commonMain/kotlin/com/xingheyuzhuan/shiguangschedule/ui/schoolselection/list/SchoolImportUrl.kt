package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.XatuImport
import school_index.Adapter
import school_index.AdapterCategory
import school_index.School

internal data class SchoolImportMatch(val schoolName: String, val adapter: Adapter, val destination: Destination.WebView)

internal fun schoolImportDestination(adapter: Adapter, resourceFolder: String): Destination.WebView {
    val file = adapter.asset_js_path.ifBlank { "${adapter.adapter_id}.js" }
    val assetPath = "$resourceFolder/$file"
    val url = if (assetPath == XatuImport.adapterPath) XatuImport.loginUrl
    else adapter.import_url.orEmpty().ifBlank { "about:blank" }
    return Destination.WebView(initialUrl = url, assetJsPath = assetPath)
}

internal data class SchoolImportUrl(val host: String, val path: String) {
    companion object {
        private val urlPattern = Regex(
            """^https?://([a-z0-9](?:[a-z0-9.-]*[a-z0-9])?)(?::(\d{1,5}))?(/[^?#\s\\]*)?(?:\?[^#\s\\]*)?(?:#[^\s\\]*)?$""",
            RegexOption.IGNORE_CASE
        )

        fun parse(input: String): SchoolImportUrl? {
            val raw = input.trim()
            val url = if ("://" in raw) raw else "https://$raw"
            val match = urlPattern.matchEntire(url) ?: return null
            val host = match.groupValues[1].lowercase()
            if (!host.contains('.') || host.contains("..")) return null
            val port = match.groupValues[2]
            if (port.isNotEmpty() && port.toInt() !in 1..65535) return null
            val path = match.groupValues[3].trimEnd('/').lowercase()
            return SchoolImportUrl(host, path)
        }
    }
}

internal fun findSchoolImports(input: SchoolImportUrl, schools: List<School>): List<SchoolImportMatch> {
    val candidates = schools.flatMap { school ->
        school.adapters.filter { adapter ->
            adapter.category == AdapterCategory.BACHELOR_AND_ASSOCIATE ||
                adapter.category == AdapterCategory.POSTGRADUATE
        }.mapNotNull { adapter ->
            val destination = schoolImportDestination(adapter, school.resource_folder)
            val urls = listOfNotNull(adapter.import_url, destination.initialUrl).toMutableList()
            if (destination.assetJsPath == XatuImport.adapterPath) urls.add(XatuImport.examHomeUrl)
            val matches = urls.mapNotNull(SchoolImportUrl::parse).filter { it.host == input.host }
            if (matches.isEmpty()) return@mapNotNull null
            val score = matches.maxOf { candidate ->
                when {
                    candidate.path.isEmpty() -> 0
                    input.path == candidate.path -> candidate.path.length + 1
                    input.path.startsWith(candidate.path + "/") -> candidate.path.length
                    else -> 0
                }
            }
            score to SchoolImportMatch(school.name, adapter, destination)
        }
    }
    val bestScore = candidates.maxOfOrNull { it.first } ?: return emptyList()
    return candidates.filter { it.first == bestScore }.map { it.second }
}
