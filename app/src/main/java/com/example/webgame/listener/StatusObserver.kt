package com.example.webgame.listener
import kotlin.concurrent.Volatile

class StatusObserver {
    private var onStatusListener: OnStatusListener? = null

    fun addListener(listener: OnStatusListener?) {
        onStatusListener = listener
    }

    fun removeListener() {
        onStatusListener = null
    }

    val listener: OnStatusListener?
        get() = onStatusListener

    companion object {
        @Volatile
        var instance: StatusObserver? = null
            get() {
                if (field == null) {
                    synchronized(StatusObserver::class.java) {
                        field = StatusObserver()
                    }
                }
                return field
            }
            private set
    }
}
