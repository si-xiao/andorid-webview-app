package com.example.webgame.utils

/**
 * 转义 JSON 字符串，防止 JSON 字符串中的特殊字符被 JavaScript 解析为 HTML 标签
 */
fun escapeJsonString(json: String): String {
    return json
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}