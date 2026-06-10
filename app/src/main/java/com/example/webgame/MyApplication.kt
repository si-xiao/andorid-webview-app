package com.example.webgame

import android.app.Application
import com.example.webgame.utils.JPushUtil
import com.example.webgame.utils.StorageUtil

/**
 * 全局 Application
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 全局初始化 sp
        StorageUtil.init(this)
        // 初始化 jpush
        JPushUtil.getInstance(this)
    }
}
