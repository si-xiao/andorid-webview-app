package com.example.webgame.utils

import android.annotation.SuppressLint
import android.content.Context
import com.engagelab.privates.common.global.MTGlobal
import com.engagelab.privates.core.api.MTCorePrivatesApi
import com.engagelab.privates.push.api.MTPushPrivatesApi
import com.engagelab.privates.push.api.NotificationMessage
import com.example.webgame.BuildConfig
import com.example.webgame.jpushAppKey

/**
 * 极光推送工具类
 */
class JPushUtil private constructor(private val context: Context) {

    companion object {
        private const val TAG = "JPushUtil"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: JPushUtil? = null

        fun getInstance(context: Context): JPushUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JPushUtil(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Engagelab注册ID，唯一标识，可用于推送
     */
    var registrationID: String = ""
        private set
    fun setRegistrationID(registrationID: String) {
        this.registrationID = registrationID
    }

    /**
     * 当前设备的userId，Engagelab唯一标识，可用于推送
     */
    var userId: Long? = null
        private set
    fun setUserId(userId: Long) {
        this.userId = userId
    }

    var needPushMsg: Map<String, Any>? = null

    /**
     * 发送本地通知
     */
    fun sendLocalNotification(title: String? = null, content: String? =  null) {
        // 构建一个基础的通知，其中messageId和content是必须，否则通知无法展示
        val notificationMessage = NotificationMessage()
            .setMessageId("12345")
            .setNotificationId(12345)
            .setTitle(title ?: "重磅通知")
            .setContent(content ?: "您有一份饿了么外卖即将送达，请注意查收！registrationID: $registrationID, userId: $userId")
        // 展示通知
        MTPushPrivatesApi.showNotification(context, notificationMessage)
    }

    init {
        // 初始化 JPush SDK
        // 必须在application.onCreate中配置，不要判断进程，sdk内部有判断
        MTCorePrivatesApi.configDebugMode(context,  true)
        if (BuildConfig.DEBUG) {
            // 设置国家码 -- 正式环境请不要设置
            // 用于在debug环境下测试 FCM通道 的推送效果
            MTGlobal.setCountryCode("US")
        }
        // 配置推送的应用key，存放在WebUrl.kt中，自动化打包脚本（根目录下codemagic.yaml）会修改其值
        MTCorePrivatesApi.configAppKey(context, jpushAppKey)
        // 语音播报功能设置
        MTPushPrivatesApi.setEnablePushTextToSpeech(context, true)
        // 初始化推送服务api，很多配置项都必须在此 init 之前设置，不然无效
        MTPushPrivatesApi.init(context)
    }
}