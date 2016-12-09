package com.tencent.doh.demo.demo;


import android.app.Application;

public class QQApplication extends Application {
    private static QQApplication sApp;
    public static QQApplication self() {
        return sApp;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        sApp = this;
        
    }
}
