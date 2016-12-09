package com.tencent.doh.pluginframework.util;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.Config;


public class ApkUtils {
    
    public static boolean isAppInstalledFromSystem(String packageName) {
        if (!TextUtils.isEmpty(packageName)) {
             PackageInfo info = getInstalledPackageInfo(packageName,0);
            return null != info;
        }
        return false;
    }
    
    /**
     * 获得已安装的APK包信息
     * @param pkgName apk包名
     * @param flag 需要签名等其它信息的要指定flag, 如果只想获取安装包信息的flag建议置为0 这样可以提高访问速度
     * @return
     */
    public static PackageInfo getInstalledPackageInfo(String pkgName, int flag) {
        if (TextUtils.isEmpty(pkgName)) {
            return null;
        }

        PackageInfo pkgInfo = null;
        Context context = Config.getContext();
        if (context == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            try {
                pkgInfo = pm.getPackageInfo(pkgName, flag); // flag为0提高访问速度
            } catch (NameNotFoundException e) {
//              e.printStackTrace();
            } catch (RuntimeException e) {
//              e.printStackTrace();
            }
        }
        return pkgInfo;
    }

    public static String getApplicationName(Context context) {
        if (context == null)
            return "";
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName =
                (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }
}


