package com.example.k_trader;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.OrderManager;
import com.example.k_trader.base.DatabaseOrderManager;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * Created by 김무창 on 2017-12-20.
 */

public class MainPage extends Fragment {

    public static final int JOB_ID_FIRST = 1;
    public static final int JOB_ID_REGULAR = 2;

    private static final String KEY_TRADING_STATE = "KEY_TRADING_STATE";

    private android.support.design.widget.FloatingActionButton fabTradingToggle;
    
    // 코인 정보 표시용 TextView들
    private TextView textCoinType;
    private TextView textCurrentPrice;
    private TextView textPriceChange;
    private TextView textActiveOrders;
    // private Button btnPreference; // App bar 메뉴로 이동
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private ComponentName component;
    private MainActivity mainActivity;
    private boolean isTradingStarted = false;
    private DatabaseOrderManager databaseOrderManager;
    private CompositeDisposable disposables;
    private TransactionItemFragment transactionItemFragment;
    private TransactionLogFragment transactionLogFragment;
    
    // BroadcastReceiver for card data updates
    private BroadcastReceiver cardDataReceiver;
    
    // 실시간 관찰을 위한 필드들
    private com.example.k_trader.database.CoinPriceInfoRepository coinPriceInfoRepository;
    private com.example.k_trader.database.TransactionInfoRepository transactionInfoRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.main_page, container,false);
        mainActivity = (MainActivity) getActivity();
        
        // UI 컴포넌트 초기화
        fabTradingToggle = layout.findViewById(R.id.fabTradingToggle);
        
        // 코인 정보 TextView들 초기화
        textCoinType = layout.findViewById(R.id.textCoinType);
        textCurrentPrice = layout.findViewById(R.id.textCurrentPrice);
        textPriceChange = layout.findViewById(R.id.textPriceChange);
        textActiveOrders = layout.findViewById(R.id.textActiveOrders);
        
        // btnPreference = layout.findViewById(R.id.imageButtonPreference); // App bar 메뉴로 이동
        tabLayout = layout.findViewById(R.id.tabLayout);
        viewPager = layout.findViewById(R.id.viewPager);

        // ViewPager와 TabLayout 설정
        setupViewPagerAndTabs();

        // 상태 복원
        if (savedInstanceState != null) {
            isTradingStarted = savedInstanceState.getBoolean(KEY_TRADING_STATE);
        }

        // Floating Action Button 상태 초기화
        updateTradingToggleButton(isTradingStarted);

        // JobScheduler 초기화
        initializeJobScheduler();

        // 버튼 이벤트 설정
        setupButtonListeners();

        // DatabaseOrderManager 초기화
        initializeDatabaseManager();

        // 즉시 초기 데이터 로드 시작 (Fragment 생성 후 약간의 지연)
        viewPager.postDelayed(this::loadInitialDataImmediately, 1000); // 1초 지연
        
        // 코인 정보 초기화
        updateCoinInfo();
        
        // BroadcastReceiver 초기화 및 등록
        setupCardDataReceiver();
        
        // 실시간 관찰 시작
        startReactiveObservations();

        return layout;
    }

    /**
     * DatabaseOrderManager 초기화
     */
    private void initializeDatabaseManager() {
        // DatabaseOrderManager 초기화
        if (getContext() != null) {
            databaseOrderManager = new DatabaseOrderManager(getContext());
            coinPriceInfoRepository = new com.example.k_trader.database.CoinPriceInfoRepository(getContext());
            transactionInfoRepository = new com.example.k_trader.database.TransactionInfoRepository(getContext());
            disposables = new CompositeDisposable();
        }
    }

    /**
     * 즉시 초기 데이터 로드 (Fragment 준비와 관계없이)
     */
    private void loadInitialDataImmediately() {
        if (databaseOrderManager == null) {
            Log.w("[K-TR]", "[MainPage] DatabaseOrderManager가 초기화되지 않음");
            return;
        }
        
        Log.d("[K-TR]", "[MainPage] 즉시 초기 데이터 로드 시작");
        
        Completable immediateLoad = databaseOrderManager.initializeAndSyncData("MainPage 즉시 초기화")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> Log.d("[K-TR]", "[MainPage] 즉시 초기 데이터 로드 완료"))
                .doOnError(error -> Log.e("[K-TR]", "[MainPage] 즉시 초기 데이터 로드 실패", error));
        
        disposables.add(immediateLoad.subscribe());
    }

    /**
     * 버튼 눌린 효과 애니메이션
     */
    private void animateButtonPress(View button) {
        // 스케일 다운 애니메이션
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }

    /**
     * ViewPager와 TabLayout을 설정하는 메서드
     */
    private void setupViewPagerAndTabs() {
        // ViewPager 어댑터 설정
        TransactionPagerAdapter pagerAdapter = new TransactionPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        
        // TabLayout과 ViewPager 연결
        tabLayout.setupWithViewPager(viewPager);
        
        // 기본 탭을 Transaction Item으로 설정 (첫 번째 탭)
        viewPager.setCurrentItem(0);
    }

    /**
     * JobScheduler를 초기화하는 메서드
     */
    private void initializeJobScheduler() {
        // page switching으로 인한 재방문이 아닌 첫 방문일 때만 초기화 한다.
        if (mainActivity.jobScheduler == null) {
            component = new ComponentName(mainActivity, TradeJobService.class.getName());
        }
        
        // 주기적인 데이터 동기화 설정 (5분마다)
        if (databaseOrderManager != null) {
            Completable periodicSync = databaseOrderManager.periodicSyncData("주기적 동기화")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .repeat()
                    .delay(5, java.util.concurrent.TimeUnit.MINUTES)
                    .doOnComplete(() -> Log.d("[K-TR]", "[MainPage] 주기적 데이터 동기화 완료"))
                    .doOnError(error -> Log.e("[K-TR]", "[MainPage] 주기적 데이터 동기화 실패", error));
            
            disposables.add(periodicSync.subscribe());
        }
    }

    /**
     * 버튼 이벤트 리스너를 설정하는 메서드
     */
    @SuppressWarnings("ConstantConditions")
    private void setupButtonListeners() {
        fabTradingToggle.setOnClickListener(v -> {
            if (isTradingStarted) {
                stopTrading();
            } else {
                startTrading();
            }
        });
    }
    
    /**
     * 트레이딩 시작
     */
    private void startTrading() {
        Log.d("KTrader", "[MainPage] Start Trading button clicked");
        Log.d("KTrader", "[MainPage] mainActivity: " + (mainActivity != null ? "not null" : "null"));
        Log.d("KTrader", "[MainPage] component: " + (component != null ? "not null" : "null"));
        
        String packageName = mainActivity.getPackageName();
        PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);

        // 배터리 최적화 무시 요청 (트레이딩 앱의 경우 백그라운드 실행이 필요)
        // Play Store 정책에 따라 적절한 사용 사례임을 명시
        // 트레이딩 앱은 실시간 주문 처리를 위해 백그라운드 실행이 필수적임
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Log.d("KTrader", "[MainPage] Requesting battery optimization exemption");
            Intent i = new Intent();
            i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + packageName));
            startActivity(i);
        } else {
            Log.d("KTrader", "[MainPage] Battery optimization already exempted");
        }

        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
        Log.d("KTrader", "[MainPage] Canceling existing buy orders");
        new Thread(() -> {
            OrderManager orderManager = new OrderManager();
            orderManager.cancelAllBuyOrders();
            Log.d("KTrader", "[MainPage] Existing buy orders canceled");
        }).start();

        // JOB_ID_REGULAR가 1분 후부터 스케줄링 되기 때문에 1회성으로 한번 더 실행
        Log.d("KTrader", "[MainPage] Creating job schedules");
        JobInfo firstTradeJob = new JobInfo.Builder(JOB_ID_FIRST, component)
                .setMinimumLatency(1000) // 1000 ms
                .build();

        JobInfo tradeJob = new JobInfo.Builder(JOB_ID_REGULAR, component)
                .setMinimumLatency((long) GlobalSettings.getInstance().getTradeInterval() * 1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        Log.d("KTrader", "[MainPage] Trade interval: " + GlobalSettings.getInstance().getTradeInterval() + " seconds");

        mainActivity.jobScheduler = (JobScheduler) mainActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (mainActivity.jobScheduler != null) {
            int firstJobResult = mainActivity.jobScheduler.schedule(firstTradeJob);
            int regularJobResult = mainActivity.jobScheduler.schedule(tradeJob);
            Log.d("KTrader", "[MainPage] Job scheduling results - First: " + firstJobResult + ", Regular: " + regularJobResult);
        } else {
            Log.e("KTrader", "[MainPage] JobScheduler is null");
        }

        isTradingStarted = true;
        updateTradingToggleButton(isTradingStarted);
        Log.d("KTrader", "[MainPage] Trading started successfully");
    }
    
    /**
     * 트레이딩 중지
     */
    private void stopTrading() {
        Log.d("KTrader", "[MainPage] Stop Trading button clicked");
        
        if (mainActivity.jobScheduler != null) {
            mainActivity.jobScheduler.cancelAll();
            Log.d("KTrader", "[MainPage] All jobs canceled");
        }

        isTradingStarted = false;
        updateTradingToggleButton(isTradingStarted);
        Log.d("KTrader", "[MainPage] Trading stopped successfully");
    }
    
    /**
     * 트레이딩 토글 버튼 상태 업데이트
     */
    private void updateTradingToggleButton(boolean isTradingStarted) {
        if (fabTradingToggle != null) {
            if (isTradingStarted) {
                // 트레이딩 중 - Stop 아이콘과 빨간색
                fabTradingToggle.setImageResource(android.R.drawable.ic_media_pause);
                fabTradingToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(android.R.color.holo_red_dark)));
            } else {
                // 트레이딩 중지 - Play 아이콘과 초록색
                fabTradingToggle.setImageResource(android.R.drawable.ic_media_play);
                fabTradingToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#4CAF50")));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // SettingsActivity에서 돌아올 때 코인 정보 업데이트
        Log.d("KTrader", "[MainPage] onResume - updating coin info");
        updateCoinInfo();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TRADING_STATE, isTradingStarted);
    }

    // 주어진 코인 가격에 대한 이익금(EARNINGS_RATIO)을 리턴한다.
    // 매도 리스트를 discrete하게 만들기 위해 주어진 가격에서 가장 앞자리만 남기고 절사한 금액의 이익금을 계산한다.
    // 예를 들어 주어진 가격이 4,325만원이라면 4,000으로 절사하고 그 EARNINGS_RATIO 금액(ex: earnings_ratio가 1%인 경우 40만원)을 리턴
    public static int getProfitPrice(int basePrice) {
        return (int)(getFloorPrice(basePrice) * (GlobalSettings.getInstance().getEarningRate() / 100.0));
    }

    // 주어진 코인 가격에 대한 매수구간(BUY_INTERVAL)을 리턴한다.
    // 매도 리스트를 discrete하게 만들기 위해 주어진 base price 가격에서 가장 앞자리만 남기고 절사한 금액을 사용한다.
    // 예를 들어 주어진 가격이 4,325만원이라면 4,000으로 절사하고 그 buy interval 금액(ex: 0.5%인 경우 20만원)을 리턴
    public static int getSlotIntervalPrice(int basePrice) {
        return (int)(getFloorPrice(basePrice) * (GlobalSettings.getInstance().getSlotIntervalRate() / 100.0));
    }

    public static int getProfitPrice() throws Exception {
        OrderManager orderManager = new OrderManager();
        int currentPrice;                  // 비트코인 현재 시장가

        JSONObject dataObj = orderManager.getCurrentPrice("");
        if (dataObj == null) {
            throw new Exception("Unknown network issue happens");
        }

        JSONArray dataArray = (JSONArray) dataObj.get("bids"); // 매수가
        if (dataArray != null && !dataArray.isEmpty()) {
            JSONObject item = (JSONObject) dataArray.get(0); // 기본 5개 아이템 중 첫번째 아이템 사용
            String priceStr = (String) item.get("price");
            if (priceStr != null) {
                currentPrice = (int) Double.parseDouble(priceStr);
                return getProfitPrice(currentPrice);
            }
        }

        throw new Exception("dataObj == null");
    }

    public static int getFloorPrice(int price) {
        int precision = 0;

        // 자리수 구하기
        while(price > 10) {
            price /= 10;
            precision++;

            // 천만 단위까지만 floor 시킴
            if (precision > 6)
                break;
        }

        // 절사된 floor value 구하기
        int floor = price;
        for (int i = 0; i<precision; i++) {
            floor *= 10;
        }

        return floor;
    }

    /**
     * Transaction Fragment들을 관리하는 PagerAdapter
     */
    public class TransactionPagerAdapter extends FragmentPagerAdapter {
        
        public TransactionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    transactionItemFragment = new TransactionItemFragment();
                    return transactionItemFragment;
                case 1:
                    transactionLogFragment = new TransactionLogFragment();
                    return transactionLogFragment;
                default:
                    transactionItemFragment = new TransactionItemFragment();
                    return transactionItemFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Transaction Item";
                case 1:
                    return "Transaction Log";
                default:
                    return "Transaction Item";
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // JobScheduler 정리
        if (component != null && getContext() != null) {
            JobScheduler jobScheduler = (JobScheduler) getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(JOB_ID_FIRST);
                jobScheduler.cancel(JOB_ID_REGULAR);
            }
        }
        
        // RxJava 리소스 정리
        stopReactiveObservations();
        
        // DatabaseOrderManager 정리
        if (databaseOrderManager != null) {
            databaseOrderManager.dispose();
        }
        
        // BroadcastReceiver 해제
        if (cardDataReceiver != null && getContext() != null) {
            android.support.v4.content.LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(cardDataReceiver);
        }

    }
    
    /**
     * BroadcastReceiver 설정
     */
    private void setupCardDataReceiver() {
        cardDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("KTrader", "[MainPage] BroadcastReceiver received: " + intent.getAction());
                
                // BROADCAST_CARD_DATA, BROADCAST_TRANSACTION_DATA만 처리
                if (intent.getAction() != null && 
                    (intent.getAction().equals(TransactionItemFragment.BROADCAST_CARD_DATA) ||
                     intent.getAction().equals(TransactionItemFragment.BROADCAST_TRANSACTION_DATA))) {
                    
                    // 카드 데이터에서 가격 정보 추출하여 UI 업데이트
                    String btcCurrentPrice = intent.getStringExtra("btcCurrentPrice");
                    String hourlyChange = intent.getStringExtra("hourlyChange");
                    String dailyChange = intent.getStringExtra("dailyChange");
                    
                    Log.d("KTrader", "[MainPage] Received data - Price: " + btcCurrentPrice + ", HourlyChange: " + hourlyChange + ", DailyChange: " + dailyChange);
                    
                    // 가격 정보가 null이거나 0인 경우 API를 직접 호출
                    if (btcCurrentPrice == null || btcCurrentPrice.equals("0") || btcCurrentPrice.equals("null")) {
                        Log.w("KTrader", "[MainPage] Price data is null or 0, calling API directly");
                        fetchCurrentPriceFromApi();
                    } else if (textCurrentPrice != null) {
                        textCurrentPrice.setText(btcCurrentPrice);
                        Log.d("KTrader", "[MainPage] Updated current price: " + btcCurrentPrice);
                    }
                    
                    // CoinInfo에는 전일 대비 등락률 표시
                    if (dailyChange != null && textPriceChange != null) {
                        textPriceChange.setText(dailyChange);
                        // 등락률에 따라 색상 변경 (+이면 빨간색, -이면 파란색)
                        if (dailyChange.startsWith("+")) {
                            textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else if (dailyChange.startsWith("-")) {
                            textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                        } else {
                            textPriceChange.setTextColor(getResources().getColor(android.R.color.black));
                        }
                        Log.d("KTrader", "[MainPage] Updated daily price change (CoinInfo): " + dailyChange);
                    }
                }
            }
        };
        
        // BroadcastReceiver 등록 - 두 액션 모두 등록
        if (getContext() != null) {
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(TransactionItemFragment.BROADCAST_CARD_DATA);
            filter.addAction(TransactionItemFragment.BROADCAST_TRANSACTION_DATA);
            android.support.v4.content.LocalBroadcastManager.getInstance(getContext()).registerReceiver(cardDataReceiver, filter);
            Log.d("KTrader", "[MainPage] BroadcastReceiver registered for both actions");
        }
    }
    
    /**
     * 코인 데이터 새로고침 (외부에서 호출 가능)
     */
    public void refreshCoinData() {
        Log.d("KTrader", "[MainPage] refreshCoinData() called - Thread: " + Thread.currentThread().getName());
        Log.d("KTrader", "[MainPage] refreshCoinData() - getActivity(): " + (getActivity() != null ? "not null" : "null"));
        Log.d("KTrader", "[MainPage] refreshCoinData() - getContext(): " + (getContext() != null ? "not null" : "null"));
        
        if (getActivity() != null) {
            Log.d("KTrader", "[MainPage] refreshCoinData() - scheduling UI thread task");
            getActivity().runOnUiThread(() -> {
                Log.d("KTrader", "[MainPage] refreshCoinData() - UI thread task started");
                Log.d("KTrader", "[MainPage] refreshCoinData() - calling updateCoinInfo()");
                
                // 코인 타입만 업데이트 (가격과 활성 거래 수는 이전 값 유지)
                updateCoinTypeOnly();
                
                Log.d("KTrader", "[MainPage] refreshCoinData() - calling fetchLatestCoinData()");
                // API에서 최신 데이터 가져오기
                fetchLatestCoinData();
                Log.d("KTrader", "[MainPage] refreshCoinData() - UI thread task completed");
            });
        } else {
            Log.w("KTrader", "[MainPage] refreshCoinData() - getActivity() is null, cannot proceed");
        }
    }
    
    /**
     * 코인 타입만 업데이트 (새로고침 시 깜박임 방지)
     */
    private void updateCoinTypeOnly() {
        Log.d("KTrader", "[MainPage] updateCoinTypeOnly() called");
        if (textCoinType == null) {
            Log.w("KTrader", "[MainPage] updateCoinTypeOnly() - textCoinType is null, returning");
            return;
        }
        
        // SharedPreferences에서 직접 코인 타입 읽어오기
        android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        String coinType = sharedPreferences.getString(com.example.k_trader.base.GlobalSettings.COIN_TYPE_KEY_NAME, com.example.k_trader.base.GlobalSettings.COIN_TYPE_DEFAULT_VALUE);
        
        Log.d("KTrader", "[MainPage] Reading coin type from preferences: " + coinType);
        
        // GlobalSettings도 업데이트
        com.example.k_trader.base.GlobalSettings.getInstance().setCoinType(coinType);
        
        // 코인 타입만 표시 (가격과 활성 거래 수는 이전 값 유지)
        textCoinType.setText(coinType);
        
        Log.d("KTrader", "[MainPage] Coin type updated, keeping previous price and active orders values");
    }
    
    /**
     * API에서 최신 코인 데이터 가져오기
     */
    private void fetchLatestCoinData() {
        if (databaseOrderManager == null) return;
        
        // 현재 설정된 코인 타입에 따라 API 호출
        String coinType = GlobalSettings.getInstance().getCoinType();
        
        // 직접 API 호출하여 가격 정보 가져오기
        fetchCurrentPriceFromApi();
        
        databaseOrderManager.periodicSyncData("refresh")
            .subscribe(
                () -> {
                    Log.d("KTrader", "[MainPage] Coin data refreshed successfully");
                    // 활성 거래 수 다시 업데이트
                    updateActiveOrdersCount();
                },
                throwable -> {
                    Log.e("KTrader", "[MainPage] Error refreshing coin data", throwable);
                    Toast.makeText(getContext(), "데이터 새로고침 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
            );
    }
    
    /**
     * API에서 현재 가격 및 등락률 정보 직접 가져오기
     */
    private void fetchCurrentPriceFromApi() {
        try {
            Log.d("KTrader", "[MainPage] Starting direct API call for price and change data...");
            
            // OrderManager를 통해 가격과 등락률 정보 가져오기
            com.example.k_trader.base.OrderManager orderManager = new com.example.k_trader.base.OrderManager();
            
            // 백그라운드에서 API 호출
            new Thread(() -> {
                try {
                    Log.d("KTrader", "[MainPage] Calling OrderManager APIs...");
                    
                    // 현재 가격 가져오기
                    JSONObject priceData = orderManager.getCurrentPrice("refresh");
                    int currentPrice = 0;
                    if (priceData != null && priceData.containsKey("bids")) {
                        JSONArray bids = (JSONArray) priceData.get("bids");
                        if (bids != null && !bids.isEmpty()) {
                            JSONObject firstBid = (JSONObject) bids.get(0);
                            String priceStr = (String) firstBid.get("price");
                            if (priceStr != null) {
                                currentPrice = (int) Double.parseDouble(priceStr);
                                Log.d("KTrader", "[MainPage] Got current price: " + currentPrice);
                            }
                        }
                    }
                    
                    // Ticker 정보에서 등락률 가져오기
                    String dailyChange = "+0.00%";
                    try {
                        JSONObject tickerData = orderManager.getTicker("refresh");
                        if (tickerData != null && tickerData.containsKey("data")) {
                            JSONObject data = (JSONObject) tickerData.get("data");
                            
                            if (data.containsKey("fluctate_rate_24H")) {
                                String rawDailyChange = data.get("fluctate_rate_24H").toString();
                                Log.d("KTrader", "[MainPage] Raw daily change: " + rawDailyChange);
                                try {
                                    double changeValue = Double.parseDouble(rawDailyChange);
                                    if (changeValue >= 0) {
                                        dailyChange = String.format("+%.2f%%", changeValue);
                                    } else {
                                        dailyChange = String.format("%.2f%%", changeValue);
                                    }
                                    Log.d("KTrader", "[MainPage] Formatted daily change: " + dailyChange);
                                } catch (NumberFormatException e) {
                                    Log.e("KTrader", "[MainPage] Error parsing daily change: " + rawDailyChange, e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("KTrader", "[MainPage] Error getting ticker data", e);
                    }
                    
                    final int finalCurrentPrice = currentPrice;
                    final String finalDailyChange = dailyChange;
                    
                    // UI 스레드에서 업데이트
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // 현재 가격 업데이트 (깜박임 방지)
                            if (textCurrentPrice != null && finalCurrentPrice > 0) {
                                String formattedPrice = String.format(java.util.Locale.getDefault(), "₩%,d", finalCurrentPrice);
                                textCurrentPrice.setText(formattedPrice);
                                Log.d("KTrader", "[MainPage] Updated current price: " + formattedPrice);
                            } else if (finalCurrentPrice <= 0) {
                                Log.d("KTrader", "[MainPage] Skipping price update - current price is 0 or invalid");
                            }
                            
                            // 전일 대비 등락률 업데이트 (CoinInfo용)
                            if (textPriceChange != null) {
                                textPriceChange.setText(finalDailyChange);
                                
                                // 등락률에 따라 색상 변경
                                if (finalDailyChange.startsWith("+")) {
                                    textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                } else if (finalDailyChange.startsWith("-")) {
                                    textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                                } else {
                                    textPriceChange.setTextColor(getResources().getColor(android.R.color.black));
                                }
                                Log.d("KTrader", "[MainPage] Updated daily price change: " + finalDailyChange);
                            }
                            
                            // 마지막 동기화 시간 업데이트
                            Log.d("KTrader", "[MainPage] About to call updateLastSyncTime()");
                            updateLastSyncTime();
                        });
                    }
                    
                } catch (Exception e) {
                    Log.e("KTrader", "[MainPage] Error fetching price and change data", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "가격 정보를 가져올 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
            
        } catch (Exception e) {
            Log.e("KTrader", "Error in fetchCurrentPriceFromApi", e);
        }
    }
    
    /**
     * 가격 정보를 UI에 표시
     */
    private void updatePriceDisplay(int currentPrice) {
        Log.d("KTrader", "Updating price display with: " + currentPrice);
        
        if (textCurrentPrice != null) {
            String formattedPrice = String.format(java.util.Locale.getDefault(), "₩%,d", currentPrice);
            textCurrentPrice.setText(formattedPrice);
            Log.d("KTrader", "Updated current price display: " + formattedPrice);
        } else {
            Log.w("KTrader", "textCurrentPrice is null");
        }
        
        // 등락률은 TransactionInfo에서 전일 대비 등락률로 업데이트됨
        // 여기서는 하드코딩하지 않음
    }
    
    /**
     * 코인 정보 업데이트
     */
    private void updateCoinInfo() {
        Log.d("KTrader", "[MainPage] updateCoinInfo() called");
        if (textCoinType == null) {
            Log.w("KTrader", "[MainPage] updateCoinInfo() - textCoinType is null, returning");
            return;
        }
        
        // SharedPreferences에서 직접 코인 타입 읽어오기
        android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        String coinType = sharedPreferences.getString(com.example.k_trader.base.GlobalSettings.COIN_TYPE_KEY_NAME, com.example.k_trader.base.GlobalSettings.COIN_TYPE_DEFAULT_VALUE);
        
        Log.d("KTrader", "[MainPage] Reading coin type from preferences: " + coinType);
        
        // GlobalSettings도 업데이트
        com.example.k_trader.base.GlobalSettings.getInstance().setCoinType(coinType);
        
        // 코인 타입 표시
        textCoinType.setText(coinType);
        
        // 현재 가격과 활성 거래 수는 이전 값을 유지 (깜박임 방지)
        // API 호출로 실제 값이 업데이트될 때까지 기존 값 유지
        Log.d("KTrader", "[MainPage] Keeping previous values to prevent flickering");
        
        // 활성 거래 수는 DB에서 가져오기
        Log.d("KTrader", "[MainPage] About to call updateActiveOrdersCount()");
        updateActiveOrdersCount();
    }
    
    /**
     * 활성 거래 수 업데이트 (API 직접 호출)
     */
    private void updateActiveOrdersCount() {
        Log.d("KTrader", "[MainPage] updateActiveOrdersCount() called");
        Log.d("KTrader", "[MainPage] textActiveOrders: " + (textActiveOrders != null ? "not null" : "null"));
        
        if (textActiveOrders == null) {
            Log.w("KTrader", "[MainPage] Cannot update active orders count - textActiveOrders is null");
            return;
        }
        
        // 현재 표시된 값이 없거나 기본값인 경우에만 초기값 설정
        String currentText = textActiveOrders.getText().toString();
        if (currentText.isEmpty() || currentText.equals("S0 : B0")) {
            textActiveOrders.setText("S0 : B0");
            Log.d("KTrader", "[MainPage] Set initial active orders count: S0 : B0");
        } else {
            Log.d("KTrader", "[MainPage] Keeping current active orders count: " + currentText);
        }
        
        Log.d("KTrader", "[MainPage] Updating active orders count from API...");
        
        // API에서 직접 활성 주문 조회
        new Thread(() -> {
            try {
                OrderManager orderManager = new OrderManager();
                JSONArray dataArray = orderManager.getPlacedOrderList("MainPage 활성 주문 조회");
                
                int sellCount = 0;
                int buyCount = 0;
                
                if (dataArray != null) {
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject item = (JSONObject) dataArray.get(i);
                        String type = (String) item.get("type");
                        
                        if ("ask".equals(type)) {
                            sellCount++;
                        } else if ("bid".equals(type)) {
                            buyCount++;
                        }
                    }
                }
                
                final String formattedText = "S" + sellCount + " : B" + buyCount;
                
                // UI 스레드에서 업데이트
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        textActiveOrders.setText(formattedText);
                        Log.d("KTrader", "[MainPage] Updated active orders count from API: " + formattedText);
                    });
                }
                
            } catch (Exception e) {
                Log.e("KTrader", "[MainPage] Error getting active orders from API", e);
                // API 호출 실패 시에만 기본값으로 설정 (깜박임 방지)
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (textActiveOrders != null) {
                            String currentValue = textActiveOrders.getText().toString();
                            if (currentValue.isEmpty() || currentValue.equals("S0 : B0")) {
                                textActiveOrders.setText("S0 : B0");
                                Log.d("KTrader", "[MainPage] Set default active orders count due to API error");
                            } else {
                                Log.d("KTrader", "[MainPage] Keeping current value despite API error: " + currentValue);
                            }
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 현재 가격과 등락률 업데이트 (API 호출 결과로부터)
     */
    public void updatePriceInfo(String currentPrice, String priceChange) {
        if (textCurrentPrice != null) {
            textCurrentPrice.setText(currentPrice);
        }
        if (textPriceChange != null) {
            textPriceChange.setText(priceChange);
            // 등락률에 따라 색상 변경
            if (priceChange.startsWith("+")) {
                textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (priceChange.startsWith("-")) {
                textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                textPriceChange.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }
    
    /**
     * 실시간 관찰 시작
     */
    private void startReactiveObservations() {
        Log.d("KTrader", "[MainPage] startReactiveObservations() called");
        Log.d("KTrader", "[MainPage] disposables: " + (disposables != null ? "not null" : "null"));
        Log.d("KTrader", "[MainPage] coinPriceInfoRepository: " + (coinPriceInfoRepository != null ? "not null" : "null"));
        Log.d("KTrader", "[MainPage] databaseOrderManager: " + (databaseOrderManager != null ? "not null" : "null"));
        
        if (disposables == null || coinPriceInfoRepository == null || databaseOrderManager == null || transactionInfoRepository == null) {
            Log.w("KTrader", "[MainPage] Cannot start reactive observations - required components are null");
            return;
        }
        
        Log.d("KTrader", "[MainPage] Starting reactive observations");
        
        // CoinInfo는 TransactionInfoEntity의 dailyChange만 사용하므로 CoinPriceInfoRepository 관찰 제거
        
        // 2. 활성 주문 수 실시간 관찰
        disposables.add(
            databaseOrderManager.observeActiveSellOrdersCount()
                .subscribe(
                    sellCount -> {
                        Log.d("KTrader", "[MainPage] SELL orders count updated: " + sellCount);
                        // BUY 주문 수도 함께 조회하여 업데이트
                        updateActiveOrdersDisplay(sellCount, null);
                    },
                    throwable -> Log.e("KTrader", "[MainPage] Error observing SELL orders count", throwable)
                )
        );
        
        disposables.add(
            databaseOrderManager.observeActiveBuyOrdersCount()
                .subscribe(
                    buyCount -> {
                        Log.d("KTrader", "[MainPage] BUY orders count updated: " + buyCount);
                        // SELL 주문 수도 함께 조회하여 업데이트
                        updateActiveOrdersDisplay(null, buyCount);
                    },
                    throwable -> Log.e("KTrader", "[MainPage] Error observing BUY orders count", throwable)
                )
        );
        
        // 3. Transaction 정보 실시간 관찰
        disposables.add(
            transactionInfoRepository.observeLatestTransactionInfo()
                .subscribe(
                    transactionInfo -> {
                        Log.d("KTrader", "[MainPage] Transaction info updated: " + transactionInfo.toString());
                        updateUIWithTransactionInfo(transactionInfo);
                    },
                    throwable -> Log.e("KTrader", "[MainPage] Error observing transaction info", throwable)
                )
        );
    }
    
    /**
     * 활성 주문 수 표시 업데이트
     */
    private void updateActiveOrdersDisplay(Integer sellCount, Integer buyCount) {
        if (textActiveOrders == null) return;
        
        // 현재 표시된 값을 파싱하여 업데이트
        String currentText = textActiveOrders.getText().toString();
        int currentSell = 0;
        int currentBuy = 0;
        
        try {
            // 새로운 형식 파싱: "S0 : B0"
            if (currentText.contains("S") && currentText.contains("B")) {
                String[] parts = currentText.split(" : ");
                if (parts.length == 2) {
                    currentSell = Integer.parseInt(parts[0].substring(1)); // "S" 제거
                    currentBuy = Integer.parseInt(parts[1].substring(1));   // "B" 제거
                }
            }
        } catch (Exception e) {
            Log.w("KTrader", "[MainPage] Error parsing current active orders text: " + currentText);
        }
        
        // 새로운 값으로 업데이트
        int newSell = sellCount != null ? sellCount : currentSell;
        int newBuy = buyCount != null ? buyCount : currentBuy;
        
        String newText = "S" + newSell + " : B" + newBuy;
        textActiveOrders.setText(newText);
        Log.d("KTrader", "[MainPage] Updated active orders display: " + newText);
    }
    
    /**
     * Transaction 정보로 UI 업데이트
     */
    private void updateUIWithTransactionInfo(com.example.k_trader.database.TransactionInfoEntity transactionInfo) {
        if (transactionInfo == null) return;
        
        // 현재 가격 업데이트
        if (textCurrentPrice != null && transactionInfo.getBtcCurrentPrice() != null) {
            textCurrentPrice.setText(transactionInfo.getBtcCurrentPrice());
            Log.d("KTrader", "[MainPage] Updated current price from DB: " + transactionInfo.getBtcCurrentPrice());
        }
        
        // 전일 대비 등락률 업데이트 (CoinInfo용)
        if (textPriceChange != null && transactionInfo.getDailyChange() != null) {
            textPriceChange.setText(transactionInfo.getDailyChange());
            
            // 등락률에 따라 색상 변경
            if (transactionInfo.getDailyChange().startsWith("+")) {
                textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else if (transactionInfo.getDailyChange().startsWith("-")) {
                textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                textPriceChange.setTextColor(getResources().getColor(android.R.color.black));
            }
            Log.d("KTrader", "[MainPage] Updated daily price change from DB: " + transactionInfo.getDailyChange());
        }
    }
    
    /**
     * 실시간 관찰 중지
     */
    private void stopReactiveObservations() {
        if (disposables != null && !disposables.isDisposed()) {
            Log.d("KTrader", "[MainPage] Stopping reactive observations");
            disposables.clear();
        }
    }
    
    /**
     * 마지막 동기화 시간 업데이트 (Appbar에 표시)
     */
    private void updateLastSyncTime() {
        Log.d("KTrader", "[MainPage] updateLastSyncTime() called");
        Log.d("KTrader", "[MainPage] mainActivity: " + (mainActivity != null ? "not null" : "null"));
        
        if (mainActivity != null) {
            Log.d("KTrader", "[MainPage] Calling mainActivity.updateLastSyncTime()");
            mainActivity.updateLastSyncTime();
            Log.d("KTrader", "[MainPage] Updated last sync time in Appbar");
        } else {
            Log.w("KTrader", "[MainPage] Cannot update last sync time - mainActivity is null");
        }
    }
}
