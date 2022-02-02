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
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;

import com.example.k_trader.base.OrderManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * Created by 김무창 on 2017-12-20.
 */

public class MainPage extends Fragment {

    public static final String BROADCAST_LOG_MESSAGE = "TRADE_LOG";

    public static final int JOB_ID_FIRST = 1;
    public static final int JOB_ID_REGULAR = 2;

    private final static int MAX_BUFFER = 10000;

    public static Context context;

    EditText editText;
    Button btnStartTrading;
    Button btnStopTrading;
    Button btnScrollToBottom;
    ImageButton btnPreference;
    ScrollView scrollView;
    CheckBox checkBox;

    ComponentName component;
    MainActivity mainActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.main_page, container,false);
        mainActivity = (MainActivity) getActivity();
        editText = (EditText) layout.findViewById(R.id.editText);
        btnStartTrading = layout.findViewById(R.id.button);
        btnStopTrading = layout.findViewById(R.id.button2);
        btnScrollToBottom = layout.findViewById(R.id.button3);
        btnPreference = layout.findViewById(R.id.imageButtonPreference);
        scrollView = (ScrollView)layout.findViewById(R.id.scrollView1);
        checkBox = (CheckBox)layout.findViewById(R.id.checkBox);

        // page switching으로 인한 재방문이 아닌 첫 방문일 때만 초기화 한다.
        if (mainActivity.jobScheduler == null) {
            btnStopTrading.setEnabled(false);

            component = new ComponentName(mainActivity, TradeJobService.class.getName());

            IntentFilter theFilter = new IntentFilter();
            theFilter.addAction(BROADCAST_LOG_MESSAGE);
            LocalBroadcastManager.getInstance(mainActivity.getApplicationContext()).registerReceiver(new MyReceiver(), theFilter);
        }

        if (mainActivity.getApplicationContext() != null)
            context = mainActivity.getApplicationContext();

        btnStartTrading.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String packageName = mainActivity.getPackageName();
                PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);

                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent i = new Intent();
                    i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + packageName));
                    startActivity(i);
                }

                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
                        // cancel all buy request
                        OrderManager orderManager = new OrderManager();
                        orderManager.cancelAllOrders();
                    }
                }.start();

                // JOB_ID_REGULAR가 1분 후부터 스케줄링 되기 때문에 1회성으로 한번 더 실행
                JobInfo firstTradeJob = new JobInfo.Builder(JOB_ID_FIRST, component)
                        .setMinimumLatency(1000) // 1000 ms
                        .build();

                JobInfo tradeJob = new JobInfo.Builder(JOB_ID_REGULAR, component)
                        .setMinimumLatency(60 * 1000) // 1 minute
                        .build();

                mainActivity.jobScheduler = (JobScheduler) mainActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                mainActivity.jobScheduler.schedule(firstTradeJob);
                mainActivity.jobScheduler.schedule(tradeJob);

                btnStartTrading.setEnabled(false);
                btnStopTrading.setEnabled(true);
            }
        });

        btnStopTrading.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mainActivity.jobScheduler != null) {
                    mainActivity.jobScheduler.cancelAll();
                }

                btnStartTrading.setEnabled(true);
                btnStopTrading.setEnabled(false);
            }
        });

        btnScrollToBottom.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        btnPreference.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(mainActivity, SettingActivity.class));
            }
        });

        // page switching으로 인한 재방문인 경우에는 필요한 일만 하고 리턴한다.
        if (mainActivity.jobScheduler != null) {
            btnStartTrading.setEnabled(false);
            btnStopTrading.setEnabled(true);
        }

        return layout;
    }

    private void log_info(final String log) {
        Runnable runnable = new Runnable() {
            public void run() {
                mainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        editText.append(log + "\r\n");

                        CharSequence charSequence = editText.getText();
                        if (charSequence.length() > MAX_BUFFER)
                            editText.getEditableText().delete(0, charSequence.length() - MAX_BUFFER);

                        // scroll to bottom
                        if (checkBox.isChecked()) {
                            scrollView.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                }
                            });
                        }
                    }
                });
            }
        };
        runnable.run();

        Log.d("KTrader", log);
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log_info(intent.getStringExtra("log"));
        }
    }

    public static int getProfitPrice(int basePrice) {
        int index = 0;

        basePrice /= 10000;

        while(basePrice > 10) {
            basePrice /= 10;
            index++;
        }

        int profit = basePrice;
        for (int i = 0; i<index; i++) {
            profit *= 10;
        }

        return profit * 100;   // basePrice의 1%만 리턴
    }

    public static int getProfitPrice() throws Exception {
        OrderManager orderManager = new OrderManager();
        int currentPrice;                  // 비트코인 현재 시장가

        JSONObject result = orderManager.getCurrentPrice("");
        if (result == null) {
            throw new Exception("Unknown network issue happens");
        }

        JSONObject dataObj = (JSONObject) result.get("data");
        if (dataObj != null) {
            JSONArray dataArray = (JSONArray) dataObj.get("bids"); // 매수가
            if (dataArray != null) {
                JSONObject item = (JSONObject) dataArray.get(0); // 기본 5개 아이템 중 첫번째 아이템 사용
                currentPrice = (int) Double.parseDouble((String) item.get("price"));
                return getProfitPrice(currentPrice);
            }
        }

        throw new Exception("dataObj == null");
    }
}
