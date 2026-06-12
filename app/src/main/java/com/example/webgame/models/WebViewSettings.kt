package com.example.webgame.models

import android.os.Build
import android.webkit.WebSettings

/**
 * WebView 配置
 */
data class WebViewSettings(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = true,
    val allowContentAccess: Boolean = true,
    val loadWithOverviewMode: Boolean = true,
    val useWideViewPort: Boolean = true,
    val supportMultipleWindows: Boolean = true,
    val javaScriptCanOpenWindowsAutomatically: Boolean = true,
    val mediaPlaybackRequiresUserGesture: Boolean = false,
    val allowUniversalAccessFromFileURLs: Boolean = true,
    val mixedContentMode: Int = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW,
    val userAgent: String? = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.BRAND} ${Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Mobile Safari/537.36",
    val zoomControlEnabled: Boolean = false,
    val displayZoomControls: Boolean = false
)
