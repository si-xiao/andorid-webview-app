package com.example.webgame.utils.webController

enum class InjectWay {
    /**
     * 使用 evaluateJavascript + uri scheme 的复杂方式
     */
    BY_EVALUATEJS_WITH_URI_SCHEME,
    /**
     * 使用 addJavascriptInterface 的简单传统方式
     */
    BY_ADDJSINTERFACE,
}

/**
 * JS Bridge 注入方式
 */
class InjectJsBridgeWay {
    companion object {
        /**
         * 当前使用的 JS Bridge 注入方式
         */
        private var currentWay: InjectWay = InjectWay.BY_ADDJSINTERFACE

        /**
         * 获取当前使用的 JS Bridge 注入方式
         *
         * 有两种注入方式：
         * ```javascript
         * 1. 使用 evaluateJavascript + uri scheme 的方式
         *      InjectWay.BY_EVALUATEJS_WITH_URI_SCHEME
         *
         * 2. 使用 addJavascriptInterface + evaluateJavascript 的传统方式
         *      InjectWay.BY_ADDJSINTERFACE
         * ```
         */
        fun getCurrentWay(): InjectWay {
            return currentWay
        }
    }
}