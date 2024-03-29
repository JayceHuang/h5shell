
package com.tencent.doh.plugins.audio;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * 录音剩余时间计时
 */
public class RemainingTimeCalculator {
    public static final int UNKNOWN_LIMIT = 0;

    public static final int FILE_SIZE_LIMIT = 1;

    public static final int DISK_SPACE_LIMIT = 2;

    private static final int EXTERNAL_STORAGE_BLOCK_THREADHOLD = 32;

    private int mCurrentLowerLimit = UNKNOWN_LIMIT;

    private File mRecordingFile;

    private long mMaxBytes;

    private int mBytesPerSecond;

    private long mBlocksChangedTime;

    private long mLastBlocks;

    private long mFileSizeChangedTime;

    private long mLastFileSize;

    public RemainingTimeCalculator() {
    }

    public void reset() {
        mCurrentLowerLimit = UNKNOWN_LIMIT;
        mBlocksChangedTime = -1;
        mFileSizeChangedTime = -1;
    }

    public long getTimeRemaining() {
        StatFs fs = null;
        long blocks = -1;
        long blockSize = -1;
        long now = System.currentTimeMillis();

        fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        blocks = fs.getAvailableBlocks() - EXTERNAL_STORAGE_BLOCK_THREADHOLD;
        blockSize = fs.getBlockSize();
        if (blocks < 0) {
            blocks = 0;
        }

        if (mBlocksChangedTime == -1 || blocks != mLastBlocks) {
            mBlocksChangedTime = now;
            mLastBlocks = blocks;
        }
        long result = mLastBlocks * blockSize / mBytesPerSecond;
        result -= (now - mBlocksChangedTime) / 1000;

        if (mRecordingFile == null) {
            mCurrentLowerLimit = DISK_SPACE_LIMIT;
            return result;
        }

        mRecordingFile = new File(mRecordingFile.getAbsolutePath());
        long fileSize = mRecordingFile.length();
        if (mFileSizeChangedTime == -1 || fileSize != mLastFileSize) {
            mFileSizeChangedTime = now;
            mLastFileSize = fileSize;
        }

        long result2 = (mMaxBytes - fileSize) / mBytesPerSecond;
        result2 -= (now - mFileSizeChangedTime) / 1000;
        result2 -= 1; // just for safety

        mCurrentLowerLimit = result < result2 ? DISK_SPACE_LIMIT : FILE_SIZE_LIMIT;

        return Math.min(result, result2);
    }

    public void setBitRate(int bitRate) {
        mBytesPerSecond = bitRate / 8;
    }
}
