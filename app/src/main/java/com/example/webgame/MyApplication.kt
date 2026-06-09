package com.example.webgame

import android.app.Application
import com.example.webgame.utils.StorageUtil

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 全局初始化 sp
        StorageUtil.init(this)
    }
}
