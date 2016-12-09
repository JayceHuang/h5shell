package com.tencent.doh.demo.demo;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.tencent.doh.demo.R;
import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.webview.DohWebChromeClient;
import com.tencent.doh.pluginframework.webview.DohWebView;
import com.tencent.doh.pluginframework.webview.DohWebViewClient;
import com.tencent.doh.ui.activity.DohWebViewActivity;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.WebView;

/**
 * 只是作为演示挂号平台运行效果
 */
public class MedActivity extends DohWebViewActivity {

    private Config mConfig;
    private String mWxAppId = "wxc3484b7ca5a5617d";
    private String mCloudAppId = "10055928";
    private String mCloudId = "AKIDZadCCSIjd7vNKnrirKXXTy4daZCfo08F";
    private String mCloudKey = "h6HCB8grhcAMUMvtZesXCgeYrpWK4KCj";
    private String mCacheFileDirPath;
    private String mOriginalUserAgent;
    private String mWeChatUserAgent;
    private String mFileBucket = "filestorage";
    private String mPhotoBucket = "photostorage";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.med);
        setNavigationIcon(R.drawable.back);
        setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mCacheFileDirPath = this.getCacheDir().getAbsolutePath();
        mWebView = (DohWebView) this.findViewById(R.id.webview);
        mConfig = new Config.Builder(getApplicationContext())
                .setCloudParameters(new Config.CloudParameters(mCloudAppId,mCloudId,mCloudKey,mFileBucket,mPhotoBucket))
                .setWxParameters(new Config.WxParameters(mWxAppId))
                .build();

        mWebView.onCreate(this, mConfig);
        // http://wximg.qq.com/cityservices/3rd/html/310000_hlwj_guahao.html，不能包含微信UA，否则医院跳转不了，why？
        mOriginalUserAgent = mWebView.getSettings().getUserAgentString();
        // 号源方（例如：微医）后台会检测客户端UA，需要包含微信的UA信息
        mWeChatUserAgent = mWebView.getSettings().getUserAgentString() + "MicroMessenger/6.3.22.821";

        mWebView.setWebViewClient(new DohWebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.startsWith("https://guahao.wecity.qq.com")) {
                    // 号源方（例如：微医）后台会检测客户端UA，需要包含微信的UA信息，否则出现不支持平台错误页面
                    mWebView.getSettings().setUserAgentString(mWeChatUserAgent);
                    // 需要保存openId，最后一步跳转到号源方（例如：微医）需要用到，否则跳转不了
//                    view.loadUrl("javascript:sessionStorage.setItem('openId', 'ogrqxwxQgDV0CmNj6RhTbYAqGfGA');");
                } else {
                    mWebView.getSettings().setUserAgentString(mOriginalUserAgent);
                }
            }
        });
        mWebView.setWebChromeClient(new DohWebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                setTitle(title);
            }
        });
        setCookie(this, "https://guahao.wecity.qq.com", "code=003EOxEB0GUxEa2HOtFB06CzEB0EOxEL; domain=.qq.com; path=/");
        // 微信城市服务->上海挂号平台入口
        mWebView.loadUrl("http://wximg.qq.com/cityservices/3rd/html/310000_hlwj_guahao.html");
    }

    /**
     * TODO:
     *
     * @param context
     * @param url
     * @param cookie
     */
    private static void setCookie(Context context, String url, String cookie) {
        CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie(url, cookie);
        CookieSyncManager.getInstance().sync();
    }

    @Override
    public void onBackPressed() {
        setCookie(this, "https://guahao.wecity.qq.com", "code=003EOxEB0GUxEa2HOtFB06CzEB0EOxEL; domain=.qq.com; path=/");
        super.onBackPressed();
    }

}
