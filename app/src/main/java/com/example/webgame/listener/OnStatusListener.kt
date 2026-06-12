package com.example.webgame.listener

interface OnStatusListener {
    fun onConnectStatus(status: Boolean)

    fun onNotificationStatus(status: Boolean)
}
