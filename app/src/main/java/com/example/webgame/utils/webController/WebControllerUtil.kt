package com.example.webgame.utils.webController

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.example.webgame.utils.JPushUtil
import com.example.webgame.utils.StorageUtil
import com.example.webgame.utils.escapeJsonString
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * webview相关控制工具类
 *
 * 使用的 addJavascriptInterface 的传统方式实现的 JSBridge
 */
class WebControllerUtil private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WebControllerUtil"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: WebControllerUtil? = null

        fun getInstance(context: Context): WebControllerUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebControllerUtil(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main)

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
        setupJavaScriptInterface()
    }

    /**
     * 设置 JavaScript 桥接接口，统一注入 NativeBridge，后续再通过 evaluateJavascript 的形式注入jsBridge、ownBridge2
     * 这样在H5中可以调用：
     * ```javascript
     * // window.jsBridge.postMessage这是必须要注册的接口名，为了兼容apk上架包
     * window.jsBridge.postMessage("eventName", "{'key': 'value', 'key2': 'value2'}");
     *
     * // window.ownBridge2.postMessage是为了区分自研包和上架包，只有自研包有注册此接口名
     * window.ownBridge2.postMessage("eventName", "{'key': 'value', 'key2': 'value2'}");
     * ```
     */
    private fun setupJavaScriptInterface() {
        webView?.addJavascriptInterface(JsBridgeInterface(), "jsBridge")
        webView?.addJavascriptInterface(JsBridgeInterface(), "ownBridge2")
    }

    /**
     * JavaScript 桥接接口类
     */
    inner class JsBridgeInterface {

        /**
         * 暴漏给 JS 调用的桥接方法，
         * 通过@JavascriptInterface注入 callNative 方法，这样在H5中可以调用 window.NativeBridge.callNative
         *
         * 参数：bridgeName 可选值 jsBridge、ownBridge2
         */
        @JavascriptInterface
        fun postMessage(eventName: String, data: String?): String {
            Log.d(TAG, "JS called Android handler: eventName: $eventName, data: $data")
            // 返回给 JS
            return try {
                val (success, result) = handleNativeEvent(eventName, data)
//                gson.toJson(JsonObject().apply {
//                    addProperty("success", success)
//                    addProperty("data", result)
//                })
                result
            } catch (e: Exception) {
//                gson.toJson(JsonObject().apply {
//                    addProperty("success", false)
//                    addProperty("data", e.message)
//                })
                e.message ?: "throw Error: ${e.javaClass.simpleName}"
            }
        }

        /**
         * 处理原生事件
         */
        private fun handleNativeEvent(eventName: String, data: String?): Pair<Boolean, String> {
            return when (eventName) {
                "sendMessageToNative" -> true to handleSendMessageToNative(data)
                "getVersion" -> true to getVersion()
                "toggleLog" -> true to toggleLog()
                "setLoginInfo" -> true to setLoginInfo(data)
                "getLoginInfo" -> true to getLoginInfo()
                "setSession" -> true to setSession(data)
                "getSession" -> true to getSession()
                "setChannelAndOtherInfo" -> true to setChannelAndOtherInfo(data)
                "getChannelAndOtherInfo" -> true to getChannelAndOtherInfo()
                "setUserInfo" -> true to setUserInfo(data)
                "getUserInfo" -> true to getUserInfo()
                "clearUserInfo" -> true to clearUserInfo()
                "logout" -> true to logout()
                "openWindow" -> true to openWindow(data)
                "getClipboardText" -> true to getClipboardText()
                "clearClipboardText" -> true to clearClipboardText()
                "resetClipboardStatus" -> true to resetClipboardStatus()
                "closeSplashScreen" -> true to closeSplashScreen()
                else -> false to "Invalid eventName: $eventName"
            }
        }

        /**
         * 处理 sendMessageToNative 事件，实际项目并没有使用，仅调试用，暂留
         */
        private fun handleSendMessageToNative(data: String?): String {
            try {
                val json = gson.fromJson(data, JsonObject::class.java)
                val type = json.get("type")?.asString

                if (type == "NativeInfo") {
                    val result = JsonObject()
                    result.addProperty("status", "000000")

                    val received = JsonObject()
                    received.addProperty("regId", JPushUtil.getInstance(context).registrationID)
                    received.addProperty("platform", "android")
                    result.add("received", received)

                    return gson.toJson(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sendMessageToNative", e)
            }

            return gson.toJson(JsonObject().apply {
                addProperty("status", "000000")
            })
        }

        /**
         * 获取apk版本号
         */
        private fun getVersion(): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "1.0.0"
            } catch (e: Exception) {
                Log.e(TAG, "Error getting version", e)
                "1.0.0"
            }
        }

        /**
         * 切换日志开关
         */
        private fun toggleLog(): String {
//            LogUtil.toggleLog()
//            Log.d(TAG, "Log toggled - isDebug: ${LogUtil.isDebug}")
            return ""
        }

        /**
         * 保存登录信息
         */
        private fun setLoginInfo(data: String?): String {
            scope.launch {
                try {
                    StorageUtil.setLoginInfo(data ?: "")
                    Log.d(TAG, "setLoginInfo saved: $data")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving loginInfo", e)
                }
            }
            return ""
        }

        /**
         * 获取登录信息
         */
        private fun getLoginInfo(): String {
            return runBlockingTask {
                try {
                    val loginInfo = StorageUtil.getLoginInfo()
                    Log.d(TAG, "getLoginInfo: $loginInfo")
                    loginInfo.ifEmpty { "" }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting loginInfo", e)
                    ""
                }
            }
        }

        /**
         * 保存会话信息，含有H5的用户信息和登录token
         */
        private fun setSession(data: String?): String {
            scope.launch {
                try {
                    StorageUtil.setSession(data ?: "")
                    Log.d(TAG, "setSession saved: $data")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving session", e)
                }
            }
            return ""
        }

        /**
         * 获取会话信息，含有H5的用户信息和登录token
         */
        private fun getSession(): String {
            return runBlockingTask {
                try {
                    val session = StorageUtil.getSession()
                    Log.d(TAG, "getSession: $session")
                    session.ifEmpty { "" }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting session", e)
                    ""
                }
            }
        }

        /**
         * 存储渠道和别的网站信息
         */
        private fun setChannelAndOtherInfo(data: String?): String {
            scope.launch {
                try {
                    StorageUtil.setChannelAndOtherInfo(data ?: "")
                    Log.d(TAG, "setChannelAndOtherInfo saved: $data")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving channelAndOtherInfo", e)
                }
            }
            return ""
        }

        /**
         * 获取渠道和别的网站信息
         */
        private fun getChannelAndOtherInfo(): String {
            return runBlockingTask {
                try {
                    val info = StorageUtil.getChannelAndOtherInfo()
                    Log.d(TAG, "getChannelAndOtherInfo: $info")
                    info.ifEmpty { "" }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting channelAndOtherInfo", e)
                    ""
                }
            }
        }

        /**
         * 保存用户信息，只有用户名和密码,H5点击登录时调用
         */
        private fun setUserInfo(data: String?): String {
            scope.launch {
                try {
                    StorageUtil.setUserInfo(data ?: "")
                    Log.d(TAG, "setUserInfo saved: $data")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving userInfo", e)
                }
            }
            return ""
        }

        /**
         * 获取用户信息，只有用户名和密码
         */
        private fun getUserInfo(): String {
            return runBlockingTask {
                try {
                    val userInfo = StorageUtil.getUserInfo()
                    Log.d(TAG, "getUserInfo: $userInfo")
                    userInfo.ifEmpty { "" }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting userInfo", e)
                    ""
                }
            }
        }

        /**
         * 清空用户信息
         */
        private fun clearUserInfo(): String {
            scope.launch {
                try {
                    StorageUtil.clearUserInfo()
                    Log.d(TAG, "clearUserInfo executed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing userInfo", e)
                }
            }
            return ""
        }

        /**
         * 自定义桥接方法 - 登出操作（需要清空用户登录信息、剪贴板内容、初始化剪贴板读取状态为false）
         *
         * H5会回调此方法，通知 flutter 做登出相关事项
         */
        private fun logout(): String {
            scope.launch {
                try {
                    // 由于在此之前登出操作在H5中已通过 setSession 桥接方法清除掉了flutter这边缓存的用户登录信息，
                    // 不过这里为了安全起见，需要加固判断下用户信息session是否已清空，若已清空，则不需要重复清空
                    if (StorageUtil.getSession().isNotEmpty()) {
                        StorageUtil.setSession("")
                    }
                    // H5触发登出操作时，flutter 清空剪贴板内容
                    clearClipboard()
                    // H5触发登出操作时，flutter 初始化剪贴板读取状态为false，
                    // 避免下次重新打开app时，由于此状态被设置为了true，从而导致flutter不再提供剪切板信息给H5使用
                    StorageUtil.setClipboardRead(false)
                    Log.d(TAG, "logout executed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error logout", e)
                }
            }
            return "true"
        }

        /**
         * 自定义桥接方法 - 打开新窗口
         */
        private fun openWindow(data: String?): String {
            scope.launch {
                try {
                    if (data.isNullOrEmpty()) return@launch

                    val jsonArray = gson.fromJson(data, Array<JsonObject>::class.java)
                    if (jsonArray.isNotEmpty()) {
                        val url = jsonArray[0].get("url")?.asString
                        if (!url.isNullOrEmpty()) {
                            val uri = url.toUri()
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(context, intent, null)
                            Log.d(TAG, "openWindow opened: $url")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening window", e)
                }
            }
            return ""
        }

        /**
         * 自定义桥接方法 - 获取剪贴板内容
         */
        private fun getClipboardText(): String {
            Log.d(TAG, "getClipboardText called")
            return runBlockingTask {
                try {
                    var text = ""
                    if (!StorageUtil.getClipboardRead()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            text = clipData.getItemAt(0).text.toString()
                            StorageUtil.setClipboardRead(true)
                            if (text.isNotEmpty()) {
                                StorageUtil.setInfoParam(text)
                            }
                        }
                    }
                    Log.d(TAG, "getClipboardText result: $text, getClipboardRead=${StorageUtil.getClipboardRead()}")
                    text
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting clipboard text", e)
                    ""
                }
            }
        }

        /**
         * 自定义桥接方法 - 清空剪贴板内容
         */
        private fun clearClipboardText(): String {
            return try {
                clearClipboard()
                Log.d(TAG, "clearClipboardText executed")
                "true"
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing clipboard", e)
                "false"
            }
        }

        /**
         * 自定义桥接方法 - 重置剪贴板读取状态为false
         */
        private fun resetClipboardStatus(): String {
            return runBlockingTask {
                try {
                    StorageUtil.setClipboardRead(false)
                    Log.d(TAG, "resetClipboardStatus executed")
                    "true"
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting clipboard status", e)
                    "false"
                }
            }
        }

        /**
         * 自定义桥接方法 - 关闭启动屏，目前项目中并没有使用，所以这里暂时不做处理
         */
        private fun closeSplashScreen(): String {
            Log.d(TAG, "closeSplashScreen called")
            // TODO: 移除启动屏
            return ""
        }

        /**
         * 清空剪贴板内容，最终执行的方法
         */
        private fun clearClipboard() {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }

        /**
         * 创建一个可用于运行阻塞任务的方法
         */
        private fun runBlockingTask(block: suspend () -> String): String {
            return try {
                kotlinx.coroutines.runBlocking { block() }
            } catch (e: Exception) {
                Log.e(TAG, "Error in blocking task", e)
                ""
            }
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