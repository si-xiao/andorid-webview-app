package com.example.webgame

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

// 定义页面日志 tag
private const val TAG = "WebUrlManager"

// const val webUrl: String = "http://192.168.15.200:5174/,https://mainurl1.xyz/,https://b1hoe9.com/"
 const val webUrl: String = "https://mainurl1.xyz/home"
//const val webUrl: String = "http://192.168.15.199:5174"
// const val webUrl: String = "http://192.168.18.182:4396"
const val jpushAppKey: String = "b1a61e2fe4004a419c7f996c"
// 暂留着，flutter里边也没做处理，应该是已经没用的字段
var prefixParams: String = ""

fun _appendPrefixParams(baseUrl: String): String {
    val url = baseUrl.trim()
    if (prefixParams.isEmpty()) return url
    return if (url.contains("?")) {
        "$url&$prefixParams"
    } else {
        "$url?$prefixParams"
    }
}

suspend fun _isUrlHealthy(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val uri = URL(baseUrl)
        // 构建健康检查 URL
        val checkUri = URL("${uri.protocol}://${uri.host}:${if (uri.port != -1) uri.port else ""}/metaapi/v1/domain/check_domain")

        Log.d(TAG, "tox _isUrlHealthy check = $checkUri")

        // 创建连接（不使用代理）
        val connection = checkUri.openConnection(Proxy.NO_PROXY) as HttpURLConnection
        connection.connectTimeout = 3000 // 3秒超时
        connection.readTimeout = 3000
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        val responseBody = if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
        } else {
            null
        }

        connection.disconnect()

        // 解析 JSON 响应
        if (responseBody != null) {
            val jsonObject = JSONObject(responseBody)
            val apiCode = jsonObject.optInt("code", -1)

            Log.d(TAG, "_isUrlHealthy check = $checkUri, http = $responseCode, apiCode = $apiCode")

            return@withContext apiCode == 200
        }

        Log.d(TAG, "_isUrlHealthy check = $checkUri, http = $responseCode, no body")
        false

    } catch (e: Exception) {
        Log.e(TAG, "_isUrlHealthy health error for $baseUrl: ${e.message}")
        false
    }
}

/// 当 webUrl 配置多个域名时，按顺序探测，命中 200 后使用该地址。
suspend fun getRandomWebUrl(): String = withContext(Dispatchers.IO) {
    val urlList = webUrl
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (urlList.isNotEmpty()) {
        for (baseUrl in urlList) {
            val isAvailable = _isUrlHealthy(baseUrl)
            Log.d(TAG, "getRandomWebUrl check baseUrl = $baseUrl, isHealthy = $isAvailable")

            if (isAvailable) {
                return@withContext _appendPrefixParams(baseUrl)
            }
        }
    }

    // fallback：使用第一个 URL 或原始 URL
    val fallback = _appendPrefixParams(if (urlList.isNotEmpty()) urlList.first() else webUrl)
    Log.d(TAG, "getRandomWebUrl fallback = $fallback, prefixParams = $prefixParams")

    return@withContext fallback
}

/// 默认允许打开的链接
val urlWhiteList: List<String> = listOf(
    "https://www.kkgametop.xyz",
    "http://192.168.18.182",
    "https://reimagined-memory-jjgwj4xwqgxrfq4p4-8080.app.github.dev",
    // Google 登录需要
    "https://accounts.google.com",
    // Google 登录需要
    "https://accounts.youtube.com"
)

/// 判断 URL 是否以白名单中的任一链接开头
fun isUrlInWhiteList(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false // 空 URL 直接返回 false
    // 遍历白名单，检查 URL 是否以列表中的某个前缀开头
    return urlWhiteList.any { prefix ->
        url.startsWith(prefix)
    }
}

