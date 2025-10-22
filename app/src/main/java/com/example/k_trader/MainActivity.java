package com.example.k_trader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.app.job.JobScheduler;
import android.graphics.Color;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.database.OrderRepository;
import com.example.k_trader.dialog.ProgressDialogManager;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    int MAX_PAGE = 3;
    Fragment cur_fragment = new Fragment();
    ViewPager viewPager;
    
    // Appbar의 TextView들
    private android.widget.TextView textAppTitle;
    private android.widget.TextView textLastSyncTime;

    public static final int STORAGE_PERMISSION_REQUEST = 0;
    public static final int NOTIFICATION_PERMISSION_REQUEST = 1;

    public static final String BROADCAST_PROGRESS_MESSAGE = "PROGRESS_MESSAGE";
    static int progress;

    JobScheduler jobScheduler;
    public static org.apache.log4j.Logger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Appbar TextView들 초기화
        textAppTitle = findViewById(R.id.textAppTitle);
        textLastSyncTime = findViewById(R.id.textLastSyncTime);
        
        android.util.Log.d("KTrader", "[MainActivity] Appbar TextView initialization:");
        android.util.Log.d("KTrader", "[MainActivity] textAppTitle: " + (textAppTitle != null ? "not null" : "null"));
        android.util.Log.d("KTrader", "[MainActivity] textLastSyncTime: " + (textLastSyncTime != null ? "not null" : "null"));
        
        // 테마에 따라 Appbar 텍스트 색상 설정
        setAppBarTextColorsByTheme();
        
        // 테마에 따라 Status Bar 색상 동적 설정
        setStatusBarColorByTheme();
        
        // App bar 색상 설정
        setAppBarColorByTheme();
        
        // Notification 권한 요청 (Android 13 이상)
        requestNotificationPermission();

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

        // 로그 저장 기능 활성화 (권한 불필요 - 앱 내부 저장소 사용)
