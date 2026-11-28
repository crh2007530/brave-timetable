package com.xingheyuzhuan.shiguangschedule.ui.campus

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.*

private const val LAUNDRY_PACKAGE = "com.midea.vm.washer"

@Composable
internal actual fun LaundryActions(onOpenProvider: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val directory = remember(context) { File(context.cacheDir, "share_temp/campus-qr") }
    var photoName by rememberSaveable { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<StringResource?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val photo = photoName?.let { File(directory, it) } ?: return@rememberLauncherForActivityResult
        photoName = null
        if (!captured) {
            photo.delete()
            return@rememberLauncherForActivityResult
        }
        reading = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    readCampusQr(context.contentResolver, photoUri(context, photo)) { laundryTarget(it) != null }
                }
                val target = result?.let(::laundryTarget)
                status = when {
                    result == null -> Res.string.campus_laundry_qr_unreadable
                    target == null -> Res.string.campus_laundry_qr_foreign
                    !openLaundryTarget(context, target) -> Res.string.campus_laundry_qr_open_failed
                    else -> null
                }
            } finally {
                photo.delete()
                reading = false
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !reading && photoName == null, onClick = {
            status = null
            var photo: File? = null
            if (runCatching {
                directory.mkdirs()
                val capture = File.createTempFile("laundry-", ".jpg", directory)
                photo = capture
                photoName = capture.name
                camera.launch(photoUri(context, capture))
            }.isFailure) {
                photo?.delete()
                photoName = null
                status = Res.string.campus_laundry_camera_unavailable
            }
        }) {
            Text(stringResource(Res.string.campus_laundry_scan))
        }
        TextButton(onClick = onOpenProvider) {
            Text(stringResource(Res.string.campus_laundry_open))
        }
    }
    status?.let { Text(stringResource(it)) }
}

private fun photoUri(context: Context, photo: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)

private fun laundryTarget(value: String): Uri? {
    val raw = value.trim()
    val link = runCatching { URI(raw) }.getOrNull() ?: return null
    if (link.isOpaque || link.rawUserInfo != null || link.port !in setOf(-1, 443)) return null
    val host = link.host?.lowercase() ?: return null
    val uri = Uri.parse(raw).normalizeScheme()
    val legacyWasherCode = host == "app.littleswan.com" && uri.path == "/u_download.html" &&
        uri.getQueryParameter("type") == "Ujing" && !uri.getQueryParameter("uuid").isNullOrBlank()
    return when (uri.scheme) {
        "ujing" -> uri.takeIf { host == "index" && link.port == -1 }
        "https" -> uri.takeIf {
            host == "ujing.online" || host.endsWith(".ujing.online") || legacyWasherCode
        }
        "http" -> uri.takeIf { legacyWasherCode && link.port == -1 }
        else -> null
    }
}

private fun openLaundryTarget(context: Context, uri: Uri): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    if (runCatching { context.startActivity(Intent(intent).setPackage(LAUNDRY_PACKAGE)) }.isSuccess) return true
    return uri.scheme in setOf("http", "https") && runCatching { context.startActivity(intent) }.isSuccess
}
