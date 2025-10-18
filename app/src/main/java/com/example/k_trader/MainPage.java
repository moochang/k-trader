package com.example.k_trader;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
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

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.OrderManager;
import com.google.gson.Gson;

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
    private Button btnPreference;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TransactionPagerAdapter pagerAdapter;

    private ComponentName component;
    private MainActivity mainActivity;
    private boolean isTradingStarted = false;

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
        btnPreference = layout.findViewById(R.id.imageButtonPreference);
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

        return layout;
    }

    /**
     * ViewPager와 TabLayout을 설정하는 메서드
     */
    private void setupViewPagerAndTabs() {
        // ViewPager 어댑터 설정
        pagerAdapter = new TransactionPagerAdapter(getChildFragmentManager());
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
    }

    /**
     * 버튼 이벤트 리스너를 설정하는 메서드
     */
    private void setupButtonListeners() {
        btnStartTrading.setOnClickListener(v -> {
            String packageName = mainActivity.getPackageName();
            PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent i = new Intent();
                i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + packageName));
                startActivity(i);
            }

            // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
            new Thread(() -> {
                // cancel all buy request
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
            btnStartTrading.setEnabled(!isTradingStarted);
            btnStopTrading.setEnabled(isTradingStarted);
        });

        btnStopTrading.setOnClickListener(v -> {
            if (mainActivity.jobScheduler != null) {
                mainActivity.jobScheduler.cancelAll();
            }

            isTradingStarted = false;
            btnStartTrading.setEnabled(!isTradingStarted);
            btnStopTrading.setEnabled(isTradingStarted);
        });

        btnScrollToBottom.setOnClickListener(v -> {
            // 현재 선택된 Fragment가 TransactionLogFragment인지 확인하고 스크롤
            TransactionLogFragment logFragment = (TransactionLogFragment) pagerAdapter.getItem(viewPager.getCurrentItem());
            if (logFragment != null) {
                logFragment.scrollToBottom();
            }
        });

        btnPreference.setOnClickListener(v -> {
            startActivity(new Intent(mainActivity, SettingActivity.class));
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_TRADING_STATE, isTradingStarted);
    }

    // 주어진 bitcoin 가격에 대한 이익금(EARNINGS_RATIO)을 리턴한다.
    // 매도 리스트를 discrete하게 만들기 위해 주어진 가격에서 가장 앞자리만 남기고 절사한 금액의 이익금을 계산한다.
    // 예를 들어 주어진 가격이 4,325만원이라면 4,000으로 절사하고 그 EARNINGS_RATIO 금액(ex: earnings_ratio가 1%인 경우 40만원)을 리턴
    public static int getProfitPrice(int basePrice) {
        return (int)(getFloorPrice(basePrice) * (GlobalSettings.getInstance().getEarningRate() / 100.0));
    }

    // 주어진 bitcoin 가격에 대한 매수구간(BUY_INTERVAL)을 리턴한다.
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
    public static class TransactionPagerAdapter extends FragmentPagerAdapter {
        
        public TransactionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new TransactionItemFragment();
                case 1:
                    return new TransactionLogFragment();
                default:
                    return new TransactionItemFragment();
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
}
