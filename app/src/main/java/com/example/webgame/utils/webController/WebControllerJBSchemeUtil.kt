package com.example.webgame.utils.webController

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.webgame.utils.JsBridgeEventUtil.Companion.handleNativeEvent
import com.example.webgame.utils.JsBridgeSchemeConstants
import com.example.webgame.utils.escapeJsonString
import com.google.gson.Gson
import java.net.URLDecoder

// 字符串安全转 JS 字符串
private fun String.toJsString() = "'${replace("'", "\\'")}'"

/**
 * webview相关控制工具类
 *
 * 使用的 evaluateJavascript + uri scheme 的方式实现的 JSBridge
 */
class WebControllerJBSchemeUtil private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WebControllerJBSchemeUtil"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: WebControllerJBSchemeUtil? = null

        fun getInstance(context: Context): WebControllerJBSchemeUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebControllerJBSchemeUtil(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val gson = Gson()

    /**
     * WebView 实例
     */
    private var webView: WebView? = null
    fun getWebview(): WebView? {
        return webView
    }

    /**
     * 是否webview已创建
     */
    var isWebViewCreated: Boolean = false
    /**
     * 是否URL已加载完成
     */
    var isUrlLoadStop: Boolean = false

    /**
     * JS Bridge 注入状态标记（防止 onPageStarted/onPageFinished 多次调用导致重复注入）
     */
    var isJsBridgeInjected: Boolean = false

    fun initWebFields(webView: WebView, isCreated: Boolean) {
        this.webView = webView
        this.isWebViewCreated = isCreated
        // 新 URL 加载时重置注入状态，允许重新注入
        isJsBridgeInjected = false
    }

    /**
     * 注入 User Scripts - 在页面加载前注入
     * 必须在 loadUrl 之前调用
     */
    fun injectUserScriptsBeforeLoad(runTag: String = "") {
        // 官方文档级注入（刷新/跳转自动执行）
        // 立即注入，此时页面还未开始加载
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Log.d(TAG, "$runTag: WebViewFeature.DOCUMENT_START_SCRIPT supported")
            WebViewCompat.addDocumentStartJavaScript(
                webView!!,
                JsBridgeSchemeConstants.Companion.GLOBAL_PROXY_JS,
                emptySet()
            )
            isJsBridgeInjected = true
        } else {
            Log.d(TAG, "$runTag: WebViewFeature.DOCUMENT_START_SCRIPT not supported")
            isJsBridgeInjected = false
        }
    }

    /**
     * 在页面加载过程中注入 JS Bridge（适用于所有版本）
     * 应该在 WebViewClient.onPageStarted 中调用
     */
    fun injectUserScriptsOnPageStarted(runTag: String = "") {
        // 低版本需要在页面开始加载时注入
        injectJsBridge(runTag)
    }

    /**
     * 注入自定义的 JavaScript 桥接代码 window.jsBridge.postMessage、window.ownBridge2.postMessage
     */
    fun injectJsBridge(runTag: String = "") {
        if (isJsBridgeInjected) {
            Log.d(TAG, "$runTag JS Bridge already injected, skipping duplicate injection")
            return
        }
        injectJsBridgeInternal(runTag, JsBridgeSchemeConstants.Companion.GLOBAL_PROXY_JS) { success ->
            if (success) {
                isJsBridgeInjected = true
                Log.d(TAG, "$runTag JS Bridge injected successfully")
            } else {
                Log.w(TAG, "$runTag JS Bridge injection failed, will retry on next opportunity")
            }
        }
    }

    /**
     * 内部方法：执行 JS 注入
     */
    private fun injectJsBridgeInternal(runTag: String = "", jsCode: String, callback: ((Boolean) -> Unit)? = null) {
        webView?.post {
            val currentWebView = webView
            if (currentWebView == null) {
                Log.w(TAG, "$runTag WebView is null, skipping JS injection")
                callback?.invoke(false)
                return@post
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    currentWebView.evaluateJavascript(jsCode) { result ->
                        Log.d(TAG, "$runTag JS injection result: $result")
                        callback?.invoke(true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "$runTag Error in evaluateJavascript", e)
                    callback?.invoke(false)
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    currentWebView.loadUrl("javascript:$jsCode")
                    Log.d(TAG, "$runTag JS injection executed (legacy mode)")
                    callback?.invoke(true)
                } catch (e: Exception) {
                    Log.e(TAG, "$runTag Error injecting JS bridge (legacy)", e)
                    callback?.invoke(false)
                }
            }
        }
    }

    /**
     * 处理 JS Bridge Scheme，返回 响应数据给 JS
     */
    fun handlerJsBridgeScheme(uri: Uri) {
        val bridgeName = uri.host ?: ""
        val seqId = uri.getQueryParameter("seq")
        val event = uri.getQueryParameter("event") ?: ""
        val data = uri.getQueryParameter("data")?.let {
            URLDecoder.decode(it, "UTF-8")
        } ?: ""

        // 业务分发（多 bridge 统一管理）
        val (success, result) = handleNativeEvent(context, bridgeName, event, data)
        Log.d(TAG, "<==========tox==========>handleNativeEvent event: $event, success: $success, result: $result")

        // 回调 JS
        val respJs = "window.__onNativeResponse(" +
                "${seqId?.toJsString()}," +
                "$success," +
                result.toJsString() +
                ")"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView?.evaluateJavascript(respJs) { res ->
                Log.d(TAG, "<==========tox==========>SDK >= 19 JS Bridge Scheme => JS injection result: $res")
            }
        } else {
            webView?.loadUrl("javascript:$respJs")
            Log.d(TAG, "<==========tox==========>SDK < 19 JS Bridge Scheme => JS injection result: $result")
        }
    }

    fun handlePush(extrasMsg: Map<String, Any>) {
        Log.d(TAG, "handlePush extrasMsg = $extrasMsg")

        if (extrasMsg.containsKey("url")) {
            var url = extrasMsg["url"] as? String ?: return

            val currentUrl = webView?.url
            if (!currentUrl.isNullOrEmpty()) {
                val uri = currentUrl.toUri()
                val domain = uri.host ?: ""
                val port = if (uri.port > 0 && uri.port.toString().startsWith("517")) ":${uri.port}" else ""
                url = "$domain$port$url"
            }

            Log.d(TAG, "Notification->跳转url: $url")

            if (url.isNotEmpty()) {
                webView?.post {
                    webView?.loadUrl(url)
                }
            }
        }
    }

    /**
     * 原生调用 JS，主动将数据传递给 JavaScript
     *
     * H5中可以通过 window.nativeCallJs 方法接收数据，示例如下：
     * ```javascript
     * window.nativeCallJs = function(message) {
     *   // 可在H5中做一些处理
     *   console.log('Received from Native:', message);
     *   return 'Data received successfully';
     * }
     * ```
     */
    fun evaluateJavascript(jsonData: Map<String, Any>?) {
        if (jsonData == null) return

        if (isUrlLoadStop) {
            val jsonString = gson.toJson(jsonData)
            val escapedJson = escapeJsonString(jsonString)

            val jsCode = """
                try {
                    const data = JSON.parse('$escapedJson');
                    if (typeof window.nativeCallJs === 'function') {
                        window.nativeCallJs(data);
                    } else {
                        console.log('Received from Native:', data);
                    }
                } catch (e) {
                    console.error('Error parsing JSON from Native:', e);
                }
            """.trimIndent()

            webView?.post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView?.evaluateJavascript("javascript:$jsCode", null)
                } else {
                    // 兼容在 Android 4.4 以下版本，使用 loadUrl 方法
                    webView?.loadUrl("javascript:$jsCode")
                }
            }
        }
    }
}