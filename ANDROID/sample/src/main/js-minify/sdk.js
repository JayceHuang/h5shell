/**
 * @namespace core
 * @desc mqqapi内核的方法和属性
 */
;
(function(name, definition) {

    // JSSDK需把ua检查提前
    var ua = navigator.userAgent;

    // 通用SDK解决方案
    var REGEXP_QQJSSDK = /QQJSSDK\/([\d\.]+)/;

    //iframe替代方案
    var PROMPT_AVAILABLE = window.PROMPT_AVAILABLE = false;
    var CONSOLE_AVAILABLE = window.CONSOLE_AVAILABLE = false;

    /**
     * @attribute core.SDKVersion
     * @desc SDK 版本号
     */
    var SDKVersion = function (re) {
        return re && re[1];
    }(ua.match(REGEXP_QQJSSDK));

    /**
     * @attribute core.jssdk
     * @desc 最新版通用解决方法，true为支持
     */
    var jssdk = !!SDKVersion;

    // 当前native不支持jssdk
    if ( !jssdk ) {
        // 不支持jssdk就不玩了，交给下面的旧的qqapi.js
        return console.log('当前环境不支持通用JSSDK.');

    // 当前native支持jssdk，且`mqq`变量已经存在
    // 说明在这之前已经引入通用的jssdk包或者旧的qqapi包
    // 无论是新包还是旧包，都不会往下走下去
    // 因为旧包封装的`invoke`会判断手Q版本来走不同的协议
    // jssdk的协议用的是手Q最新的协议，所以同样能正常使用，只需要native端做好接口迁移即可
    } else if ( this[name] ) {
        return;
    }


    var mqq = this[name] = definition();
    // 带上版本跟标识
    mqq.SDKVersion = SDKVersion;
    mqq.jssdk = jssdk;

    if (typeof define === 'function' && (define.amd || define.cmd)) {
        define(mqq);
    } else if (typeof module === 'object') {
        module.exports = mqq;
    }

    if (mqq.android) { //android qzone qqjssdk>1.3 ping操作，iframe替代方案

        var qqJsSdkVer = function (re) {
            return re && re[1];
        }(ua.match(REGEXP_QQJSSDK));

        if(ua.match(/qzone/i)  && (qqJsSdkVer >= 1.3) ){
            prompt("PING_FOR_OPTIMAL_PATH","pingJsbridge://");
            console.log("pingJsbridge://");
        }

    }

})('mqq', function(options, undefined) {
    "use strict";

    var exports = {};

    var ua = navigator.userAgent;

    var firebug = window.MQQfirebug; // 调试插件引用

    var SLICE = Array.prototype.slice;

    var toString = Object.prototype.toString;

    // 通用SDK解决方案
    // var REGEXP_JSSDK = /QQJSSDK\/([\d\.]+)/;

    // var REGEXP_IOS_QQ = /(iPad|iPhone|iPod).*? (IPad)?QQ\/([\d\.]+)/;
    var REGEXP_IOS = /(iPad|iPhone|iPod).*? (IPad)?/;
    // var REGEXP_ANDROID_QQ = /\bV1_AND_SQI?_([\d\.]+)(.*? QQ\/([\d\.]+))?/;
    var REGEXP_ANDROID = /Android/

    var UUIDSeed = 1; //从1开始, 因为QQ浏览器的注入广告占用了0, 避免冲突

    var aCallbacks = {}; // 调用回调

    var isInitEvent = false; // 通知终端初始化事件模块

    // 如果已经注入则开启调试模式
    if ( firebug ) {
        exports.debuging = true;
        ua = firebug.ua || ua;
    } else {
        exports.debuging = false;
    }

    /**
     * @attribute core.iOS
     * @desc 如果在 iOS QQ中，值为 true，否则为 false
     * @support iOS 4.2
     * @support android 4.2
     */
    exports.iOS = REGEXP_IOS.test(ua);
    /**
     * @attribute core.android
     * @desc 如果在 android QQ中，值为 true，否则为 false
     * @support iOS 4.2
     * @support android 4.2
     */
    exports.android = REGEXP_ANDROID.test(ua);

    if (exports.iOS && exports.android) {

        /*
         * 同时是 iOS 和 android 是不可能的, 但是有些国产神机很恶心,
         * 明明是 android, ua 上还加上个 iPhone 5s...
         * 这里要 fix 掉
         */
        exports.iOS = false;
    }

    /**
     * @attribute core.version
     * @desc mqqapi自身的版本号
     * @support iOS 4.2
     * @support android 4.2
     */
    exports.version = '20150308005';

    /**
     * @function core.compare
     * @desc 比较版本号，返回比较结果（-1，0，1）。如果当前 QQVersion 小于给定版本，返回 -1，等于返回 0，大于返回 1
     * @param {String} version
     *
     * @example
     * mqq.QQVersion = "4.7";
     * mqq.compare("10.0");// 返回-1
     * mqq.compare("4.5.1");// 返回1
     */
    exports.compare = function (version) {
        var a = exports.SDKVersion.split('.');
        var b = String(version).split('.');
        try {
            for (var i = 0, len = Math.max(a.length, b.length); i < len; i++) {
                var l = isFinite(a[i]) && Number(a[i]) || 0,
                    r = isFinite(b[i]) && Number(b[i]) || 0;
                if (l < r) {
                    return -1;
                } else if (l > r) {
                    return 1;
                }
            }
        } catch (e) {
            return -1;
        }
        return 0;
    }

    if (!exports.android && !exports.iOS) {
        console.log('QQJSSDK: not android or ios');
    }

    /*
     * 调用日志输出
     */
    function log (params) {
        var firebug = window.MQQfirebug;
        if ( exports.debuging && firebug && firebug.log ) {
            try {
                firebug.log(params);
            } catch (e) {}
        }
    }

    /*
     * 生成全局SN，并储存回调
     */
    function storeCallback(callback) {
        var sn = UUIDSeed++;
        if (callback) {
            aCallbacks[sn] = callback;
        }
        return sn;
    }

    /*
     * 回调统一执行入口
     */
    function execGlobalCallback(sn) {
        var callback = typeof sn === 'function' ? sn : aCallbacks[sn];
        var argus = SLICE.call(arguments, 1);
        if (typeof callback === 'function') {
            setTimeout(function() {
                // 调试
                log({
                    sn: sn
                });
                callback.apply(null, argus);
            }, 0);
        } else {
            console.log('QQJSSDK: not found such callback: ' + sn);
        }
    }

    /*
     * 使用 iframe 发起伪协议请求给客户端
     */
    function openURL(url, ns, method, sn) {
        // Console.debug('openURL: ' + url);

        //iframe替代方案
        if(window.CONSOLE_AVAILABLE){
            console.log(url);
            return;
        }else if(window.PROMPT_AVAILABLE){
            prompt("USE_PROMPT_CONNECT",url);
            return;
        }

        var iframe = document.createElement('iframe');
        iframe.style.cssText = 'display:none;width:0px;height:0px;';
        var failCallback = function() {

            /*
                正常情况下是不会回调到这里的, 只有客户端没有捕获这个 url 请求,
                浏览器才会发起 iframe 的加载, 但这个 url 实际上是不存在的,
                会触发 404 页面的 onload 事件
            */
            sn && execGlobalCallback(sn, {
                retcode: -201,
                msg: 'error'
            });
        };
        if (exports.iOS) {

            /*
                ios 必须先赋值, 然后 append, 否者连续的 api调用会间隔着失败
                也就是 api1(); api2(); api3(); api4(); 的连续调用,
                只有 api1 和 api3 会真正调用到客户端
            */
            iframe.onload = failCallback;
            iframe.src = url;
        }
        var container = document.body || document.documentElement;
        container.appendChild(iframe);

        /*
            android 这里必须先添加到页面, 然后再绑定 onload 和设置 src
            1. 先设置 src 再 append 到页面, 会导致在接口回调(callback)中嵌套调用 api会失败,
                iframe会直接当成普通url来解析
            2. 先设置onload 在 append , 会导致 iframe 先触发一次 about:blank 的 onload 事件

         */

        if (exports.android) { // android 必须先append 然后赋值
            iframe.onload = failCallback;
            iframe.src = url;
        }

        // 调试
        log({
            ns: ns,
            method: method,
            url: url
        });

        // android 捕获了iframe的url之后, 也是中断 js 进程的, 所以这里可以用个 setTimeout 0 来删除 iframe
        setTimeout(function() {
            iframe.parentNode.removeChild(iframe);
        }, 0);

        // return returnValue;
        return null;
    }

    /**
     * @function core.invoke
     * @desc mqq 核心方法，用于调用客户端接口。
     * @param {String} namespace 命名空间
     * @param {String} method 接口名字
     * @param {Object} [params] API 调用的参数
     * @param {Function} [callback] API 调用的回调
     * @example
     * // 调用普通接口
     * // ios, android
     * mqq.invoke("ns", "method");
     *
     * @example
     * // 调用有返回值的接口
     * mqq.invoke("ns", "method", function(data){
     *     console.log(data);
     * });
     *
     * @example
     * // 调用有异步回调的接口
     * // ios, android
     * mqq.invoke("ns", "method", {
     *     "callback": handler, // 生成回调名字, 让客户端返回时可调用
     *     "p1": "p1",
     *     "p2": "p2",
     *     "p3": "p3"
     * });
     */
    function invokeClientMethod(ns, method, argus, callback) {
        // 参数合法性检查
        if (!ns || !method) {
            return null;
        }
        var arg = arguments,
            cb = arg[arg.length-1],
            url,
            sn,
            params; // sn 是回调函数的序列号

        if ( arg.length > 2 ) {
            if ( toString.call(argus) === '[object Object]' ) {
                params = argus;
            } else {
                params = {};
            }

            if ( typeof cb === 'function' ) {
                sn = storeCallback(cb);
                params.callback = String(sn);
            }
        }

        // 通用版SDK
        if ( exports.jssdk ) {
            // jsbridge://ns/method?p=test&p2=xxx&p3=yyy#123
            url = 'jsbridge://' + encodeURIComponent(ns) + '/' + encodeURIComponent(method);

            if ( params ) {
                url += '?p=' + encodeURIComponent(JSON.stringify(params))
            }

            openURL(url, ns, method, sn);

        }

        return null;

    }

    //////////////////////////////////// event /////////////////////////////////////////////////

    /**
     * @function core.addEventListener
     * @desc 监听客户端事件，该事件可能来自客户端业务逻辑，也可能是其他 WebView 使用 dispatchEvent 抛出的事件
     * @param {String} eventName 事件名字
     * @param {Function} handler 事件的回调处理函数
     * @param {Object} handler.data 该事件传递的数据
     * @param {Object} handler.source 事件来源
     * @param {string} handler.source.url 抛出该事件的页面地址
     * @example
     * mqq.addEventListener("hiEvent", function(data, source){
     *     console.log("someone says hi", data, source);
     * })
     *
     */
    function addEventListener(eventName, handler) {

        // android 在使用事件之前需先初始化
        if ( !isInitEvent && exports.android ) {
            isInitEvent = true;
            mqq.invoke('event', 'init')
        }

        if (eventName === 'qbrowserVisibilityChange') {

            // 兼容旧的客户端事件
            document.addEventListener(eventName, handler, false);
            return true;
        }
        var evtKey = 'evt-' + eventName;
        (aCallbacks[evtKey] = aCallbacks[evtKey] || []).push(handler);
        return true;
    }

    /**
     * @function core.removeEventListener
     * @desc 移除客户端事件的监听器
     * @param {String} eventName 事件名字
     * @param {Function} [handler] 事件的回调处理函数，不指定 handler 则删除所有该事件的监听器
     *
     */
    function removeEventListener(eventName, handler) {
        var evtKey = 'evt-' + eventName;
        var handlers = aCallbacks[evtKey];
        var flag = false;
        if (!handlers) {
            return false;
        }
        if (!handler) {
            delete aCallbacks[evtKey];
            return true;
        }

        for (var i = handlers.length - 1; i >= 0; i--) {
            if (handler === handlers[i]) {
                handlers.splice(i, 1);
                flag = true;
            }
        }

        return flag;
    }

    // 这个方法时客户端回调页面使用的, 当客户端要触发事件给页面时, 会调用这个方法
    function execEventCallback(eventName /*, data, source*/ ) {
        var evtKey = 'evt-' + eventName;
        var handlers = aCallbacks[evtKey];
        var argus = SLICE.call(arguments, 1);
        if (handlers) {
            handlers.forEach(function(handler) {
                execGlobalCallback(handler, argus);
            });
        }
    }
    /**
     * @function core.dispatchEvent
     * @desc 抛出一个事件给客户端或者其他 WebView，可以用于 WebView 间通信，或者通知客户端对特殊事件做处理（客户端需要做相应开发）
     * @param {String} eventName 事件名字
     * @param {Object} options 事件参数
     * @param {Boolean} echo 当前webview是否能收到这个事件，默认为true
     * @param {Boolean} broadcast 是否广播模式给其他webview，默认为true
     * @param {Array|String} domains 指定能接收到事件的域名，默认只有同域的webview能接收，支持通配符，比如"*.qq.com"匹配所有qq.com和其子域、"*"匹配所有域名。注意当前webview是否能接收到事件只通过echo来控制，这个domains限制的是非当前webview。
     * @example
     * //1. WebView 1(www.qq.com) 监听 hello 事件
     * mqq.addEventListener("hello", function(data, source){
     *    console.log("someone says hi to WebView 1", data, source)
     * });
     * //2. WebView 2(www.tencent.com) 监听 hello 事件
     * mqq.addEventListener("hello", function(data, source){
     *    console.log("someone says hi to WebView 2", data, source)
     * });
     * //3. WebView 2 抛出 hello 事件
     * //不传配置参数，默认只派发给跟当前 WebView 相同域名的页面, 也就是只有 WebView 2能接收到该事件（WebView 1 接收不到事件，因为这两个 WebView 的域名不同域）
     * mqq.dispatchEvent("hello", {name: "abc", gender: 1});
     *
     * //echo 为 false, 即使 WebView 2 的域名在 domains 里也不会收到事件通知, 该调用的结果是 WebView 1 将接收到该事件
     * mqq.dispatchEvent("hello", {name:"alloy", gender:1}, {
     *     //不把事件抛给自己
     *     echo: false,
     *     //广播事件给其他 WebView
     *     broadcast: true,
     *     //必须是这些域名的 WebView 才能收到事件
     *     domains: ["*.qq.com", "*.tencent.com"]
     * });
     *
     * //echo 和 broadcast 都为 false, 此时不会有 WebView 会接收到事件通知, 但是客户端仍会收到事件, 仍然可以对该事件做处理, 具体逻辑可以每个业务自己处理
     * mqq.dispatchEvent("hello", {name:"alloy", gender:1}, {
     *     echo: false,
     *     broadcast: false,
     *     domains: []
     * });
     *
     * @support iOS 5.0
     * @support android 5.0
     */
    function dispatchEvent(eventName, data, options) {

        var params = {
            event: eventName,
            data: data || {},
            options: options || {}
        };

        invokeClientMethod('event', 'dispatchEvent', params);
    }

    /**
     * @event qbrowserTitleBarClick
     * @desc 点击标题栏事件，监听后点击手机QQ标题栏就会收到通知，可以用来实现点击标题滚动到顶部的功能
     * @param {Function} callback 事件回调
     * @param {Object} callback.data 事件参数
     * @param {Object} callback.data.x 点击位置的屏幕x坐标
     * @param {Object} callback.data.y 点击位置的屏幕y坐标
     * @param {Object} callback.source 事件来源
     * @example
     * mqq.addEventListener("qbrowserTitleBarClick", function(data, source){
     *     console.log("Receive event: qbrowserTitleBarClick, data: " + JSON.stringify(data) + ", source: " + JSON.stringify(source));
     * });
     *
     * @support iOS 5.2
     * @support android 5.2
     */

    /**
     * @event qbrowserOptionsButtonClick
     * @desc Android 的物理菜单键的点击事件，点击后会收到通知
     * @param {Function} callback 事件回调
     * @param {Object} callback.data 事件参数
     * @param {Object} callback.source 事件来源
     * @example
     * mqq.addEventListener("qbrowserOptionsButtonClick", function(data, source){
     *     console.log("Receive event: qbrowserOptionsButtonClick, data: " + JSON.stringify(data) + ", source: " + JSON.stringify(source));
     * });
     *
     * @support iOS not support
     * @support android 5.2
     */

    /**
     * @event qbrowserPullDown
     * @desc 页面下拉刷新时候会抛出该事件，主要用于与setPullDown交互，具体可参考setPullDown
     * @example
     * mqq.addEventListener("qbrowserPullDown", function () {
     *     // ... Your Code ...
     * });
     * @note 该事件可配合下拉刷新做交互，具体可参考`setPullDown`
     *
     * @support iOS 5.3
     * @support android 5.3
     */


    //////////////////////////////////// end event /////////////////////////////////////////////////

    // for debug
    exports.__aCallbacks = aCallbacks;


    exports.invoke = invokeClientMethod;
    exports.execGlobalCallback = execGlobalCallback;

    // event
    exports.addEventListener = addEventListener;
    exports.removeEventListener = removeEventListener;

    exports.execEventCallback = execEventCallback;
    exports.dispatchEvent = dispatchEvent;

    return exports;

});

