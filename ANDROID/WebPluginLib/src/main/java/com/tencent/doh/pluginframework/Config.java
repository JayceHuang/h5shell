package com.tencent.doh.pluginframework;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.MResource;
import com.tencent.doh.pluginframework.util.StorageUtils;
import com.tencent.doh.pluginframework.webview.PluginInfo;
import com.tencent.smtt.sdk.WebSettings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by amytwang on 2016/8/4.
 * modify by zxf on 2016/9/2
 */
public class Config {
    public static final String DEFAULT_FONT_JS_ID = "setFont";
    public static final String DEFAULT_REFRESH_JS_ID = "refresh";

    private static boolean mDebug = false;
    private static PluginInfo[] mPluginInfo = null;
    private static String mAuthorizeRule = null;
    private static Context mContext = null;

    private static CloudParameters mCloudParameters;
    private static WxParameters mWxParameters;
    private static XgParameters mXgParameters;

    private static String mTencentQQAppid = null;
    private static String mCacheFileDir = null; //保存文件的路徑
    private static Map<String, Integer> mJsResIdMap = new HashMap<>();//String为js传过来的key，id为本地资源
    private static int mWebTextSizeIndex = 1;//0是小，1标准，2大，3加大.暂时用数字//
    private static Map<Integer,WebSettings.TextSize> mWebTextSizeMap = new HashMap<>();

    {
        mWebTextSizeMap.put(0,WebSettings.TextSize.SMALLER);
        mWebTextSizeMap.put(1,WebSettings.TextSize.NORMAL);
        mWebTextSizeMap.put(2,WebSettings.TextSize.LARGER);
        mWebTextSizeMap.put(3,WebSettings.TextSize.LARGEST);
    }

    private static Config instance ;

    private static synchronized Config getInstance(){
        if(instance == null){
            instance = new Config();
        }
        return instance;
    }

    private Config(){
        appendDefaultResIdMap();
        mWebTextSizeIndex = getWebTextSizeIndexInternal();
        mCacheFileDir = StorageUtils.getCacheDir(mContext,"cache");
    }

    public static class Builder{
        public Builder(Context mContext){
            Config.mContext = mContext;
        }

        /**
         * 设置腾讯云参数
         * @param parameters
         * @return
         */
        public Builder setCloudParameters(CloudParameters parameters){
            Config.mCloudParameters=parameters;
            return this;
        }

        /**
         * 设置微信参数
         * @param parameters
         * @return
         */
        public Builder setWxParameters(WxParameters parameters){
            Config.mWxParameters = parameters;
            return this;
        }

        /**
         * 设置信鸽参数
         * @param parameters
         * @return
         */
        public Builder setXgParameters(XgParameters parameters){
            Config.mXgParameters = parameters;
            return this;
        }

        public Builder setResIds(Map<String, Integer> resIds) {
            Config.appendResIdMap(resIds);
            return this;
        }

        public Builder setDebug(boolean bDebug){
            Config.mDebug = bDebug;
            LogUtils.setDebug(bDebug);
            return this;
        }

        public Builder setAuthorizeRule(String authorizeRule){
            Config.mAuthorizeRule = authorizeRule;
            return this;
        }

//        public Builder setPluginInfo(PluginInfo[] pluginInfo) {
//            Config.mPluginInfo = pluginInfo;
//            return this;
//        }

        public Builder setTencentQQAppid(String qqAppid) {
            Config.mTencentQQAppid = qqAppid;
            return this;
        }

        public Config build(){
            return Config.getInstance();
        }
    }


    private void appendDefaultResIdMap() {
        if (mJsResIdMap == null)
            return;

        Context context = getContext();
        if (context == null)
            return;

        mJsResIdMap.put(DEFAULT_FONT_JS_ID, MResource.getIdByName(context, "drawable", "txwp_set_font"));
        mJsResIdMap.put(DEFAULT_REFRESH_JS_ID, MResource.getIdByName(context, "drawable", "txwp_refresh"));

    }

