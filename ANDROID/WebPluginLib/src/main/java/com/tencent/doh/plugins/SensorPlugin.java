package com.tencent.doh.plugins;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.util.Utils;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.pluginframework.util.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by ippan on 2015/2/5.
 */
public class SensorPlugin extends WebViewPlugin {

    private static long sLastLocationTime = 0;
    private static double sLastLongitude = 0;
    private static double sLastLatitude = 0;

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("vibrate".equals(method)   ) {
            vibrate(args );
        } else if ("startAccelerometer".equals(method)) {
            startAccelerometer(args);
        } else if ("stopAccelerometer".equals(method) ) {
            if(TextUtils.equals(args[0], NULL_PARAM)) {
                stopAccelerometer();
            }
        } else if ("startCompass".equals(method)   ) {
            startCompass(args);
        } else if ("stopCompass".equals(method)   ) {
            if(args.length == 0){
                stopCompass();
            }
        } else if ("getLocation".equals(method)   ) {
            getLocation(args );
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        stopAccelerometer();
        stopCompass();
    }

    protected final int HANDLER_LISTEN = 0x123;

    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == HANDLER_LISTEN) {
                updateMicStatus((String) msg.obj);
            }
        }
    };


    protected MediaRecorder mMediaRecorder;
    protected int SPACE = 100;// 间隔取样时间

    protected void updateMicStatus(String callback) {
        if (mMediaRecorder != null) {
            int max = mMediaRecorder.getMaxAmplitude();
            int db = (int) (20 * Math.log10(max));
            if (!TextUtils.isEmpty(callback)) {
                callJs(callback, "true", Integer.toString(db));
                Message msg2 = new Message();
                msg2.what = HANDLER_LISTEN;
                msg2.obj = callback;
                mHandler.sendMessageDelayed(msg2, SPACE);
            }
        }
    }


    /**
     * 手机震动
     *
     * @param args[0] 震动持续时间
     */
    final void vibrate(String[] args  ) {
        if(args.length == 1){
            String milliseconds = args[0];
        if (TextUtils.isEmpty(milliseconds)) return;
        long m = 0;

        try {
            m = new JSONObject(milliseconds).optLong("time");
        } catch (Exception e) {
            LogUtils.d(TAG, "vibrate json parse error");
            e.printStackTrace();
        }

        if (m > 0) {
            Vibrator vib = (Vibrator) mRuntime.context.getSystemService(Service.VIBRATOR_SERVICE);
            if (vib == null) return;
            vib.vibrate(m);
        }
        }
    }

    ////////////////////传感器相关 ///////////////////////////////
    protected final byte SENSOR_TYPE_ACCELEROMETER = 0;
    protected final byte SENSOR_TYPE_LIGHT = 1;
    protected final byte SENSOR_TYPE_DIRECTION = 2;
    protected final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;
    protected SensorManager mSensorManager;
    protected QQSensorEventListener mAccelerometerListener;

    /**
     * 开始获取三个方向的重力加速度
     *
     * @param callBack
     * @return
     */
    final void startAccelerometer(String[] args) {
        if (args.length == 1) {
            final String callBack = Utils.getCallbackName(args[0]);
            if (mSensorManager == null) {
                mSensorManager = (SensorManager) mRuntime.context.getSystemService(Context.SENSOR_SERVICE);
            }
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (sensors.size() > 0) {
                Sensor sensor = sensors.get(0);
                if (mAccelerometerListener != null) {
                    stopAccelerometer();
                }
                mAccelerometerListener = new QQSensorEventListener(SENSOR_TYPE_ACCELEROMETER, callBack);
                mSensorManager.registerListener(mAccelerometerListener, sensor, SENSOR_DELAY);
            } else {
                callJs(callBack, "false");
            }
        }
    }

    final void stopAccelerometer( ) {
           if (mSensorManager != null && mAccelerometerListener != null) {
               mSensorManager.unregisterListener(mAccelerometerListener);
               mAccelerometerListener = null;
           }
    }


    /**
     * 传感器listener统一封装, 一切为了放法数 = =
     */
    class QQSensorEventListener implements SensorEventListener {
        protected byte mSensorType;
        protected String mCallBack;
        private JSONObject accData;

        public QQSensorEventListener(byte sensorType, String callBack) {
            mSensorType = sensorType;
            mCallBack = callBack;
            accData = new JSONObject();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //(window.mqq && mqq.execGlobalCallback).apply(window, [\"2\", {\"retcode\" : 0, \"msg\" : \"\", \"data\" : {}}]);
            switch (mSensorType) {
                case SENSOR_TYPE_ACCELEROMETER:
                    try {
                        accData.put("x", event.values[0]);
                        accData.put("y", event.values[1]);
                        accData.put("z", event.values[2]);
                        callJs(mCallBack, getResult(accData));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case SENSOR_TYPE_LIGHT:
                    float lux = event.values[0];
                    callJs(mCallBack, String.valueOf(true), String.valueOf(lux));
                    break;
                case SENSOR_TYPE_DIRECTION:
                    float x = event.values[0];
                    callJs(mCallBack, String.valueOf(true), String.valueOf(x));
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }


    protected QQSensorEventListener mDirectionListener;

    /**
     * 获取手机方向
     * 返回值  float  东（90） 南（180） 西（270） 北（360）
     *
     * @param callBack
     * @return
     */
    final void startCompass(String[] args  ) {
        if(args.length == 1){
            String  callBack =    Utils.getCallbackName(args[0]);
            if (mSensorManager == null) {
                mSensorManager = (SensorManager) mRuntime.context.getSystemService(Context.SENSOR_SERVICE);
            }
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
            if (sensors.size() > 0) {
                Sensor sensor = sensors.get(0);
                if (mDirectionListener != null) {
                    stopCompass();
                }
                mDirectionListener = new QQSensorEventListener(SENSOR_TYPE_DIRECTION, callBack);
                mSensorManager.registerListener(mDirectionListener, sensor, SENSOR_DELAY);
            } else {
                callJs(callBack, "false");
            }
        }
    }

    void stopCompass() {
        if (mSensorManager != null && mDirectionListener != null) {
            mSensorManager.unregisterListener(mDirectionListener);
            mDirectionListener = null;
        }
    }

    LocationManager mLocationManager;

    class StatusLocationListener implements LocationListener {
        boolean mFinished = false;
        String mCallback;

        public StatusLocationListener(String callback) {
            mCallback = callback;
        }

        @Override
        public void onLocationChanged(Location location) {
            double lon = location.getLongitude(), lat = location.getLatitude();
            callJs(mCallback, "0", Double.toString(lon), Double.toString(lat));
            mLocationManager.removeUpdates(this);
            mFinished = true;
            synchronized (SensorPlugin.class) {
                sLastLongitude = lon;
                sLastLatitude = lat;
                sLastLocationTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

    void getLocation(String[] args  ) {
        if(args.length == 1){
            String param = args[0];

        try {
            JSONObject json = new JSONObject(param);
            final String callback = json.getString("callback");
            long allowCacheTime = json.optLong("allowCacheTime", 0) * 1000; //JS接口的单位是秒, JAVA的单位是毫秒
            long current = System.currentTimeMillis();
            if (current - sLastLocationTime < allowCacheTime) { //可以用缓存
                double lon, lat;
                synchronized (SensorPlugin.class) {
                    lon = sLastLongitude;
                    lat = sLastLatitude;
                }
                JSONObject res = new JSONObject();
                res.put("latitude", Double.toString(lat));
                res.put("longitude", Double.toString(lon));
                callJs(callback, getResult(res));
                return;
            }

            if (mLocationManager == null) {
                mLocationManager = (LocationManager) mRuntime.context.getSystemService(Context.LOCATION_SERVICE);
            }
            final StatusLocationListener listener = new StatusLocationListener(callback);
            boolean hasProvider = false;
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
                hasProvider = true;
            }
            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
                hasProvider = true;
            }
            if (!hasProvider) {

                JSONObject res = new JSONObject();
                res.put("latitude", "0");
                res.put("longitude", "0");
                callJs(callback, getResult(RET_CODE_FAIL, "", res));

            } else {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!listener.mFinished) {
                            mLocationManager.removeUpdates(listener);
                            try {
                                JSONObject res = new JSONObject();
                                res.put("latitude", "0");
                                res.put("longitude", "0");
                                callJs(callback, getResult(RET_CODE_FAIL, "", res));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 10000);
            }
        } catch (JSONException e) {
            LogUtils.d(TAG, "getLocation json parse error");
            e.printStackTrace();
        }
    }
    }
}