// Simple API Adapter
(function() {
    var doh = window.doh = {};

// 登录接口
    doh.login = function(args, callback) {
        mqq.invoke("wx", "login", args, callback);
    };

// 分享接口
    doh.shareQZone = function(args, callback) {
        mqq.invoke("share", "shareQZone", args, callback);
    };

    doh.shareQQ = function(args, callback) {
        mqq.invoke("share", "shareQQ", args, callback);
    };

    doh.shareTimeline = function(args, callback) {
        mqq.invoke("share", "shareTimeline", args, callback);
    };

    doh.shareAppMessage = function(args, callback) {
        mqq.invoke("share", "shareAppMessage", args, callback);
    };

// 图片接口
    doh.chooseImage = function(args, callback) {
        mqq.invoke("image", "chooseImage", args, callback);
    };

    doh.previewImage = function(args) {
        mqq.invoke("image", "previewImage", args);
    };

    doh.uploadImage = function(args, callback) {
        mqq.invoke("image", "uploadImage", args, callback);
    };

    doh.downloadImage = function(args, callback) {
        mqq.invoke("image", "downloadImage", args, callback);
    };

// 音频接口
    doh.startRecord = function() {
        mqq.invoke("audio", "startRecord");
    };

    doh.stopRecord = function(callback) {
        mqq.invoke("audio", "stopRecord", callback);
    };

    doh.playVoice = function(args) {
        mqq.invoke("audio", "playVoice", args);
    };

    doh.onVoiceRecordEnd = function(callback) {
        mqq.invoke("audio", "onVoiceRecordEnd", callback);
    };

    doh.onVoicePlayEnd = function(callback) {
        mqq.invoke("audio", "onVoicePlayEnd", callback);
    };

    doh.pauseVoice = function(args) {
        mqq.invoke("audio", "pauseVoice", args);
    };

    doh.stopVoice = function(args) {
        mqq.invoke("audio", "stopVoice", args);
    };

    doh.uploadVoice = function(args, callback) {
        mqq.invoke("audio", "uploadVoice", args, callback);
    };

    doh.downloadVoice = function(args, callback) {
        mqq.invoke("audio", "downloadVoice", args, callback);
    };

// 信鸽接口
    doh.registerPush = function(args, callback) {
        mqq.invoke("xgpush", "registerPush", args, callback);
    };

    doh.unregisterPush = function(args, callback) {
        mqq.invoke("xgpush", "unregisterPush", args, callback);
    };

    doh.setTag = function(args, callback) {
        mqq.invoke("xgpush", "setTag", args, callback);
    };

    doh.delTag = function(args, callback) {
        mqq.invoke("xgpush", "delTag", args, callback);
    };

    doh.addLocalNotification = function(args, callback) {
        mqq.invoke("xgpush", "addLocalNotification", args, callback);
    };

    doh.clearLocalNotifications = function(args, callback) {
        mqq.invoke("xgpush", "clearLocalNotifications", args, callback);
    };

    doh.cancelNotification = function(args, callback) {
        mqq.invoke("xgpush", "cancelNotification", args, callback);
    };

    doh.setListener = function(args, callback) {
        mqq.invoke("xgpush", "setListener", args, callback);
    };

// 设备信息接口
    doh.getDeviceInfo = function(callback) {
        mqq.invoke("device", "getDeviceInfo", callback);
    };

    doh.getNetworkType = function(callback) {
        mqq.invoke("device", "getNetworkType", callback);
    };

//UI接口
    doh.setTitle = function(args) {
        mqq.invoke("ui", "setTitle", args);
    };

    doh.showCloseButton = function() {
        mqq.invoke("ui", "showCloseButton");
    };


    doh.hideCloseButton = function() {
        mqq.invoke("ui", "hideCloseButton");
    };


    doh.setOptionsMenus = function(args) {
        mqq.invoke("ui", "setOptionsMenus", args);
    };


    doh.showOptionsMenu = function() {
        mqq.invoke("ui", "showOptionsMenu");
    };


    doh.hideOptionsMenu = function() {
        mqq.invoke("ui", "hideOptionsMenu");
    };


    doh.onOptionsMenuClick = function(callback) {
        mqq.invoke("ui", "onOptionsMenuClick",callback);
    };

//APP接口
    doh.isAppInstalled = function(args, callback) {
        mqq.invoke("app","isAppInstalled", args, callback);
    };

    doh.launchApp = function(args) {
        mqq.invoke("app","launchApp", args);
    };

// 位置信息接口
    doh.openLocation = function(callback) {
        mqq.invoke("location","openLocation",callback);
    };

    doh.getLocation = function(callback) {
        mqq.invoke("location","getLocation",callback);
    };

})();

