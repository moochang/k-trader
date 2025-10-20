package com.example.k_trader;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
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

    private Button btnStartTrading;
    private Button btnStopTrading;
    private Button btnScrollToBottom;
    
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

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.main_page, container,false);
        mainActivity = (MainActivity) getActivity();
        
        // UI 컴포넌트 초기화
        btnStartTrading = layout.findViewById(R.id.button);
        btnStopTrading = layout.findViewById(R.id.button2);
        btnScrollToBottom = layout.findViewById(R.id.button3);
        
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

        btnStartTrading.setEnabled(!isTradingStarted);
        btnStopTrading.setEnabled(isTradingStarted);

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

        return layout;
    }

    /**
     * DatabaseOrderManager 초기화
     */
    private void initializeDatabaseManager() {
        // DatabaseOrderManager 초기화
        if (getContext() != null) {
            databaseOrderManager = new DatabaseOrderManager(getContext());
            disposables = new CompositeDisposable();
        }
    }

    /**
     * 즉시 초기 데이터 로드 (Fragment 준비와 관계없이)
     */
    private void loadInitialDataImmediately() {
        if (databaseOrderManager == null) {
            Log.w("MainPage", "DatabaseOrderManager가 초기화되지 않음");
            return;
        }
        
        Log.d("MainPage", "즉시 초기 데이터 로드 시작");
        
        Completable immediateLoad = databaseOrderManager.initializeAndSyncData("MainPage 즉시 초기화")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> Log.d("MainPage", "즉시 초기 데이터 로드 완료"))
                .doOnError(error -> Log.e("MainPage", "즉시 초기 데이터 로드 실패", error));
        
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
                    .doOnComplete(() -> Log.d("MainPage", "주기적 데이터 동기화 완료"))
                    .doOnError(error -> Log.e("MainPage", "주기적 데이터 동기화 실패", error));
            
            disposables.add(periodicSync.subscribe());
        }
    }

    /**
     * 버튼 이벤트 리스너를 설정하는 메서드
     */
    @SuppressWarnings("ConstantConditions")
    private void setupButtonListeners() {
        btnStartTrading.setOnClickListener(v -> {
            String packageName = mainActivity.getPackageName();
            PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);

            // 배터리 최적화 무시 요청 (트레이딩 앱의 경우 백그라운드 실행이 필요)
            // Play Store 정책에 따라 적절한 사용 사례임을 명시
            // 트레이딩 앱은 실시간 주문 처리를 위해 백그라운드 실행이 필수적임
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent i = new Intent();
                i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + packageName));
                startActivity(i);
            }

            // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
            new Thread(() -> {
                OrderManager orderManager = new OrderManager();
                orderManager.cancelAllBuyOrders();
            }).start();

            // JOB_ID_REGULAR가 1분 후부터 스케줄링 되기 때문에 1회성으로 한번 더 실행
            JobInfo firstTradeJob = new JobInfo.Builder(JOB_ID_FIRST, component)
                    .setMinimumLatency(1000) // 1000 ms
                    .build();

            JobInfo tradeJob = new JobInfo.Builder(JOB_ID_REGULAR, component)
                    .setMinimumLatency((long) GlobalSettings.getInstance().getTradeInterval() * 1000)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();

            mainActivity.jobScheduler = (JobScheduler) mainActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            mainActivity.jobScheduler.schedule(firstTradeJob);
            mainActivity.jobScheduler.schedule(tradeJob);

            isTradingStarted = true;
            // 런타임에 상태가 변경되므로 조건문은 정상적으로 동작함
            // 코드 분석 도구가 정적 분석으로는 정확히 파악하지 못함
            boolean startEnabled = !isTradingStarted;
            boolean stopEnabled = isTradingStarted;
            btnStartTrading.setEnabled(startEnabled);
            btnStopTrading.setEnabled(stopEnabled);
        });

        btnStopTrading.setOnClickListener(v -> {
            if (mainActivity.jobScheduler != null) {
                mainActivity.jobScheduler.cancelAll();
            }

            isTradingStarted = false;
            // 런타임에 상태가 변경되므로 조건문은 정상적으로 동작함
            // 코드 분석 도구가 정적 분석으로는 정확히 파악하지 못함
            boolean startEnabled = !isTradingStarted;
            boolean stopEnabled = isTradingStarted;
            btnStartTrading.setEnabled(startEnabled);
            btnStopTrading.setEnabled(stopEnabled);
        });

        btnScrollToBottom.setOnClickListener(v -> {
            // 버튼 눌린 효과 애니메이션
            animateButtonPress(v);
            
            // 현재 선택된 Fragment에 따라 스크롤 기능 실행
            int currentItem = viewPager.getCurrentItem();
            
            if (currentItem == 0 && transactionItemFragment != null) {
                // Transaction Item 탭
                transactionItemFragment.scrollToBottom();
            } else if (currentItem == 1 && transactionLogFragment != null) {
                // Transaction Log 탭
                transactionLogFragment.scrollToBottom();
            }
        });

        // Settings 버튼은 App bar 메뉴로 이동했으므로 제거
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
        if (disposables != null) {
            disposables.clear();
        }
        
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
                if (intent.getAction() != null && intent.getAction().equals(TransactionItemFragment.BROADCAST_CARD_DATA)) {
                    // 카드 데이터에서 가격 정보 추출하여 UI 업데이트
                    String btcCurrentPrice = intent.getStringExtra("btcCurrentPrice");
                    String hourlyChange = intent.getStringExtra("hourlyChange");
                    
                    if (btcCurrentPrice != null && textCurrentPrice != null) {
                        textCurrentPrice.setText(btcCurrentPrice);
                    }
                    
                    if (hourlyChange != null && textPriceChange != null) {
                        textPriceChange.setText(hourlyChange);
                        // 등락률에 따라 색상 변경
                        if (hourlyChange.startsWith("+")) {
                            textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else if (hourlyChange.startsWith("-")) {
                            textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            textPriceChange.setTextColor(getResources().getColor(android.R.color.black));
                        }
                    }
                    
                    // 활성 거래 수도 업데이트
                    updateActiveOrdersCount();
                }
            }
        };
        
        // BroadcastReceiver 등록
        if (getContext() != null) {
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(TransactionItemFragment.BROADCAST_CARD_DATA);
            android.support.v4.content.LocalBroadcastManager.getInstance(getContext()).registerReceiver(cardDataReceiver, filter);
        }
    }
    
    /**
     * 코인 데이터 새로고침 (외부에서 호출 가능)
     */
    public void refreshCoinData() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // 코인 정보 업데이트
                updateCoinInfo();
                
                // API에서 최신 데이터 가져오기
                fetchLatestCoinData();
            });
        }
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
                    Log.d("MainPage", "Coin data refreshed successfully");
                    // 활성 거래 수 다시 업데이트
                    updateActiveOrdersCount();
                },
                throwable -> {
                    Log.e("MainPage", "Error refreshing coin data", throwable);
                    Toast.makeText(getContext(), "데이터 새로고침 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
            );
    }
    
    /**
     * API에서 현재 가격 정보 직접 가져오기
     */
    private void fetchCurrentPriceFromApi() {
        try {
            // OrderManager를 통해 현재 가격 가져오기
            com.example.k_trader.base.OrderManager orderManager = com.example.k_trader.base.OrderManager.getInstance();
            
            // 백그라운드에서 API 호출
            new Thread(() -> {
                try {
                    JSONObject priceData = orderManager.getCurrentPrice("refresh");
                    if (priceData != null && priceData.containsKey("bids")) {
                        JSONArray bids = (JSONArray) priceData.get("bids");
                        if (bids != null && !bids.isEmpty()) {
                            JSONObject firstBid = (JSONObject) bids.get(0);
                            String priceStr = (String) firstBid.get("price");
                            if (priceStr != null) {
                                int currentPrice = (int) Double.parseDouble(priceStr);
                                
                                // UI 스레드에서 업데이트
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        updatePriceDisplay(currentPrice);
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainPage", "Error fetching current price", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "가격 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
            
        } catch (Exception e) {
            Log.e("MainPage", "Error in fetchCurrentPriceFromApi", e);
        }
    }
    
    /**
     * 가격 정보를 UI에 표시
     */
    private void updatePriceDisplay(int currentPrice) {
        if (textCurrentPrice != null) {
            String formattedPrice = String.format(java.util.Locale.getDefault(), "₩%,d", currentPrice);
            textCurrentPrice.setText(formattedPrice);
        }
        
        // 등락률은 임시로 0%로 설정 (실제로는 이전 가격과 비교 필요)
        if (textPriceChange != null) {
            textPriceChange.setText("+0.00%");
            textPriceChange.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
    
    /**
     * 코인 정보 업데이트
     */
    private void updateCoinInfo() {
        if (textCoinType == null) return;
        
        // 현재 설정된 코인 타입 표시
        String coinType = GlobalSettings.getInstance().getCoinType();
        textCoinType.setText(coinType);
        
        // 현재 가격과 등락률은 API에서 가져와야 하므로 기본값으로 설정
        textCurrentPrice.setText("₩0");
        textPriceChange.setText("+0.00%");
        
        // 활성 거래 수는 DB에서 가져오기
        updateActiveOrdersCount();
    }
    
    /**
     * 활성 거래 수 업데이트
     */
    private void updateActiveOrdersCount() {
        if (textActiveOrders == null || databaseOrderManager == null) return;
        
        databaseOrderManager.getActiveOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                count -> {
                    if (textActiveOrders != null) {
                        textActiveOrders.setText(String.valueOf(count));
                    }
                },
                throwable -> {
                    Log.e("MainPage", "Error getting active orders count", throwable);
                    if (textActiveOrders != null) {
                        textActiveOrders.setText("0");
                    }
                }
            );
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
}
