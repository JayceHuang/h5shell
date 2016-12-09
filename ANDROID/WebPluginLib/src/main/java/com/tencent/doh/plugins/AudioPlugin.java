package com.tencent.doh.plugins;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.doh.pluginframework.util.FileUtils;
import com.tencent.doh.pluginframework.util.LRUCache;
import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.doh.pluginframework.util.MResource;
import com.tencent.doh.pluginframework.webview.WebViewPlugin;
import com.tencent.doh.plugins.audio.RecorderManager;
import com.tencent.doh.plugins.audio.RecorderService;
import com.tencent.doh.plugins.audio.RemainingTimeCalculator;
import com.tencent.doh.plugins.cloud.CloudManager;
import com.tencent.doh.plugins.cloud.CloudUtils;
import com.tencent.doh.plugins.cloud.DownloadManager;
import com.tencent.doh.plugins.cloud.UploadManager;
import com.tencent.download.Downloader;
import com.tencent.download.core.DownloadResult;
import com.tencent.upload.task.ITask;
import com.tencent.upload.task.IUploadTaskListener;
import com.tencent.upload.task.data.FileInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by shibinhuang on 2016/8/1.
 * 录音音频相关接口
 */
public class AudioPlugin extends WebViewPlugin implements RecorderManager.OnStateChangedListener {

    private Context mContext;

    private static final String AUDIO_AMR = "audio/amr";

    public final static String LOCAL_RES_HEADER = "doh://audio/";//头名称

    public static final String LOCAL_ID = "localId";

    public static final String SERVER_ID = "serverId";

    private static final String FILE_EXTENSION_AMR = ".amr";

    public static final int BITRATE_AMR = 2 * 1024 * 8;

    private String mRequestedType = AUDIO_AMR;

    private RecorderManager mRecorderManager;

    private RecorderReceiver mReceiver;

    private RemainingTimeCalculator mRemainingTimeCalculator;

    private String mTimerFormat;

    private Dialog mLoadingDialog;

    private BroadcastReceiver mSDCardMountEventReceiver = null;

    public static LRUCache<String, String> mAudioCache = new LRUCache<>(100);//音频列表的映射队列

    public static LRUCache<String, String> mAudioCloudCache = new LRUCache<>(100);//音频列表的上传下载队列

    private String onVoiceRecordEndcallBack =null;

    private String onVoicePlayEndcallBack =null;

    private long mLastClickTime;//上次的点击时间

    private Handler handler = new Handler();

    @Override
    protected void onCreate() {
        super.onCreate();
        initData();
        initState();
        FileUtils.deleteTimeoutFile(RecorderManager.getAudioCachePath(),3);
    }

    private void initData() {
        mContext = mRuntime.getContext();
        mRecorderManager = new RecorderManager(mContext);
        mRecorderManager.setOnStateChangedListener(this);
        mReceiver = new RecorderReceiver();
        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mTimerFormat = "%02d:%02d";
        mLastClickTime = 0;
        registerExternalStorageListener();
    }

