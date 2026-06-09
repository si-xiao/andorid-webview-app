package com.example.webgame.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 存储工具类，用于存储和读取登录信息、会话信息、用户信息
 */
object StorageUtil {

    private const val PREFS_NAME = "app_prefs"

    // Keys
    private const val KEY_LOGIN_INFO = "loginInfo"
    private const val KEY_SESSION = "session"
    private const val KEY_CHANNEL_AND_OTHER_INFO = "channelAndOtherInfo"
    private const val KEY_USER_INFO = "userInfo"
    private const val KEY_INFO_PARAM = "infoParam"
    private const val KEY_LOGIN_STATUS = "loginStatus"
    private const val KEY_CLIPBOARD_READ = "clipboardRead"

    private var sharedPreferences: SharedPreferences? = null

    /**
     * 初始化 SharedPreferences
     */
    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
        }
    }

    /**
     * 确保已初始化
     */
    private fun getPrefs(): SharedPreferences {
        return sharedPreferences ?: throw IllegalStateException(
            "StorageUtil not initialized. Call init() first."
        )
    }

    // ==================== 登录信息 ====================

    /**
     * 保存登录信息
     */
    fun setLoginInfo(value: String) {
        getPrefs().edit { putString(KEY_LOGIN_INFO, value) }
    }

    /**
     * 读取登录信息
     */
    fun getLoginInfo(): String {
        return getPrefs().getString(KEY_LOGIN_INFO, "") ?: ""
    }

    // ==================== 会话信息 ====================

    /**
     * 保存会话信息
     */
    fun setSession(value: String) {
        getPrefs().edit { putString(KEY_SESSION, value) }
    }

    /**
     * 读取会话信息
     */
    fun getSession(): String {
        return getPrefs().getString(KEY_SESSION, "") ?: ""
    }

    // ==================== 渠道等其它信息 ====================

    /**
     * 保存渠道等其它信息
     */
    fun setChannelAndOtherInfo(value: String) {
        getPrefs().edit { putString(KEY_CHANNEL_AND_OTHER_INFO, value) }
    }

    /**
     * 读取渠道等其它信息
     */
    fun getChannelAndOtherInfo(): String {
        return getPrefs().getString(KEY_CHANNEL_AND_OTHER_INFO, "") ?: ""
    }

    // ==================== 用户信息 ====================

    /**
     * 保存用户信息
     */
    fun setUserInfo(value: String) {
        getPrefs().edit { putString(KEY_USER_INFO, value) }
    }

    /**
     * 读取用户信息
     */
    fun getUserInfo(): String {
        return getPrefs().getString(KEY_USER_INFO, "") ?: ""
    }

    /**
     * 清空用户信息
     */
    fun clearUserInfo() {
        getPrefs().edit { remove(KEY_USER_INFO) }
    }

    // ==================== Info 参数 ====================

    /**
     * 读取 info=参数
     */
    fun getInfoParam(): String {
        return getPrefs().getString(KEY_INFO_PARAM, "") ?: ""
    }

    /**
     * 保存 info=参数
     */
    fun setInfoParam(value: String) {
        getPrefs().edit { putString(KEY_INFO_PARAM, value) }
    }

    // ==================== 登录状态 ====================

    /**
     * 读取登录状态 1=未登录 2=已登录
     */
    fun getLoginStatus(): String {
        return getPrefs().getString(KEY_LOGIN_STATUS, "") ?: ""
    }

    /**
     * 保存登录状态
     */
    fun setLoginStatus(value: String) {
        getPrefs().edit { putString(KEY_LOGIN_STATUS, value) }
    }

    // ==================== 剪贴板读取状态 ====================

    /**
     * 获取剪贴板读取状态
     */
    fun getClipboardRead(): Boolean {
        return getPrefs().getBoolean(KEY_CLIPBOARD_READ, false)
    }

    /**
     * 设置剪贴板读取状态
     */
    fun setClipboardRead(value: Boolean) {
        getPrefs().edit { putBoolean(KEY_CLIPBOARD_READ, value) }
    }

    /**
     * 清除所有数据
     */
    fun clearAll() {
        getPrefs().edit { clear() }
    }
}
