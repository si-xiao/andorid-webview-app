package com.example.webgame.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 处理各类js事件
 *
 * 注意：只适用于 WebControllerJBSchemeUtil 类
 */
class JsBridgeEventUtil {
    companion object {
        const val TAG = "JsBridgeEventUtil"
        private val gson = Gson()
        private val scope = CoroutineScope(Dispatchers.Main)

        // 业务处理：多 bridge + 多事件统一分发
        fun handleNativeEvent(
            context: Context,
            bridgeName: String,
            event: String,
            data: String
        ): Pair<Boolean, String> {
            Log.d(TAG, "JS called Android handler: $event, data: $data")
            return try {
                when (bridgeName) {
                    "jsBridge" -> when (event) {
                        "sendMessageToNative" -> true to handleSendMessageToNative(context, data)
                        "getVersion" -> true to getVersion(context)
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
                        "logout" -> true to logout(context)
                        "openWindow" -> true to openWindow(context, data)
                        "getClipboardText" -> true to getClipboardText(context)
                        "clearClipboardText" -> true to clearClipboardText(context)
                        "resetClipboardStatus" -> true to resetClipboardStatus()
                        "closeSplashScreen" -> true to closeSplashScreen()
                        else -> false to handleAdjustEvents(event, data)
                    }
                    "ownBridge2" -> when (event) {
                        "sendMessageToNative" -> true to handleSendMessageToNative(context, data)
                        "getVersion" -> true to getVersion(context)
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
                        "logout" -> true to logout(context)
                        "openWindow" -> true to openWindow(context, data)
                        "getClipboardText" -> true to getClipboardText(context)
                        "clearClipboardText" -> true to clearClipboardText(context)
                        "resetClipboardStatus" -> true to resetClipboardStatus()
                        "closeSplashScreen" -> true to closeSplashScreen()
                        else -> false to handleAdjustEvents(event, data)
                    }
                    else -> false to "Unknown bridge"
                }
            } catch (e: Exception) {
                false to (e.message ?: "throw Error")
            }
        }

        /**
         * 处理 sendMessageToNative 事件，实际项目并没有使用，仅调试用，暂留
         */
        private fun handleSendMessageToNative(context: Context, data: String?): String {
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
         * 处理 Adjust 事件，自研 apk 不需要处理，暂留
         */
        private fun handleAdjustEvents(eventName: String, data: String?): String {
            val adjustEvents = listOf(
                "login", "logout", "registerClick", "register",
                "rechargeClick", "firstrecharge", "recharge", "enterGame"
            )

            if (eventName in adjustEvents) {
                Log.d(TAG, "Adjust event: $eventName, data: $data")
                // 集成 Adjust SDK 进行埋点
                // AdjustUtils.trackEvent(eventName, data)
            }

            return gson.toJson(JsonObject().apply {
                addProperty("status", "000000")
            })
        }

        /**
         * 获取apk版本号
         */
        private fun getVersion(context: Context): String {
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
        private fun logout(context: Context): String {
            scope.launch {
                try {
                    // 由于在此之前登出操作在H5中已通过 setSession 桥接方法清除掉了flutter这边缓存的用户登录信息，
                    // 不过这里为了安全起见，需要加固判断下用户信息session是否已清空，若已清空，则不需要重复清空
                    if (StorageUtil.getSession().isNotEmpty()) {
                        StorageUtil.setSession("")
                    }
                    // H5触发登出操作时，flutter 清空剪贴板内容
                    clearClipboard(context)
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
        private fun openWindow(context: Context, data: String?): String {
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
        private fun getClipboardText(context: Context): String {
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
                    Log.d(TAG, "getClipboardText result: $text")
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
        private fun clearClipboardText(context: Context): String {
            return try {
                clearClipboard(context)
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
        private fun clearClipboard(context: Context) {
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
}