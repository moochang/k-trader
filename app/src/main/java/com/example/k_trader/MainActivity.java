package com.example.k_trader;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.k_trader.base.GlobalSettings;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    int MAX_PAGE = 3;
    Fragment cur_fragment = new Fragment();
    ViewPager viewPager;

    public static final int STORAGE_PERMISSION_REQUEST = 0;

    public static final String BROADCAST_PROGRESS_MESSAGE = "PROGRESS_MESSAGE";
    private static Timer mTimer;
    private static ProgressDialog dialog;
    static int progress;

    JobScheduler jobScheduler;
    public static org.apache.log4j.Logger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 테마에 따라 Status Bar 색상 동적 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColorByTheme();
        }
        
        // App bar 색상 설정
        setAppBarColorByTheme();

//        int id = 0;
        viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new adapter(getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 2)
                    ProcessedOrderPage.getInstance().refresh();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 이상 버전이면
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Log 저장을 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
            }

            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
                SharedPreferences.Editor prefsEditr = sharedPreferences.edit();
                prefsEditr.putBoolean("FILE_LOG_ENABLED", true);
                prefsEditr.apply();
                logger = org.apache.log4j.Logger.getLogger("MainActivity");
            }
        }

        IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(BROADCAST_PROGRESS_MESSAGE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new MyReceiver(), theFilter);

        // Load app settings (data/data/(package_name)/shared_prefs/SharedPreference)
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        GlobalSettings.getInstance().setApiKey(sharedPreferences.getString(GlobalSettings.API_KEY_KEY_NAME, ""))
                                    .setApiSecret(sharedPreferences.getString(GlobalSettings.API_SECRET_KEY_NAME, ""))
                                    .setUnitPrice(sharedPreferences.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE))
                                    .setTradeInterval(sharedPreferences.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE))
                                    .setFileLogEnabled(sharedPreferences.getBoolean(GlobalSettings.FILE_LOG_ENABLED_KEY_NAME, false))
                                    .setEarningRate(sharedPreferences.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE))
                                    .setSlotIntervalRate(sharedPreferences.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE));

        if (GlobalSettings.getInstance().getApiKey().isEmpty() || GlobalSettings.getInstance().getApiSecret().isEmpty()) {
            Toast.makeText(this, "거래를 위해서는 Key와 Secret값 설정이 필요합니다.", Toast.LENGTH_SHORT).show();
            // Launch setting activity
            startActivity(new Intent(this, SettingActivity.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Resources res = getResources();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle("Exit program")
                .setContentText("OnDestroy is called")
                .setTicker("OnDestroy is called")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL);

        builder.setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm != null)
            nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private class adapter extends FragmentPagerAdapter {
        public adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position < 0 || MAX_PAGE <= position)
                return null;

            switch (position) {
                case 0:
                    cur_fragment = new MainPage();
                    break;
                case 1:
                    cur_fragment = new PlacedOrderPage();
                    break;
                case 2:
                    cur_fragment = ProcessedOrderPage.getInstance();
                    break;
            }
            return cur_fragment;
        }

        @Override
        public int getCount() {
            return MAX_PAGE;
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress = intent.getIntExtra("progress", 0);
//            Log.d("KTrader", "received progress : " + String.valueOf(progress));

            mTimer = new Timer();
            mTimer.schedule(new MyTask(), 0, progress);

            dialog = ProgressDialog.show(MainActivity.this, "Wait",
                    "다음 Request가 허용될 때까지 대기.. 최대 10초", true);
        }
    }

    static class MyTask extends TimerTask {
        public void run() {
            // Do what you wish here with the dialog
            if (dialog != null) {
                dialog.cancel();
                dialog.dismiss();
            }
        }
    }

    @Override
    public void onBackPressed() {
//        Toast.makeText(this, "Back button pressed.", Toast.LENGTH_SHORT).show();
        // ��׶��� ������ ��ȯ�Ѵ�.
        moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_REQUEST && grantResults.length > 0) {
            SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
            SharedPreferences.Editor prefsEditr = sharedPreferences.edit();

            if (grantResults[0] == 0) {
                // 권한 승인 됨
                prefsEditr.putBoolean("FILE_LOG_ENABLED", true);
                GlobalSettings.getInstance().setFileLogEnabled(true);
            } else {
                prefsEditr.putBoolean("FILE_LOG_ENABLED", false);
                GlobalSettings.getInstance().setFileLogEnabled(false);
                Toast.makeText(this, "Log 저장을 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
            prefsEditr.apply();
        }
    }
    
    /**
     * 현재 테마에 따라 Status Bar 색상을 설정하는 메서드
     */
    private void setStatusBarColorByTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 현재 테마가 Light 테마인지 확인
            boolean isLightTheme = isLightTheme();
            
            int statusBarColor;
            if (isLightTheme) {
                statusBarColor = getResources().getColor(R.color.status_bar_light);
                // Light 테마에서는 Status bar 아이콘을 어둡게 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // API 23 이상에서 SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 제거
                    int flags = getWindow().getDecorView().getSystemUiVisibility();
                    flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    getWindow().getDecorView().setSystemUiVisibility(flags);
                }
            } else {
                statusBarColor = getResources().getColor(R.color.status_bar_dark);
                // Dark 테마에서는 Status bar 아이콘을 밝게 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // API 23 이상에서 SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 추가
                    int flags = getWindow().getDecorView().getSystemUiVisibility();
                    flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    getWindow().getDecorView().setSystemUiVisibility(flags);
                }
            }
            
            getWindow().setStatusBarColor(statusBarColor);
        }
    }
    
    /**
     * 현재 테마가 Light 테마인지 확인하는 메서드
     */
    private boolean isLightTheme() {
        // 현재 앱이 Light 테마를 사용하고 있는지 확인
        // AppTheme의 parent가 Theme.AppCompat.Light.DarkActionBar이므로 Light 테마
        return true; // 현재 앱은 Light 테마 사용
    }
    
    /**
     * 현재 테마에 따라 App Bar 색상을 설정하는 메서드
     */
    private void setAppBarColorByTheme() {
        // 현재 테마가 Light 테마인지 확인
        boolean isLightTheme = isLightTheme();
        
        int appBarColor;
        if (isLightTheme) {
            appBarColor = getResources().getColor(R.color.app_bar_light);
        } else {
            appBarColor = getResources().getColor(R.color.app_bar_dark);
        }
        
        // ActionBar 색상 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(appBarColor));
            
            // ActionBar 그림자 효과 제거
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getSupportActionBar().setElevation(0);
            }
            
            // 텍스트 색상 설정 (흰색 배경에서는 어두운 색 사용)
            if (isLightTheme) {
                // Light 테마에서는 어두운 텍스트 색상 사용
                setActionBarTitleColor(getResources().getColor(android.R.color.black));
            } else {
                // Dark 테마에서는 밝은 텍스트 색상 사용
                setActionBarTitleColor(getResources().getColor(android.R.color.white));
            }
        }
    }
    
    /**
     * ActionBar 타이틀 색상을 설정하는 메서드
     */
    private void setActionBarTitleColor(int color) {
        if (getSupportActionBar() != null) {
            // ActionBar의 타이틀을 커스텀 TextView로 설정
            android.widget.TextView titleView = new android.widget.TextView(this);
            titleView.setText(getSupportActionBar().getTitle());
            titleView.setTextColor(color);
            titleView.setTextSize(18);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // ActionBar에 커스텀 타이틀 설정
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setCustomView(titleView);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
        }
    }
}
