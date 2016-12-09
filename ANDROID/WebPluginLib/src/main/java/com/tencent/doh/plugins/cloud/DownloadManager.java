package com.tencent.doh.plugins.cloud;


import com.tencent.download.Downloader;
import com.tencent.download.core.DownloadResult;

import java.io.File;

/**
 * 腾讯云下载专用
 */
public class DownloadManager extends CloudManager{

    private static class Singleton {
        private static DownloadManager sInstance = new DownloadManager();
    }

    public static DownloadManager getInstance() {
        return Singleton.sInstance;
    }

    /**
     * 下载
     * @param url
     * @param listener
     */
    public void download(final String url, final Downloader.DownloadListener listener) {
        UIDownloadListener uiListener = new UIDownloadListener(listener);
        if (mDownloader.hasCache(url)) { // 文件有缓存，不用重新下载
            final File file = mDownloader.getCacheFile(url);
            if (file != null && file.exists()) {
                DownloadResult result = new DownloadResult(url);
                result.setPath(file.getAbsolutePath());
                uiListener.onDownloadSucceed(url, result);
                return;
            }
        }
        mDownloadListenerMap.put(url, uiListener);
        mDownloader.download(url, uiListener);
    }

}
