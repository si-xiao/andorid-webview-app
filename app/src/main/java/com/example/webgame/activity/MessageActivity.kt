package com.example.webgame.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.webgame.ui.theme.Pink80
import com.example.webgame.ui.theme.WebgameTheme
import com.example.webgame.ui.theme.transparentColor
import com.example.webgame.utils.LogUtil
import org.json.JSONObject

/**
 * 用于点击通知后activity跳转
 *
 *
 * 确保没有调用[MTPushPrivatesApi.configOldPushVersion]，否则通知点击跳转不会跳转到此页面
 *
 *
 * 不需要调用[MTPushPrivatesApi.reportNotificationOpened]，sdk内部已做处理
 */
class MessageActivity : ComponentActivity() {
    private var message: String? by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.d(TAG, "onCreate")
        // 启用 Edge-to-Edge 布局
        enableEdgeToEdge()
        onIntent(intent)
        setContent {
            WebgameTheme {
                MessageScreen(message)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        onIntent(intent)
        LogUtil.d(TAG, "onNewIntent")
    }

    private fun onIntent(intent: Intent?) {
        try {
            Toast.makeText(applicationContext, TAG, Toast.LENGTH_SHORT).show()
            if (intent == null) {
                return
            }
            val notificationMessage = intent.getStringExtra("message_json")
            if (notificationMessage == null) {
                return
            }
            val toLogString: String? = LogUtil.toLogString(JSONObject(notificationMessage))
            LogUtil.d(TAG, "notificationMessage:$toLogString")
            message = toLogString
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "UserActivity400"
    }
}

@Composable
fun MessageScreen(message: String?) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(transparentColor)
                .padding(paddingValues)
        ) {
            Text(
                text = message ?: "No message receive",
                modifier = Modifier.padding(16.dp),
                color = Pink80
            )
        }
    }
}
