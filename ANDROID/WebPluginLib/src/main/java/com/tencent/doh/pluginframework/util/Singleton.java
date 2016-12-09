package com.tencent.doh.pluginframework.util;


import com.tencent.doh.pluginframework.annotation.Public;

public abstract class Singleton<T, P> {
    private volatile T mInstance;

    protected abstract T create(P p);

    @Public
    public final T get(P p) {
        if (mInstance == null) {
            synchronized (this) {
                if (mInstance == null) {
                    mInstance = create(p);
                }
            }
        }
        return mInstance;
    }
}
