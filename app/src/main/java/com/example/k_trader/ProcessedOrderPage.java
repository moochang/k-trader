package com.example.k_trader;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.k_trader.base.OrderManager;
import com.example.k_trader.base.TradeData;
import com.example.k_trader.base.TradeDataManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.example.k_trader.base.TradeDataManager.Status.PROCESSED;
import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.NONE;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;

/**
 * Created by 김무창 on 2017-12-20.
 * 현재까지 처리 완료 된 Sell / Buy 리스트를 보여주는 화면을 관리한다.
 */

public class ProcessedOrderPage extends Fragment {

    MainActivity mainActivity;
    ArrayList<Listviewitem> list;
    Button btnRefresh;
    ListView listView;
    TextView textView;
    Spinner spinnerRange;

    double feeTotal;
    int priceTotal;
    int buyTotal;
    int sellTotal;
    int rangeDays;

    private TradeDataManager tradedataManager;
    private OrderManager orderManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.processed_order_page, container,false);
        listView = (ListView)layout.findViewById(R.id.listview);
        textView = (TextView)layout.findViewById(R.id.textView2);

        list = new ArrayList<>();
        mainActivity = (MainActivity) getActivity();
        btnRefresh = layout.findViewById(R.id.refresh);
        spinnerRange = layout.findViewById(R.id.spinner);

        ArrayAdapter<String> sAdapter = new ArrayAdapter<String>(mainActivity.getApplicationContext(), R.layout.spinner_item, new String[] {"1일", "2일", "5일", "7일", "10일", "15일", "30일"});
        spinnerRange.setAdapter(sAdapter);
        spinnerRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?>  parent, View view, int position, long id) {
                rangeDays = Integer.parseInt(sAdapter.getItem(position).replace("일", ""));
            }
            public void onNothingSelected(AdapterView<?>  parent) {
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        if (tradedataManager == null)
            tradedataManager = new TradeDataManager();

        if (orderManager == null)
            orderManager = new OrderManager();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                list.clear();

                ListviewAdapter adapter = new ListviewAdapter(mainActivity.getApplicationContext(), R.layout.list_item, list);
                listView.setAdapter(adapter);

                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
                        feeTotal = 0;
                        priceTotal = 0;
                        int offset = 0;
                        buyTotal = 0;
                        sellTotal = 0;
                        boolean condition = true;

                        tradedataManager.clear();
                        Calendar currentTime = Calendar.getInstance();
                        {
                            while(condition) {
                                // 매수/매도 완료 리스트를 가져온다.
                                JSONObject result = orderManager.getProcessedOrderList("", offset, "50");
                                if (result == null) {
                                    return;
                                }

                                JSONArray dataArray = (JSONArray) result.get("data");
                                if (dataArray != null) {
                                    for (int i = 0; i < dataArray.size(); i++) {
                                        JSONObject item = (JSONObject) dataArray.get(i);

                                        //{"search":"1","btc_remain":"5.42478202","price":-99330,"fee":"0.00003225","krw_remain":4275528,"units":"+ 0.01286775","transfer_date":"1557543507259903","btc1krw":7700000}
                                        //{"search":"2","btc_remain":"5.41191427","price":1012350,"fee":"2537.22","krw_remain":4374858,"units":"- 0.1331","transfer_date":"1557538928212073","btc1krw":7625000}

                                        //{"search":"2","btc_remain":"0.75468327","price":101010,"fee":"0","krw_remain":9157566,"units":"0.0078","transfer_date":"1566201030737","btc1krw":"12950000"}
                                        Log.d("KTrader", item.toString());

                                        int search = Integer.parseInt((String) item.get("search"));
                                        long processedTimeInMillis;
                                        String date_string = (String) item.get("transfer_date");
                                        if (date_string.length() == 13)
                                            processedTimeInMillis = Long.parseLong((String) item.get("transfer_date"));
                                        else // micro second
                                            processedTimeInMillis = Long.parseLong((String) item.get("transfer_date")) / 1000;

                                        if ((currentTime.getTimeInMillis() - processedTimeInMillis) / 1000 / 60 / 60 / 24.0 > rangeDays) {
                                            condition = false;
                                            break;
                                        }

                                        if (tradedataManager.findByProcessedTime(processedTimeInMillis) == null && convertSearchType(search) != NONE) {
                                            tradedataManager.add(tradedataManager.build()
                                                    .setType(convertSearchType(search))
                                                    .setStatus(PROCESSED)
                                                    .setUnits(((float) Double.parseDouble(((String) item.get("units")).replace(" ", "").replace("-", ""))))
                                                    .setPrice(Math.abs(Integer.parseInt(((String) item.get("price")))))
                                                    .setFeeRaw((String) item.get("fee"))
                                                    .setProcessedTime(processedTimeInMillis));
                                        }
                                    }
                                }

                                // 다음 50개 거래 리스트를 가져온다.
                                offset += 50;
                            }

                            // 리스트에 추가한다.
                            for (TradeData data : tradedataManager.getList()) {
                                String text;
                                Calendar completeTime = Calendar.getInstance();
                                completeTime.setTimeInMillis(data.getProcessedTime());
                                String date = String.format(Locale.getDefault(), "%02d/%02d %02d:%02d:%02d"
                                        , completeTime.get(Calendar.MONTH) + 1, completeTime.get(Calendar.DATE)
                                        , completeTime.get(Calendar.HOUR_OF_DAY), completeTime.get(Calendar.MINUTE), completeTime.get(Calendar.SECOND));

                                if (data.getType() == BUY) {
                                    text = "매수 완료 : ";
                                    buyTotal++;
                                } else if (data.getType() == SELL) {
                                    text = "매도 완료 : ";
                                    sellTotal++;
                                } else
                                    continue;

                                text += data.getUnits() + " : " + String.format(Locale.getDefault(), "%,d", data.getPrice()) + " : " + date + " : " + (int)data.getFeeEvaluated();

                                Listviewitem listItem = new Listviewitem(0, text);
                                list.add(listItem);
//                                Log.d("KTrader", text);

                                feeTotal += data.getFeeEvaluated();
                                priceTotal += (data.getPrice() * data.getUnits());
                            }
                        }

                        // run ui thread to prevent 'CalledFromWrongThreadException'
                        Runnable runnable = new Runnable() {
                            public void run() {
                                mainActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        ListviewAdapter adapter = new ListviewAdapter(mainActivity.getApplicationContext(), R.layout.list_item, list);
                                        listView.setAdapter(adapter);

                                        textView.setText("거래회수 : " + (buyTotal + sellTotal) + "회 (매수 : " + buyTotal + ", 매도 : " + sellTotal + ")\r\n"
                                                        + "거래대금 : " + String.format(Locale.getDefault(), "%,d원", priceTotal) + "\r\n"
                                                        + "수수료    : " + String.format(Locale.getDefault(), "%,d원", (int)feeTotal));
                                    }
                                });
                            }
                        };
                        runnable.run();
                    }
                }.start();
            }
        });

        return layout;
    }

    private TradeDataManager.Type convertSearchType(int search) {
        switch(search) {
            case 1 : return BUY;
            case 2 : return SELL;
        }
        return NONE;
    }
}