    private void initState() {
        if (mRecorderManager.getRecordState() == RecorderManager.RECORDING_STATE) {
            String preExtension = AUDIO_AMR.equals(mRequestedType) ? FILE_EXTENSION_AMR
                    : FILE_EXTENSION_AMR;
            if (!mRecorderManager.getRecordFile().getName().endsWith(preExtension)) {
                mRecorderManager.reset();
            } else {
                if (AUDIO_AMR.equals(mRequestedType)) {
                    mRemainingTimeCalculator.setBitRate(BITRATE_AMR);
                }
            }
        } else {
            File file = mRecorderManager.getRecordFile();
            if (file != null && !file.exists()) {
                mRecorderManager.reset();
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(RecorderService.RECORDER_SERVICE_BROADCAST_NAME);
        mContext.registerReceiver(mReceiver, filter);
        updateRecordTime();
    }

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        LogUtils.i("", " handleJsRequest url = " + url + " pkgName = " + pkgName + " method = " + method + " args = " + args.length);
        //不响应多次点击
        if (System.currentTimeMillis() - mLastClickTime < 500 && !"onVoiceRecordEnd".equalsIgnoreCase(method) && !"onVoicePlayEnd".equalsIgnoreCase(method)) {
            return true;
        }
        //少于1.5秒的录音不予记录
        if ("stopRecord".equalsIgnoreCase(method)
                && System.currentTimeMillis() - mLastClickTime < 1500) {
            return true;
        }
        if ("startRecord".equalsIgnoreCase(method) ) {
            startRecord();
        } else if ("stopRecord".equalsIgnoreCase(method)) {
            stopRecord(args);
        } else if ("playVoice".equalsIgnoreCase(method) ) {
            playVoice(args);
        } else if ("pauseVoice".equalsIgnoreCase(method) ) {
            pauseVoice(args);
        } else if ("stopVoice".equalsIgnoreCase(method) ) {
            stopVoice(args);
        } else if ("uploadVoice".equalsIgnoreCase(method) ) {
            uploadVoice(args);
        } else if ("downloadVoice".equalsIgnoreCase(method) ) {
            downloadVoice(args);
        } else if ("onVoiceRecordEnd".equalsIgnoreCase(method) ) {
            registRecordEndCallBack(args);
        }else if ("onVoicePlayEnd".equalsIgnoreCase(method)&&args.length==1) {
            registVoicePlayEndCallBack(args);
        } else {
            return false;
        }
        mLastClickTime = System.currentTimeMillis();
        return true;
    }

    private void callBackJsResult(String message,String name, String value , String rsp) {
        try {
            JSONObject json = new JSONObject(rsp);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1 = new JSONObject();
                json1.put("msg", message);
                json1.put(name, value);
                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void registRecordEndCallBack(String[] args){
        if(args.length==1){
            onVoiceRecordEndcallBack=args[0];
        }
    }

    private void registVoicePlayEndCallBack(String[] args){
        if(args.length==1){
            onVoicePlayEndcallBack=args[0];
        }
    }


    private void startRecord() {
        mRemainingTimeCalculator.reset();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            stopAudioPlayback();
            if (AUDIO_AMR.equals(mRequestedType)) {
                mRemainingTimeCalculator.setBitRate(BITRATE_AMR);
                mRecorderManager.startRecording(FILE_EXTENSION_AMR);
            } else {
                throw new IllegalArgumentException(
                        "Invalid output file type requested");
            }
        } else {
            updateRecordTime();
        }
    }

    private String getReqAudioId(String[] args, String getIdName) {
        JSONObject reqParam = null;
        String audioId;
        try {
            reqParam = new JSONObject(args[0]);
            audioId = reqParam.getString(getIdName);
        } catch (JSONException e) {
            e.printStackTrace();
            audioId = "";
        }
        return audioId;
    }

    private boolean isShowDialog(String[] args){
    JSONObject reqParam = null;
    int isShowProgressTips =1;
    try {
        reqParam = new JSONObject(args[0]);
        if (reqParam.has("isShowProgressTips")) {
            isShowProgressTips = reqParam.getInt("isShowProgressTips");
        }
    } catch (JSONException e) {
        e.printStackTrace();
    }
    return isShowProgressTips == 1;
    }

    private void uploadVoice(final String[] args ) {
        if(args.length==1){

        String localId = getReqAudioId(args, LOCAL_ID);
        if (TextUtils.isEmpty(localId) || !mAudioCache.containsKey(localId)) {
            Toast.makeText(mContext, "沒有可上传的本地文件", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = mAudioCache.get(localId);
//         String path = mRecorderManager.getRecordFilePath();
        String name = mRecorderManager.getRecordFile().getName();
        if(isShowDialog(args)){
             showLoadingDialog();
        }
        UploadManager.getInstance().uploadSingleFile(CloudManager.FILE_BUKET, path, name, new IUploadTaskListener() {
            @Override
            public void onUploadSucceed(FileInfo fileInfo) {

                dismissDialog();
                String serverId = CloudUtils.md5(fileInfo.url);
                callBackJsResult("onUploadSucceed",SERVER_ID, serverId,args[0]);
                if (!mAudioCloudCache.containsKey(serverId)) {
                    mAudioCloudCache.put(serverId , fileInfo.url);
                }
            }

            @Override
            public void onUploadFailed(int i, String s) {
                dismissDialog();
                callBackJsResult("onUploadFailed", SERVER_ID,"",args[0]);
                LogUtils.i("", "onUploadFailed = " + i);
            }

            @Override
            public void onUploadProgress(long l, long l1) {

            }

            @Override
            public void onUploadStateChange(ITask.TaskState taskState) {

            }
        });
    }

    }

    private void downloadVoice(final String[] args) {
        if(args.length==1){
        String serverId = getReqAudioId(args, SERVER_ID);
        if (TextUtils.isEmpty(serverId) || !mAudioCloudCache.containsKey(serverId)) {
            Toast.makeText(mContext, "没有对应的url地址！", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = mAudioCloudCache.get(serverId);
        if(isShowDialog(args)){
            showLoadingDialog();
        }
        DownloadManager.getInstance().download(url, new Downloader.DownloadListener() {
            @Override
            public void onDownloadCanceled(String s) {
                dismissDialog();
            }

            @Override
            public void onDownloadFailed(String s, DownloadResult downloadResult) {
                LogUtils.i("", "onDownloadFailed = ");
                dismissDialog();
                callBackJsResult("onDownloadFailed", LOCAL_ID,"",args[0]);
            }

            @Override
            public void onDownloadSucceed(String s, DownloadResult downloadResult) {
                dismissDialog();
                LogUtils.i("", "onDownloadSucceed = " + downloadResult.getPath() + " temp = " + downloadResult.getTmpPath());
                String localId =   CloudUtils.md5(downloadResult.getPath());
                callBackJsResult("onDownloadSucceed", LOCAL_ID,localId,args[0]);
            }

            @Override
            public void onDownloadProgress(String s, long l, float v) {

            }
        });
        }
    }

    private void showLoadingDialog() {
        mLoadingDialog = new AlertDialog.Builder(mRuntime.getActivity()).create();
        mLoadingDialog.show();
        mLoadingDialog.setContentView(MResource.getIdByName(mRuntime.getContext(),"layout","txwp_view_loading_dialog"));
    }

    private void dismissDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }

    /**
     * 停止录音并保存文件
     */
    private void stopRecord(  String[] args) {
        if(args.length==1){
            if (null == mRecorderManager.getRecordFile()) {
                Toast.makeText(mContext, "您还没开始录音！", Toast.LENGTH_SHORT).show();
                return;
            }
            if(mRecorderManager.getRecordState()==RecorderManager.IDLE_STATE || mRecorderManager.getRecordState() == RecorderManager.PLAYING_STATE){
                //没有录音,不予反馈
                return;
            }
            String md5 = saveRecorrdFile();
            callBackJsResult("stopRecord", LOCAL_ID,md5,args[0]);
        }
    }

    private String saveRecorrdFile(){
        mRecorderManager.stop();
        String md5 = mRecorderManager.getRecordFilePathCacheMd5();
        String path = mRecorderManager.getRecordFilePath();
        if (!mAudioCache.containsKey(md5)) {
            mAudioCache.put(md5, path);//将列表存入map
        }
        return md5;
    }


    private void playVoice(String[] args) {
        if(args.length==1){
            String localId = getReqAudioId(args, LOCAL_ID);
            File file = mRecorderManager.getRecordFile();
            if (  null == file || !mAudioCache.containsKey(localId)) {
                Toast.makeText(mContext, "没有可播放的音频", Toast.LENGTH_SHORT).show();
                return;
            }
            mRecorderManager.startPlayback( localId);
        }
    }

    private void pauseVoice(String[] args) {
        if(args.length==1){
            mRecorderManager.pausePlayback();
            String audioId =   mRecorderManager.getRecordFilePathCacheMd5();
            callBackJsResult("pauseVoice", LOCAL_ID,audioId,args[0]);
        }
    }

    private void stopVoice(String[] args) {
        if(args.length==1){
            mRecorderManager.stopPlayback();
            String audioId =   mRecorderManager.getRecordFilePathCacheMd5();
            callBackJsResult("stopVoice",LOCAL_ID, audioId,args[0]);
        }
    }

   /*
    * 注册SD卡挂载的广播事件
    */
    private void registerExternalStorageListener() {
        if (mSDCardMountEventReceiver == null) {
            mSDCardMountEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mRecorderManager.reset();
                    updateRecordTime();
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme("file");
            mContext.registerReceiver(mSDCardMountEventReceiver, iFilter);
        }
    }

    private void stopAudioPlayback() {
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);
    }

    /**
     * 更新录音时间
     */
    private void updateRecordTime() {
        if(mRecorderManager!=null){
            int recordState = mRecorderManager.getRecordState();
            long time = mRecorderManager.getRecordTime();
            //录音时常控制在一分钟
            if (time == 60 && recordState == RecorderManager.RECORDING_STATE) {
                mRecorderManager.stop();
                if(onVoiceRecordEndcallBack !=null){
                    saveRecorrdFile();
                    String audioId =   mRecorderManager.getRecordFilePathCacheMd5();
                    callBackJsResult("onVoiceRecordEnd", LOCAL_ID,audioId,onVoiceRecordEndcallBack);
                    LogUtils.i("","播放状态 已经录音60秒！");
                }
                return;
            }
            String timeStr = String.format(mTimerFormat, time / 60, time % 60);
            LogUtils.i("", " now time = " + timeStr);
            boolean ongoing = recordState == RecorderManager.RECORDING_STATE
                    || recordState == RecorderManager.PLAYING_STATE;
            if (ongoing) {
                handler.postDelayed(mUpdateTimer, 500);
            }
        }else {
            LogUtils.e("","updateRecordTime manager is null!!!");
        }
    }

    /**
     * 更新时间的线程
     */
    private Runnable mUpdateTimer = new Runnable() {
        public void run() {
            updateRecordTime();
        }
    };

    //播放状态的回调
    public void onStateChangedCallBack(int state) {
        LogUtils.i("","播放状态 state = " +state);
        if (state == RecorderManager.PLAYING_STATE_COMPLETE) {
            if(onVoicePlayEndcallBack!=null){
                String audioId =   mRecorderManager.getRecordFilePathCacheMd5();
                callBackJsResult("onVoicePlayEnd", LOCAL_ID,audioId,onVoicePlayEndcallBack);
            }
            return;
        }
        updateRecordTime();
    }

    //错误码的回调
    public void onErrorCallBack(int error) {
    }

    private class RecorderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent
                    .hasExtra(RecorderService.RECORDER_SERVICE_BROADCAST_STATE)) {
                boolean isRecording = intent
                        .getBooleanExtra(
                                RecorderService.RECORDER_SERVICE_BROADCAST_STATE,
                                false);
                if(mRecorderManager!=null){
                    mRecorderManager.setState(isRecording ? RecorderManager.RECORDING_STATE
                            : RecorderManager.IDLE_STATE);
                }
            } else if (intent
                    .hasExtra(RecorderService.RECORDER_SERVICE_BROADCAST_ERROR)) {
                int error = intent.getIntExtra(
                        RecorderService.RECORDER_SERVICE_BROADCAST_ERROR, 0);
                if(mRecorderManager!=null){
                    mRecorderManager.setError(error);
                }
            }
        }
    }

   /*
    * 关闭页面反注册SD卡的广播
     */
    @Override
    public void onDestroy() {
        deleteFile();
        if (mSDCardMountEventReceiver != null) {
            mContext.unregisterReceiver(mSDCardMountEventReceiver);
            mSDCardMountEventReceiver = null;
        }
        super.onDestroy();
    }

    private void deleteFile() {
        mRecorderManager.delete();
        mRecorderManager=null;
    }
}
