package com.tencent.doh.plugins.share;

import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseResp;

/**
 * Created by amytwang on 2016/7/29.
 */
public class WxShareManager {

    public static final int SHARE_EVENT_OK = 0;
    public static final int SHARE_EVENT_FAIL = 1;

    private WXShareCallback mWXCallback = null;

    private static class Singleton {
        private static WxShareManager sInstance = new WxShareManager();
    }

    public static WxShareManager getInstance() {
        return Singleton.sInstance;
    }

    private WxShareManager() {

    }

    public void registerCallback(WXShareCallback callback) {
        mWXCallback = callback;
    }

    public void shareEvent(int event, Object object) {

        if (mWXCallback == null)
            return;

        if (event == SHARE_EVENT_OK) {
            mWXCallback.onShareSuccess();
        } else {
            BaseResp resp = (BaseResp) object;
            mWXCallback.onShareFail(resp.errCode, resp.errStr);
        }
    }

    public boolean onShareMsgRsp(BaseResp resp) {

        if (resp == null)
            return false;

        if (resp.getType() == ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX) {
            // 分享到微信的回调
            onShareResult(resp);
            return true;
        }
        return false;
    }

    private void onShareResult(BaseResp resp) {
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                WxShareManager.getInstance().shareEvent(WxShareManager.SHARE_EVENT_OK, null);
                break;
            default:
                WxShareManager.getInstance().shareEvent(WxShareManager.SHARE_EVENT_FAIL, resp);
                break;
        }
    }

    public interface WXShareCallback {
        void onShareSuccess();

        void onShareFail(int errorCode, String msg);
    }
}
