package com.tencent.doh.plugins;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.util.Utils;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.smtt.sdk.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 此插件需要在AndroidManifest.xml增加以下内容(其中$YOUR PERMISSION$自定)
 * <pre>
 * &lt;uses-permission android:name="$YOUR PERMISSION$" /&gt;
 * &lt;permission android:name="$YOUR PERMISSION$" android:protectionLevel="signature" /&gt;
 *
 * &lt;application ...&gt;
 *     &lt;meta-data android:name="ak_webview_sdk_broadcast_permission" android:value="$YOUR PERMISSION$" /&gt;
 * </pre>
 * author:pel
 */
public class EventPlugin extends WebViewPlugin {
    public static final String ACTION_WEBVIEW_DISPATCH_EVENT = "com.qzone.qqjssdk.action.ACTION_WEBVIEW_DISPATCH_EVENT";
    static final String KEY_BROADCAST = "broadcast";
    static final String KEY_UNIQUE = "unique";
    static final String KEY_EVENT = "event";
    static final String KEY_DATA = "data";
    static final String KEY_DOMAINS = "domains";
    static final String KEY_SOURCE = "source";
    static final String KEY_ECHO = "echo";
    static final String KEY_URL = "url";
    static final String KEY_OPTIONS = "options";
    String mUniqueMark;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            boolean broadcast = intent.getBooleanExtra(KEY_BROADCAST, true);
            if (!broadcast) {
                return;
            }
            String unique = intent.getStringExtra(KEY_UNIQUE);
            if (unique != null && unique.equals(mUniqueMark)) { //不接收自己发的
                return;
            }
            String event = intent.getStringExtra(KEY_EVENT);
            if (TextUtils.isEmpty(event)) {
                return;
            }
            String dataStr = intent.getStringExtra(KEY_DATA);
            JSONObject data = null;
            if (dataStr != null) {
                try {
                    data = new JSONObject(dataStr);
                } catch (JSONException e) {
                    return;
                }
            }
            ArrayList<String> domains = intent.getStringArrayListExtra(KEY_DOMAINS);
            if (domains == null) {
                return;
            }
            String sourceStr = intent.getStringExtra(KEY_SOURCE);
            JSONObject source = null;
            if (sourceStr != null) {
                try {
                    source = new JSONObject(sourceStr);
                } catch (JSONException e) {
                    return;
                }
            }
            WebView webview = mRuntime.getWebView();
            if (webview == null) {
                return;
            }
            String currentUrl = webview.getUrl();
            if (currentUrl == null) {
                return;
            }
            Uri uri = Uri.parse(currentUrl);
            String host = uri.getHost();
            for (int i = 0, len = domains.size(); i < len; ++i) {
                if (Utils.isDomainMatch(domains.get(i), host)) {
                    dispatchJsEvent(event, data, source);
                    break;
                }
            }
            /*if(host == null || TextUtils.isEmpty(host)){
                dispatchJsEvent(event, data, source);
            }*/
        }
    };

    @Override
    protected void onCreate() {
        super.onCreate();
        Activity activity = mRuntime.getActivity();
        mUniqueMark = Long.toString(System.currentTimeMillis()) + Integer.toString(activity.hashCode());
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_WEBVIEW_DISPATCH_EVENT);
        activity.registerReceiver(receiver, filter, getPermission(mRuntime.context), null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRuntime.getActivity().unregisterReceiver(receiver);
    }

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if (KEY_EVENT.equals(pkgName)) {
            if ("init".equals(method)) {
                // 触发插件初始化, 不做其它事情
                return true;
            } else if ("dispatchEvent".equals(method)) {
                if (dispatchEvent(args)) {
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    private boolean dispatchEvent(String[] args) {
        if (args.length != 1) {
            return true;
        }
        try {
            WebView webview = mRuntime.getWebView();
            if (webview == null) {
                return true;
            }
            JSONObject json = new JSONObject(args[0]);
            String event = json.optString(KEY_EVENT);
            if (TextUtils.isEmpty(event)) {
                LogUtils.w(TAG, "param event is requested");
                return true;
            }
            JSONObject data = json.optJSONObject(KEY_DATA);
            JSONObject options = json.optJSONObject(KEY_OPTIONS);
            boolean echo = true;
            boolean broadcast = true;
            ArrayList<String> domains = new ArrayList<String>();
            String currentUrl = webview.getUrl();
            if (options != null) {
                echo = options.optBoolean(KEY_ECHO, true);
                broadcast = options.optBoolean(KEY_BROADCAST, true);
                JSONArray d = options.optJSONArray(KEY_DOMAINS);
                if (d != null) {
                    for (int i = 0, len = d.length(); i < len; ++i) {
                        String domain = d.optString(i);
                        if (!TextUtils.isEmpty(domain)) {
                            domains.add(domain);
                        }
                    }
                }
            }
            JSONObject source = new JSONObject();
            source.put(KEY_URL, currentUrl);
            if (domains.size() == 0 && currentUrl != null) {
                Uri uri = Uri.parse(currentUrl);
                if (uri.isHierarchical()) {
                    domains.add(uri.getHost());
                }
            }
            Intent intent = new Intent(ACTION_WEBVIEW_DISPATCH_EVENT);
            intent.putExtra(KEY_BROADCAST, broadcast);
            intent.putExtra(KEY_UNIQUE, mUniqueMark);
            intent.putExtra(KEY_EVENT, event);
            if (data != null) {
                intent.putExtra(KEY_DATA, data.toString());
            }
            intent.putStringArrayListExtra(KEY_DOMAINS, domains);
            intent.putExtra(KEY_SOURCE, source.toString());
            mRuntime.context.sendBroadcast(intent, getPermission(mRuntime.context));
            if (echo) {
                dispatchJsEvent(event, data, source);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 向webveiw发广播
     *
     * @param event   和web侧约定的事件名
     * @param data    消息体, 可以为null
     * @param domains 能够接收此广播的域名, 支持通配符, 不能为null, 例如["*.qq.com, m.qzone.com"], ["*"]
     * @param referer 发送此广播的页面url, web侧用于检查来源, 可以为null
     */
    public static void sendWebBroadcast(Context context, String event, JSONObject data, ArrayList<String> domains, String referer) {
        Intent intent = new Intent(ACTION_WEBVIEW_DISPATCH_EVENT);
        intent.putExtra("event", event);
        if (data != null) {
            intent.putExtra("data", data.toString());
        }
        intent.putStringArrayListExtra("domains", domains);
        JSONObject source = new JSONObject();
        try {
            source.put("url", referer);
        } catch (JSONException e) {
            // ignored
        }
        intent.putExtra("source", source.toString());
        context.sendBroadcast(intent, getPermission(context));
    }

    private static String sPermission = null;

    private static String getPermission(Context context) {
        if (sPermission != null) {
            return sPermission;
        }
        String permission = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            permission = appInfo.metaData.getString("ak_webview_sdk_broadcast_permission");
        } catch (PackageManager.NameNotFoundException e) {
            //ignored
        }
        if (TextUtils.isEmpty(permission)) {
            LogUtils.e("ak_webview_sdk", "\"ak_webview_sdk_broadcast_permission\" meta data not found");
            return null;
        }
        return sPermission = permission;
    }

}
