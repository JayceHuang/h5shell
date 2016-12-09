package com.tencent.doh.plugins.cloud;

import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.doh.pluginframework.util.LogUtils;
import com.tencent.upload.task.ITask;
import com.tencent.upload.task.IUploadTaskListener;
import com.tencent.upload.task.UploadTask;
import com.tencent.upload.task.data.FileInfo;
import com.tencent.upload.task.impl.FileUploadTask;
import com.tencent.upload.task.impl.PhotoUploadTask;

import java.util.Set;

/**
 * 腾讯云上传专用
 */
public class UploadManager extends CloudManager {

    private static class Singleton {
        private static UploadManager sInstance = new UploadManager();
    }

    public static UploadManager getInstance() {
        return Singleton.sInstance;
    }


    /**
     * 单任务上传
     * @param type 服务器对应的bucket目录
     * @param path 文件本地路径
     * @param name 文件服务器上的名字
     */
    public void uploadSingleFile(String type,String path , String name , IUploadTaskListener listener) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(name)) {
            Toast.makeText(mContext, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        String destPath = "/"+ name;
        FileUploadTask fileUploadTask = new FileUploadTask( type, path, destPath, "", listener);
        //上传任务开始
        String sign = CloudUtils.getSign( type);
        fileUploadTask.setAuth( sign);
        mFileUploadManager.upload(fileUploadTask); // 开始上传
    }


    /**
     * 批量上传
     * @param uploadFileInfos
     * @param listener
     * @return
     */
    public int uploadBatchFile(Set<UploadFileInfo> uploadFileInfos, UploadListener listener) {
        if (uploadFileInfos == null || uploadFileInfos.isEmpty()) {
            return 0;
        }
        int requestId = createUploadRequestId();
        BatchUploadTask batchUploadTask = new BatchUploadTask(requestId, listener);
        mBatchUploadTaskMap.put(requestId, batchUploadTask);
        //遍历上传队列
        for (UploadFileInfo uploadFileInfo : uploadFileInfos) {
            UploadTask task = null;
            switch (uploadFileInfo.type) {
                case FILE:
                    task = createFileUploadTask(requestId, uploadFileInfo);
                    break;
                case PHOTO:
                    task = createPhotoUploadTask(requestId, uploadFileInfo);
                    break;
            }
            //添加任务
            batchUploadTask.addTask(task);
        }
        //执行上传
        batchUploadTask.upload();
        return requestId;
    }



   /**
    * 文件上传任务
    * @param requestId
    * @param uploadFileInfo
    * @return
            */
    private UploadTask createFileUploadTask(int requestId, UploadFileInfo uploadFileInfo) {
        LogUtils.i(TAG, "createFileUploadTask uploadFileInfo:" + uploadFileInfo);
        FileUploadTask uploadTask = new FileUploadTask( FILE_BUKET, uploadFileInfo.path,
                uploadFileInfo.name, null, new UploadTaskListener(requestId, uploadFileInfo) {
            @Override
            public void onUploadSucceed(FileInfo result) {
                LogUtils.d(TAG, "onFileUploadSucceed:" + getRequestId() + ":" + getUploadFileInfo().path + ":"
                        + result.url);
                doUploadSucceed(getRequestId(), getUploadFileInfo().path, result);
            }

            @Override
            public void onUploadStateChange(ITask.TaskState state) {
            }

            @Override
            public void onUploadProgress(long totalSize, long uploadSize) {
                LogUtils.d(TAG, "onFileUploadProgress:" + getRequestId() + ":" + totalSize + ":" + uploadSize);
                doUploadProgress(getRequestId(), getUploadFileInfo().path, totalSize, uploadSize);
            }

            @Override
            public void onUploadFailed(int errorCode, String errorMsg) {
                LogUtils.d(TAG, "onFileUploadFailed:" + getRequestId() + ":" + getUploadFileInfo().path + ":"
                        + errorCode + ":" + errorMsg);
                doUploadFailed(getRequestId(), getUploadFileInfo().path, errorCode, errorMsg);
            }
        });
        String sign = CloudUtils.getSign( FILE_BUKET);
        uploadTask.setAuth( sign);
        return uploadTask;
    }

    /**
     * 图片上传任务
     * @param requestId
     * @param uploadFileInfo
     * @return
     */
    private UploadTask createPhotoUploadTask(int requestId, UploadFileInfo uploadFileInfo) {
        LogUtils.i(TAG, "createPhotoUploadTask uploadFileInfo:" + uploadFileInfo);
        PhotoUploadTask uploadTask = new PhotoUploadTask(uploadFileInfo.path, new UploadTaskListener(requestId,
                uploadFileInfo) {
            @Override
            public void onUploadSucceed(FileInfo result) {
                LogUtils.d(TAG, "onPhotoUploadSucceed:" + getRequestId() + ":" + getUploadFileInfo().path + ":"
                        + result.url);
                doUploadSucceed(getRequestId(), getUploadFileInfo().path, result);
            }

            @Override
            public void onUploadStateChange(ITask.TaskState state) {
                LogUtils.d(TAG, "onPhotoUploadStateChange:" + getRequestId() + ":" + state + ":" + state);
            }

            @Override
            public void onUploadProgress(long totalSize, long uploadSize) {
                LogUtils.d(TAG, "onPhotoUploadProgress:" + getRequestId() + ":" + totalSize + ":" + uploadSize);
                doUploadProgress(getRequestId(), getUploadFileInfo().path, totalSize, uploadSize);
            }

            @Override
            public void onUploadFailed(int errorCode, String errorMsg) {
                LogUtils.d(TAG, "onPhotoUploadFailed:" + getRequestId() + ":" + getUploadFileInfo().path + ":" + errorCode
                        + ":" + errorMsg);
                doUploadFailed(getRequestId(), getUploadFileInfo().path, errorCode, errorMsg);
            }
        });
        uploadTask.setBucket( PHOTO_BUKET);
        String sign = CloudUtils.getSign( PHOTO_BUKET);
        uploadTask.setAuth( sign);
        return uploadTask;
    }

    private void doUploadFailed(int requestId, String path, int errorCode, String errorMsg) {
        LogUtils.d(TAG, "doUploadFailed requestId:" + requestId);
        BatchUploadTask batchUploadTask = mBatchUploadTaskMap.remove(requestId);
        if (batchUploadTask != null) {
            LogUtils.d(TAG, "doUploadFailed cancel:" + requestId);
            batchUploadTask.cancel(); // 取消
            batchUploadTask.onUploadFailed(errorCode, errorMsg); // 通知回调
        }
    }

    private void doUploadSucceed(int requestId, String path, FileInfo result) {
        LogUtils.d(TAG, "doUploadSucceed requestId:" + requestId);
        BatchUploadTask batchUploadTask = mBatchUploadTaskMap.get(requestId);
        if (batchUploadTask != null) {
            batchUploadTask.setUrl(path, result.url);
            if (batchUploadTask.isFinish()) { // 批量上传完成
                mBatchUploadTaskMap.remove(requestId);
                LogUtils.d(TAG, "doUploadSucceed finish:" + requestId);
                batchUploadTask.onUploadSucceed();// 通知回调
            }
        }
    }

    /**
     * 上传requestId
     * @return
     */
    private synchronized int createUploadRequestId() {
        return mUploadRequestId++;
    }

    private void doUploadProgress(int requestId, String path, long totalSize, long uploadSize) {
        LogUtils.d(TAG, "onUploadProgress requestId:" + requestId + ";totalSize:" + totalSize + ";uploadSize:" + uploadSize);
        BatchUploadTask batchUploadTask = mBatchUploadTaskMap.get(requestId);
        if (batchUploadTask != null) {
            batchUploadTask.onUploadProgress(path, totalSize, uploadSize); // 通知回调
        }
    }

    public static class UploadFileInfo {
        public String path;
        public String name;
        public UploadFileType type;

        public UploadFileInfo(String path, String name,UploadFileType type) {
            this.path = path;//文件的路径
            this.name = name;//文件在服务器上的名字
            this.type = type;//文件的类型
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (!(other instanceof UploadFileInfo)) {
                return false;
            }
            UploadFileInfo otherInfo = (UploadFileInfo) other;
            if (path != null && type != null) {
                if (path.equals(otherInfo.path) && type.equals(otherInfo.type)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = 1;
            if (path != null) {
                result = result * 31 + path.hashCode();
            }
            if (type != null) {
                result = result * 31 + type.hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            return path + ":" + type;
        }
    }


    /**
     * 包装上传监听器，附带额外数据
     * @author trentyang
     *
     */
    private static class UploadTaskListener implements IUploadTaskListener {

        private int mRequestId;
        private UploadFileInfo mUploadFileInfo;

        public UploadTaskListener(int requestId, UploadFileInfo uploadFileInfo) {
            mRequestId = requestId;
            mUploadFileInfo = uploadFileInfo;
        }

        public int getRequestId() {
            return mRequestId;
        }

        public UploadFileInfo getUploadFileInfo() {
            return mUploadFileInfo;
        }

        @Override
        public void onUploadSucceed(FileInfo result) {
        }

        @Override
        public void onUploadStateChange(ITask.TaskState state) {
        }

        @Override
        public void onUploadProgress(long totalSize, long uploadSize) {
        }

        @Override
        public void onUploadFailed(int errorCode, String errorMsg) {
        }

    }



}
