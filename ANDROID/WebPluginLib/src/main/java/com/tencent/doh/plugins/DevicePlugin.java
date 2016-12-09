package com.tencent.doh.plugins;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.webview.WebViewPlugin;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by pelli on 2015/2/11.
 */
public class DevicePlugin extends WebViewPlugin {

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("getDeviceInfo".equals(method)) {
              getDeviceInfo(args);
        }else if("getNetworkType".equals(method)){
              getNetworkType(args);
        }else {
            return false;
        }
        return false;
    }

    private void getNetworkType(String[] args) {
      String type = getNetWorkState();
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1=new JSONObject();
                json1.put("networkType", type);
                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getDeviceInfo(String[] args){
        TelephonyManager tm = (TelephonyManager) mRuntime.getActivity().getSystemService(mRuntime.getActivity().TELEPHONY_SERVICE);
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1=new JSONObject();
                json1.put("model", android.os.Build.MANUFACTURER);
                json1.put("systemVersion", android.os.Build.VERSION.RELEASE);
                json1.put("identifier", tm.getDeviceId());
                json1.put("systemName", "android");
                json1.put("modelVersion", android.os.Build.MODEL);
                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getNetWorkState(){
        String typeStr = "notReachable";
        ConnectivityManager connectivityManager = (ConnectivityManager) mRuntime.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                switch (networkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        typeStr = "wifi";
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        int subType = networkInfo.getSubtype();
                        switch (subType) {
                            // 2G
                            case TelephonyManager.NETWORK_TYPE_GPRS:
                            case TelephonyManager.NETWORK_TYPE_EDGE:
                            case TelephonyManager.NETWORK_TYPE_CDMA:
                            case TelephonyManager.NETWORK_TYPE_1xRTT:
                            case TelephonyManager.NETWORK_TYPE_IDEN:
                                typeStr = "2g";
                                break;
                            // 3G
                            case TelephonyManager.NETWORK_TYPE_UMTS:
                            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                            case TelephonyManager.NETWORK_TYPE_HSDPA:
                            case TelephonyManager.NETWORK_TYPE_HSUPA:
                            case TelephonyManager.NETWORK_TYPE_HSPA:
                            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                            case TelephonyManager.NETWORK_TYPE_EHRPD:
                            case TelephonyManager.NETWORK_TYPE_HSPAP:
                                typeStr = "3g";
                                break;
                            // 4G
                            case TelephonyManager.NETWORK_TYPE_LTE:
                                typeStr = "4g";
                                break;
                            default:
                                typeStr = "unknown";
                                break;
                        }
                        break;
                    default:
                        typeStr = "unknown";
                        break;
                }
            }
        }
        return typeStr;
    }
}