//        enableFileLogging();

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
                                    .setSlotIntervalRate(sharedPreferences.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE))
                                    .setCoinType(sharedPreferences.getString(GlobalSettings.COIN_TYPE_KEY_NAME, GlobalSettings.COIN_TYPE_DEFAULT_VALUE))
                                    .setAutoScroll(sharedPreferences.getBoolean(GlobalSettings.AUTO_SCROLL_KEY_NAME, GlobalSettings.AUTO_SCROLL_DEFAULT_VALUE));

        if (GlobalSettings.getInstance().getApiKey().isEmpty() || GlobalSettings.getInstance().getApiSecret().isEmpty()) {
            Toast.makeText(this, "거래를 위해서는 Key와 Secret값 설정이 필요합니다.", Toast.LENGTH_SHORT).show();
            // Launch setting activity
            startActivity(new Intent(this, SettingActivity.class));
        }
    }
    
    /**
     * Notification 권한 요청 (Android 13 이상)
     */
    private void requestNotificationPermission() {
        android.util.Log.d("KTrader", "[MainActivity] requestNotificationPermission() 시작");
        android.util.Log.d("KTrader", "[MainActivity] Android SDK 버전: " + Build.VERSION.SDK_INT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.util.Log.d("KTrader", "[MainActivity] Android 13+ 감지 - Notification 권한 확인");
            
            int permissionStatus = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS);
            android.util.Log.d("KTrader", "[MainActivity] 현재 권한 상태: " + permissionStatus);
            android.util.Log.d("KTrader", "[MainActivity] PERMISSION_GRANTED: " + android.content.pm.PackageManager.PERMISSION_GRANTED);
            
            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("KTrader", "[MainActivity] Notification 권한 요청 시작");
                ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_REQUEST);
            } else {
                android.util.Log.d("KTrader", "[MainActivity] Notification 권한 이미 허용됨");
            }
        } else {
            android.util.Log.d("KTrader", "[MainActivity] Android 13 미만 - Notification 권한 불필요");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        android.util.Log.d("KTrader", "[MainActivity] onRequestPermissionsResult() 호출");
        android.util.Log.d("KTrader", "[MainActivity] requestCode: " + requestCode);
        android.util.Log.d("KTrader", "[MainActivity] NOTIFICATION_PERMISSION_REQUEST: " + NOTIFICATION_PERMISSION_REQUEST);
        
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            android.util.Log.d("KTrader", "[MainActivity] Notification 권한 요청 결과 처리");
            
            if (grantResults.length > 0) {
                android.util.Log.d("KTrader", "[MainActivity] grantResults.length: " + grantResults.length);
                android.util.Log.d("KTrader", "[MainActivity] grantResults[0]: " + grantResults[0]);
                android.util.Log.d("KTrader", "[MainActivity] PERMISSION_GRANTED: " + android.content.pm.PackageManager.PERMISSION_GRANTED);
                
                if (grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.d("KTrader", "[MainActivity] Notification 권한 허용됨");
                    Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    android.util.Log.d("KTrader", "[MainActivity] Notification 권한 거부됨");
                    Toast.makeText(this, "알림 권한이 거부되었습니다. 거래 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show();
                }
            } else {
                android.util.Log.e("KTrader", "[MainActivity] grantResults가 비어있음");
            }
        } else {
            android.util.Log.d("KTrader", "[MainActivity] 다른 권한 요청 결과: " + requestCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // 메뉴 아이콘 색상 설정
        setMenuIconColorsByTheme(menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_bithumb) {
            launchBithumbApp();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshCoinInfo();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        } else if (id == R.id.action_clear) {
            clearAllDatabaseRecords();
            return true;
        } else if (id == R.id.action_scroll_to_bottom) {
            scrollToBottom();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 코인 정보 새로고침
     */
    private void refreshCoinInfo() {
        android.util.Log.d("KTrader", "[MainActivity] refreshCoinInfo() called");
        try {
            // MainPage Fragment 찾기
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.Fragment fragment = fragmentManager.findFragmentById(android.R.id.content);
            
            android.util.Log.d("KTrader", "[MainActivity] Fragment found: " + (fragment != null ? fragment.getClass().getSimpleName() : "null"));
            
            if (fragment instanceof MainPage) {
                android.util.Log.d("KTrader", "[MainActivity] Found MainPage fragment, calling refreshCoinData()");
                MainPage mainPage = (MainPage) fragment;
                mainPage.refreshCoinData();
                Toast.makeText(this, "코인 정보를 새로고침합니다.", Toast.LENGTH_SHORT).show();
            } else {
                // ViewPager에서 MainPage 찾기
                android.util.Log.d("KTrader", "[MainActivity] Fragment not found directly, searching in ViewPager");
                ViewPager viewPager = findViewById(R.id.viewpager);
                android.util.Log.d("KTrader", "[MainActivity] ViewPager found: " + (viewPager != null ? "not null" : "null"));
                
                if (viewPager != null) {
                    android.support.v4.app.FragmentPagerAdapter adapter = (android.support.v4.app.FragmentPagerAdapter) viewPager.getAdapter();
                    android.util.Log.d("KTrader", "[MainActivity] ViewPager adapter found: " + (adapter != null ? "not null" : "null"));
                    
                    if (adapter != null) {
                        // 현재 활성화된 Fragment 가져오기
                        android.support.v4.app.Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + viewPager.getCurrentItem());
                        android.util.Log.d("KTrader", "[MainActivity] Current fragment from tag: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                        
                        if (currentFragment instanceof MainPage) {
                            MainPage mainPage = (MainPage) currentFragment;
                            android.util.Log.d("KTrader", "[MainActivity] Found active MainPage fragment, calling refreshCoinData()");
                            try {
                                mainPage.refreshCoinData();
                                android.util.Log.d("KTrader", "[MainActivity] refreshCoinData() call completed successfully");
                                Toast.makeText(this, "코인 정보를 새로고침합니다.", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                android.util.Log.e("KTrader", "[MainActivity] Error calling refreshCoinData()", e);
                                Toast.makeText(this, "새로고침 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            android.util.Log.w("KTrader", "[MainActivity] Current fragment is not MainPage or is null");
                            
                            // 대안: adapter.getItem() 방식도 시도
                            MainPage mainPage = (MainPage) adapter.getItem(0);
                            android.util.Log.d("KTrader", "[MainActivity] Trying adapter.getItem(0): " + (mainPage != null ? "not null" : "null"));
                            
                            if (mainPage != null) {
                                android.util.Log.d("KTrader", "[MainActivity] Fragment class: " + mainPage.getClass().getSimpleName());
                                android.util.Log.d("KTrader", "[MainActivity] Fragment toString: " + mainPage.toString());
                                android.util.Log.d("KTrader", "[MainActivity] Fragment isAdded: " + mainPage.isAdded());
                                android.util.Log.d("KTrader", "[MainActivity] Fragment isDetached: " + mainPage.isDetached());
                            }
                        }
                    } else {
                        android.util.Log.w("KTrader", "[MainActivity] ViewPager adapter is null");
                    }
                } else {
                    android.util.Log.w("KTrader", "[MainActivity] ViewPager is null");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "새로고침 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 빗썸 앱을 실행하는 메서드
     */
    private void launchBithumbApp() {
        try {
            android.util.Log.d("KTrader", "[MainActivity] Attempting to launch Bithumb app");
            
            // 설치된 모든 앱 중에서 빗썸 관련 앱 찾기
            android.util.Log.d("KTrader", "[MainActivity] Searching for installed Bithumb-related apps...");
            java.util.List<android.content.pm.ApplicationInfo> installedApps = getPackageManager().getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA);
            
            for (android.content.pm.ApplicationInfo appInfo : installedApps) {
                String packageName = appInfo.packageName;
                android.util.Log.d("KTrader", "[MainActivity] Checking package: " + packageName);
                
                if (packageName.toLowerCase().contains("bithumb") || packageName.toLowerCase().contains("btc")) {
                    android.util.Log.d("KTrader", "[MainActivity] Found potential Bithumb app: " + packageName);
                    
                    // 특정 액티비티로 직접 실행 시도
                    Intent specificIntent = new Intent();
                    specificIntent.setComponent(new android.content.ComponentName(packageName, "com.btckorea.bithumb.native_.presentation.MainNavigationActivity"));
                    specificIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    try {
                        startActivity(specificIntent);
                        android.util.Log.d("KTrader", "[MainActivity] Launched Bithumb app with specific activity: " + packageName);
                        Toast.makeText(this, "빗썸 앱을 실행합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    } catch (Exception e) {
                        android.util.Log.w("KTrader", "[MainActivity] Failed to launch specific activity for " + packageName + ": " + e.getMessage());
                    }
                    
                    // 일반적인 앱 실행 시도
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        android.util.Log.d("KTrader", "[MainActivity] Launching Bithumb app with package: " + packageName);
                        startActivity(launchIntent);
                        Toast.makeText(this, "빗썸 앱을 실행합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // 가능한 빗썸 앱 패키지명들
            String[] possiblePackages = {
                "com.btckorea.bithumb",
                "com.bithumb.android",
                "com.bithumb",
                "kr.co.bithumb"
            };
            
            // 각 패키지명을 시도해보기
            for (String packageName : possiblePackages) {
                android.util.Log.d("KTrader", "[MainActivity] Trying package: " + packageName);
                
                // 먼저 런치 인텐트가 있는지 확인
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    android.util.Log.d("KTrader", "[MainActivity] Package found with launch intent: " + packageName);
                    
                    // 특정 액티비티로 직접 실행 시도
                    Intent specificIntent = new Intent();
                    specificIntent.setComponent(new android.content.ComponentName(packageName, "com.btckorea.bithumb.native_.presentation.MainNavigationActivity"));
                    specificIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    try {
                        startActivity(specificIntent);
                        android.util.Log.d("KTrader", "[MainActivity] Launched Bithumb app with specific activity: " + packageName);
                        Toast.makeText(this, "빗썸 앱을 실행합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    } catch (Exception e) {
                        android.util.Log.w("KTrader", "[MainActivity] Failed to launch specific activity for " + packageName + ": " + e.getMessage());
                        
                        // 특정 액티비티 실패 시 일반 런치 인텐트 사용
                        android.util.Log.d("KTrader", "[MainActivity] Falling back to launch intent for: " + packageName);
                        startActivity(launchIntent);
                        Toast.makeText(this, "빗썸 앱을 실행합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    android.util.Log.d("KTrader", "[MainActivity] No launch intent found for package: " + packageName);
                }
            }
            
            // 모든 방법이 실패한 경우 Play Store로 이동
            android.util.Log.d("KTrader", "[MainActivity] All methods failed, redirecting to Play Store");
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
            playStoreIntent.setData(android.net.Uri.parse("market://details?id=com.btckorea.bithumb"));
            
            if (playStoreIntent.resolveActivity(getPackageManager()) != null) {
                android.util.Log.d("KTrader", "[MainActivity] Opening Play Store app");
                startActivity(playStoreIntent);
                Toast.makeText(this, "빗썸 앱을 설치해주세요.", Toast.LENGTH_LONG).show();
            } else {
                // Play Store 앱이 없는 경우 웹 브라우저로 이동
                android.util.Log.d("KTrader", "[MainActivity] Play Store app not found, opening web browser");
                Intent webIntent = new Intent(Intent.ACTION_VIEW);
                webIntent.setData(android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.btckorea.bithumb"));
                startActivity(webIntent);
                Toast.makeText(this, "빗썸 앱을 설치해주세요.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("KTrader", "[MainActivity] Error launching Bithumb app: " + e.getMessage());
            Toast.makeText(this, "빗썸 앱 실행 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 모든 DB 기록을 삭제하는 메서드
     */
    private void clearAllDatabaseRecords() {
        OrderRepository orderRepository = OrderRepository.getInstance(this);
        
        orderRepository.deleteAllOrders()
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Toast.makeText(this, "모든 DB 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show(),
                        throwable -> Toast.makeText(this, "DB 삭제 중 오류가 발생했습니다: " + throwable.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        android.util.Log.d("KTrader", "[MainActivity] onDestroy() 시작");

        try {
            Resources res = getResources();

            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            android.util.Log.d("KTrader", "[MainActivity] PendingIntent 생성 완료");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "exit_channel");

            builder.setContentTitle("Exit program")
                    .setContentText("OnDestroy is called")
                    .setTicker("OnDestroy is called")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_ALL);

            builder.setCategory(Notification.CATEGORY_MESSAGE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);

            android.util.Log.d("KTrader", "[MainActivity] NotificationCompat.Builder 생성 완료");

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (nm != null) {
                android.util.Log.d("KTrader", "[MainActivity] NotificationManager 획득 성공");
                
                // 안드로이드 8.0 이상 노티피케이션을 사용하기 위해서는 하나 이상의 알림 채널을 만들어야한다.
                NotificationChannel exitChannel = new NotificationChannel("exit_channel", "Exit Notifications", NotificationManager.IMPORTANCE_DEFAULT);
                exitChannel.setDescription("Exit notification channel");
                exitChannel.enableLights(true);
                exitChannel.setLightColor(Color.RED);

                android.util.Log.d("KTrader", "[MainActivity] NotificationChannel 생성 완료 - ID: exit_channel");
                
                nm.createNotificationChannel(exitChannel);
                android.util.Log.d("KTrader", "[MainActivity] NotificationChannel 등록 완료");
                
                // 채널 생성 확인
                NotificationChannel createdChannel = nm.getNotificationChannel("exit_channel");
                if (createdChannel != null) {
                    android.util.Log.d("KTrader", "[MainActivity] 채널 생성 확인 성공 - 중요도: " + createdChannel.getImportance());
                } else {
                    android.util.Log.e("KTrader", "[MainActivity] 채널 생성 확인 실패");
                }
                
                nm.notify(999, builder.build());
                android.util.Log.d("KTrader", "[MainActivity] Exit Notification 등록 완료 - ID: 999");
            } else {
                android.util.Log.e("KTrader", "[MainActivity] NotificationManager 획득 실패");
            }
        } catch (Exception e) {
            android.util.Log.e("KTrader", "[MainActivity] onDestroy() Notification 오류", e);
        }
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
            android.util.Log.d("KTrader", "[MainActivity] received progress : " + String.valueOf(progress));

            // ProgressDialogManager를 사용하여 다이얼로그 표시
            ProgressDialogManager.show(MainActivity.this, progress);
        }
    }

    static class MyTask extends TimerTask {
        public void run() {
            // ProgressDialogManager를 사용하여 다이얼로그 닫기
            ProgressDialogManager.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /**
     * 현재 테마에 따라 Status Bar 색상을 설정하는 메서드
     */
    private void setStatusBarColorByTheme() {
        // 현재 테마가 Light 테마인지 확인
        boolean isLightTheme = isLightTheme();
        
        int statusBarColor;
        if (isLightTheme) {
            statusBarColor = ContextCompat.getColor(this, R.color.status_bar_light);
            // Light 테마에서는 Status bar 아이콘을 어둡게 설정
            getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            statusBarColor = ContextCompat.getColor(this, R.color.status_bar_dark);
            // Dark 테마에서는 Status bar 아이콘을 밝게 설정
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
            
        getWindow().setStatusBarColor(statusBarColor);
    }
    
    /**
     * 현재 테마가 Light 테마인지 확인하는 메서드
     */
    private boolean isLightTheme() {
        // Android 시스템의 다크 모드 설정 확인
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * 현재 테마에 따라 App Bar 색상을 설정하는 메서드
     */
    private void setAppBarColorByTheme() {
        // 현재 테마가 Light 테마인지 확인
        boolean isLightTheme = isLightTheme();
        
        int appBarColor;
        if (isLightTheme) {
            appBarColor = ContextCompat.getColor(this, R.color.app_bar_light);
        } else {
            appBarColor = ContextCompat.getColor(this, R.color.app_bar_dark);
        }
        
        // ActionBar 색상 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(appBarColor));
            
            // ActionBar 그림자 효과 제거
            getSupportActionBar().setElevation(0);
            
            // 텍스트 색상 설정 (흰색 배경에서는 어두운 색 사용)
            if (isLightTheme) {
                // Light 테마에서는 어두운 텍스트 색상 사용
                setActionBarTitleColor(ContextCompat.getColor(this, android.R.color.black));
            } else {
                // Dark 테마에서는 밝은 텍스트 색상 사용
                setActionBarTitleColor(ContextCompat.getColor(this, android.R.color.white));
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
    
    /**
     * 현재 테마에 따라 메뉴 아이콘 색상을 설정하는 메서드
     */
    private void setMenuIconColorsByTheme(android.view.Menu menu) {
        // 현재 테마가 Light 테마인지 확인
        boolean isLightTheme = isLightTheme();
        
        int iconColor;
        if (isLightTheme) {
            // Light 테마에서는 어두운 색상 사용
            iconColor = ContextCompat.getColor(this, android.R.color.black);
        } else {
            // Dark 테마에서는 밝은 색상 사용
            iconColor = ContextCompat.getColor(this, android.R.color.white);
        }
        
        // 각 메뉴 아이템의 아이콘 색상 설정
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        MenuItem bithumbItem = menu.findItem(R.id.action_bithumb);
        
        if (refreshItem != null) {
            android.graphics.drawable.Drawable refreshIcon = refreshItem.getIcon();
            if (refreshIcon != null) {
                refreshIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
                refreshItem.setIcon(refreshIcon);
            }
        }
        
        if (bithumbItem != null) {
            android.graphics.drawable.Drawable bithumbIcon = bithumbItem.getIcon();
            if (bithumbIcon != null) {
                bithumbIcon.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
                bithumbItem.setIcon(bithumbIcon);
            }
        }
    }
    
    /**
     * Appbar의 마지막 동기화 시간 업데이트
     */
    public void updateLastSyncTime() {
        android.util.Log.d("KTrader", "[MainActivity] updateLastSyncTime() called");
        android.util.Log.d("KTrader", "[MainActivity] textLastSyncTime: " + (textLastSyncTime != null ? "not null" : "null"));
        
        if (textLastSyncTime != null) {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String currentTime = timeFormat.format(new java.util.Date());
            android.util.Log.d("KTrader", "[MainActivity] Generated time: " + currentTime);
            
            textLastSyncTime.setText(currentTime);
            android.util.Log.d("KTrader", "[MainActivity] Updated last sync time in Appbar: " + currentTime);
        } else {
            android.util.Log.w("KTrader", "[MainActivity] Cannot update last sync time - textLastSyncTime is null");
        }
    }
    
    /**
     * 테마에 따라 Appbar 텍스트 색상 설정
     */
    private void setAppBarTextColorsByTheme() {
        boolean isLightTheme = isLightTheme();
        
        int textColor;
        if (isLightTheme) {
            textColor = ContextCompat.getColor(this, android.R.color.black);
        } else {
            textColor = ContextCompat.getColor(this, android.R.color.white);
        }
        
        if (textAppTitle != null) {
            textAppTitle.setTextColor(textColor);
        }
        
        if (textLastSyncTime != null) {
            textLastSyncTime.setTextColor(textColor);
        }
        
        android.util.Log.d("KTrader", "[MainActivity] Set Appbar text colors - Theme: " + (isLightTheme ? "Light" : "Dark") + ", Color: " + (isLightTheme ? "Black" : "White"));
    }
    
    /**
     * 현재 선택된 Fragment에 따라 스크롤 기능 실행
     */
    private void scrollToBottom() {
        android.util.Log.d("KTrader", "[MainActivity] scrollToBottom() called");
        
        try {
            if (viewPager != null) {
                int currentItem = viewPager.getCurrentItem();
                android.util.Log.d("KTrader", "[MainActivity] Current tab: " + currentItem);
                
                // FragmentManager를 통해 현재 Fragment 가져오기
                android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                android.support.v4.app.Fragment currentFragment = fragmentManager.findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + currentItem);
                
                if (currentFragment != null) {
                    android.util.Log.d("KTrader", "[MainActivity] Current fragment: " + currentFragment.getClass().getSimpleName());
                    
                    if (currentItem == 0) {
                        // Transaction Item 탭
                        if (currentFragment instanceof TransactionItemFragment) {
                            TransactionItemFragment transactionItemFragment = (TransactionItemFragment) currentFragment;
                            transactionItemFragment.scrollToBottom();
                            android.util.Log.d("KTrader", "[MainActivity] Scrolled TransactionItemFragment to bottom");
                        } else {
                            android.util.Log.w("KTrader", "[MainActivity] Fragment is not TransactionItemFragment");
                        }
                    } else if (currentItem == 1) {
                        // Transaction Log 탭
                        if (currentFragment instanceof TransactionLogFragment) {
                            TransactionLogFragment transactionLogFragment = (TransactionLogFragment) currentFragment;
                            transactionLogFragment.scrollToBottom();
                            android.util.Log.d("KTrader", "[MainActivity] Scrolled TransactionLogFragment to bottom");
                        } else {
                            android.util.Log.w("KTrader", "[MainActivity] Fragment is not TransactionLogFragment");
                        }
                    } else {
                        android.util.Log.w("KTrader", "[MainActivity] Unknown tab: " + currentItem);
                    }
                } else {
                    android.util.Log.w("KTrader", "[MainActivity] Current fragment is null");
                }
            } else {
                android.util.Log.w("KTrader", "[MainActivity] ViewPager is null");
            }
        } catch (Exception e) {
            android.util.Log.e("KTrader", "[MainActivity] Error in scrollToBottom()", e);
            Toast.makeText(this, "스크롤 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
