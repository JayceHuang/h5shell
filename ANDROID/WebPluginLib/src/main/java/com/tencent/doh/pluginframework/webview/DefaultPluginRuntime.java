package com.tencent.doh.pluginframework.webview;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import com.tencent.smtt.sdk.WebView;

public class DefaultPluginRuntime{

    private WeakReference<WebView> mWebView;
    private WeakReference<Activity> mActivity;
    public final Context context;
    public DefaultPluginRuntime(WebView webView, Activity activity) {
        mWebView = new WeakReference<WebView>(webView);
        mActivity = new WeakReference<Activity>(activity);
        context = activity.getApplicationContext();
    }
    public WebView getWebView() {
        return mWebView.get();
    }

    public Activity getActivity() {
        return mActivity.get();
    }

    public Context getContext() {
        return context;
    }

    /**
     * should override in sub class
     * @return nonempty string
     */
    public String getAccount() {
        return "0";
    }
}

