package com.tencent.doh.demo.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.tencent.doh.demo.R;
import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.webview.DohWebView;
import com.tencent.doh.pluginframework.webview.PluginInfo;
import com.tencent.doh.ui.activity.DohWebViewActivity;
import com.tencent.smtt.sdk.WebSettings;

import java.util.HashMap;
import java.util.Map;

public class DemoActivity extends DohWebViewActivity  {

    private String mWxAppId = "wxc3484b7ca5a5617d";
    private String mWxAppSecret = "c1da3b918b1f0060834ab21db248f09d";
    private String mCloudAppId = "10055928";
    private String mCloudId = "AKIDZadCCSIjd7vNKnrirKXXTy4daZCfo08F";
    private String mCloudKey = "h6HCB8grhcAMUMvtZesXCgeYrpWK4KCj";
    private String mTencentQQAppid = "1105519707";
    private long   mXgAccessId = 2100215418;
    private String mXgAccessKey = "AM25P1PJ32HF";
    private String mFileBucket = "filestorage";
    private String mPhotoBucket = "photostorage";

    public Config mConfig;

//    private PluginInfo[] mList = new PluginInfo[]{
//            new PluginInfo(ShortcutPlugin.class, "shortcut", "mqq.shortcut api", "1.0")
//    };

    private String MY_CMD_JSON_STRING = "{\\*.dd.com\":[\"*\"], \"pub.dd.com\":[\"*\"]}";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo);
        setTitle("demo");
        mWebView = (DohWebView) this.findViewById(R.id.webview);
        mConfig = new Config.Builder(getApplicationContext())
                .setCloudParameters(new Config.CloudParameters(mCloudAppId,mCloudId,mCloudKey,mFileBucket,mPhotoBucket))
                .setWxParameters(new Config.WxParameters(mWxAppId))
                .setXgParameters(new Config.XgParameters(mXgAccessId,mXgAccessKey))
                .setResIds(buildResIds())
                .setTencentQQAppid(mTencentQQAppid)
                .setDebug(true)
                .setAuthorizeRule(MY_CMD_JSON_STRING)
                .build();

        mWebView.onCreate(this, mConfig);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE); // 不缓存，方便测试在Server端修改js
        if (!SharedPreferencesUtils.getUseRemoteRes(getApplicationContext()))
            mWebView.loadUrl("file:///android_asset/demo.html");
        else {
            String ip = SharedPreferencesUtils.getIp(getApplicationContext());
            String name = SharedPreferencesUtils.getName(getApplicationContext());
            mWebView.loadUrl("http://" + ip + "/" + name);
        }
    }

    private Map<String, Integer> buildResIds() {
        Map<String, Integer> resIds = new HashMap<>();
        resIds.put("close", R.drawable.webview_close_normal);
        return resIds;
    }

}
