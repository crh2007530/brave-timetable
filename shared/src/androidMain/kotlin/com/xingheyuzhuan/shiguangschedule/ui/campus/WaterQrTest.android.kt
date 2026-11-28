package com.xingheyuzhuan.shiguangschedule.ui.campus

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.*

@Composable
internal actual fun WaterQrTest() {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<StringResource?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image ->
        if (image == null) return@rememberLauncherForActivityResult
        reading = true
        status = null
        page = null
        scope.launch {
            try {
                val qr = withContext(Dispatchers.Default) { readCampusQr(resolver, image, ::isWaterCheckout) }
                if (qr == null) {
                    status = Res.string.campus_water_qr_unreadable
                } else if (isWaterCheckout(qr)) {
                    pending = qr
                } else {
                    status = Res.string.campus_water_qr_foreign
                }
            } finally {
                reading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(enabled = !reading, onClick = { picker.launch("image/*") }) {
            Text(stringResource(Res.string.campus_water_qr_pick))
        }
        Text(stringResource(Res.string.campus_water_qr_hint), style = MaterialTheme.typography.bodySmall)
        status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        page?.let { url ->
            TextButton(onClick = { page = null }) {
                Text(stringResource(Res.string.campus_water_qr_close))
            }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportMultipleWindows(false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (!request.isForMainFrame || isWaterCheckout(request.url.toString())) return false
                                status = Res.string.campus_water_qr_external
                                return true
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) status = Res.string.campus_water_qr_load_error
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(440.dp),
                onRelease = { it.destroy() }
            )
        }
    }

    pending?.let { url ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(Res.string.campus_water_qr_confirm)) },
            text = { Text(stringResource(Res.string.campus_water_qr_confirm_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    page = url
                }) { Text(stringResource(Res.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(Res.string.action_cancel)) }
            }
        )
    }
}

private fun isWaterCheckout(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("88wise.com", ignoreCase = true) &&
        uri.userInfo == null && uri.port in setOf(-1, 443)
}

internal fun readCampusQr(resolver: ContentResolver, image: Uri, matches: (String) -> Boolean): String? {
    val bitmap = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, image)) { decoder, info, _ ->
                val size = info.size
                val longest = maxOf(size.width, size.height)
                if (longest > 2048) {
                    decoder.setTargetSize(
                        (size.width.toLong() * 2048 / longest).toInt().coerceAtLeast(1),
                        (size.height.toLong() * 2048 / longest).toInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(image)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (maxOf(bounds.outHeight, bounds.outWidth) / inSampleSize > 2048) inSampleSize *= 2
            }
            resolver.openInputStream(image)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } ?: return null

    return try {
        decodeCampusQr(bitmap, matches)
    } finally {
        bitmap.recycle()
    }
}

private fun decodeCampusQr(bitmap: Bitmap, matches: (String) -> Boolean): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return decodeWaterQrPixels(width, height, pixels, matches)
}
