package com.example.k_trader;

import android.app.Application;
import android.content.Context;

public class KTraderApplication extends Application {
    private static KTraderApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