    private static void appendResIdMap(Map<String, Integer> jsResIdMap) {

        if (mJsResIdMap == null || jsResIdMap == null)
            return;

        int defaultResId = MResource.getIdByName(getContext(), "drawable", "txwp_mune_default");

        Iterator<Map.Entry<String, Integer>> it = jsResIdMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            String key = entry.getKey();
            int value = entry.getValue();
            if (value <= 0)
                jsResIdMap.put(key, defaultResId);
        }

        mJsResIdMap.putAll(jsResIdMap);
    }

    public static int getResId(String JsId)
    {
        int defaultResId = MResource.getIdByName(getContext(), "drawable", "txwp_mune_default");

        if (mJsResIdMap == null )
            return defaultResId;

        if (mJsResIdMap.containsKey(JsId))
            return  mJsResIdMap.get(JsId);

        return  defaultResId;
    }

    public static String getCacheFileDirPath() {
//        mCacheFileDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        return mCacheFileDir;
    }

    public static Context getContext() {
        return mContext;
    }

    public static Map<String, Integer> getJsResIdMap() {
        return mJsResIdMap;
    }

    public static int getWebTextSizeIndex() {
        return mWebTextSizeIndex;
    }

    public static boolean isDebug(){
        return mDebug;
    }

    public static String getAuthorizeRule(){
        return Config.mAuthorizeRule;
    }

    public static PluginInfo[] getPluginInfo() {
        return mPluginInfo;
    }


    public static String getTencentQQAppid() {
        return Config.mTencentQQAppid;
    }

    public static CloudParameters getCloudParameters() {
        return Config.mCloudParameters;
    }

    public static WxParameters getWxParameters() {
        return Config.mWxParameters;
    }

    public static XgParameters getXgParameters() {
        return Config.mXgParameters;
    }
    public static void setWebTextSizeIndex(int i){
        if (i < 0 || i > mWebTextSizeMap.size())
            return;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        pref.edit().putInt("web_font_size",i).commit();
        mWebTextSizeIndex = i;
    }

    private static int getWebTextSizeIndexInternal(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return  pref.getInt("web_font_size",1);
    }

    public static WebSettings.TextSize getWebTextSize(int index) {
        if (mWebTextSizeMap.containsKey(index))
            return  mWebTextSizeMap.get(index);
        return WebSettings.TextSize.NORMAL;
    }


    /**
     * 腾讯云配置参数,由腾讯云官方平台获取
     */
    public static class CloudParameters{

        private  String mCloudAppId = null;
        private  String mCloudSecretId = null;
        private  String mCloudSecretKey = null;
        private  String mFileBucket = null;
        private  String mPhotoBucket = null;

        public CloudParameters(String appId,String secretId,String secretKey,String fileBucket,String photoBucket){
            mCloudAppId = appId;
            mCloudSecretId = secretId;
            mCloudSecretKey = secretKey;
            mFileBucket = fileBucket;
            mPhotoBucket = photoBucket;
        }

        public  String getCloudAppId() {
            return mCloudAppId;
        }

        public  String getCloudSecretId() {
            return mCloudSecretId;
        }

        public  String getCloudSecretKey() {
            return mCloudSecretKey;
        }
        public  String getFileBucket() {
            return mFileBucket;
        }
        public  String getPhotoBucket() {
            return mPhotoBucket;
        }


    }


    public static class WxParameters{

        private  String mWxAppId = null; //微信相关参数,通过微信开放平台获取
        private  String mWxAppSecret = null;//暂不传入

        public WxParameters(String appId){
            mWxAppId = appId;
        }

        public  String getWxAppId() {
            return mWxAppId;
        }

        public  String getWxAppSecret() {
            return mWxAppSecret;
        }
    }

    public static class XgParameters{

        private  long  mAccessId = 0;
        private  String mAccessKey = null;

        public XgParameters(long accessId,String accessKey){
            mAccessId = accessId;
            mAccessKey = accessKey;
        }

        public  long getAccessId() {
            return mAccessId;
        }

        public  String getAccessKey() {
            return mAccessKey;
        }
    }



}
