package com.tencent.doh.demo.demo;

import android.util.Log;

import com.tencent.doh.pluginframework.webview.WebViewPlugin;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by benpeng on 2016/9/12.
 */
public class ShortcutPlugin extends WebViewPlugin {
    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("addShortcut".equals(method)) {
            try {
                JSONObject json = new JSONObject(args[0]);
                //成功、失败回调
                String callback = json.optString(KEY_CALLBACK);
                Log.d(ShortcutPlugin.class.getSimpleName(),"addShortcut");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return super.handleJsRequest(url, pkgName, method, args);
    }
}
