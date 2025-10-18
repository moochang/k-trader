package com.example.k_trader;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.OrderManager;
import com.google.gson.Gson;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * Created by 김무창 on 2017-12-20.
 */

public class MainPage extends Fragment {

    public static final String BROADCAST_LOG_MESSAGE = "TRADE_LOG";
    public static final String BROADCAST_CARD_DATA = "TRADE_CARD_DATA";
    public static final String BROADCAST_ERROR_CARD = "TRADE_ERROR_CARD";

    public static final int JOB_ID_FIRST = 1;
    public static final int JOB_ID_REGULAR = 2;

    private static final String KEY_TRADING_STATE = "KEY_TRADING_STATE";
    private static final String LOG_RECEIVER_STATE = "LOG_RECEIVER_STATE";

    private final static int MAX_BUFFER = 10000;

    EditText editText;
    Button btnStartTrading;
    Button btnStopTrading;
    Button btnScrollToBottom;
    Button btnPreference;
    TabLayout tabLayout;
    ScrollView scrollView;
    RecyclerView recyclerViewCards;
    CardAdapter cardAdapter;
    CheckBox checkBox;

    ComponentName component;
    MainActivity mainActivity;
    boolean isTradingStarted = false;
    LogReceiver logReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.main_page, container,false);
        mainActivity = (MainActivity) getActivity();
        editText = layout.findViewById(R.id.editText);
        btnStartTrading = layout.findViewById(R.id.button);
        btnStopTrading = layout.findViewById(R.id.button2);
        btnScrollToBottom = layout.findViewById(R.id.button3);
        btnPreference = layout.findViewById(R.id.imageButtonPreference);
        tabLayout = layout.findViewById(R.id.tabLayout);
        scrollView = layout.findViewById(R.id.scrollView1);
        recyclerViewCards = layout.findViewById(R.id.recyclerViewCards);
        
        // RecyclerView 설정
        recyclerViewCards.setLayoutManager(new LinearLayoutManager(getContext()));
        cardAdapter = new CardAdapter();
        recyclerViewCards.setAdapter(cardAdapter);
        checkBox = layout.findViewById(R.id.checkBox);

        if (savedInstanceState != null) {
            isTradingStarted = savedInstanceState.getBoolean(KEY_TRADING_STATE);
            String json = savedInstanceState.getString(LOG_RECEIVER_STATE);
            Gson gson = new Gson();
            logReceiver = gson.fromJson(json, LogReceiver.class);
        }

        btnStartTrading.setEnabled(!isTradingStarted);
        btnStopTrading.setEnabled(isTradingStarted);

        // page switching으로 인한 재방문이 아닌 첫 방문일 때만 초기화 한다.
        if (mainActivity.jobScheduler == null) {
            component = new ComponentName(mainActivity, TradeJobService.class.getName());

            if (logReceiver != null) {
                LocalBroadcastManager.getInstance(mainActivity.getApplicationContext()).unregisterReceiver(logReceiver);
            }
            IntentFilter theFilter = new IntentFilter();
            theFilter.addAction(BROADCAST_LOG_MESSAGE);
            theFilter.addAction(BROADCAST_CARD_DATA);
            theFilter.addAction(BROADCAST_ERROR_CARD);
            logReceiver = new LogReceiver();
            LocalBroadcastManager.getInstance(mainActivity.getApplicationContext()).registerReceiver(logReceiver, theFilter);
        }

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
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });

        btnPreference.setOnClickListener(v -> {
            startActivity(new Intent(mainActivity, SettingActivity.class));
        });

        // TabLayout 초기화 및 탭 추가
        setupTabLayout();

        return layout;
    }

    private void setupTabLayout() {
        if (tabLayout != null) {
            // Transaction Item 탭 추가 (첫 번째 탭)
            TabLayout.Tab itemTab = tabLayout.newTab();
            itemTab.setText(R.string.transaction_item);
            tabLayout.addTab(itemTab);
            
            // Transaction log 탭 추가 (두 번째 탭)
            TabLayout.Tab logTab = tabLayout.newTab();
            logTab.setText(R.string.transaction_log);
            tabLayout.addTab(logTab);
            
            // 기본 탭을 Transaction Item으로 설정 (첫 번째 탭)
            TabLayout.Tab defaultTab = tabLayout.getTabAt(0);
            if (defaultTab != null) {
                defaultTab.select();
            }
            
            // 탭 선택 리스너 설정
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    // 탭이 선택되었을 때의 동작
                    if (tab.getText().equals(getString(R.string.transaction_log))) {
                        // Transaction log 탭 선택 시
                        scrollView.setVisibility(View.VISIBLE);
                        recyclerViewCards.setVisibility(View.GONE);
                    } else if (tab.getText().equals(getString(R.string.transaction_item))) {
                        // Transaction Item 탭 선택 시
                        scrollView.setVisibility(View.GONE);
                        recyclerViewCards.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // 탭이 선택 해제되었을 때의 동작
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // 같은 탭이 다시 선택되었을 때의 동작
                }
            });
            
            // 초기 상태 설정 (Transaction Item 탭이 기본으로 선택됨)
            scrollView.setVisibility(View.GONE);
            recyclerViewCards.setVisibility(View.VISIBLE);
        }
    }

    private void log_info(final String log) {
        Runnable runnable = () -> {
            mainActivity.runOnUiThread(() -> {
                editText.append(log + "\r\n");

                CharSequence charSequence = editText.getText();
                if (charSequence.length() > MAX_BUFFER)
                    editText.getEditableText().delete(0, charSequence.length() - MAX_BUFFER);

                // scroll to bottom
                if (checkBox.isChecked()) {
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                }
            });
        };
        runnable.run();

        Log.d("KTrader", log);
    }

    private class LogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_LOG_MESSAGE)) {
                String log = intent.getStringExtra("log");
                log_info(log);
            } else if (intent.getAction().equals(BROADCAST_CARD_DATA)) {
                // 카드 데이터 처리
                String transactionTime = intent.getStringExtra("transactionTime");
                String btcCurrentPrice = intent.getStringExtra("btcCurrentPrice");
                String hourlyChange = intent.getStringExtra("hourlyChange");
                String estimatedBalance = intent.getStringExtra("estimatedBalance");
                String lastBuyPrice = intent.getStringExtra("lastBuyPrice");
                String lastSellPrice = intent.getStringExtra("lastSellPrice");
                String nextBuyPrice = intent.getStringExtra("nextBuyPrice");
                
                CardAdapter.TransactionCard card = new CardAdapter.TransactionCard(
                    transactionTime, btcCurrentPrice, hourlyChange, estimatedBalance,
                    lastBuyPrice, lastSellPrice, nextBuyPrice
                );
                
                if (cardAdapter != null) {
                    cardAdapter.addCard(card);
                }
            } else if (intent.getAction().equals(BROADCAST_ERROR_CARD)) {
                // 에러 카드 데이터 처리
                String errorTime = intent.getStringExtra("errorTime");
                String errorType = intent.getStringExtra("errorType");
                String errorMessage = intent.getStringExtra("errorMessage");
                String errorCode = intent.getStringExtra("errorCode");
                String logInfo = intent.getStringExtra("logInfo");
                
                CardAdapter.ErrorCard errorCard = new CardAdapter.ErrorCard(
                    errorTime, errorType, errorMessage, errorCode, logInfo
                );
                
                if (cardAdapter != null) {
                    cardAdapter.addErrorCard(errorCard);
                }
            }
        }
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_TRADING_STATE, isTradingStarted);

        Gson gson = new Gson();
        String json = gson.toJson(logReceiver);
        outState.putString(LOG_RECEIVER_STATE, json);
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

    // CardAdapter 클래스
    public static class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final java.util.List<Object> cardList = new java.util.ArrayList<>();
        
        private static final int TYPE_TRANSACTION = 0;
        private static final int TYPE_ERROR = 1;

        public static class TransactionCard {
            public String transactionTime;
            public String btcCurrentPrice;
            public String hourlyChange;
            public String estimatedBalance;
            public String lastBuyPrice;
            public String lastSellPrice;
            public String nextBuyPrice;

            public TransactionCard(String transactionTime, String btcCurrentPrice, String hourlyChange,
                                String estimatedBalance, String lastBuyPrice, String lastSellPrice, String nextBuyPrice) {
                this.transactionTime = transactionTime;
                this.btcCurrentPrice = btcCurrentPrice;
                this.hourlyChange = hourlyChange;
                this.estimatedBalance = estimatedBalance;
                this.lastBuyPrice = lastBuyPrice;
                this.lastSellPrice = lastSellPrice;
                this.nextBuyPrice = nextBuyPrice;
            }
        }

        public static class ErrorCard {
            public String errorTime;
            public String errorType;
            public String errorMessage;
            public String errorCode;
            public String logInfo;

            public ErrorCard(String errorTime, String errorType, String errorMessage, String errorCode, String logInfo) {
                this.errorTime = errorTime;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
                this.errorCode = errorCode;
                this.logInfo = logInfo;
            }
        }

        public static class CardViewHolder extends RecyclerView.ViewHolder {
            TextView textTransactionTime;
            TextView textBtcCurrentPrice;
            TextView textHourlyChange;
            TextView textEstimatedBalance;
            TextView textLastBuyPrice;
            TextView textLastSellPrice;
            TextView textNextBuyPrice;

            public CardViewHolder(View itemView) {
                super(itemView);
                textTransactionTime = itemView.findViewById(R.id.textTransactionTime);
                textBtcCurrentPrice = itemView.findViewById(R.id.textBtcCurrentPrice);
                textHourlyChange = itemView.findViewById(R.id.textHourlyChange);
                textEstimatedBalance = itemView.findViewById(R.id.textEstimatedBalance);
                textLastBuyPrice = itemView.findViewById(R.id.textLastBuyPrice);
                textLastSellPrice = itemView.findViewById(R.id.textLastSellPrice);
                textNextBuyPrice = itemView.findViewById(R.id.textNextBuyPrice);
            }
        }

        public static class ErrorViewHolder extends RecyclerView.ViewHolder {
            TextView textErrorTime;
            TextView textErrorType;
            TextView textErrorMessage;
            TextView textErrorCode;
            TextView textLogInfo;

            public ErrorViewHolder(View itemView) {
                super(itemView);
                textErrorTime = itemView.findViewById(R.id.textErrorTime);
                textErrorType = itemView.findViewById(R.id.textErrorType);
                textErrorMessage = itemView.findViewById(R.id.textErrorMessage);
                textErrorCode = itemView.findViewById(R.id.textErrorCode);
                textLogInfo = itemView.findViewById(R.id.textLogInfo);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_TRANSACTION) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
                return new CardViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.error_card_view, parent, false);
                return new ErrorViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof CardViewHolder) {
                TransactionCard card = (TransactionCard) cardList.get(position);
                CardViewHolder cardHolder = (CardViewHolder) holder;
                cardHolder.textTransactionTime.setText(card.transactionTime);
                cardHolder.textBtcCurrentPrice.setText(card.btcCurrentPrice);
                cardHolder.textHourlyChange.setText(card.hourlyChange);
                cardHolder.textEstimatedBalance.setText(card.estimatedBalance);
                cardHolder.textLastBuyPrice.setText(card.lastBuyPrice);
                cardHolder.textLastSellPrice.setText(card.lastSellPrice);
                cardHolder.textNextBuyPrice.setText(card.nextBuyPrice);
            } else if (holder instanceof ErrorViewHolder) {
                ErrorCard card = (ErrorCard) cardList.get(position);
                ErrorViewHolder errorHolder = (ErrorViewHolder) holder;
                errorHolder.textErrorTime.setText(card.errorTime);
                errorHolder.textErrorType.setText(card.errorType);
                errorHolder.textErrorMessage.setText(card.errorMessage);
                errorHolder.textErrorCode.setText(card.errorCode);
                errorHolder.textLogInfo.setText(card.logInfo);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (cardList.get(position) instanceof TransactionCard) {
                return TYPE_TRANSACTION;
            } else {
                return TYPE_ERROR;
            }
        }

        @Override
        public int getItemCount() {
            return cardList.size();
        }

        public void addCard(TransactionCard card) {
            cardList.add(0, card); // 최신 카드를 맨 위에 추가
            notifyItemInserted(0);
        }

        public void addErrorCard(ErrorCard card) {
            cardList.add(0, card); // 최신 에러 카드를 맨 위에 추가
            notifyItemInserted(0);
        }
    }
}
