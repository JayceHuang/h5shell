package com.tencent.doh.plugins.login;

import android.content.Context;

import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.NetworkUtils;
import com.tencent.doh.pluginframework.util.PackageUtils;
import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

/**
 * * modify by benpeng on 2016/8/29.
 */
public class LoginManager {

    private String TAG = LoginManager.class.getSimpleName();

    public static final String WX_PACKAGE_NAME = "com.tencent.mm";// 微信的包名

    public static final int LOGIN_ERROR_NO_NETWORK = 5;
    public static final int LOGIN_ERROR_WX_INSTALL = LOGIN_ERROR_NO_NETWORK + 1;
    public static final int LOGIN_AUTH_LOGIN_ERROR = LOGIN_ERROR_WX_INSTALL + 1;
    public static final int LOGIN_AUTH_LOGIN_CANCEL = LOGIN_AUTH_LOGIN_ERROR + 1;
    public static final int LOGIN_AUTH_LOGIN_SUCESS = LOGIN_AUTH_LOGIN_CANCEL + 1;

    private String mRequestCode;//getToken 接口需传入此值，用于换取 accessToken

    private LoginCallback mLoginCallback;

    public enum LoginEvent {
        WX_LOGIN_RESP
    }

    private static class Singleton {
        private static LoginManager sInstance = new LoginManager();
    }

    public static LoginManager getInstance() {
        return Singleton.sInstance;
    }

    private LoginManager() {
        reset();
    }

    public void reset() {
        mRequestCode = null;
        mLoginCallback = null;
    }

    public void login(LoginCallback loginCallback) {
//        //有登录在处理的话先不处理其他请求
//        if (mLoginCallback != null)
//            return;
        reset();
        mLoginCallback = loginCallback;

        if (!needToLoginWithNet())
            return;

        loginWx();
    }

    private boolean needToLoginWithNet() {
        if (!NetworkUtils.isNetworkAvailable(Config.getContext())) {
            onLoginCallBack(LOGIN_ERROR_NO_NETWORK);
            return false;
        }
        else if (!PackageUtils.isAppInstalledFromSystem(WX_PACKAGE_NAME)) {
            onLoginCallBack(LOGIN_ERROR_WX_INSTALL);
            return false;
        }
        return true;
    }

    public void loginWx() {
        LogUtils.d(TAG, "loginWx");
        Context context = Config.getContext();
        if (Config.getWxParameters().getWxAppId() == null){
            LogUtils.e(TAG,"weixin appid is null !!");
            return;
        }
        IWXAPI api = WXAPIFactory.createWXAPI(context, Config.getWxParameters().getWxAppId(), true);
        api.registerApp(Config.getWxParameters().getWxAppId());

        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "wechat_sdk_demo_test";
        api.sendReq(req);
    }

    public boolean onLoginWxEventRsp(LoginEvent event, Object wxLoginRsp) {
        LogUtils.d(TAG, "onLoginWxEventRsp");
        if (wxLoginRsp == null){
            return false;
        }

        switch (event) {
            case WX_LOGIN_RESP:
                if (!(wxLoginRsp instanceof BaseResp))
                    return false;
                BaseResp resp = (BaseResp) wxLoginRsp;
                if (resp.getType() != ConstantsAPI.COMMAND_SENDAUTH) {
                    return false;
                }
                LogUtils.d(TAG, "onLoginWxEventRsp WX_LOGIN_RESP:" + resp.errCode);
                switch (resp.errCode) {
                    case BaseResp.ErrCode.ERR_OK:
                        if (resp instanceof SendAuth.Resp) {
                            SendAuth.Resp mResp = (SendAuth.Resp) resp;
                            authSucess(mResp.code);
                        }
                        break;
                    case BaseResp.ErrCode.ERR_USER_CANCEL:
                        authFailed(LOGIN_AUTH_LOGIN_CANCEL);
                        break;
                    case BaseResp.ErrCode.ERR_AUTH_DENIED:
                        authFailed(LOGIN_AUTH_LOGIN_ERROR);
                        break;
                }
                break;
        }
        return true;
    }

    public void authSucess(String code) {
        LogUtils.d(TAG, "AuthSucess");
        mRequestCode = code;
        onLoginCallBack(LOGIN_AUTH_LOGIN_SUCESS);
    }

    public void authFailed(int loginState) {
        LogUtils.d(TAG, "authFailed:" + loginState);
        onLoginCallBack(loginState);
    }

    private void onLoginCallBack(final int loginState) {
        if (mLoginCallback == null)
            return;

        mLoginCallback.onLoginFinish(loginState, mRequestCode);
    }

    public interface LoginCallback {
        void onLoginFinish(int loginState, String code);
    }
}
