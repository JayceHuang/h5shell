package com.tencent.doh.plugins;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.util.ApkUtils;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.NumberUtils;
import com.tencent.doh.plugins.share.WxShareManager;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXMusicObject;
import com.tencent.mm.sdk.modelmsg.WXVideoObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by mangosmwang on 2015/3/11.
 */
public class SharePlugin extends WebViewPlugin {

    private static final String PACKAGE_NAME_QQ = "com.tencent.mobileqq";
    private static final String APPID_TENCENT = Config.getTencentQQAppid();

    // QQ
    private Tencent mTencent;

    // Qzone
    private static final String PACKAGE_NAME_QZONE = "com.qzone";
    public static final String KEY_SHARE_APPID = "SHARE_SUBTYPE";// 分享应用ID
    public static final String KEY_SHARE_APPNAME = "SHARE_SOURCE"; // 分享来源
    public static final String KEY_SHARE_TITLE = "SHARE_TITLE";// 分享标题
    public static final String KEY_SHARE_CONTENT = "SHARE_CONTENT";// 分享描述
    public static final String KEY_SHARE_THUMB = "SHARE_THUMB";// 分享缩微图
    public static final String ENTRANCE_FROM = "entranceFrom";// 分享类型
    public static final int ENTRANCE_FROM_SHARE = 9;// 分享值
    public static final int QQ_NOT_EXIST = -2;
    public static final int WX_NOT_EXIST = -2;
    public static final int USER_CANCEL_SHARE = -1;
    public static final int SHARE_OTHER_ERROR = -3;

    public static String SHARE_APPNAME = "";// 应用名称

    // WX
    public final static String KEY_SHARE_TO_WX_CALLBACK = "shareToWXCallback";
    public final static String KEY_WX_TITLE = "share2wx_title";
    public final static String KEY_WX_DRAWABLE = "share2wx_drawable";
    public final static String KEY_WX_URL = "share2wx_url";
    public final static String KEY_WX_DATA_URL = "share2wx_data_url"; //针对music和video的数据链接
    public final static String KEY_WX_SUMMARY = "share2wx_summary";
    public final static String KEY_WX_TYPE = "share2wx_type";
    public final static String KEY_WX_TL_SCENE = "share2wx_tl_scene";

    public final static int KEY_WX_TYPE_LINK = 0;  //默认分享类型为link
    public final static int KEY_WX_TYPE_MUSIC = 1; //分享类型为music
    public final static int KEY_WX_TYPE_VIDEO = 2; //分享类型为video

    private static final int ICON_DOWNLOAD_FINISHED = 0;
    private Bitmap mWxBitmap;
    private Bundle mWxBundle;
    private Handler mWxIconHandler;
    private IWXAPI mAPI;

