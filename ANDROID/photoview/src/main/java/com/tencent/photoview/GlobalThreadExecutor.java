package com.tencent.photoview;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by bryonliu on 2015/10/5.
 */
public class GlobalThreadExecutor {

    private static final int MAX_THREAD_NUM = 10;
    private Executor mExecutor;

    private static class Singleton {
        private static GlobalThreadExecutor sInstance = new GlobalThreadExecutor();
    }

    public static GlobalThreadExecutor getInstance() {
        return Singleton.sInstance;
    }

    private GlobalThreadExecutor() {
        mExecutor = Executors.newFixedThreadPool(MAX_THREAD_NUM);
    }

    public void start(Runnable runnable) {
        mExecutor.execute(runnable);
    }

}
