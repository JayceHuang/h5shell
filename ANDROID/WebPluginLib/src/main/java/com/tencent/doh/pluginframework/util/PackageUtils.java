package com.tencent.doh.pluginframework.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.tencent.doh.pluginframework.Config;

/**
 * 
 * @author rongodzhang
 *
 */
public class PackageUtils {
	
	private static final String CURRENT_UIN = "current_uin";
    private static String TAG = "PackageUtils";


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
			} catch (Exception e) {
			}
		}
		return pkgInfo;
	}






	/**
	 * 判断应用是否安装
	 * @param context
	 * @param pkgName 包名
	 * @return
	 */
	public static boolean isAppInstalled(Context context, String pkgName) {
	    if (TextUtils.isEmpty(pkgName)) { // zivon add, 如果传入的包名为空, 则视同没有安装
	        return false;
	    }
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(pkgName.trim(), 0);
			if (pi == null) {
				return false;
			}

		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * 判断应用是否安装.返回版本号
	 * @param context
	 * @param pkgName 包名
	 * @return
	 */
	public static String checkAppInstalled(Context context, String pkgName) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(pkgName.trim(), 0);
			if (pi == null) {
				return "0";
			}
			return pi.versionName;

		} catch (Exception e) {
			return "0";
		} 
	}
	
	
	/**
	 * 批量判断应用是否安装 返回版本号
	 * js接口测试传入字符串数组无效，改用|分隔，返回0表示没安装
	 * @param context
	 * @param arrayStr
	 * @return
	 */
	public static String checkAppInstalledBatch(Context context, String arrayStr) {
		if (arrayStr == null) {
			return "0";
		}
		PackageManager pm = context.getPackageManager();
		String[] array = arrayStr.split("\\|");
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i ++) {
			if (i != 0) {
				buffer.append("|");
			}
			try {
				PackageInfo pi = pm.getPackageInfo(array[i].trim(), 0);
				if (pi == null) {
					buffer.append(0);
				} else {
					buffer.append(pi.versionName);
				}
				
			} catch (Exception e) {
				buffer.append(0);
			}
		}
		return buffer.toString();
	}
	
	
	
	/**
	 * 批量判断应用是否安装
	 * js接口测试传入字符串数组无效，改用|分隔，返回0表示没安装
	 * @param context
	 * @param arrayStr
	 * @return
	 */
	public static String isAppInstalledBatch(Context context, String arrayStr) {
		if (arrayStr == null) {
			return "0";
		}
		PackageManager pm = context.getPackageManager();
		String[] array = arrayStr.split("\\|");
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i ++) {
			if (i != 0) {
				buffer.append("|");
			}
			try {
				PackageInfo pi = pm.getPackageInfo(array[i].trim(), 0);
				if (pi == null) {
					buffer.append(0);
				} else {
					buffer.append(1);
				}
				
			} catch (Exception e) {
				buffer.append(0);
			}
		}
		return buffer.toString();
	}
	
	/**
	 * 启动app
	 * @param context
	 * @param pkgName
	 * @return
	 */
	public static boolean startAppWithPkgName(Context context, String pkgName, String uin){
		try {
				LogUtils.d(TAG, "<--startAppWithPkgName pkgName="+pkgName+",openid="+uin);
//            //启动app上报 by pricezhang
//			try{
//			    QQAppInterface app;
//			    app = ((BaseActivity)context).app;
//			    StartAppObserverHandler startAppHandler = (StartAppObserverHandler)app.getBusinessHandler(QQAppInterface.STARTAPPOBSERVER_HANDLER);
//				startAppHandler.SendStartedAppInfo(pkgName.trim());
//			}catch(Exception e){
//				QLog.d(StartAppObserverHandler.TAG, QLog.CLR, "<-- AppStartedObserver Failed!");
//			}
			
			Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName.trim());
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (uin != null && uin.length() > 4) {
				intent.putExtra(CURRENT_UIN, uin);
			}
			intent.putExtra("platformId", "qq_m");
			context.startActivity(intent);
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	/**
	 * 批量判断应用是否安装 返回版本号 js接口测试传入字符串数组无效，改用|分隔，返回0表示没安装 jlin
	 * 
	 * @param context
	 * @param arrayStr
	 * @return
	 */
	public static String getAppsVerionCodeBatch(Context context, String arrayStr) {
		if (arrayStr == null) {
			return "0";
		}
		PackageManager pm = context.getPackageManager();
		String[] array = arrayStr.split("\\|");
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			if (i != 0) {
				buffer.append("|");
			}
			try {
				PackageInfo pi = pm.getPackageInfo(array[i].trim(), 0);
				if (pi == null) {
					buffer.append(0); // 表示未安装的
				} else {
					buffer.append(pi.versionCode);
				}

			} catch (Exception e) {
				buffer.append(0);
			}
		}
		return buffer.toString();
	}
}
