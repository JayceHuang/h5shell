package com.tencent.doh.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by benpeng on 2016/9/20.
 */
public class SharedPreferencesUtils {
    public static final String USE_REMOTE_RES = "use_remote_res";
    public static final String IP = "ip";
    public static final String NAME = "name";
    public static void saveUseRemoteRes(Context context, boolean useRemote){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(USE_REMOTE_RES,useRemote).commit();
    }

    public static boolean getUseRemoteRes(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(USE_REMOTE_RES,true);
    }

    public static void saveIp(Context context, String ip){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(IP,ip).commit();
    }

    public static String getIp(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(IP,"10.66.199.219:9090");
    }

    public static void saveName(Context context, String name){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(NAME,name).commit();
    }

    public static String getName(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(NAME,"demo.html");
    }
}
