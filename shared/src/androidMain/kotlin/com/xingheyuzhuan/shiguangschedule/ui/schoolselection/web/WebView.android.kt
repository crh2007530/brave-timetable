package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

/**
 * Android 端的 WebViewController 实现
 */
class AndroidWebViewController : WebViewController {
    var webViewInstance: WebView? = null

    override val currentUrl: String
        get() = webViewInstance?.url ?: ""

    override fun reload() {
        webViewInstance?.reload()
    }

    override fun goBack(): Boolean {
        return if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
            true
        } else {
            false
        }
    }

    override fun canGoBack(): Boolean {
        return webViewInstance?.canGoBack() == true
    }

    override fun setDevToolsEnabled(enabled: Boolean) {
        WebView.setWebContentsDebuggingEnabled(enabled)
    }

    override fun executeScript(jsCode: String) {
        evaluateJavascript(jsCode, null)
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        webViewInstance?.post {
            val finalScript = if (script.contains("_shiguangBridgeInjected")) {
                script
            } else {
                """
                $JS_BRIDGE_INIT
                $script
                """.trimIndent()
            }

            webViewInstance?.evaluateJavascript(finalScript) { res ->
                callback?.invoke(res)
            }
        }
    }
}

@Composable
actual fun rememberWebViewController(): WebViewController {
    return remember { AndroidWebViewController() }
}

class NativeBridge(private val handleMessage: (String) -> Unit) {
    @JavascriptInterface
    @Suppress("Unused")
    fun postMessage(jsonMessage: String) {
        handleMessage(jsonMessage)
    }
}

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(
    modifier: Modifier,
    url: String,
    isDesktopMode: Boolean,
    isDevToolsEnabled: Boolean,
    isXatuImport: Boolean,
    controller: WebViewController,
    bridgeHandler: WebBridgeHandler,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateToSchedule: () -> Unit,
    onSslError: (failingUrl: String, onProceed: () -> Unit, onCancel: () -> Unit) -> Unit
) {
    val androidController = controller as? AndroidWebViewController

    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    val currentOnTitleChange by rememberUpdatedState(onTitleChange)
    val currentIsDesktopMode by rememberUpdatedState(isDesktopMode)
    val currentOnSslError by rememberUpdatedState(onSslError)

    var loadedBaseUrl by remember { mutableStateOf("") }
    var isFirstLaunch by remember { mutableStateOf(true) }

    // 监听外部强制 URL 变更
    LaunchedEffect(url) {
        if (url.isNotBlank() && url != "about:blank" && url != loadedBaseUrl) {
            loadedBaseUrl = url
            androidController?.webViewInstance?.let { wv ->
                if (wv.url != url) {
                    wv.loadUrl(url)
                }
            }
        }
    }

    // 监听 桌面/移动 模式动态切换
    LaunchedEffect(isDesktopMode) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }

        androidController?.webViewInstance?.let { wv ->
            val delegate = WebCompatDelegate(wv, isXatuImport)
            delegate.enhanceSettings(isDesktopMode)
            if (isDesktopMode) {
                wv.setInitialScale(100)
            } else {
                wv.setInitialScale(0)
            }

            val currentRealUrl = wv.url?.takeIf { it.isNotBlank() && it != "about:blank" } ?: url
            if (currentRealUrl.isNotBlank() && currentRealUrl != "about:blank") {
                wv.loadUrl(currentRealUrl)
            }
        }
    }

    // 监听开发者工具开关
    LaunchedEffect(isDevToolsEnabled) {
        WebView.setWebContentsDebuggingEnabled(isDevToolsEnabled)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    if (isXatuImport) useXatuWebProfile(this)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    if (isDesktopMode) {
                        setInitialScale(100)
                    } else {
                        setInitialScale(0)
                    }

                    androidController?.webViewInstance = this

                    val delegate = WebCompatDelegate(this, isXatuImport)
                    delegate.enhanceSettings(isDesktopMode)

                    if (isXatuImport && WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                        WebViewCompat.addWebMessageListener(
                            this,
                            "_shiguangNativeBridge",
                            setOf("http://jwgl2018.xatu.edu.cn", "https://jwgl2018.xatu.edu.cn")
                        ) { view, message, _, isMainFrame, _ ->
                            if (isMainFrame && XatuImport.acceptsImportFrom(view.url.orEmpty())) {
                                message.data?.let(bridgeHandler::onMessageReceived)
                            }
                        }
                    } else {
                        val bridge = NativeBridge { message ->
                            if (isXatuImport) {
                                post {
                                    if (XatuImport.acceptsImportFrom(this.url.orEmpty())) {
                                        bridgeHandler.onMessageReceived(message)
                                    }
                                }
                            } else {
                                bridgeHandler.onMessageReceived(message)
                            }
                        }
                        addJavascriptInterface(bridge, "_shiguangNativeBridge")
                    }

                    val baseChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            currentOnProgressChange(newProgress / 100f)
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                currentOnTitleChange(title)
                            }
                        }
                    }

                    val baseViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url ?: return false
                            // EAMS can redirect its legacy HTTP pages to CAS. Keep sign-in on HTTPS.
                            if (isXatuImport && request.isForMainFrame && url.scheme == "http" &&
                                url.host == "authserver.xatu.edu.cn" && url.path == "/authserver/login"
                            ) {
                                view?.loadUrl(url.buildUpon().scheme("https").build().toString())
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            currentOnProgressChange(0.1f)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            currentOnProgressChange(1.0f)
                            view?.evaluateJavascript("document.title") { value ->
                                val unquoted = value?.trim('"')?.replace("\\\"", "\"")?.trim() ?: ""
                                if (unquoted.isNotBlank() && unquoted != "null") {
                                    currentOnTitleChange(unquoted)
                                }
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            if (isXatuImport) {
                                handler?.cancel()
                                return
                            }
                            val failingUrl = error?.url ?: view?.url ?: ""
                            currentOnSslError(
                                failingUrl,
                                { handler?.proceed() },
                                { handler?.cancel() }
                            )
                        }
                    }

                    webChromeClient = delegate.wrapWebChromeClient(baseChromeClient) { progressInt ->
                        currentOnProgressChange(progressInt / 100f)
                    }

                    webViewClient = delegate.wrapWebViewClient(baseViewClient) { currentIsDesktopMode }

                    if (url.isNotBlank() && url != "about:blank") {
                        loadedBaseUrl = url
                        loadUrl(url)
                    }
                }
            },
            update = { webView ->
                androidController?.webViewInstance = webView
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            androidController?.webViewInstance?.let { wv ->
                val cookieManager = webViewCookieManager(wv)
                wv.stopLoading()
                wv.webChromeClient = null
                wv.webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        return true
                    }
                }

                (wv.parent as? ViewGroup)?.removeView(wv)

                wv.clearHistory()
                wv.clearCache(true)
                wv.clearFormData()
                wv.clearSslPreferences()
                wv.destroy()
                if (isXatuImport) {
                    cookieManager.flush()
                } else {
                    cookieManager.removeAllCookies { cookieManager.flush() }
                }
            }

            androidController?.webViewInstance = null
        }
    }
}
