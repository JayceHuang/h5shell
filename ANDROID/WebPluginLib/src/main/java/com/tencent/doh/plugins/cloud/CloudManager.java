package com.tencent.doh.plugins.cloud;

import android.content.Context;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.download.Downloader;
import com.tencent.download.core.DownloadResult;
import com.tencent.doh.pluginframework.Config;
import com.tencent.upload.Const;
import com.tencent.upload.UploadManager;
import com.tencent.upload.task.UploadTask;
import com.tencent.upload.task.impl.FileUploadTask;
import com.tencent.upload.task.impl.PhotoUploadTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 腾讯云
 */
public class CloudManager {

    protected String TAG = CloudManager.class.getSimpleName();
    /**
     *  腾讯云申请的APP KEY
     *  QQ号 3310355768 密码 tencentc215
     *  https://console.qcloud.com/
     */
    public static final String APP_ID =  Config.getCloudParameters().getCloudAppId();
    public static final String SECRET_ID = Config.getCloudParameters().getCloudSecretId();
    public static final String SECRET_KEY = Config.getCloudParameters().getCloudSecretKey();

    public static final String FILE_BUKET = Config.getCloudParameters().getFileBucket();   //文件网盘
    public static final String PHOTO_BUKET = Config.getCloudParameters().getPhotoBucket();  //照片网盘
    public static final long SIGN_EXPIRED_SECOND = 24 * 60;

    private static final int DOWNLOADER_MAX_CONCURRENT = 3;

    protected Context mContext;
    protected UploadManager mPhotoUploadManager;
    protected UploadManager mFileUploadManager;
    protected Downloader mDownloader;
    protected ConcurrentHashMap<Integer, BatchUploadTask> mBatchUploadTaskMap;   // 上传队列
    protected ConcurrentHashMap<String, UIDownloadListener> mDownloadListenerMap;// 下载队列
    protected int mUploadRequestId = 1;//起始ID

    public CloudManager(){
        mContext = Config.getContext();
        if (mContext == null) {
            return;
        }
        mPhotoUploadManager = new UploadManager(mContext,  APP_ID, Const.FileType.Photo, null);
        mFileUploadManager = new UploadManager(mContext, APP_ID, Const.FileType.File, null);
        mBatchUploadTaskMap = new ConcurrentHashMap< >();
        mDownloadListenerMap = new ConcurrentHashMap< >();
        mDownloader = new Downloader(mContext, APP_ID, "qcloudDownloader");
        mDownloader.setMaxConcurrent(DOWNLOADER_MAX_CONCURRENT);
        mDownloader.enableHTTPRange(true);
        mDownloader.enableKeepAlive(true);
    }

    public enum UploadFileType {
        FILE, PHOTO
    }

    /**
     * 批量上传任务控制器
     * @author trentyang
     *
     */
    protected class BatchUploadTask {
        private int mRequestId;
        private List<UploadTask> mTasks;
        private Map<String, String> mPath2Url;
        private UploadListener mListener;

        public BatchUploadTask(int requestId, UploadListener listener) {
            mRequestId = requestId;
            mTasks = Collections.synchronizedList(new ArrayList<UploadTask>());
            mPath2Url = new ConcurrentHashMap<String, String>();
            mListener = listener;
        }

        public void addTask(UploadTask task) {
            if (task != null) {
                mTasks.add(task);
            }
        }

        public void setUrl(String path, String url) {
            mPath2Url.put(path, url);
        }

        public void cancel() {
            synchronized (CloudManager.class) {
                for (UploadTask task : mTasks) {
                    task.cancel();
                }
            }
        }

        public boolean isFinish() {
            return mPath2Url.size() == mTasks.size();
        }

        public void upload() {
            synchronized (CloudManager.class) {
                for (UploadTask task : mTasks) {
                    //批量上传
                    if (task instanceof PhotoUploadTask) {
                        mPhotoUploadManager.upload(task);
                    } else if (task instanceof FileUploadTask) {
                        mFileUploadManager.upload(task);
                    }
                }
            }
        }

        public void onUploadSucceed() {
            if (mListener != null) {
                mListener.onUploadSucceed(mRequestId, mPath2Url);
            }
        }

        public void onUploadFailed(int errorCode, String errorMsg) {
            if (mListener != null) {
                mListener.onUploadFailed(mRequestId, errorCode, errorMsg, mPath2Url);
            }
        }

