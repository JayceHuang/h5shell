package com.tencent.doh.plugins.xgpush.receiver;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.android.tpush.XGPushBaseReceiver;
import com.tencent.android.tpush.XGPushClickedResult;
import com.tencent.android.tpush.XGPushRegisterResult;
import com.tencent.android.tpush.XGPushShowedResult;
import com.tencent.android.tpush.XGPushTextMessage;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.plugins.XgPushPlugin;

public class DohPushReceiver extends XGPushBaseReceiver {
	public static final String LogTag = "TPushReceiver";

	// 通知展示
	@Override
	public void onNotifactionShowedResult(Context context,
			XGPushShowedResult notifiShowedRlt) {
		if (context == null || notifiShowedRlt == null) {
			return;
		}
		LogUtils.d(LogTag, "onNotifactionShowedResult-----> msg = " + notifiShowedRlt.toString());

		Intent intent = new Intent(XgPushPlugin.ACTION_SET_LISTENER_SHOW);
		Bundle bundle = new Bundle();
		bundle.putInt(XgPushPlugin.KEY_CODE, 0);
		JSONObject data = new JSONObject();
		try {
			data.put("title", notifiShowedRlt.getTitle());
			data.put("content", notifiShowedRlt.getContent());
			data.put("customContent", notifiShowedRlt.getCustomContent());
			data.put("msgid", notifiShowedRlt.getMsgId());
			data.put("activity", notifiShowedRlt.getActivity());
			data.put("actionType", notifiShowedRlt.getNotificationActionType());
			bundle.putString(XgPushPlugin.KEY_DATA, data.toString());

			intent.putExtra(XgPushPlugin.KEY_RESULT, bundle);
			context.sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnregisterResult(Context context, int errorCode) {
		if (context == null) {
			return;
		}
		LogUtils.d(LogTag, "onUnregisterResult-------->errorCode = " + errorCode);
		
		Intent intent = new Intent(XgPushPlugin.ACTION_UNREGISTER_PUSH);
		Bundle bundle = new Bundle();
		bundle.putInt(XgPushPlugin.KEY_CODE, errorCode);
		if (errorCode != XGPushBaseReceiver.SUCCESS) {
			bundle.putString(XgPushPlugin.KEY_MSG, "unregister push fail");
		}
		intent.putExtra(XgPushPlugin.KEY_RESULT, bundle);
		context.sendBroadcast(intent);
	}

	@Override
	public void onSetTagResult(Context context, int errorCode, String tagName) {
		if (context == null) {
			return;
		}
		LogUtils.d(LogTag, "onSetTagResult-------->tag = " + tagName + ", errorCode = " + errorCode);

		Intent intent = new Intent(XgPushPlugin.ACTION_SET_TAG);
		Bundle bundle = new Bundle();
		bundle.putInt(XgPushPlugin.KEY_CODE, errorCode);
		JSONObject data = new JSONObject();
		try {
			if (errorCode != XGPushBaseReceiver.SUCCESS) {
				bundle.putString(XgPushPlugin.KEY_MSG, "set tag fail");
			}
			data.put("tag", tagName);
			bundle.putString(XgPushPlugin.KEY_DATA, data.toString());
			intent.putExtra(XgPushPlugin.KEY_RESULT, bundle);
			context.sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDeleteTagResult(Context context, int errorCode, String tagName) {
		if (context == null) {
			return;
		}
		LogUtils.d(LogTag, "onDeleteTagResult-------->tag = " + tagName + ", errorCode = " + errorCode);
		
		Intent intent = new Intent(XgPushPlugin.ACTION_DEL_TAG);
		Bundle bundle = new Bundle();
		bundle.putInt(XgPushPlugin.KEY_CODE, errorCode);
		JSONObject data = new JSONObject();
		try {
			if (errorCode != XGPushBaseReceiver.SUCCESS) {
				bundle.putString(XgPushPlugin.KEY_MSG, "delete tag fail");
			}
			data.put("tag", tagName);
			bundle.putString(XgPushPlugin.KEY_DATA, data.toString());
			intent.putExtra(XgPushPlugin.KEY_RESULT, bundle);
			context.sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 通知点击回调 actionType=1为该消息被清除，actionType=0为该消息被点击
	@Override
	public void onNotifactionClickedResult(Context context,
			XGPushClickedResult message) {
		if (context == null || message == null) {
			return;
		}
		String text = "";
		Intent intent = new Intent();
		if (message.getActionType() == XGPushClickedResult.NOTIFACTION_CLICKED_TYPE) {
			// 通知在通知栏被点击啦。。。。。
			// APP自己处理点击的相关动作
			text = "通知被打开 :" + message;
			intent.setAction(XgPushPlugin.ACTION_SET_LISTENER_CLICK);
		} else if (message.getActionType() == XGPushClickedResult.NOTIFACTION_DELETED_TYPE) {
			// 通知被清除啦。。。。
			// APP自己处理通知被清除后的相关动作
			text = "通知被清除 :" + message;
			intent.setAction(XgPushPlugin.ACTION_SET_LISTENER_CLEAR);
		}
		LogUtils.d(LogTag, "onNotifactionClickedResult----> msg = " + text);
		
		Bundle bundle = new Bundle();
		bundle.putInt(XgPushPlugin.KEY_CODE, 0);
		JSONObject data = new JSONObject();
		try {
			data.put("title", message.getTitle());
			data.put("content", message.getContent());
			data.put("customContent", message.getCustomContent());
			data.put("msgid", message.getMsgId());
			data.put("activity", message.getActivityName());
			data.put("actionType", message.getNotificationActionType());
			bundle.putString(XgPushPlugin.KEY_DATA, data.toString());

			intent.putExtra(XgPushPlugin.KEY_RESULT, bundle);
			context.sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRegisterResult(Context context, int errorCode,
			XGPushRegisterResult message) {
	}

	// 消息透传
	@Override
	public void onTextMessage(Context context, XGPushTextMessage message) {
		// TODO Auto-generated method stub
		String text = "收到消息:" + message.toString();
		LogUtils.d(LogTag, "onTextMessage----> message = " + text);
		
		Intent intent = new Intent(XgPushPlugin.ACTION_SET_LISTENER_MSG);
		Bundle bundle = new Bundle();
		bundle.putInt(XgPushPlugin.KEY_CODE, 0);
		JSONObject data = new JSONObject();
		try {
			data.put("title", message.getTitle());
			data.put("content", message.getContent());
			data.put("customContent", message.getCustomContent());
			bundle.putString(XgPushPlugin.KEY_DATA, data.toString());
			
			intent.putExtra(XgPushPlugin.KEY_RESULT, bundle);
			context.sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
