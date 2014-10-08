package com.testing.titans;

import android.app.Application;

import com.testing.titans.utils.TitansLog;

public class TitansApp extends Application {

    private static final boolean DEBUG = true;
    private static final String TAG = TitansApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate");
    }

    private void log(String msg) {
        if (DEBUG) {
            TitansLog.log(TAG, msg);
        }
    }

}
