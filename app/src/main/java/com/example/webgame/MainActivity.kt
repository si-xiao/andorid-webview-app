package com.example.webgame

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.webgame.ui.theme.WebgameTheme
import com.example.webgame.ui.theme.appColor
import com.example.webgame.widgets.ComposableWebView
import com.example.webgame.widgets.WebViewSettings
import com.example.webgame.widgets.WebViewState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用 Edge-to-Edge 布局
        enableEdgeToEdge()
        
        // 设置状态栏和导航栏为透明（先设置为透明，然后在 Composable 中控制颜色）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Android 11+ 使用新的 API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // 设置沉浸式模式 - 滑动时显示系统栏
            window.insetsController?.let { controller ->
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 及以下使用传统方式
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        setContent {
            WebgameTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    var webUrl by remember { mutableStateOf("") }
    val webViewState = remember { mutableStateOf(WebViewState()) }
    var showProgressBar by remember { mutableStateOf(true) }

    // 初始化：获取 Web URL
    LaunchedEffect(Unit) {
        // 设置前缀参数
//        prefixParams = "info=abc123&channel=google"

        // 获取可用的 URL
        val url = getRandomWebUrl()
        Log.d("MainActivity", "加载 URL: $url")
        webUrl = url
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appColor)
                .padding(paddingValues)
        ) {
            if (webUrl.isNotEmpty()) {
                ComposableWebView(
                    url = webUrl,
                    modifier = Modifier.fillMaxSize(),
                    state = webViewState,
                    onProgressChanged = { progress ->
                        Log.d("MainActivity", "进度: $progress%")
                        showProgressBar = progress < 100
                    },
                    onPageStarted = { url ->
                        Log.d("MainActivity", "页面开始加载: $url")
                    },
                    onPageFinished = { url ->
                        Log.d("MainActivity", "页面加载完成: $url")
                    },
                    onReceivedError = { description, failingUrl ->
                        Log.e("MainActivity", "加载错误: $description, URL: $failingUrl")
                    },
                    onReceivedHttpError = { statusCode, url ->
                        Log.e("MainActivity", "HTTP 错误: $statusCode, URL: $url")
                    },
                    shouldOverrideUrlLoading = { url ->
                        Log.d("MainActivity", "拦截 URL: $url")
                        false // 允许加载
                    },
                    onConsoleMessage = { message ->
                        Log.d("MainActivity", "H5 Console: $message")
                    }
                )

                // 进度条
                if (showProgressBar) {
                    LinearProgressIndicator(
                        progress = { webViewState.value.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.TopCenter)
                    )
                }

                // 错误提示
                webViewState.value.error?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "加载失败",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                scope.launch {
                                    webUrl = getRandomWebUrl()
                                }
                            }) {
                                Text("重新加载")
                            }
                        }
                    }
                }
            } else {
                // 加载中
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WebgameTheme {
        MainScreen()
    }
}