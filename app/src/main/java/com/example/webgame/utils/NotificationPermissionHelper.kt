package com.example.webgame.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.engagelab.privates.push.api.MTPushPrivatesApi

/**
 * 通知权限工具类
 */
class NotificationPermissionHelper private constructor(private val context: ComponentActivity) {
    companion object {
        const val TAG = "NotificationPermissionHelper"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: NotificationPermissionHelper? = null

        fun getInstance(context: ComponentActivity): NotificationPermissionHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPermissionHelper(context).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * 暴漏检查通知权限以及申请通知权限整合后的入口方法
     */
    fun checkPermission() {
        if (!hasNotificationPermission(context)) {
            // 申请通知权限
            LogUtil.d(TAG, "申请通知权限")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 创建通知权限 launcher 用于申请权限
     */
    private val notificationLauncher = createPermissionLauncher(context) { granted ->
            if (granted) {
                // 用户允许
                LogUtil.d(TAG, "用户允许通知权限")
            } else {
                // 用户拒绝
                LogUtil.d(TAG, "用户拒绝通知权限，须引导用户去设置中开启")
                // 拒绝后弹窗引导去设置
                showPermissionDeniedDialog()
            }
        }

    /**
     * 拒绝后弹窗引导去设置
     * ```javascript
     * title = "需要通知权限"
     * message = "通知权限已被关闭，无法接收消息提醒，请前往设置中手动开启。"
     * positiveButton = "去设置"
     * negativeButton = "取消"
     * ```
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(context)
            .setTitle("Permissão de notificação necessária")
            .setMessage("A permissão de notificação está desativada. Não será possível receber alertas de mensagens. Acesse as configurações para ativá-la manualmente.")
            .setPositiveButton("Acessar configurações") { _, _ ->
                // 没有开启通知开关，使用 推送api 提示用户前往系统通知开关设置页面手动打开通知开关
                MTPushPrivatesApi.goToAppNotificationSettings(context)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    /**
     * 是否有通知权限（Android13以下默认true）
     */
    private fun hasNotificationPermission(context: ComponentActivity): Boolean {
        return if (isNeedRequestPermission()) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            hasPermission
        } else {
            true
        }
    }

    /**
     * 创建权限请求 Launcher，可用于申请通知权限和权限获取结果
     */
    private fun createPermissionLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
        }
    }

    /**
     * 判断是不是 Android13+，需要申请权限
     */
    private fun isNeedRequestPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}