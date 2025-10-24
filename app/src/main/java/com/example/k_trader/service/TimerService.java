package com.example.k_trader.service;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.example.k_trader.KTraderApplication;
import com.example.k_trader.ui.fragment.TransactionLogPage;

/**
 * Created by 김무창 on 2018-03-04.
 */

public class TimerService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 서비스에서 가장 먼저 호출됨(최초에 한번만)
        log_info("서비스의 onCreate");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 서비스가 호출될 때마다 실행
        log_info("서비스의 onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 서비스가 종료될 때 실행
        log_info("서비스의 onDestroy");
    }

    private void log_info(final String log) {
        Intent intent = new Intent(TransactionLogPage.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);
    }
}
