package com.tencent.doh.pluginframework.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.AuthorizeConfig;
import com.tencent.doh.pluginframework.webview.DohWebView;
import com.tencent.smtt.sdk.WebView;

import java.lang.reflect.Constructor;

import org.json.JSONObject;

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();


    /**
     * copy from {@link org.json.JSONStringer#string}
     * 把字符串转换为js接收的字符串, 以""包括
     * @param value
     * @return
     */
    public static String toJsString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(1024);
        out.append("\"");
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);

            /*
             * From RFC 4627, "All Unicode characters may be placed within the
             * quotation marks except for the characters that must be escaped:
             * quotation mark, reverse solidus, and the control characters
             * (U+0000 through U+001F)."
             */
            switch (c) {
                case '"':
                case '\\':
                case '/':
                    out.append('\\').append(c);
                    break;

                case '\t':
                    out.append("\\t");
                    break;

                case '\b':
                    out.append("\\b");
                    break;

                case '\n':
                    out.append("\\n");
                    break;

                case '\r':
                    out.append("\\r");
                    break;

                case '\f':
                    out.append("\\f");
                    break;

                default:
                    if (c <= 0x1F) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }

        }
        out.append("\"");
        return out.toString();
    }


    /**
     * 判断wifi是否连接上
     */
    public static String checkWiFiActive(Context inContext) {
        Context context = inContext.getApplicationContext();
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getTypeName().equals("WIFI") && info[i].isAvailable() && info[i].isConnected()) {
                        WifiManager wifiManager = (WifiManager)inContext.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        return wifiInfo.getSSID();
                    }
                }
            }
        }
        return null;
    }

    public static Constructor<?> getConstructor(Class<?> cz,
                                                 Class<?>... parameterTypes)
            throws NoSuchMethodException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return cz.getConstructor(parameterTypes);
        }
        Constructor<?>[] cs = cz.getConstructors();
        for (Constructor<?> c : cs) {
            if (isParameterTypesMatch(parameterTypes, c.getParameterTypes())) {
                return c;
            }
        }
        throw new NoSuchMethodException();
    }
    private static boolean isParameterTypesMatch(Class<?>[] params1,
                                                 Class<?>[] params2) {
        if (params1.length != params2.length) {
            return false;
        }
        for (int i = 0, len = params1.length; i < len; ++i) {
            if (params1[i] != params2[i]) {
                return false;
            }
        }
        return true;
    }

    private static final char[] digits = new char[] { '0', '1', '2', '3', '4',//
            '5', '6', '7', '8', '9',//
            'A', 'B', 'C', 'D', 'E',//
            'F' };
    public static String String2HexString(String string) {
        byte[] temp = string.getBytes();
        String hexString = bytes2HexStr(temp);
        return hexString;
    }
    public static String bytes2HexStr(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        char[] buf = new char[2 * bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            buf[2 * i + 1] = digits[b & 0xF];
            b = (byte) (b >>> 4);
            buf[2 * i + 0] = digits[b & 0xF];
        }
        return new String(buf);
    }

    private static final String CALLBACK_NAME_HOLDER = "((0))";
    private static final String CALLBACK_PARAM_HOLDER = "((1))";
    private static final String CALL_JS_DEFAULT_TPL = "(window.mqq && mqq.version > 20140616001 && mqq.execGlobalCallback || function(cb) {window[cb] && window[cb].apply(window, [].slice.call(arguments, 1));}).apply(window, [((0)), ((1))]);";
    private static String sCallJsTpl = null;
    public static void callJs(WebView webview, String func, String... args) {
        if (sCallJsTpl == null) { // 从authConf读取模版
            String tpl = AuthorizeConfig.getInstance(webview.getContext().getApplicationContext()).getExtraString("jscallback", null);
            if (tpl != null && tpl.contains(CALLBACK_NAME_HOLDER) && tpl.contains(CALLBACK_PARAM_HOLDER)) {
                sCallJsTpl = tpl;
            } else {
                sCallJsTpl = CALL_JS_DEFAULT_TPL;
            }
        }
        StringBuilder param = new StringBuilder();
        if (args != null && args.length > 0 && !"".equals(args[0])) {
            param.append(args[0]);
            for (int i = 1, len = args.length; i < len; ++i) {
                param.append(',').append(args[i]);
            }
        } else {
            param.append("void(0)");
        }
        callJs(webview, sCallJsTpl.replace(CALLBACK_NAME_HOLDER, Utils.toJsString(func)).replace(CALLBACK_PARAM_HOLDER, param));
    }

    public static void callJs(WebView webview, String func, JSONObject result) {
        if (sCallJsTpl == null) { // 从authConf读取模版
            String tpl = AuthorizeConfig.getInstance(webview.getContext().getApplicationContext()).getExtraString("jscallback", null);
            if (tpl != null && tpl.contains(CALLBACK_NAME_HOLDER) && tpl.contains(CALLBACK_PARAM_HOLDER)) {
                sCallJsTpl = tpl;
            } else {
                sCallJsTpl = CALL_JS_DEFAULT_TPL;
            }
        }
        callJs(webview, sCallJsTpl.replace(CALLBACK_NAME_HOLDER, Utils.toJsString(func)).replace(CALLBACK_PARAM_HOLDER, result.toString()));
    }

    public static void callJs(final WebView webview, final String script) {
        if (webview == null) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (webview instanceof DohWebView) {
                        if (!((DohWebView) webview).isDestroyed) {
                            LogUtils.i(TAG, "WebViewPlugin：loadUrlOriginal script=" + script);
                            ((DohWebView) webview).loadUrlOriginal("javascript:" + script);
                        }
                    } else {
                        LogUtils.i(TAG,"WebViewPlugin：loadUrl script="+script);
                        webview.loadUrl("javascript:" + script);
                    }
                } catch (Exception e) {
                    LogUtils.d(TAG, "WebViewPlugin：webview load script exception");
                    e.printStackTrace();
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            webview.post(r);
        }
    }

    /**
     * 取得回调函数名
     */
    public static String getCallbackName(String obj) {
        String result = "";
        try{
            JSONObject tmp = new JSONObject(obj);
            result = tmp.optString("callback");
        }catch (Exception e){
            LogUtils.e(TAG,"getCallbackName json parse error");
            e.printStackTrace();
        }
        return result;
    }


    /**
     *
     * @param pattern 考虑到效率, 约定pattern的*只会有以下4种形式, 其余情况均为全匹配
     *                1. *
     *                2. *.*
     *                3. *string
     *                4. string*
     *                </ul>
     * @param domain
     * @return
     */
    public static boolean isDomainMatch(String pattern, String domain) {
        if (TextUtils.isEmpty(pattern) || TextUtils.isEmpty(domain)) {
            return false;
        }
        if ("*".equals(pattern)) {
            return true;
        } else if ("*.*".equals(pattern)) {
            return domain.indexOf('.') != -1;
        } else if (pattern.startsWith("*")) {
            return domain.endsWith(pattern.substring(1));
        } else if (pattern.endsWith("*")) {
            return domain.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return domain.equals(pattern);
        }
    }
}
