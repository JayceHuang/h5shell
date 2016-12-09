
package com.tencent.doh.plugins.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Environment;

import com.tencent.doh.pluginframework.Config;
import com.tencent.doh.plugins.AudioPlugin;
import com.tencent.doh.plugins.cloud.CloudUtils;

import java.io.File;
import java.io.IOException;

public class RecorderManager implements OnCompletionListener, OnErrorListener {

    public static final String AUDIO_DIR = "/audio";//文件夹名字

    public static final int IDLE_STATE = 0;

    public static final int RECORDING_STATE = IDLE_STATE+1;

    public static final int PLAYING_STATE = RECORDING_STATE+1;//播放的状态

    public static final int PLAYING_PAUSED_STATE =PLAYING_STATE+1 ;//暂停的状态

    public static final int PLAYING_STATE_COMPLETE = PLAYING_PAUSED_STATE+1;

    private int mRecordState = IDLE_STATE;//当前录音机的状态

    public static final int STORAGE_ACCESS_ERROR = -1;

    public static final int INTERNAL_ERROR = STORAGE_ACCESS_ERROR-1;

    public static final int IN_CALL_RECORD_ERROR = INTERNAL_ERROR-1;

    public static final int URI_ERROR = IN_CALL_RECORD_ERROR-1;

    /**
     * 播放状态的回调
     * @author shibinhuang
     *
     */
    public interface OnStateChangedListener {
        void onStateChangedCallBack(int state);

        void onErrorCallBack(int error);
    }

    private Context mContext;

    private OnStateChangedListener mOnStateChangedListener = null;

    private long mRecordStart = 0; // time at which latest record or play

    private int mRecordLength = 0;

    private File mRecordFile = null;

    private File mRecordFileDir = null;//录音文件保存目录

    private MediaPlayer mMediaPlayer = null;

    public static  String getAudioCachePath(){
        return Config.getCacheFileDirPath()
                + AUDIO_DIR;
    }

