package com.example.webgame.utils

import android.annotation.SuppressLint
import android.content.Context

/**
 * 极光推送工具类
 */
class JPushUtil private constructor(private val context: Context) {

    companion object {
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

    var registrationID: String = ""
        private set

    var needPushMsg: Map<String, Any>? = null

    init {
        // TODO: 初始化 JPush SDK
        // JPushInterface.init(context)
        // registrationID = JPushInterface.getRegistrationID(context)
    }
}