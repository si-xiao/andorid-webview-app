package com.example.webgame.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    suspend fun setLoginInfo(data: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putString(KEY_LOGIN_INFO, data).apply() }
        }
    }

    /**
     * 读取登录信息
     */
    suspend fun getLoginInfo(): String {
        return withContext(Dispatchers.IO) {
            getPrefs().getString(KEY_LOGIN_INFO, "") ?: ""
        }
    }

    // ==================== 会话信息 ====================

    /**
     * 保存会话信息
     */
    suspend fun setSession(data: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putString(KEY_SESSION, data).apply() }
        }
    }

    /**
     * 读取会话信息
     */
    suspend fun getSession(): String {
        return withContext(Dispatchers.IO) {
            getPrefs().getString(KEY_SESSION, "") ?: ""
        }
    }

    // ==================== 渠道等其它信息 ====================

    /**
     * 保存渠道等其它信息
     */
    suspend fun setChannelAndOtherInfo(data: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putString(KEY_CHANNEL_AND_OTHER_INFO, data).apply() }
        }
    }

    /**
     * 读取渠道等其它信息
     */
    suspend fun getChannelAndOtherInfo(): String {
        return withContext(Dispatchers.IO) {
            getPrefs().getString(KEY_CHANNEL_AND_OTHER_INFO, "") ?: ""
        }
    }

    // ==================== 用户信息 ====================

    /**
     * 保存用户信息
     */
    suspend fun setUserInfo(data: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putString(KEY_USER_INFO, data).apply() }
        }
    }

    /**
     * 读取用户信息
     */
    suspend fun getUserInfo(): String {
        return withContext(Dispatchers.IO) {
            getPrefs().getString(KEY_USER_INFO, "") ?: ""
        }
    }

    /**
     * 清空用户信息
     */
    suspend fun clearUserInfo() {
        withContext(Dispatchers.IO) {
            getPrefs().edit {
                remove(KEY_USER_INFO).remove(KEY_SESSION).apply()
            }
        }
    }

    // ==================== Info 参数 ====================

    /**
     * 读取 info=参数
     */
    suspend fun getInfoParam(): String {
        return withContext(Dispatchers.IO) {
            getPrefs().getString(KEY_INFO_PARAM, "") ?: ""
        }
    }

    /**
     * 保存 info=参数
     */
    suspend fun setInfoParam(param: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putString(KEY_INFO_PARAM, param).apply() }
        }
    }

    // ==================== 登录状态 ====================

    /**
     * 读取登录状态 1=未登录 2=已登录
     */
    suspend fun getLoginStatus(): String {
        return withContext(Dispatchers.IO) {
            getPrefs().getString(KEY_LOGIN_STATUS, "") ?: ""
        }
    }

    /**
     * 保存登录状态
     */
    suspend fun setLoginStatus(data: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putString(KEY_LOGIN_STATUS, data).apply() }
        }
    }

    // ==================== 剪贴板读取状态 ====================

    /**
     * 获取剪贴板读取状态
     */
    suspend fun getClipboardRead(): Boolean {
        return withContext(Dispatchers.IO) {
            getPrefs().getBoolean(KEY_CLIPBOARD_READ, false)
        }
    }

    /**
     * 设置剪贴板读取状态
     */
    suspend fun setClipboardRead(read: Boolean) {
        withContext(Dispatchers.IO) {
            getPrefs().edit { putBoolean(KEY_CLIPBOARD_READ, read).apply() }
        }
    }

    /**
     * 清除所有数据
     */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            getPrefs().edit { clear().apply() }
        }
    }
}
