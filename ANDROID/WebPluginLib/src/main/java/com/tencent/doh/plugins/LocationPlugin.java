package com.tencent.doh.plugins;

import android.content.Intent;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.ui.activity.LocationActivity;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shibinhuang on 2016/8/23.
 */
public class LocationPlugin extends WebViewPlugin implements
        TencentLocationListener  {
    private static long sLastLocationTime = 0;

    private  long mStartTime;
    private String mCallBack;
    private  double mLongitude = 0;//经度
    private  double mLatitude = 0;//纬度
    private float mAccuracy = 0;//精确度
    private float mSpeed = 0;//速度

    private String mTencentMapKey = "UITBZ-DY7WF-ZKPJR-JN7ZP-JQNX3-S2F7P";

    private TencentLocationManager mLocationManager;

    @Override
    protected void onCreate() {
        super.onCreate();
         mLocationManager = TencentLocationManager.getInstance(mRuntime.getContext());
         mLocationManager.setCoordinateType(TencentLocationManager.COORDINATE_TYPE_WGS84);
        mLocationManager.setKey(mTencentMapKey);
    }

    @Override
    protected void onDestroy() {
       super.onDestroy();
        mLocationManager.removeUpdates(this);
    }

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        LogUtils.i(""," LocationPlugin handleJsRequest = " + method);
        if ("getLocation".equals(method)) {
            //TODO:根据参数2设置type
            getLocation(args );
        }else if("openLocation".equals(method)){
             openLocation(args );
        }else {
            return false;
        }
        return  true;
    }

    private void openLocation(String[] args){
        if (mRuntime.getActivity() == null){
            return;
        }
        JSONObject reqParam;
        final String callback;
        try {
            reqParam = new JSONObject(args[0]);
            callback = reqParam.optString(KEY_CALLBACK);
            double latitude = reqParam.getDouble("latitude");
            double longitude = reqParam.getDouble("longitude");
            String name = reqParam.getString("name");
            int scale = reqParam.getInt("scale");
            String address = reqParam.getString("address");
            startLocationActivity(latitude,longitude,scale,name,address);
        }catch (Exception e){
            startLocationActivity(39.9,116.4,9,"天安门广场","北京市东城区东长安街");
        }
    }

    private void startLocationActivity(double latitude,double longitude,int scale,String name,String address){
        Intent intent = new Intent(mRuntime.getActivity(), LocationActivity.class);
        intent.putExtra(LocationActivity.LATITUDE, latitude);
        intent.putExtra(LocationActivity.LONGITUDE, longitude);
        intent.putExtra(LocationActivity.SCALE, scale);
        intent.putExtra(LocationActivity.NAME,name);
        intent.putExtra(LocationActivity.ADDRESS,address);
        startActivityForResult(intent, LOCATION);
    }

    public final static byte LOCATION = 0;

    private void getLocation(String[] args) {
        try {
            JSONObject json = new JSONObject(args[0]);
            mCallBack = json.getString("callback");
            long allowCacheTime = json.optLong("allowCacheTime", 0) * 1000;
            long current = System.currentTimeMillis();
            if (current - sLastLocationTime < allowCacheTime) {
                //可以用缓存
                rspJs();
                return;
            }

            final TencentLocationRequest req = TencentLocationRequest.create();
            req.setInterval(3000);   //3s回调一次
            req.setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_POI);
            req.setAllowCache(true); // 都设置cache为true
            mStartTime = System.currentTimeMillis();
            mLocationManager.requestLocationUpdates(req, this );//請求定位
        } catch (JSONException e) {
            LogUtils.d(TAG, "getLocation json parse error");
            e.printStackTrace();
        }
    }

    private void rspJs()  {
        try {
        JSONObject res = new JSONObject();
        res.put("latitude", mLatitude);
        res.put("longitude", mLongitude);
        res.put("speed", mSpeed);
        res.put("accuracy", mAccuracy);
        callJs(mCallBack, getResult(res));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onLocationChanged(TencentLocation location, int err, String s) {
        if (err == TencentLocation.ERROR_OK) {
            synchronized (LocationPlugin.class) {
                mLongitude = location.getLongitude();
                mLatitude = location.getLatitude();
                mAccuracy =    location.getAccuracy();
                mSpeed = location.getSpeed();
                sLastLocationTime = System.currentTimeMillis();
            }
            rspJs();
        } else if (System.currentTimeMillis() - mStartTime < 10000) {
            return;
        } else {
            rspJs();
        }
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onStatusUpdate(String s, int i, String s1) {

    }
}
