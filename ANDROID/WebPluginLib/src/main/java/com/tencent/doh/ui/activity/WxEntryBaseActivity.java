package com.tencent.doh.ui.activity;

import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.plugins.share.WxShareManager;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.doh.plugins.login.LoginManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public abstract class WxEntryBaseActivity extends Activity implements IWXAPIEventHandler {

    // IWXAPI 是第三方app和微信通信的openapi接口
    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, Config.getWxParameters().getWxAppId(), false);
        api.registerApp(Config.getWxParameters().getWxAppId());
        api.handleIntent(this.getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq arg0) {
        finish();
    }

    @Override
    public void onResp(BaseResp resp) {
        if (WxShareManager.getInstance().onShareMsgRsp(resp)){
            finish();
            return;
        }else if (LoginManager.getInstance().onLoginWxEventRsp(LoginManager.LoginEvent.WX_LOGIN_RESP, resp)){
            finish();
            return;
        }
        finish();
    }
}
