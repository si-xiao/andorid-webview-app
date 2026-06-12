package com.example.webgame.utils


class Constants {
    companion object {
        const val TAG = "Constants"

        /**
         * 长连接状态是否连接
         */
        var isConnectEnable: Boolean = false
        /**
         * 通知开关状态是否开启
         */
        var isNotificationEnable: Boolean = false
    }
}

class JsBridgeSchemeConstants {
    companion object {
        /**
         * 自定义JsBridge scheme 协议名
         */
        const val JS_BRIDGE_SCHEME_NAME = "nativebridge"

        // Bridge Ready Promise 脚本 - 须最先注入
        private const val BRIDGE_READY_SOURCE = """
            (function() {
                if (window.__jsBridgeReadyPromise) return;
        
                window.__jsBridgeReadyPromise = new Promise(function(resolve) {
                    var settled = false;
                    var maxWaitMs = 10000;
                    var start = Date.now();
        
                    function isReady() {
                        return !!(
                            window.jsBridge &&
                            typeof window.jsBridge.postMessage === 'function'
                        );
                    }
        
                    function done() {
                        if (settled) return;
                        settled = true;
                        resolve();
                    }
                    
                    console.log('[[JSBridge]] Waiting for bridge isReady =', isReady());
                    if (isReady()) {
                        done();
                        return;
                    }
        
                    window.addEventListener('inAppWebViewPlatformReady', done, { once: true });
        
                    var timer = setInterval(function() {
                        console.log('[[JSBridge]] setInterval for bridge isReady =', isReady());
                        if (isReady() || Date.now() - start > maxWaitMs) {
                            clearInterval(timer);
                            done();
                        }
                    }, 30);
                });
            })();
        """

        /**
         * 中转JS脚本：代理原有 bridge 对象，转发为自定义协议（这样可以兼容H5 JS代码完全不用改）
         *
         * 同时支持 window.jsBridge 和 window.ownBridge2
         */
        val GLOBAL_PROXY_JS = """
            (function(){
                const promises = {};
                let seq = 1;
                
                // 原生统一回调入口
                window.__onNativeResponse = function (seqId, success, data) {
                    const p = promises[seqId];
                    if (!p) return;
                    delete promises[seqId];
                    
                    if (success) {
                      p.resolve(data);
                    } else {
                      p.reject(new Error(data || "Native error"));
                    }
                };
                
                // 创建支持 await + 超时的 bridge
                function createBridge(bridgeName) {
                    return {
                      postMessage: async function (eventName, data, timeout = 15000) {
                        const seqId = "seq_" + seq++;
                    
                        return new Promise((resolve, reject) => {
                          // 超时
                          const timer = setTimeout(() => {
                            delete promises[seqId];
                            reject(new Error("Timeout calling native: " + eventName));
                          }, timeout);
                    
                          // 存起来等待原生回调
                          promises[seqId] = {
                            resolve: (result) => {
                              clearTimeout(timer);
                              resolve(result);
                            },
                            reject: (err) => {
                              clearTimeout(timer);
                              reject(err);
                            },
                          };
                    
                          // 统一协议：scheme://bridge?seq&event&data
                          const encodedEvent = encodeURIComponent(eventName);
                          const encodedData = encodeURIComponent(data ?? "");
                          window.location.href = `${JS_BRIDGE_SCHEME_NAME}://` + bridgeName + "?seq=" + seqId + "&event=" + encodedEvent + "&data=" + encodedData;
                        });
                      },
                    };
                }
                // 挂载多个 bridge
                if (!window.jsBridge) {
                    window.jsBridge = createBridge("jsBridge");
                }
                if (!window.ownBridge2) {
                    window.ownBridge2 = createBridge("ownBridge2");
                }
            })();
        """.trimIndent()

        /**
         * JS脚本：注入原有 bridge 对象，转发为自定义协议（这样可以兼容H5 JS代码完全不用改）
         *
         * 同时支持 window.jsBridge 和 window.ownBridge2
         *
         * 包含 bridge ready 检测
         */
        val GLOBAL_REJECT_JS = """
            $BRIDGE_READY_SOURCE
            (function(){
                // 包装成 async/await
                function wrapPromise(call) {
                    return new Promise(async (resolve, reject) => {
                      try {
                        const json = JSON.parse(await call());
                        if (json.success) resolve(json.data);
                        else reject(new Error(json.data));
                      } catch (e) {
                        reject(e);
                      }
                    });
                }
                console.log('tox============>window.jsBridge', window.jsBridge);
                // 创建多个 bridge
                if (!window.jsBridge) {
                    console.log('tox============>创建 jsBridge');
                    window.jsBridge = {
                        postMessage: async (eventName, data) => wrapPromise(async () => 
                          // console.log('[[JSBridge]] call jsBridge.postMessage:', eventName, data);
                          await window.__flutterBridgeReadyPromise;
                          window.NativeBridge.callNative("jsBridge", eventName, JSON.stringify(data));
                        )
                    };
                }
                console.log('tox============>window.ownBridge2', window.ownBridge2);
                if (!window.ownBridge2) {
                    console.log('tox============>创建 ownBridge2');
                    window.ownBridge2 = {
                        postMessage: async (eventName, data) => wrapPromise(async () => 
                          // console.log('[[JSBridge]] call ownBridge2.postMessage:', eventName, data);
                          await window.__flutterBridgeReadyPromise;
                          window.NativeBridge.callNative("ownBridge2", eventName, JSON.stringify(data));
                        )
                    };
                }
            })();
        """.trimIndent()
    }
}
