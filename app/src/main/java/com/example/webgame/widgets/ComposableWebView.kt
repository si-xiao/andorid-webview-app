package com.example.webgame.widgets

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webgame.isUrlInWhiteList
import androidx.core.net.toUri

/**
 * WebView 状态数据类
 */
data class WebViewState(
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * WebView 配置
 */
data class WebViewSettings(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = true,
    val allowContentAccess: Boolean = true,
    val loadWithOverviewMode: Boolean = true,
    val useWideViewPort: Boolean = true,
    val supportMultipleWindows: Boolean = true,
    val javaScriptCanOpenWindowsAutomatically: Boolean = true,
    val mediaPlaybackRequiresUserGesture: Boolean = false,
    val allowUniversalAccessFromFileURLs: Boolean = true,
    val mixedContentMode: Int = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW,
    val userAgent: String? = "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36",
    val zoomControlEnabled: Boolean = false,
    val displayZoomControls: Boolean = false
)

/**
 * Compose WebView 组件
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposableWebView(
    url: String,
    modifier: Modifier = Modifier,
    settings: WebViewSettings = WebViewSettings(),
    onProgressChanged: ((Int) -> Unit)? = null,
    onPageStarted: ((String) -> Unit)? = null,
    onPageFinished: ((String) -> Unit)? = null,
    onReceivedError: ((String, String) -> Unit)? = null,
    onReceivedHttpError: ((Int, String) -> Unit)? = null,
    shouldOverrideUrlLoading: ((String) -> Boolean)? = null,
    onCreateWindow: ((String) -> Boolean)? = null,
    onConsoleMessage: ((String) -> Unit)? = null,
    onWebViewCreated: ((WebView) -> Unit)? = null,
    state: MutableState<WebViewState> = mutableStateOf(WebViewState())
) {
    // 获取android上下文，可做系统交互，注意只能在@Composable函数内调用
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    // 更新状态
    val updateState = { block: WebViewState.() -> WebViewState ->
        state.value = state.value.block()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 设置 WebView 背景为透明，避免闪白
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundResource(android.R.color.transparent)

                // 配置 WebView 设置
                configureSettings(this.settings, settings)

                // 设置 WebViewClient
                webViewClient = createWebViewClient(
                    onPageStarted = { url ->
                        Log.d("ComposableWebView", "onPageStarted: $url")
                        updateState { copy(isLoading = true, currentUrl = url, error = null) }
                        onPageStarted?.invoke(url)
                    },
                    onPageFinished = { url ->
                        Log.d("ComposableWebView", "onPageFinished: $url")
                        updateState {
                            copy(
                                isLoading = false,
                                canGoBack = canGoBack(),
                                canGoForward = canGoForward()
                            )
                        }
                        onPageFinished?.invoke(url)
                    },
                    onReceivedError = { errorCode, description, failingUrl ->
                        Log.e("ComposableWebView", "onReceivedError: $errorCode, $description, $failingUrl")
                        updateState { copy(error = "$errorCode: $description") }
                        onReceivedError?.invoke(description, failingUrl)
                    },
                    onReceivedHttpError = { statusCode, url ->
                        Log.e("ComposableWebView", "onReceivedHttpError: $statusCode, $url")
                        if (statusCode >= 400) {
                            updateState { copy(error = "HTTP $statusCode") }
                            onReceivedHttpError?.invoke(statusCode, url)
                        }
                    },
                    shouldOverrideUrlLoading = { url ->
                        Log.d("ComposableWebView", "shouldOverrideUrlLoading: $url")

                        // 默认白名单检查
                        if (isUrlInWhiteList(url)) {
                            return@createWebViewClient false
                        }

                        // 自定义拦截逻辑
                        shouldOverrideUrlLoading?.invoke(url) ?: false
                    }
                )

                // 设置 WebChromeClient
                webChromeClient = createWebChromeClient(
                    onProgressChanged = { progress ->
                        Log.d("ComposableWebView", "onProgressChanged: $progress")
                        updateState { copy(progress = progress) }
                        onProgressChanged?.invoke(progress)
                    },
                    onConsoleMessage = { message ->
                        Log.d("ComposableWebView", "console: $message")
                        onConsoleMessage?.invoke(message)
                    },
                    onCreateWindow = onCreateWindow
                )

                // 保存 WebView 引用
                webView = this
                onWebViewCreated?.invoke(this)
            }
        },
        modifier = modifier,
        update = { view ->
            // 当 URL 变化时加载新页面
            if (view.url != url) {
                view.loadUrl(url)
            }
        }
    )
}

/**
 * 配置 WebView 设置
 */
private fun configureSettings(webSettings: WebSettings, settings: WebViewSettings) {
    with(webSettings) {
        javaScriptEnabled = settings.javaScriptEnabled
        domStorageEnabled = settings.domStorageEnabled
        allowFileAccess = settings.allowFileAccess
        allowContentAccess = settings.allowContentAccess
        loadWithOverviewMode = settings.loadWithOverviewMode
        useWideViewPort = settings.useWideViewPort
        setSupportMultipleWindows(settings.supportMultipleWindows)
        javaScriptCanOpenWindowsAutomatically = settings.javaScriptCanOpenWindowsAutomatically
        mediaPlaybackRequiresUserGesture = settings.mediaPlaybackRequiresUserGesture
        allowUniversalAccessFromFileURLs = settings.allowUniversalAccessFromFileURLs
        mixedContentMode = settings.mixedContentMode
        settings.userAgent?.let { userAgentString = it }
        setSupportZoom(settings.zoomControlEnabled)
        displayZoomControls = settings.displayZoomControls
    }
}

/**
 * 创建 WebViewClient
 */
private fun createWebViewClient(
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onReceivedError: (Int, String, String) -> Unit,
    onReceivedHttpError: (Int, String) -> Unit,
    shouldOverrideUrlLoading: (String) -> Boolean,
): WebViewClient {
    return object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { onPageStarted(it) }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let { onPageFinished(it) }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true && error != null) {
                onReceivedError(error.errorCode, error.description.toString(), request.url.toString())
            }
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)
            if (request?.isForMainFrame == true && errorResponse != null) {
                onReceivedHttpError(errorResponse.statusCode, request.url.toString())
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            return shouldOverrideUrlLoading(url)
        }
    }
}

