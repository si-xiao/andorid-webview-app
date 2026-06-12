package com.example.webgame.component

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.engagelab.privates.common.component.MTCommonReceiver
import com.engagelab.privates.core.api.MTCorePrivatesApi
import com.engagelab.privates.push.api.CustomMessage
import com.engagelab.privates.push.api.MTPushPrivatesApi
import com.engagelab.privates.push.api.NotificationMessage
import com.engagelab.privates.push.api.PlatformTokenMessage
import com.example.webgame.listener.StatusObserver
import com.example.webgame.utils.Constants
import com.example.webgame.utils.JPushUtil

/**
 * 开发者继承MTCommonReceiver，可以获得sdk的方法回调
 * <p>
 * 所有回调均在主线程
 */
class UserReceiver : MTCommonReceiver() {

    companion object {
        private const val TAG = "UserReceiver"
    }

    /**
     * 应用通知开关状态回调
     *
     * @param context 不为空
     * @param enable  通知开关是否开，true为打开，false为关闭
     */
    override fun onNotificationStatus(context: Context, enable: Boolean) {
        Log.d(TAG, "onNotificationStatus:$enable")
        if (!enable) {
            Log.d(TAG, "onNotificationStatus 应用没有开启通知权限")
            // 没有开启通知开关，提示用户前往系统通知开关设置页面手动打开通知开关
            // MTPushPrivatesApi.goToAppNotificationSettings(context)
        }
        Constants.isNotificationEnable = enable
        StatusObserver.instance?.listener?.onNotificationStatus(enable)
    }

    /**
     * 长连接状态回调
     *
     * @param context 不为空
     * @param enable  是否连接
     */
    override fun onConnectStatus(context: Context, enable: Boolean) {
        Log.d(TAG, "onConnectState:$enable")
        Constants.isConnectEnable = enable
        StatusObserver.instance?.listener?.onConnectStatus(enable)
        // 长连接成功可获取registrationId
        if (enable) {
            val registrationId = MTCorePrivatesApi.getRegistrationId(context)
            val userId = MTCorePrivatesApi.getUserId(context)
            Log.d(TAG, "registrationId:$registrationId, userId:$userId")
            // 获取并保存设备注册ID
            JPushUtil.getInstance(context).setRegistrationID(registrationId)
            JPushUtil.getInstance(context).setUserId(userId)
            // 连接成功后发送一条本地通知，仅供测试，调试完注释
            // JPushUtil.getInstance(context).sendLocalNotification()
        }
    }

    /**
     * 通知消息到达回调
     *
     * @param context             不为空
     * @param notificationMessage 通知消息
     */
    override fun onNotificationArrived(context: Context, notificationMessage: NotificationMessage) {
        Log.d(TAG, "onNotificationArrived:${notificationMessage.toString()}")
    }

    /**
     * 通知消息在前台不显示
     *
     * @param context             不为空
     * @param notificationMessage 通知消息
     */
    override fun onNotificationUnShow(context: Context, notificationMessage: NotificationMessage) {
        Log.d(TAG, "onNotificationUnShow:${notificationMessage.toString()}")
    }

    /**
     * 通知消息点击回调
     *
     * @param context             不为空
     * @param notificationMessage 通知消息
     */
    override fun onNotificationClicked(context: Context, notificationMessage: NotificationMessage) {
        Log.d(TAG, "onNotificationClicked:${notificationMessage.toString()}")
        // 手动处理通知点击的后续操作
        /*try {
            val extras = notificationMessage.extras
            val url = extras?.get("url") as? String

            // 判断是否有特定的跳转URL
            if (!url.isNullOrEmpty()) {
                // 如果有URL参数，跳转到MainActivity并传递URL
                val intent = android.content.Intent(context, com.example.webgame.activity.MessageActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("notification_url", url)
                    putExtra("from_notification", true)
                }
                context.startActivity(intent)
                Log.d(TAG, "跳转到MainActivity，URL: $url")
            } else {
                // 如果没有特定URL，直接打开应用主界面
                val intent = android.content.Intent(context, com.example.webgame.activity.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("from_notification", true)
                }
                context.startActivity(intent)
                Log.d(TAG, "跳转到MainActivity（无URL参数）")
            }
        } catch (e: Exception) {
            Log.e(TAG, "通知点击跳转异常: ${e.message}", e)
            // 异常情况下，确保至少能打开应用
            try {
                val intent = android.content.Intent(context, com.example.webgame.activity.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.e(TAG, "兜底跳转也失败: ${ex.message}", ex)
            }
        }*/
    }

    /**
     * 通知消息删除回调
     *
     * @param context             不为空
     * @param notificationMessage 通知消息
     */
    override fun onNotificationDeleted(context: Context, notificationMessage: NotificationMessage) {
        Log.d(TAG, "onNotificationDeleted:${notificationMessage.toString()}")
    }

    /**
     * 自定义消息回调
     *
     * @param context       不为空
     * @param customMessage 自定义消息
     */
    override fun onCustomMessage(context: Context, customMessage: CustomMessage) {
        Log.d(TAG, "onCustomMessage:${customMessage.toString()}")
    }

    /**
     * 厂商token消息回调
     *
     * @param context              不为空
     * @param platformTokenMessage 厂商token消息
     */
    override fun onPlatformToken(context: Context, platformTokenMessage: PlatformTokenMessage) {
        Log.d(TAG, "onPlatformToken:${platformTokenMessage.toString()}")
    }
}