// WeChat API Adapter
(function() {
    var wx = window.wx = {};

    wx.shareTimeline = function(args) {
        mqq.invoke("share", "shareTimeline", args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success();
                }
            } else {
                if (typeof args.cancel === 'function') {
                    args.cancel();
                }
            }
        });
    };

    wx.shareAppMessage = function(args) {
        mqq.invoke("share", "shareAppMessage", args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success();
                }
            } else {
                if (typeof args.cancel === 'function') {
                    args.cancel();
                }
            }
        });
    };

    wx.shareQQ = function(args) {
        mqq.invoke("share", "shareQQ", args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success();
                }
            } else {
                if (typeof args.cancel === 'function') {
                    args.cancel();
                }
            }
        });
    };

    wx.shareQZone = function(args) {
        mqq.invoke("share", "shareQZone", args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success();
                }
            } else {
                if (typeof args.cancel === 'function') {
                    args.cancel();
                }
            }
        });
    };

    wx.chooseImage = function(args) {
        mqq.invoke('image', 'chooseImage', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.previewImage = function(args) {
        mqq.invoke('image', 'previewImage', args);
    };

    wx.uploadImage = function(args) {
        mqq.invoke('image', 'uploadImage', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.downloadImage = function(args) {
        mqq.invoke('image', 'downloadImage', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.startRecord = function() {
        mqq.invoke('audio', 'startRecord');
    };

    wx.stopRecord = function(args) {
        mqq.invoke('audio', 'stopRecord', {}, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.onVoiceRecordEnd = function(args) {
        mqq.invoke('audio', 'onVoiceRecordEnd', {}, function(ret) {
            if (ret.code == 0) {
                if (typeof args.complete === 'function') {
                    args.complete(ret.data);
                }
            }
        });
    };

    wx.onVoicePlayEnd = function(args) {
        mqq.invoke('audio', 'onVoicePlayEnd', {}, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.playVoice = function(args) {
        mqq.invoke('audio', 'playVoice', args);
    };

    wx.pauseVoice = function(args) {
        mqq.invoke('audio', 'pauseVoice', args);
    };

    wx.stopVoice = function(args) {
        mqq.invoke('audio', 'stopVoice', args);
    };

    wx.uploadVoice = function(args) {
        mqq.invoke('audio', 'uploadVoice', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.downloadVoice = function(args) {
        mqq.invoke('audio', 'downloadVoice', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.getNetworkType = function(args) {
        mqq.invoke('device', 'getNetworkType', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.openLocation = function(args) {
        mqq.invoke('location', 'openLocation', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

    wx.getLocation = function(args) {
        mqq.invoke('location', 'getLocation', args, function(ret) {
            if (ret.code == 0) {
                if (typeof args.success === 'function') {
                    args.success(ret.data);
                }
            }
        });
    };

})();