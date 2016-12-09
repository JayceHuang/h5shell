package com.tencent.doh.pluginframework.webview;

import android.graphics.Bitmap;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

public class DohWebViewClient extends WebViewClient {
    private static final String TAG = DohWebViewClient.class.getSimpleName();

    protected WebViewPluginEngine mPluginEngine;
    public static long pageStartTime = -1;
    public static long pageFinishTime = -1;
    public static String mUrl = "";
    private boolean mProtectStartTime = false;
    private boolean mProtectFinishTime = false;

    public DohWebViewClient() {
    }

    /* package */ void setPluginEngine(WebViewPluginEngine pluginEngine) {
        mPluginEngine = pluginEngine;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (!mProtectStartTime) {
            pageStartTime = System.currentTimeMillis();
        }
        mProtectStartTime = true;
        mUrl = url;

        if (mPluginEngine != null) {
            mPluginEngine.handleEvent(url, WebViewPlugin.EVENT_LOAD_START,
                    null);
        }
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (!mProtectFinishTime) {
            pageFinishTime = System.currentTimeMillis();
        }
        mProtectFinishTime = true;
        if (mPluginEngine != null) {
            mPluginEngine.handleEvent(url, WebViewPlugin.EVENT_LOAD_FINISH,
                    null);
        }
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode,
                                String description, String failingUrl) {
        if (mPluginEngine != null) {
            mPluginEngine.handleError(failingUrl,
                    WebViewPlugin.EVENT_LOAD_ERROR, errorCode);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webview, String url) {
        LogUtils.d(TAG + "shouldOverrideUrlLoading", " by iframe : " + url);
        if (mPluginEngine == null) {
            LogUtils.d(TAG + "shouldOverrideUrlLoading", "mPluginEngine is null");
        } else if (mPluginEngine.canHandleJsRequest(url)) {
            LogUtils.d(TAG + "shouldOverrideUrlLoading", "canHandleJsRequest." + url);
            return true;
        } else if (mPluginEngine.handleRequest(url)) {
            LogUtils.d(TAG + "shouldOverrideUrlLoading", "handleRequest." + url);
            return true;
        }
        return super.shouldOverrideUrlLoading(webview, url);
    }

    /**
     * Yukin:离线加载替换模式,仅对android4.0+有效 context必须在onCreate里面初始化
     * 4.7因为支持跨包加载，所有放开限制，对所有请求拦截。只要url里面包括_bid，则尝试离线加载
     *
     * @param view
     * @param url
     * @return
     */

    public WebResourceResponse shouldInterceptRequest(WebView view,
                                                      String url) {

        if (mPluginEngine == null) {
            return null;
        }
        WebResourceResponse o = null;
        try {
            o = (WebResourceResponse) mPluginEngine.handleEvent(url, WebViewPlugin.EVENT_LOAD_RESOURCE);
        } catch (Exception e) {

        }
        return o;
    }


//	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
//	WebResourceResponse shouldInterceptRequest (WebView view,
//												WebResourceRequest request)
//	{
//		if (mPluginEngine == null) {
//			return null;
//		}
//		WebResourceResponse o=null;
//		try{
//			o=(WebResourceResponse) mPluginEngine.handleEvent(request.getUrl().toString(), WebViewPlugin.EVENT_LOAD_RESOURCE);
//		}catch(Exception e){
//
//		}
//		return o;
//	}
}