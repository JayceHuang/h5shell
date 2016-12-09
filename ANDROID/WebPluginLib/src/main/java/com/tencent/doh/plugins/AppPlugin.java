package com.tencent.doh.plugins;

import android.content.Intent;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.util.PackageUtils;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xiaofuzhang on 2016/8/22.
 */
public class AppPlugin extends WebViewPlugin {

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("isAppInstalled".equals(method)  ) {
            isAppInstalled(args);
        } else if ("launchApp".equals(method)  ) {
            launchApp(args);
        } else {
            return false;
        }
        return true;
    }

    private void isAppInstalled(String[] args){
        if(args.length == 1){
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject res = new JSONObject();
                try {
                    res.put("isInstalled",
                            PackageUtils.isAppInstalled(mRuntime.context,
                                    json.getString("name").trim()));
                } catch (Exception e) {
                    e.printStackTrace();
                    res.put("isInstalled",false);
                }
                callJs(callback, getResult(res));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        }
    }

    private void launchApp(String[] args) {
        if (args.length == 1) {
            try {
                JSONObject json = new JSONObject(args[0]);
                String name = json.getString("name");
                Intent intent = mRuntime.context.getPackageManager().getLaunchIntentForPackage(name.trim());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mRuntime.context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