/**
 * 创建 WebChromeClient
 */
private fun createWebChromeClient(
    onProgressChanged: (Int) -> Unit,
    onConsoleMessage: (String) -> Unit,
    onCreateWindow: ((String) -> Boolean)? = null
): WebChromeClient {
    return object : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChanged(newProgress)
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            if (consoleMessage != null) {
                val level = getMessageLevel(consoleMessage)
                val msg = consoleMessage.message() ?: ""
                val sourceId = consoleMessage.sourceId() ?: ""
                val lineNumber = consoleMessage.lineNumber()
                val message = "[$level] $msg ($sourceId:$lineNumber)"
                onConsoleMessage(message)
            }
            return super.onConsoleMessage(consoleMessage)
        }

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
            Log.d("ComposableWebView", "onCreateWindow called")
            
            // 创建子 WebView
            val childView = view?.context?.let { WebView(it) }
            val transport = resultMsg?.obj as? WebView.WebViewTransport
            transport?.webView = childView

            if (childView != null) {
                // 配置子 WebView 客户端
                childView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        Log.d("ComposableWebView", "子窗口加载 URL: $url")

                        if (onCreateWindow != null) {
                            return onCreateWindow(url)
                        }

                        // 默认行为：在外部浏览器打开
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                            url.toUri())
                        view?.context?.startActivity(intent)
                        return true
                    }
                }
            }

            return true
        }

        private fun getMessageLevel(consoleMessage: ConsoleMessage): String {
            val level = consoleMessage.messageLevel()
            return when (level) {
                ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                ConsoleMessage.MessageLevel.LOG -> "LOG"
                ConsoleMessage.MessageLevel.WARNING -> "WARNING"
                ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                else -> "INFO"
            }
        }
    }
}