    @Override
    protected void onCreate() {
        super.onCreate();
        mAPI = WXAPIFactory.createWXAPI(mRuntime.context, Config.getWxParameters().getWxAppId(), false);
        mAPI.registerApp(Config.getWxParameters().getWxAppId());
        WxShareManager.getInstance().registerCallback(mWXShareCallback);
    }

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        Bundle bundle = new Bundle();
        if ("shareQQ".equals(method)) {
            shareQQ(bundle, args);
        } else if ("shareQZone".equals(method)) {
            shareQQZone(bundle, args);
        } else if (("shareAppMessage".equals(method) || "shareTimeline".equals(method))) {
            shareAppMessage(method, args);
        } else {
            return false;
        }
        return true;
    }

    private void shareAppMessage(String method, String[] args) {
        if (args.length == 1) {


            JSONObject reqParam;
            String callback;
            if (!mAPI.isWXAppInstalled()) {
                try {
                    reqParam = new JSONObject(args[0]);
                    callback = reqParam.optString(KEY_CALLBACK);
                    callJs(callback, getResult(WX_NOT_EXIST, "没安装微信", new JSONObject()));
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                if (mWxIconHandler == null) {
                    mWxIconHandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            switch (msg.what) {
                                case ICON_DOWNLOAD_FINISHED:
                                    if (mWxBitmap != null) {
                                        shareToWX();
                                    }
                                    break;
                            }
                        }
                    };
                }
                mWxBundle = new Bundle();
                if ("shareTimeline".equals(method)) {
                    mWxBundle.putBoolean(KEY_WX_TL_SCENE, true);
                }
                try {
                    reqParam = new JSONObject(args[0]);
                    callback = reqParam.optString(KEY_CALLBACK);
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(KEY_SHARE_TO_WX_CALLBACK, callback)
                            .commit();
                    mWxBundle.putString(KEY_WX_TITLE, reqParam.optString("title"));
                    mWxBundle.putString(KEY_WX_URL, reqParam.optString("link"));
                    mWxBundle.putString(KEY_WX_SUMMARY, reqParam.optString("desc"));
                    mWxBundle.putString(KEY_WX_DATA_URL, reqParam.optString("dataUrl"));
                    //区分类型
                    String type = reqParam.optString("type");
                    if (type.equals("music")) {
                        mWxBundle.putInt(KEY_WX_TYPE, KEY_WX_TYPE_MUSIC);
                    } else if (type.equals("video")) {
                        mWxBundle.putInt(KEY_WX_TYPE, KEY_WX_TYPE_VIDEO);
                    } else {
                        mWxBundle.putInt(KEY_WX_TYPE, KEY_WX_TYPE_LINK);
                    }
                    new DownloadWxIconThread(reqParam.optString("imgUrl")).start();// WX需要的是bitmap不是url,拉取完异步回调分享接口
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void shareQQZone(Bundle bundle, String[] args) {
        if (args.length == 1) {


            JSONObject reqParam;
            final String callback;
//            if (this.checkInstallApp(PACKAGE_NAME_QZONE)) {
//                LogUtils.d(TAG, "优先分享到独立版");
//                try {
//                    reqParam = new JSONObject(args[0]);
//                    this.shareToQzone(reqParam.optString("title"), reqParam.optString("desc"), reqParam
//                            .optString("imgUrl"), reqParam.optString("link"), Integer.parseInt(APPID_TENCENT));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else
            if (this.checkInstallApp(PACKAGE_NAME_QQ)) {
                LogUtils.d(TAG, "没装Qzone，分享到结合版");
                try {
                    reqParam = new JSONObject(args[0]);
                    callback = reqParam.optString(KEY_CALLBACK);

                    ArrayList<String> imgUrls = new ArrayList<String>();
                    imgUrls.add(reqParam.optString("imgUrl"));
                    bundle.putString(QzoneShare.SHARE_TO_QQ_TITLE, reqParam.optString("title"));
                    bundle.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imgUrls);
                    bundle.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, reqParam.optString("link"));
                    bundle.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, reqParam.optString("desc"));

                    this.shareToQzone(bundle, new IUiListener() {
                        @Override
                        public void onComplete(Object o) {
                            try {
                                JSONObject rspParam = new JSONObject();
                                callJs(callback, getResult(rspParam));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            LogUtils.d(TAG, " shareToQzone onComplete");
                        }

                        @Override
                        public void onError(UiError uiError) {
                            try {
                                JSONObject rspParam = new JSONObject();
                                callJs(callback, getResult(SHARE_OTHER_ERROR, "", rspParam));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            LogUtils.d(TAG, " shareToQzone onError");
                        }

                        @Override
                        public void onCancel() {
                            try {
                                JSONObject rspParam = new JSONObject();
                                callJs(callback, getResult(USER_CANCEL_SHARE, "用户取消分享", rspParam));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, APPID_TENCENT);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    reqParam = new JSONObject(args[0]);
                    callback = reqParam.optString(KEY_CALLBACK);
                    callJs(callback, getResult(QQ_NOT_EXIST, "没安装QQ", new JSONObject()));
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private void shareQQ(Bundle bundle, String[] args) {
        if (args.length == 1) {
            JSONObject reqParam;
            final String callback;
            try {
                reqParam = new JSONObject(args[0]);
                callback = reqParam.optString(KEY_CALLBACK);
                bundle.putString(QQShare.SHARE_TO_QQ_TITLE, reqParam.optString("title"));
                bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, reqParam.optString("imgUrl"));
                bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, reqParam.optString("link"));
                bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, reqParam.optString("desc"));

                if (this.checkInstallApp(PACKAGE_NAME_QQ)) {
                    this.shareToQQ(bundle, new IUiListener() {
                        @Override
                        public void onComplete(Object o) {
                            try {
                                JSONObject rspParam = new JSONObject();
                                callJs(callback, getResult(rspParam));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            LogUtils.d(TAG, " shareToQQ onComplete");
                        }

                        @Override
                        public void onError(UiError uiError) {
                            try {
                                JSONObject rspParam = new JSONObject();
                                callJs(callback, getResult(SHARE_OTHER_ERROR, "", rspParam));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            LogUtils.d(TAG, " shareToQQ onError");
                        }

                        @Override
                        public void onCancel() {
                            try {
                                JSONObject rspParam = new JSONObject();
                                callJs(callback, getResult(USER_CANCEL_SHARE, "用户取消分享", rspParam));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            LogUtils.d(TAG, " shareToQQ onCancel");
                        }
                    }, APPID_TENCENT);
                } else {
                    // Toast.makeText(mRuntime.context,"没装QQ哦",Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject rspParam = new JSONObject();
                        callJs(callback, getResult(QQ_NOT_EXIST, "没安装QQ", rspParam));
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检测APP是否安装
     */
    public boolean checkInstallApp(String packageName) {
        boolean result = false;
        PackageInfo pi = null;
        try {
            pi = mRuntime.context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, "NameNotFoundException: check " + packageName + " error");
        }
        if (pi != null) {
            String version = pi.versionName;
            Pattern p = Pattern.compile("\\.");
            String[] arr = p.split(version);
            if (arr != null && arr.length >= 2) {
                int mainVersion = NumberUtils.IntegerValueOf(arr[0]);
                int subVersion = NumberUtils.IntegerValueOf(arr[1]);
                if (mainVersion > 4 || mainVersion == 4 && subVersion >= 1) {
                    result = true;
                }
            }
        }
        return result;
    }

    public Tencent getTencent(String appId, Context context) {
        if (mTencent == null) {
            mTencent = Tencent.createInstance(appId, context);
        }
        return mTencent;
    }

    /**
     * 分享到手Q
     */
    public void shareToQQ(Bundle bundle, IUiListener iUiListener, String appId) {
        Tencent tencent = getTencent(appId, mRuntime.getActivity());
        try {
            if (bundle == null) {
                bundle = new Bundle();
            }
            if (TextUtils.isEmpty(bundle.getString(QQShare.SHARE_TO_QQ_TITLE))) {
                bundle.putString(QQShare.SHARE_TO_QQ_TITLE, "HybridSDK");
            }
            bundle.putString(QQShare.SHARE_TO_QQ_SITE, "HybridSDK");
            bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, "HybridSDK");
            tencent.shareToQQ(mRuntime.getActivity(), bundle, iUiListener);
        } catch (Exception e) {
            LogUtils.e(TAG, "shareToQQ exception");
        } catch (Error e) {
            LogUtils.e(TAG, "shareToQQ error");
        }
    }

    /**
     * 分享到Qzone（结合版接口）
     */
    public void shareToQzone(Bundle bundle, IUiListener iUiListener, String appId) {

        Tencent tencent = getTencent(appId, mRuntime.getActivity().getApplicationContext());
        try {
            if (bundle == null) {
                bundle = new Bundle();
            }
            if (TextUtils.isEmpty(bundle.getString(QzoneShare.SHARE_TO_QQ_TITLE))) {
                bundle.putString(QzoneShare.SHARE_TO_QQ_TITLE, "HybridSDK");
            }
            bundle.putString(QzoneShare.SHARE_TO_QQ_SITE, "HybridSDK");
            bundle.putString(QzoneShare.SHARE_TO_QQ_APP_NAME, "HybridSDK");
            tencent.shareToQzone(mRuntime.getActivity(), bundle, iUiListener);
        } catch (Exception e) {
            LogUtils.e(TAG, "shareToQzone exception");
        } catch (Error e) {
            LogUtils.e(TAG, "shareToQzone error");
        }
    }

    /**
     * 分享到QQ空间（独立版接口）
     *
     * @param title    分享标题
     * @param content  分享描述
     * @param thumburl 缩微图url
     * @param url      分享网页链接
     * @param appid    分享来源应用的ID
     */
    public void shareToQzone(String title, String content, String thumburl, String url, int appid) {

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.addCategory(Intent.CATEGORY_DEFAULT);
        shareIntent.setPackage(PACKAGE_NAME_QZONE);
        shareIntent.putExtra(ENTRANCE_FROM, ENTRANCE_FROM_SHARE);
        shareIntent.setType("image/*");

        Bundle extras = new Bundle();
        extras.putInt(KEY_SHARE_APPID, appid);
        extras.putString(KEY_SHARE_APPNAME, SHARE_APPNAME);
        extras.putString(KEY_SHARE_TITLE, title);
        extras.putString(KEY_SHARE_CONTENT, content);
        extras.putString(KEY_SHARE_THUMB, thumburl);
        extras.putString(Intent.EXTRA_SUBJECT, url);
        shareIntent.putExtras(extras);

        mRuntime.getActivity().startActivity(shareIntent);
    }

    /**
     * 预拉取WX分享Icon专用线程
     */
    private class DownloadWxIconThread extends Thread {
        private String url;

        public DownloadWxIconThread(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            InputStream in = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) (new URL(url).openConnection());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];

                in = connection.getInputStream();
                for (int count; (count = in.read(buffer)) != -1; ) {
                    out.write(buffer, 0, count);
                }
                byte data[] = out.toByteArray();

                mWxBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);//
                mWxIconHandler.sendEmptyMessage(ICON_DOWNLOAD_FINISHED);

            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        if (baos.toByteArray().length / 1024 > 10) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 50, baos);//这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    private void shareToWX() {
        if (mAPI.isWXAppInstalled()) {
            int type = mWxBundle.getInt(KEY_WX_TYPE);
            switch (type) {
                case KEY_WX_TYPE_LINK:
                    shareLinkToWX();
                    break;
                case KEY_WX_TYPE_MUSIC:
                    shareMusicToWX();
                    break;
                case KEY_WX_TYPE_VIDEO:
                    shareVideoToWX();
                    break;
                default:
                    break;
            }
        } else {
            LogUtils.e(TAG, "好像没装微信哦");

        }
    }

    //网页类型分享
    private void shareLinkToWX() {
        WXWebpageObject webpageObject = new WXWebpageObject();
        String title = mWxBundle.getString(KEY_WX_TITLE);
        String summary = mWxBundle.getString(KEY_WX_SUMMARY);
        String url = mWxBundle.getString(KEY_WX_URL);
        boolean isTLScene = mWxBundle.getBoolean(KEY_WX_TL_SCENE);
//        Bitmap drawableData = mWxBundle.getParcelable(KEY_WX_DRAWABLE);

        webpageObject.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage();
        msg.description = summary;
        msg.title = title;
        if (mWxBitmap != null) {
            msg.setThumbImage(mWxBitmap);
        }
        // TODO msg.logo
        msg.mediaObject = webpageObject;
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        if (isTLScene) {
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
        }

        mAPI.sendReq(req);
    }

    //音乐类型分享
    private void shareMusicToWX() {
        WXMusicObject musicObject = new WXMusicObject();
        String title = mWxBundle.getString(KEY_WX_TITLE);
        String summary = mWxBundle.getString(KEY_WX_SUMMARY);
        boolean isTLScene = mWxBundle.getBoolean(KEY_WX_TL_SCENE);

        musicObject.musicUrl = mWxBundle.getString(KEY_WX_DATA_URL);
        WXMediaMessage msg = new WXMediaMessage();
        msg.description = summary;
        msg.title = title;
        if (mWxBitmap != null) {
            msg.setThumbImage(mWxBitmap);
        }
        msg.mediaObject = musicObject;

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("music");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        if (isTLScene) {
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
        }

        mAPI.sendReq(req);

    }

    //视频类型分享
    private void shareVideoToWX() {
        WXVideoObject videoObject = new WXVideoObject();
        videoObject.videoUrl = mWxBundle.getString(KEY_WX_DATA_URL);

        String title = mWxBundle.getString(KEY_WX_TITLE);
        String summary = mWxBundle.getString(KEY_WX_SUMMARY);
        boolean isTLScene = mWxBundle.getBoolean(KEY_WX_TL_SCENE);

        WXMediaMessage msg = new WXMediaMessage();
        msg.description = summary;
        msg.title = title;
        if (mWxBitmap != null) {
            msg.setThumbImage(mWxBitmap);
        }
        msg.mediaObject = videoObject;

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("video");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        if (isTLScene) {
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
        }

        mAPI.sendReq(req);
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWxIconHandler != null) {
            mWxIconHandler.removeCallbacksAndMessages(null);
        }
        if (mTencent != null) {
            mTencent.releaseResource();
        }
    }

    private WxShareManager.WXShareCallback mWXShareCallback = new WxShareManager.WXShareCallback() {
        @Override
        public void onShareSuccess() {
            String callback = getShareWXCallback();
            if (!TextUtils.isEmpty(callback)) {
                try {
                    JSONObject resp = new JSONObject();
                    callJs(callback, getResult(resp));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onShareFail(int errorCode, String msg) {
            String callback = getShareWXCallback();
            if (!TextUtils.isEmpty(callback)) {
                try {
                    JSONObject resp = new JSONObject();
                    int errCode = SHARE_OTHER_ERROR;
                    if (TextUtils.isEmpty(msg)) {
                        msg = "";
                    }
                    if (errorCode == BaseResp.ErrCode.ERR_USER_CANCEL) {
                        errCode = USER_CANCEL_SHARE;
                        msg = "用户取消分享";
                    }
                    callJs(callback, getResult(errCode, msg, resp));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private String getShareWXCallback() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mRuntime.context);
        String callback = preferences.getString(KEY_SHARE_TO_WX_CALLBACK, "");
        PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                .remove(KEY_SHARE_TO_WX_CALLBACK)
                .commit();
        return callback;
    }


    @Override
    public void onActivityResult(Intent intent, byte requestCode, int resultCode) {
        if (mTencent != null) {
            mTencent.onActivityResult(requestCode, resultCode, intent);
        }
    }
}
