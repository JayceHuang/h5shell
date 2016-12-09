
package com.tencent.doh.plugins.audio;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.plugins.AudioPlugin;

import java.io.IOException;

public class RecorderService extends Service implements MediaRecorder.OnErrorListener {

    public final static String ACTION_NAME = "action_type";

    public final static int ACTION_INVALID = 0;

    public final static int ACTION_START_RECORDING = 1;

    public final static int ACTION_STOP_RECORDING = 2;

    public final static int ACTION_ENABLE_MONITOR_REMAIN_TIME = 3;

    public final static String ACTION_PARAM_PATH = "path";

    public final static String RECORDER_SERVICE_BROADCAST_NAME = "com.android.soundrecorder.broadcast";

    public final static String RECORDER_SERVICE_BROADCAST_STATE = "is_recording";

    public final static String RECORDER_SERVICE_BROADCAST_ERROR = "error_code";

    private static MediaRecorder mRecorder = null;

    private static String mFilePath = null;

    private static long mStartTime = 0;

    private RemainingTimeCalculator mRemainingTimeCalculator;

    private boolean mNeedUpdateRemainingTime;

    private TelephonyManager mTeleManager;


    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state != TelephonyManager.CALL_STATE_IDLE) {
                StopRecording();
            }
        }
    };

    private final Handler mHandler = new Handler();

    private Runnable mUpdateRemainingTime = new Runnable() {
        public void run() {
            if (mRecorder != null && mNeedUpdateRemainingTime) {
                updateRemainingTime();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mRecorder = null;
        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mNeedUpdateRemainingTime = false;
        mTeleManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTeleManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_NAME)) {
            switch (bundle.getInt(ACTION_NAME, ACTION_INVALID)) {
                case ACTION_START_RECORDING:
                    startRecording(
                            bundle.getString(ACTION_PARAM_PATH) );
                    break;
                case ACTION_STOP_RECORDING:
                    StopRecording();
                    break;
                case ACTION_ENABLE_MONITOR_REMAIN_TIME:
                    if (mRecorder != null) {
                        mNeedUpdateRemainingTime = true;
                        mHandler.post(mUpdateRemainingTime);
                    }
                    break;
                default:
                    break;
            }
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mTeleManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLowMemory() {
        StopRecording();
        super.onLowMemory();
    }

    private void startRecording(String path) {
        if (mRecorder == null) {
            mRemainingTimeCalculator.reset();
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRemainingTimeCalculator.setBitRate(AudioPlugin.BITRATE_AMR);
            mRecorder.setAudioSamplingRate(16000);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            //写入录音数据
            mRecorder.setOutputFile(path);
            mRecorder.setOnErrorListener(this);

            try {
                mRecorder.prepare();
            } catch (IOException exception) {
                sendErrorBroadcast(RecorderManager.INTERNAL_ERROR);
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                return;
            }
            try {
                mRecorder.start();
            } catch (RuntimeException exception) {
                AudioManager audioMngr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                boolean isInCall = (audioMngr.getMode() == AudioManager.MODE_IN_CALL);
                if (isInCall) {
                    sendErrorBroadcast(RecorderManager.IN_CALL_RECORD_ERROR);
                } else {
                    sendErrorBroadcast(RecorderManager.INTERNAL_ERROR);
                }
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                return;
            }
            mFilePath = path;
            mStartTime = System.currentTimeMillis();
            mNeedUpdateRemainingTime = false;
            sendStateBroadcast();
        }
    }

    private void StopRecording() {
        if (mRecorder != null) {
            mNeedUpdateRemainingTime = false;
            try {
                mRecorder.stop();
            } catch (RuntimeException e) {
            }
            mRecorder.release();
            mRecorder = null;

            sendStateBroadcast();
        }
        stopSelf();
    }

   
    private void sendStateBroadcast() {
        Intent intent = new Intent(RECORDER_SERVICE_BROADCAST_NAME);
        intent.putExtra(RECORDER_SERVICE_BROADCAST_STATE, mRecorder != null);
        sendBroadcast(intent);
    }

    private void sendErrorBroadcast(int error) {
        Intent intent = new Intent(RECORDER_SERVICE_BROADCAST_NAME);
        intent.putExtra(RECORDER_SERVICE_BROADCAST_ERROR, error);
        sendBroadcast(intent);
    }

    private void updateRemainingTime() {
        long t = mRemainingTimeCalculator.getTimeRemaining();
        LogUtils.i("updateRemainingTime", "updateRemainingTime t = " + t);
        if (t <= 0) {
            StopRecording();
            return;
        }  

        if (mRecorder != null && mNeedUpdateRemainingTime) {
            mHandler.postDelayed(mUpdateRemainingTime, 500);
        }
    }

    public static boolean isRecording() {
        return mRecorder != null;
    }

    public static String getFilePath() {
        return mFilePath;
    }

    public static long getStartTime() {
        return mStartTime;
    }

    public static void startRecording(Context context, String path ) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(ACTION_NAME, ACTION_START_RECORDING);
        intent.putExtra(ACTION_PARAM_PATH, path);
        context.startService(intent);
    }

    public static void stopRecording(Context context) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(ACTION_NAME, ACTION_STOP_RECORDING);
        context.startService(intent);
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        sendErrorBroadcast(RecorderManager.INTERNAL_ERROR);
        StopRecording();
    }
}
