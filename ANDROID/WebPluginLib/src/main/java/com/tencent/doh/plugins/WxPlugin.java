package com.tencent.doh.plugins;


import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.plugins.login.LoginManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by benpeng on 2016/8/3.
 */
public class WxPlugin extends WebViewPlugin {
    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("login".equals(method)) {
            return wxLogin(args);
        }
        return  false;
    }

    private boolean wxLogin(String[] args) {
        JSONObject reqParam;
       final String callback;
        try {
            reqParam = new JSONObject(args[0]);
            callback = reqParam.optString(KEY_CALLBACK);
            LoginManager.getInstance().login(new LoginManager.LoginCallback() {
                @Override
                public void onLoginFinish(int loginState, String requestCode) {
                    LogUtils.d(TAG,loginState+"");
                    JSONObject resp = new JSONObject();
                    if (requestCode != null )
                        try {
                            resp.put("tokenCode", requestCode);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    int code = -1;
                    switch (loginState) {
                        case LoginManager.LOGIN_AUTH_LOGIN_ERROR:
                            code = -1;
                            break;
                        case LoginManager.LOGIN_ERROR_NO_NETWORK:
                            code = -1;
                            break;
                        case LoginManager.LOGIN_AUTH_LOGIN_CANCEL:
                            code = -2;
                            break;
                        case LoginManager.LOGIN_ERROR_WX_INSTALL:
                            code = -4;
                            break;
                        case LoginManager.LOGIN_AUTH_LOGIN_SUCESS:
                            code = 0;
                            break;
                    }
                    callJs(callback, getResult(code,"",resp));
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  true;
    }
}