    public RecorderManager(Context context) {
        mContext = context;
        //录音文件存储的路径,保存在SD卡下
        File sampleDir = new File(getAudioCachePath());
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        //添加屏蔽文件
        try {
            File  nomedia = new File(sampleDir,"/.nomedia");
            if (! nomedia.exists()){
                nomedia.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecordFileDir = sampleDir;

        syncStateWithService();
    }

    public boolean syncStateWithService() {
        if (RecorderService.isRecording()) {
            mRecordState = RECORDING_STATE;
            //获取到开始时间
            mRecordStart = RecorderService.getStartTime();
          mRecordFile = new File(RecorderService.getFilePath());
            return true;
        } else if (mRecordState == RECORDING_STATE) {
            //service闲置但本地在录音
            return false;
        }
        return true;
    }


    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public int getRecordState() {
        return mRecordState;
    }

    /**
     * 获取时间秒数
     * @return
     */
    public int getRecordTime() {
        if (mRecordState == RECORDING_STATE) {
            return (int) ((System.currentTimeMillis() - mRecordStart) / 1000);
        } else if (mRecordState == PLAYING_STATE || mRecordState == PLAYING_PAUSED_STATE) {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getCurrentPosition() / 1000;
            }
        }
        return 0;
    }

    /**
     * 获取播放进度
     * @return
     */
    public float getPlayProgress() {
        if (mMediaPlayer != null) {
            return ((float) mMediaPlayer.getCurrentPosition()) / mMediaPlayer.getDuration();
        }
        return 0.0f;
    }

    /**
     * 获取录音文件
     * @return
     */
    public File getRecordFile() {
        return mRecordFile;
    }

    /**
     * 获取录音文件路径
     * @return
     */
    public String getRecordFilePath(){
    return mRecordFile.getAbsolutePath();
    }

    /**
     * 获取录音文件路径MD5
     * @return
     */
    public String getRecordFilePathForMd5(){
        String md5 =  CloudUtils.md5(getRecordFilePath());
        return md5;
    }

    /**
     * 获取包含协议头的录音文件路径MD5
     * @return
     */
    public String getRecordFilePathCacheMd5(){
        return AudioPlugin.LOCAL_RES_HEADER+getRecordFilePathForMd5();
    }

    /**
     * 重置播放状态，删除文件
     */
    public void delete() {
        stop();
        if (mRecordFile != null){
             mRecordFile.delete();
        }
        mRecordFile = null;
        mRecordLength = 0;
        signalStateChanged(IDLE_STATE);
    }

    public void clear() {
        stop();
        mRecordLength = 0;
        signalStateChanged(IDLE_STATE);
    }

    public void reset() {
        stop();
        mRecordLength = 0;
        mRecordFile = null;
        mRecordState = IDLE_STATE;

        File sampleDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + AUDIO_DIR);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        mRecordFileDir = sampleDir;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * @param suffix 文件后缀名
     */
    public void startRecording( String suffix ) {
        stop();
            try {
                //在指定文件夹生成录音文件,文件名随机
                mRecordFile = File.createTempFile("Record", suffix, mRecordFileDir);
            } catch (IOException e) {
                setError(STORAGE_ACCESS_ERROR);
                return;
            }
        RecorderService.startRecording(mContext,  mRecordFile.getAbsolutePath() );
        mRecordStart = System.currentTimeMillis();
    }

    public void stopRecording() {
        if (RecorderService.isRecording()) {
            RecorderService.stopRecording(mContext);
            mRecordLength = (int) ((System.currentTimeMillis() - mRecordStart) / 1000);
            if (mRecordLength == 0) {
                mRecordLength = 1;
            }
        }
    }

    public void startPlayback( String localId) {
        if (getRecordState() == PLAYING_PAUSED_STATE&&mMediaPlayer!=null) {
            mRecordStart = System.currentTimeMillis() - mMediaPlayer.getCurrentPosition();
            mMediaPlayer.start();
            setState(PLAYING_STATE);
        } else {
            stop();
            mMediaPlayer = new MediaPlayer();
            try {
                String audioPath ;
                if(AudioPlugin.mAudioCache.size()!=0){
                    audioPath= AudioPlugin.mAudioCache.get(localId);
                }else {
                    audioPath = mRecordFile.getAbsolutePath();
                }
                mMediaPlayer.setDataSource(audioPath);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.prepare();
                mMediaPlayer.start();

            } catch (IllegalArgumentException e) {
                setError(INTERNAL_ERROR);
                mMediaPlayer = null;
                return;
            } catch (IOException e) {
                setError(STORAGE_ACCESS_ERROR);
                mMediaPlayer = null;
                return;
            }catch(NullPointerException e){
                setError(URI_ERROR);
                mMediaPlayer = null;
                return;
            }

            mRecordStart = System.currentTimeMillis();
            setState(PLAYING_STATE);
        }
    }

    public void pausePlayback() {
        if (mMediaPlayer == null) {
            return;
        }

        mMediaPlayer.pause();
        setState(PLAYING_PAUSED_STATE);
    }

    public void stopPlayback() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        setState(IDLE_STATE);
    }

    public void stop() {
        stopRecording();
        stopPlayback();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        stop();
        setError(STORAGE_ACCESS_ERROR);
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
        setState(PLAYING_STATE_COMPLETE);
        stop();
    }

    public void setState(int state) {
        if (state == mRecordState)
            return;
        mRecordState = state;
        signalStateChanged(mRecordState);
    }

    private void signalStateChanged(int state) {
        if (mOnStateChangedListener != null){
            mOnStateChangedListener.onStateChangedCallBack(state);
        }
    }

    public void setError(int error) {
        if (mOnStateChangedListener != null){
            mOnStateChangedListener.onErrorCallBack(error);
        }
    }

}
