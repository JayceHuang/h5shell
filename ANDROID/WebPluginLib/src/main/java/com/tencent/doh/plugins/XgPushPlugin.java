package com.tencent.doh.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGLocalMessage;
import com.tencent.android.tpush.XGNotifaction;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.android.tpush.XGPushNotifactionCallback;
import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by amytwang on 2016/7/27.
 * 信鸽PUSH接口
 */
public class XgPushPlugin extends WebViewPlugin {
    private String mRegisterCallback;
    private Context mContext;
    private MessageReceiver mMsgReceiver;

    private boolean mIsRegister = false;

    public static final String KEY_CODE = "code";
    public static final String KEY_MSG = "msg";
    public static final String KEY_DATA = "data";
    public static final String KEY_RESULT = "result";
    public static final String KEY_UNREGISTER_PUSH_CALLBACK = "unregisterPushCallback";
    public static final String KEY_SET_TAG_CALLBACK = "setTagCallback";
    public static final String KEY_DEL_TAG_CALLBACK = "delTagCallback";
    public static final String KEY_ADD_LOCAL_NOTI_CALLBACK = "addLocalNotiCallback";
    public static final String KEY_SET_LISTENER_MSG_CALLBACK = "setListenerMsgCallback";
    public static final String KEY_SET_LISTENER_SHOW_CALLBACK = "setListenerShowCallback";
    public static final String KEY_SET_LISTENER_CLICK_CALLBACK = "setListenerClickCallback";
    public static final String KEY_SET_LISTENER_CLEAR_CALLBACK = "setListenerClearCallback";

    public static final String ACTION_UNREGISTER_PUSH = "com.tencent.doh.xgpush.UNREGISTER_CALLBACK";
    public static final String ACTION_SET_TAG = "com.tencent.doh.xgpush.SET_TAG_CALLBACK";
    public static final String ACTION_DEL_TAG = "com.tencent.doh.xgpush.DEL_TAG_CALLBACK";
    public static final String ACTION_SET_LISTENER_MSG = "com.tencent.doh.xgpush.SET_LISTENER_MSG_CALLBACK";
    public static final String ACTION_SET_LISTENER_SHOW = "com.tencent.doh.xgpush.SET_LISTENER_SHOW_CALLBACK";
    public static final String ACTION_SET_LISTENER_CLICK = "com.tencent.doh.xgpush.SET_LISTENER_CLICK_CALLBACK";
    public static final String ACTION_SET_LISTENER_CLEAR = "com.tencent.doh.xgpush.SET_LISTENER_CLEAR_CALLBACK";

