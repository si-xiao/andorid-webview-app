package com.example.webgame.models

/**
 * WebView 状态数据类
 */
data class WebViewState(
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