        public void onUploadProgress(String path, long totalSize, long uploadSize) {
            if (mListener != null) {
                mListener.onUploadProgress(mRequestId, path, totalSize, uploadSize);
            }
        }
    }

    /**
     * 包装一个UI线程的下载监听器
     * @author trentyang
     *
     */
    protected class UIDownloadListener implements Downloader.DownloadListener {

        private DownloadCallbackRunnable mCallbackRunnable;

        public UIDownloadListener(Downloader.DownloadListener listener) {
            mCallbackRunnable = new DownloadCallbackRunnable(listener);
        }

        @Override
        public void onDownloadSucceed(final String url, final DownloadResult result) {
            LogUtils.d(TAG, "onDownloadSucceed url:" + url + ";" + result.getPath());
            mCallbackRunnable.onDownloadSucceed(url, result);
            mDownloadListenerMap.remove(url);
        }

        @Override
        public void onDownloadProgress(final String url, final long totalSize, final float progress) {
            LogUtils.d(TAG, "onDownloadProgress url:" + url + ";" + progress + "/" + totalSize);
            mCallbackRunnable.onDownloadProgress(url, totalSize, progress);
        }

        @Override
        public void onDownloadFailed(final String url, final DownloadResult result) {
            LogUtils.d(TAG, "onDownloadFailed url:" + url + ";" + result.getErrorCode());
            mCallbackRunnable.onDownloadFailed(url, result);
            mDownloadListenerMap.remove(url);
        }

        @Override
        public void onDownloadCanceled(final String url) {
            LogUtils.d(TAG, "onDownloadCanceled url:" + url);
            mCallbackRunnable.onDownloadCanceled(url);
            mDownloadListenerMap.remove(url);
        }
    }

    /**
     * UI线程下载回调Runnable，为了提高效率，避免在onDownloadProgress中runOnUI每次都new一个Runnable
     * @author trentyang
     *
     */
    protected static class DownloadCallbackRunnable implements Runnable {

        private enum CallbackType {
            SUCCEED, PROGRESS, FAILED, CANCELED
        }

        private String mUrl;
        private long mTotalSize;
        private float mProgress;
        private DownloadResult mDownloadResult;
        private CallbackType mCallbackType;
        private WeakReference<Downloader.DownloadListener> mListenerRef;
        //这里不使用弱引用，因为总是会获取listener为空，暂时使用强引用
        private Downloader.DownloadListener mListener;

        public DownloadCallbackRunnable(Downloader.DownloadListener listener) {
            if (listener != null) {
                mListenerRef = new WeakReference<Downloader.DownloadListener>(listener);
                mListener = listener;
            }
        }

        public void onDownloadSucceed(String url, DownloadResult result) {
            mUrl = url;
            mDownloadResult = result;
            mCallbackType = CallbackType.SUCCEED;
            run();
        }

        public void onDownloadProgress(String url, long totalSize, float progress) {
            mUrl = url;
            mTotalSize = totalSize;
            mProgress = progress;
            mCallbackType = CallbackType.PROGRESS;
            run();
        }

        public void onDownloadFailed(String url, DownloadResult result) {
            mUrl = url;
            mDownloadResult = result;
            mCallbackType = CallbackType.FAILED;
            run();
        }

        public void onDownloadCanceled(String url) {
            mUrl = url;
            mCallbackType = CallbackType.CANCELED;
            run();
        }

        @Override
        public void run() {
            if (mListener != null) {
                switch (mCallbackType) {
                    case SUCCEED:
                        mListener.onDownloadSucceed(mUrl, mDownloadResult);
                        break;
                    case PROGRESS:
                        mListener.onDownloadProgress(mUrl, mTotalSize, mProgress);
                        break;
                    case FAILED:
                        mListener.onDownloadFailed(mUrl, mDownloadResult);
                        break;
                    case CANCELED:
                        mListener.onDownloadCanceled(mUrl);
                        break;
                }
            }
        }
    }

    /**
     *
     * @author trentyang
     * 腾讯云上传回调，在工作线程调用
     *
     */
    public interface UploadListener {

        void onUploadSucceed(int requestId, Map<String, String> path2Url);

        void onUploadFailed(int requestId, int errorCode, String errorMsg, Map<String, String> path2Url);

        void onUploadProgress(int requestId, String path, long totalSize, long uploadSize);

    }

    public void releaseAll() {
        mFileUploadManager.close();
        mPhotoUploadManager.close();
        mDownloader.cancelAll();
        mBatchUploadTaskMap.clear();
        mDownloadListenerMap.clear();
    }
}