    @Override
    protected void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mContext = mRuntime.getContext();
        initXgConfig();
        registerReceiver();
        XGPushManager.setNotifactionCallback(mXGNotiCallback);
    }

    private void initXgConfig(){
        XGPushConfig.setAccessId(mRuntime.getContext(),Config.getXgParameters().getAccessId());
        XGPushConfig.setAccessKey(mRuntime.getContext(),Config.getXgParameters().getAccessKey());
    }

    private void registerReceiver() {
        // 注册Callback监听器
        mMsgReceiver = new MessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UNREGISTER_PUSH);
        intentFilter.addAction(ACTION_SET_TAG);
        intentFilter.addAction(ACTION_DEL_TAG);
        intentFilter.addAction(ACTION_SET_LISTENER_MSG);
        intentFilter.addAction(ACTION_SET_LISTENER_SHOW);
        intentFilter.addAction(ACTION_SET_LISTENER_CLICK);
        intentFilter.addAction(ACTION_SET_LISTENER_CLEAR);
        mContext.registerReceiver(mMsgReceiver, intentFilter);
    }

    private void unRegisterReceiver() {
        mContext.unregisterReceiver(mMsgReceiver);
        mMsgReceiver = null;
    }

    //注册设备的
    private XGIOperateCallback mXGRegisterCallback = new XGIOperateCallback() {
        @Override
        public void onSuccess(Object data, int flag) {
            mIsRegister = true;
            try {
                JSONObject result = new JSONObject();
                result.put("token", data.toString());
                if (!TextUtils.isEmpty(mRegisterCallback)) {
                    callJs(mRegisterCallback, getResult(result));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFail(Object data, int errCode, String msg) {
            mIsRegister = false;
            try {
                if (!TextUtils.isEmpty(mRegisterCallback)) {
                    callJs(mRegisterCallback, getResult(errCode, msg, null));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    //通知在展示时会先调用这个回调，在此回调本地通知的结果
    private XGPushNotifactionCallback mXGNotiCallback = new XGPushNotifactionCallback() {
        @Override
        public void handleNotify(XGNotifaction xgNotifaction) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mRuntime.context);
            String callback = getCallback(preferences, KEY_ADD_LOCAL_NOTI_CALLBACK);
            if (!TextUtils.isEmpty(callback)) {
                JSONObject result = new JSONObject();
                try {
                    result.put("notiId", xgNotifaction.getNotifyId());
                    callJs(callback, getResult(result));
                } catch (Exception e) {
                    e.printStackTrace();
                    callJs(callback, getResult(-1, "", result));
                }
            }
            xgNotifaction.doNotify();
        }
    };

    @Override
    protected boolean handleJsRequest(String url, String pkgName,
                                      String method, String... args) {
        if ("registerPush".equals(method)) {
            registerPush(args);
        } else if ("config".equals(method)) {
            config(args);
        } else if ("unregisterPush".equals(method)) {
            unregisterPush(args);
        } else if ("setTag".equals(method)) {
            setTag(args);
        } else if ("delTag".equals(method)) {
            delTag(args);
        } else if ("addLocalNotification".equals(method)) {
            addLocalNotification(args);
        } else if ("clearLocalNotifications".equals(method)) {
            clearLocalNotifications(args);
        } else if ("cancelNotification".equals(method)) {
            cancelNotification(args);
        } else if ("setListener".equals(method)) {
            setListener(args);
        } else {
            return false;
        }
        return true;
    }

    //注册设备接口
    private void registerPush(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                mRegisterCallback = reqParam.optString(KEY_CALLBACK);
                String account = reqParam.optString("account");
                String ticket = reqParam.optString("ticket");
                int ticketType = reqParam.optInt("ticketType");

                //注册设备(3种注册方法)
                if (TextUtils.isEmpty(account)) {
                    // 注册接口
                    XGPushManager.registerPush(mContext, mXGRegisterCallback);
                } else if (TextUtils.isEmpty(ticket)) {
                    // 注册设备并绑定用户帐号
                    XGPushManager.registerPush(mContext, account, mXGRegisterCallback);
                } else {
                    // 带有登陆态的注册接口
                    XGPushManager.registerPush(mContext, account, ticket, ticketType, null, mXGRegisterCallback);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //配置接口
    private void config(String[] args) {
        if (args.length == 1) {
            String param = args[0];
            try {
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);

                //打开信鸽调试模式，true:开启，false:关闭, 上线时需要关闭
                boolean debug = reqParam.optBoolean("debug");
                XGPushConfig.enableDebug(mContext, debug);

                if (!TextUtils.isEmpty(callback)) {
                    callJs(callback, getResult(new JSONObject()));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //反注册
    private void unregisterPush(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);
                PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                        .putString(KEY_UNREGISTER_PUSH_CALLBACK, callback)
                        .commit();

                XGPushManager.unregisterPush(mContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //设置标签
    private void setTag(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);

                String tag = reqParam.optString("tag");
                if (!TextUtils.isEmpty(tag)) {
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(KEY_SET_TAG_CALLBACK, callback)
                            .commit();
                    XGPushManager.setTag(mContext, tag);
                } else {
                    try {
                        JSONObject result = new JSONObject();
                        callJs(callback, getResult(-1, "no tag", result));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //删除标签
    private void delTag(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);

                String tag = reqParam.optString("tag");
                if (!TextUtils.isEmpty(tag)) {
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(KEY_DEL_TAG_CALLBACK, callback)
                            .commit();
                    XGPushManager.deleteTag(mContext, tag);
                } else {
                    try {
                        JSONObject result = new JSONObject();
                        callJs(callback, getResult(-1, "no tag", result));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //添加本地通知
    private void addLocalNotification(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);

                if (!mIsRegister) {
                    JSONObject result = new JSONObject();
                    callJs(callback, getResult(-1, "not register", result));
                    return;
                }

                String title = reqParam.optString("title");
                String content = reqParam.optString("content");
                String date = reqParam.optString("date");
                String hour = reqParam.optString("hour");
                String min = reqParam.optString("min");
                JSONObject customContent = reqParam.optJSONObject("customContent");
                String activity = reqParam.optString("activity");
                int ring = reqParam.optInt("ring");
                int vibrate = reqParam.optInt("vibrate");
                if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(content)) {
                    //这里先记录JS回调的callback，待通知展示成功时再进行回调
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(KEY_ADD_LOCAL_NOTI_CALLBACK, callback)
                            .commit();
                    sendNotification(title, content, date, hour, min, customContent, activity, ring, vibrate);
                } else {
                    JSONObject result = new JSONObject();
                    callJs(callback, getResult(-2, "add local notification fail", result));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //删除本地通知
    private void clearLocalNotifications(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);
                XGPushManager.clearLocalNotifications(mContext);
                callJs(callback, getResult(new JSONObject()));
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //清除已展示的通知
    private void cancelNotification(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                int nid = reqParam.optInt("nid");
                String callback = reqParam.optString(KEY_CALLBACK);
                if (nid == -1) {
                    XGPushManager.cancelAllNotifaction(mContext);
                } else {
                    XGPushManager.cancelNotifaction(mContext, nid);
                }
                callJs(callback, getResult(new JSONObject()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setListener(String[] args) {
        if (args.length == 1) {
            try {
                String param = args[0];
                JSONObject reqParam = new JSONObject(param);
                String callback = reqParam.optString(KEY_CALLBACK);
                String name = reqParam.optString("name");
                String keyCallback = null;
                if (name.equals("message")) {
                    keyCallback = KEY_SET_LISTENER_MSG_CALLBACK;
                } else if (name.equals("notificationShow")) {
                    keyCallback = KEY_SET_LISTENER_SHOW_CALLBACK;
                } else if (name.equals("notificationClick")) {
                    keyCallback = KEY_SET_LISTENER_CLICK_CALLBACK;
                } else if (name.equals("notificationClear")) {
                    keyCallback = KEY_SET_LISTENER_CLEAR_CALLBACK;
                } else {
                    keyCallback = KEY_SET_LISTENER_MSG_CALLBACK;
                }

                if (!TextUtils.isEmpty(keyCallback)) {
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(keyCallback, callback)
                            .commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotification(String title, String content, String date, String hour, String min, JSONObject customContent, String activity, int ring, int vibrate) {
        XGLocalMessage message = new XGLocalMessage();
        message.setAction_type(1); //1为Activity，2为url，3为intent
        message.setTitle(title);
        message.setContent(content);
        message.setDate(date);
        message.setHour(hour);
        message.setMin(min);

        HashMap<String, String> customMap = new HashMap<String, String>();
        if (customContent != null) {
            try {
                // key为前台配置的key
                if (!customContent.isNull("key")) {
                    String value = customContent.getString("key");
                    customMap.put("key", value);
                    message.setCustomContent(customMap);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (!TextUtils.isEmpty(activity)) {
            message.setActivity(activity);
        }
        message.setRing(ring);
        message.setVibrate(vibrate);

        //发送通知
        XGPushManager.addLocalNotification(mContext, message);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterReceiver();
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            String callback = getCallback(action);
            if (TextUtils.isEmpty(callback)) {
                return;
            }

            Bundle result = intent.getBundleExtra(KEY_RESULT);
            int code = result.getInt(KEY_CODE);
            if (action.equals(ACTION_UNREGISTER_PUSH)) {
                if (code == 0) {
                    mIsRegister = false;
                }
            }
            JSONObject data = null;
            String dataString = result.getString(KEY_DATA);
            try {
                if (TextUtils.isEmpty(dataString)) {
                    data = new JSONObject();
                } else {
                    data = new JSONObject(dataString);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            String msg = result.getString(KEY_MSG);
            if (code == 0) {
                callJs(callback, getResult(data));
            } else {
                callJs(callback, getResult(code, msg, data));
            }
        }
    }

    //根据action获取callback
    private String getCallback(String action) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mRuntime.context);
        if (action.equals(ACTION_UNREGISTER_PUSH)) {
            return getCallback(preferences, KEY_UNREGISTER_PUSH_CALLBACK);
        } else if (action.equals(ACTION_SET_TAG)) {
            return getCallback(preferences, KEY_SET_TAG_CALLBACK);
        } else if (action.equals(ACTION_DEL_TAG)) {
            return getCallback(preferences, KEY_DEL_TAG_CALLBACK);
        } else if (action.equals(ACTION_SET_LISTENER_MSG)) {
            return preferences.getString(KEY_SET_LISTENER_MSG_CALLBACK, "");
        } else if (action.equals(ACTION_SET_LISTENER_SHOW)) {
            return preferences.getString(KEY_SET_LISTENER_SHOW_CALLBACK, "");
        } else if (action.equals(ACTION_SET_LISTENER_CLICK)) {
            return preferences.getString(KEY_SET_LISTENER_CLICK_CALLBACK, "");
        } else if (action.equals(ACTION_SET_LISTENER_CLEAR)) {
            return preferences.getString(KEY_SET_LISTENER_CLEAR_CALLBACK, "");
        } else {
            return null;
        }
    }

    //获取callback同时把callback移除，但setListener的callback不需要移除，后面会一直用到
    private String getCallback(SharedPreferences preferences, String key) {
        String callback = preferences.getString(key, "");
        PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                .remove(key)
                .commit();
        return callback;
    }

}
