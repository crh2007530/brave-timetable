package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

internal fun useXatuWebProfile(webView: WebView) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
        WebViewCompat.setProfile(webView, "XatuAcademic")
    }
}

internal fun webViewCookieManager(webView: WebView): CookieManager =
    if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
        WebViewCompat.getProfile(webView).cookieManager
    } else {
        CookieManager.getInstance()
    }

/**
 * WebView 代理配置与客户端包装类
 */
class WebCompatDelegate(
    private val webView: WebView,
    private val isXatuImport: Boolean = false
) {

    private val defaultUserAgent: String = webView.settings.userAgentString
    private val requestInterceptor = WebViewRequestInterceptor()

    init {
        if (!isXatuImport) {
            webView.addJavascriptInterface(WebPostBridge(), "WebPostService")
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(webView, JS_INTERCEPT_POST, setOf("*"))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun enhanceSettings(isDesktopMode: Boolean): WebCompatDelegate {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            if (isDesktopMode) {
                userAgentString = DESKTOP_USER_AGENT
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                useWideViewPort = false
                loadWithOverviewMode = false
            } else {
                userAgentString = defaultUserAgent
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        val cookieManager = webViewCookieManager(webView)
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        return this
    }

    fun wrapWebViewClient(original: WebViewClient, isDesktopModeProvider: () -> Boolean): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return original.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (request != null && !isXatuImport) {
                    val currentDesktopMode = isDesktopModeProvider()
                    val interceptedResponse = requestInterceptor.intercept(request, currentDesktopMode)
                    if (interceptedResponse != null) {
                        return interceptedResponse
                    }
                }
                return original.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                original.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                original.onPageFinished(view, url)

                if (!isXatuImport && !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    view?.evaluateJavascript(JS_INTERCEPT_POST, null)
                }
                if (!isXatuImport || XatuImport.acceptsImportFrom(url.orEmpty())) {
                    view?.evaluateJavascript(JS_BRIDGE_INIT, null)
                }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                val handledByOriginal = runCatching { original.onRenderProcessGone(view, detail) }.getOrDefault(false)
                if (!handledByOriginal) {
                    view?.let { wv ->
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                }
                return true
            }

            override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                original.onReceivedSslError(v, h, e)
            }

            override fun onReceivedError(v: WebView, q: WebResourceRequest, e: WebResourceError) =
                original.onReceivedError(v, q, e)
        }
    }

    fun wrapWebChromeClient(original: WebChromeClient, onProgress: (Int) -> Unit): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
                original.onProgressChanged(view, newProgress)
            }
            override fun onReceivedTitle(v: WebView?, t: String?) = original.onReceivedTitle(v, t)
        }
    }
}
