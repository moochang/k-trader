package com.example.k_trader.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.util.Calendar;
import java.util.Locale;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.k_trader.R;
import com.example.k_trader.ui.activity.MainActivity;
import com.example.k_trader.ui.adapter.Listviewitem;
import com.example.k_trader.ui.adapter.ListviewAdapter;
import com.example.k_trader.base.OrderManager;
import com.example.k_trader.service.TradeJobService;
import com.example.k_trader.KTraderApplication;
import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.TradeData;
import com.example.k_trader.base.TradeDataManager;
import com.example.k_trader.di.DIContainer;
import com.example.k_trader.presentation.viewmodel.ViewModels.OrderManagementViewModel;
import android.arch.lifecycle.Observer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.k_trader.base.TradeDataManager.Status.PLACED;
import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;

/**
 * Created by 김무창 on 2017-12-20.
 * 현재 Sell / Buy 대기중인 리스트를 보여주는 화면을 관리한다.
 */

public class PlacedOrderPage extends Fragment implements PopupMenu.OnMenuItemClickListener {
    private static String BY_TIME = "by Time";
    private static String BY_PRICE = "by Price";

    MainActivity mainActivity;
    ArrayList<Listviewitem> list;
    
    // ViewModel
    private OrderManagementViewModel orderManagementViewModel;
    private DIContainer diContainer;
    Button btnRefresh;
    Button btnBuyWithMarketPrice;
    ListView listView;
    Spinner spinnerSort;
    PlacedOrderPage self;
    int g_position;

    private TradeDataManager placedOrderManager;
    String sortBy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.placed_order_page, container,false);
        listView = (ListView)layout.findViewById(R.id.listview);
        self = this;
        
        // DIContainer 초기화
        diContainer = DIContainer.getInstance();
        
        // ViewModel 초기화
        orderManagementViewModel = diContainer.createOrderManagementViewModel();
        
        // ViewModel 관찰 설정
        setupViewModelObservers();

        if (placedOrderManager == null)
            placedOrderManager = new TradeDataManager();

        list = new ArrayList<>();
        mainActivity = (MainActivity) getActivity();
        btnRefresh = layout.findViewById(R.id.refresh);
        btnBuyWithMarketPrice = layout.findViewById(R.id.button4);
        spinnerSort = layout.findViewById(R.id.spinner);

        ArrayAdapter<String> sAdapter = new ArrayAdapter<String>(mainActivity.getApplicationContext(), R.layout.spinner_item, new String[] {BY_PRICE, BY_TIME});
        spinnerSort.setAdapter(sAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?>  parent, View view, int position, long id) {
                sortBy = sAdapter.getItem(position);
            }
            public void onNothingSelected(AdapterView<?>  parent) {
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (list.get(position).getData().getType() == SELL)
                    onSellItemClick(position);
                else if (list.get(position).getData().getType() == BUY)
                    onBuyItemClick(position);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                list.clear();

                ListviewAdapter adapter = new ListviewAdapter(mainActivity.getApplicationContext(), R.layout.list_item, list);
                listView.setAdapter(adapter);

                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    OrderManager orderManager = new OrderManager();
                    placedOrderManager.clear();

                    // 아직 체결 되지 않은 주문 상태인 항목들을 모두 가져온다.
                    {
                        JSONArray dataArray = null;
                        try {
                            dataArray = orderManager.getPlacedOrderList("");
                        } catch (Exception e) {
                            return;
                        }

                        for (int i = 0; i < dataArray.size(); i++) {
                            JSONObject item = (JSONObject) dataArray.get(i);
                            String id = (String) item.get("order_id");
                            placedOrderManager.add(placedOrderManager.build()
                                    .setType(orderManager.convertOrderType((String) item.get("type")))
                                    .setStatus(PLACED)
                                    .setId(id)
                                    .setUnits((float) Double.parseDouble((String) item.get("units_remaining")))
                                    .setPrice(Integer.parseInt(((String) item.get("price")).replaceAll(",", "")))
                                    .setPlacedTime(Long.parseLong((String) item.get("order_date")) / 1000));
                        }
                    }

                    int sellIndex = placedOrderManager.getSellCount();

                    for (TradeData data : placedOrderManager.getList()) {
                        String text;
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(data.getPlacedTime());
                        text = data.getType().toString()
                                + (data.getType() == SELL ? (" (" + sellIndex-- + ") : ") : " : ")   // 남아 있는 Sell count를 쉽게 알 수 있게 보여준다.
                                + String.format(Locale.getDefault(), "%.4f", data.getUnits())
                                + " : "
                                + String.format(Locale.getDefault(), "%,d", data.getPrice())
                                + " : "
                                + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d:%02d"
                                        , cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE)
                                        , cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

                        Listviewitem listItem = new Listviewitem(0, text);
                        float baseUnits = (float) ((int) ((GlobalSettings.getInstance().getUnitPrice() / (double)data.getPrice()) * 10000) / 10000.0);

                        // merge가 필요하거나 down이 필요한 item은 다른 색깔로 보여준다.
                        if (data.getUnits() < (baseUnits / 2.0) || data.getUnits() > (baseUnits * 1.5) || placedOrderManager.getByPrice(SELL, data.getPrice()).size() > 1)
                            listItem.setBgColor(-2044724);
                        else if (data.getPrice() > (TradeJobService.currentPrice * 2 - 1000000)) {
                            // down 할 수 없는 가격대는 좀 더 진한 색으로 보여준다.
                            listItem.setBgColor(-21846);
                        } else
                            listItem.setBgColor(Color.LTGRAY);
                        listItem.setTradeData(data);
                        list.add(listItem);
                        Log.d("KTrader", text);
                    }

                    Runnable runnable = new Runnable() {
                        public void run() {
                            mainActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    ListviewAdapter adapter1 = new ListviewAdapter(mainActivity.getApplicationContext(), R.layout.list_item, list);
                                    listView.setAdapter(adapter1);

                                    // sort by 적용
                                    if (sortBy.equals(BY_PRICE)) {
                                        Comparator<Listviewitem> noAsc = new Comparator<Listviewitem>() {
                                            @Override
                                            public int compare(Listviewitem item1, Listviewitem item2) {
                                                // 오름 차순 정렬
                                                return (item1.getData().getPrice() - item2.getData().getPrice());
                                            }
                                        };

                                        Collections.sort(list, noAsc);
                                        adapter1.notifyDataSetChanged();
                                    } else if (sortBy.equals(BY_TIME)) {
                                        Comparator<Listviewitem> noAsc = new Comparator<Listviewitem>() {
                                            @Override
                                            public int compare(Listviewitem item1, Listviewitem item2) {
                                                // 오름 차순 정렬
                                                return (int)(item1.getData().getPlacedTime() - item2.getData().getPlacedTime());
                                            }
                                        };

                                        Collections.sort(list, noAsc) ;
                                        adapter1.notifyDataSetChanged() ;
                                    }
                                }
                            });
                        }
                    };
                    runnable.run();
                }).start();
            }
        });

        btnBuyWithMarketPrice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Context context = getContext();
                if (context == null) {
                    context = v.getContext();
                }
                if (context != null) {
                    PopupMenu popup = new PopupMenu(context, v);
                    popup.setOnMenuItemClickListener(self);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.buy_market_price, popup.getMenu());
                    popup.show();
                }
            }
        });

        return layout;
    }

    private void onSellItemClick(int position) {
        g_position = position;
        Context context = getContext();
        if (context == null) {
            context = listView.getContext();
        }
        if (context != null) {
            PopupMenu popup = new PopupMenu(context, listView);
            popup.setOnMenuItemClickListener(self);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.placed_order_item_shortcut, popup.getMenu());
            popup.show();
        }
    }

    private void onBuyItemClick(int position) {
        Context context = getContext();
        if (context == null) {
            context = mainActivity;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("가격 조정");
        builder.setMessage("현재가 : " + String.format(Locale.getDefault(), "%,d", list.get(position).getData().getPrice()));
        builder.setNegativeButton("UP",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                        new Thread() {
                            public void run() {
                                TradeData data = list.get(position).getData();
                                OrderManager orderManager = new OrderManager();
                                float movingUnits = (float)((int)(((float)GlobalSettings.getInstance().getUnitPrice() / data.getPrice()) * 10000) / 10000.0);

                                // 옮긴 이후에 애매하게 남을거 같으면 다 옮긴다.
                                if (movingUnits * 1.5 > data.getUnits()) {
                                    movingUnits = data.getUnits();
                                }

                                // 원래 order 취소
                                if (!orderManager.cancelOrder("OrderListPage_Up_10", data)) {
                                    Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // ONE_TIME_PRICE 만큼 높은 가격의 신규 order 추가
                                JSONObject result = orderManager.addOrder("OrderListPage_Up_40", BUY, movingUnits, data.getPrice() + MainPage.getProfitPrice(data.getPrice()));
                                if (!((String)result.get("status")).equals("0000")) {
                                    Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "High Order 생성 실패", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                        }.start();
                    }
                });
        builder.setPositiveButton("DELETE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                        new Thread() {
                            public void run() {
                                TradeData data = list.get(position).getData();
                                OrderManager orderManager = new OrderManager();

                                // order 취소
                                if (!orderManager.cancelOrder("OrderListPage_Delete_1", data)) {
                                    Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                        }.start();
                    }
                });
        builder.setNeutralButton("CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(mainActivity.getApplicationContext(),"Cancel is clicked",Toast.LENGTH_LONG).show();
                    }
                });
        builder.show();
    }

    private void buyWithMarketPrice(int profit) {
        Log.d("KTrader", "[PlacedOrderPage] buyWithMarketPrice() 시작 - profit: " + profit);
        log_info("시장가 매수 시작 - profit: " + profit);
        
        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
        new Thread(() -> {
            Log.d("KTrader", "[PlacedOrderPage] buyWithMarketPrice() 스레드 시작");
            OrderManager orderManager = new OrderManager();
            Log.d("KTrader", "[PlacedOrderPage] OrderManager 생성 완료");

            // 매수 전 잔고 확인
            try {
                Log.d("KTrader", "[PlacedOrderPage] 잔고 확인 시작");
                log_info("잔고 확인 중...");
                JSONObject balanceData = orderManager.getBalance("잔고 확인");
                Log.d("KTrader", "[PlacedOrderPage] 잔고 조회 결과: " + (balanceData != null ? "성공" : "실패"));
                
                if (balanceData != null) {
                    String totalKrw = (String) balanceData.get("total_krw");
                    Log.d("KTrader", "[PlacedOrderPage] KRW 잔고: " + totalKrw);
                    
                    if (totalKrw != null) {
                        double krwBalance = Double.parseDouble(totalKrw);
                        double requiredAmount = GlobalSettings.getInstance().getUnitPrice();
                        Log.d("KTrader", "[PlacedOrderPage] 필요 금액: " + requiredAmount + ", 보유 금액: " + krwBalance);

                        if (krwBalance < requiredAmount) {
                            String message = "잔고 부족으로 시장가 매수를 건너뜁니다. 필요: " +
                                String.format(Locale.getDefault(), "%,.0f", requiredAmount) +
                                "원, 보유: " + String.format(Locale.getDefault(), "%,.0f", krwBalance) + "원";
                            Log.w("KTrader", "[PlacedOrderPage] " + message);
                            log_info(message);
                            return;
                        }
                        Log.d("KTrader", "[PlacedOrderPage] 잔고 확인 완료 - 매수 가능");
                    } else {
                        Log.e("KTrader", "[PlacedOrderPage] KRW 잔고 정보가 null");
                        log_info("KRW 잔고 정보를 가져올 수 없습니다");
                        return;
                    }
                } else {
                    Log.e("KTrader", "[PlacedOrderPage] 잔고 조회 실패");
                    log_info("잔고 조회에 실패했습니다");
                    return;
                }
            } catch (Exception e) {
                Log.e("KTrader", "[PlacedOrderPage] 잔고 확인 중 오류 발생", e);
                log_info("잔고 확인 중 오류 발생: " + e.getMessage());
                return;
            }

            // ONE_TIME_PRICE 어치 시장가 매수
            Log.d("KTrader", "[PlacedOrderPage] 시장가 매수 계산 시작");
            Log.d("KTrader", "[PlacedOrderPage] 단위 가격: " + GlobalSettings.getInstance().getUnitPrice());
            
            // 현재 설정된 코인 타입에 따라 현재가 가져오기
            int currentPrice = 0;
            try {
                JSONObject tickerData = orderManager.getTicker("시장가 매수 현재가 확인");
                if (tickerData != null) {
                    JSONObject data = (JSONObject) tickerData.get("data");
                    if (data != null) {
                        String currentPriceStr = (String) data.get("closing_price");
                        if (currentPriceStr != null) {
                            currentPrice = (int) Double.parseDouble(currentPriceStr);
                            Log.d("KTrader", "[PlacedOrderPage] 현재 가격 조회 성공: " + currentPrice);
                        } else {
                            Log.e("KTrader", "[PlacedOrderPage] closing_price 정보가 null");
                            log_info("현재가 정보를 가져올 수 없습니다");
                            return;
                        }
                    } else {
                        Log.e("KTrader", "[PlacedOrderPage] ticker data가 null");
                        log_info("현재가 데이터를 가져올 수 없습니다");
                        return;
                    }
                } else {
                    Log.e("KTrader", "[PlacedOrderPage] ticker 조회 실패");
                    log_info("현재가 조회에 실패했습니다");
                    return;
                }
            } catch (Exception e) {
                Log.e("KTrader", "[PlacedOrderPage] 현재가 조회 중 오류 발생", e);
                log_info("현재가 조회 중 오류 발생: " + e.getMessage());
                return;
            }
            
            if (currentPrice <= 0) {
                Log.e("KTrader", "[PlacedOrderPage] 현재가가 유효하지 않음: " + currentPrice);
                log_info("현재가가 유효하지 않습니다: " + currentPrice);
                return;
            }
            
            float units = (float) ((int) ((GlobalSettings.getInstance().getUnitPrice() / (double)currentPrice) * 10000) / 10000.0);
            Log.d("KTrader", "[PlacedOrderPage] 계산된 매수 수량: " + units);
            log_info("시장가 매수 시도 - 수량: " + String.format("%.4f", units) + ", 현재가: " + currentPrice);
            
            JSONObject result = orderManager.addOrderWithMarketPrice("시장가 수동 매수 +" + profit, BUY, units);
            Log.d("KTrader", "[PlacedOrderPage] 시장가 매수 결과: " + (result != null ? "성공" : "실패"));
            
            if (result == null) {
                Log.e("KTrader", "[PlacedOrderPage] 시장가 수동 매수 실패");
                log_info("시장가 수동 매수 실패");
                return;
            }
            
            Log.d("KTrader", "[PlacedOrderPage] 시장가 매수 성공 - 결과: " + result.toString());

            // 매수 결과값을 분석해서 바로 매도 요청을 한다.
            if (((String) result.get("status")).equals("0000")) {
                Log.d("KTrader", "[PlacedOrderPage] 매수 성공, 매도 준비 시작");
                
                // 시장가 매수는 즉시 체결되므로 data 배열이 없을 수 있음
                // order_id로 체결 정보를 확인하거나, 잔고 변화를 통해 추정
                String orderId = (String) result.get("order_id");
                Log.d("KTrader", "[PlacedOrderPage] 매수 주문 ID: " + orderId);
                
                // 시장가 매수로 체결된 수량과 평균 가격을 추정
                // 실제 체결된 수량은 요청한 수량과 동일하다고 가정
                float earnedUnits = units;
                int earnedPrice = currentPrice; // 현재가로 체결되었다고 가정
                
                Log.d("KTrader", "[PlacedOrderPage] 추정 체결 수량: " + earnedUnits + ", 추정 체결 가격: " + earnedPrice);
                
                // +PROFIT_PRICE 가격에 매도 요청
                int sellPrice = (int) ((earnedPrice + (MainPage.getProfitPrice(earnedPrice) * profit)) / 1000) * 1000;
                Log.d("KTrader", "[PlacedOrderPage] 매도 가격 계산: " + sellPrice);
                log_info("매도 주문 시도 - 수량: " + String.format("%.4f", earnedUnits) + ", 가격: " + String.format(Locale.getDefault(), "%,d", sellPrice));
                
                result = orderManager.addOrder("시장가 수동 매수 +" + profit, SELL, earnedUnits, sellPrice);
                Log.d("KTrader", "[PlacedOrderPage] 매도 주문 결과: " + (result != null ? "성공" : "실패"));
                
                if (result == null) {
                    Log.e("KTrader", "[PlacedOrderPage] 시장가 수동 매도 실패");
                    log_info("시장가 수동 매도 실패");
                    return;
                }

                String resultStr = "시장가 수동 매수 +" + profit + "완료 : " + String.format(Locale.getDefault(), "%,d", earnedPrice) + " -> " + String.format(Locale.getDefault(), "%,d", earnedPrice + (MainPage.getProfitPrice(earnedPrice) * profit));
                Log.d("KTrader", "[PlacedOrderPage] " + resultStr);
                log_info(resultStr);

                // 이미 완료된 시장가 매수에 대해 Noti를 받지 않도록 시간을 업데이트 한다.
                TradeJobService.lastNotiTimeInMillis = Calendar.getInstance().getTimeInMillis();
                Log.d("KTrader", "[PlacedOrderPage] 알림 시간 업데이트 완료");
            } else {
                Log.e("KTrader", "[PlacedOrderPage] 매수 실패 - 상태: " + result.get("status"));
                log_info("매수 실패 - 상태: " + result.get("status"));
            }
            
            Log.d("KTrader", "[PlacedOrderPage] buyWithMarketPrice() 완료");
        }).start();
    }

    private void log_info(final String log) {
        Intent intent = new Intent(TransactionLogPage.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);
    }
    
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.market_price_plus_1:
                buyWithMarketPrice(1);
//                Toast.makeText(mainActivity.getApplicationContext(),"Menu1 clicked",Toast.LENGTH_LONG).show();
                return true;
            case R.id.market_price_plus_2:
                buyWithMarketPrice(2);
//                Toast.makeText(mainActivity.getApplicationContext(),"Menu2 clicked",Toast.LENGTH_LONG).show();
                return true;
            case R.id.market_price_plus_3:
                buyWithMarketPrice(3);
//                Toast.makeText(mainActivity.getApplicationContext(),"Menu2 clicked",Toast.LENGTH_LONG).show();
                return true;
            case R.id.up:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    TradeData data = list.get(g_position).getData();
                    OrderManager orderManager = new OrderManager();
                    float movingUnits = (float)((int)(((float)GlobalSettings.getInstance().getUnitPrice() / data.getPrice()) * 10000) / 10000.0);

                    // 옮긴 이후에 애매하게 남을거 같으면 다 옮긴다.
                    if (movingUnits * 1.5 > data.getUnits()) {
                        movingUnits = data.getUnits();
                    }

                    // 원래 order 취소
                    if (!orderManager.cancelOrder("OrderListPage_Up_1", data)) {
                        Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ONE_TIME_PRICE 만큼 높은 가격의 신규 order 추가
                    TradeData targetData = placedOrderManager.findByPrice(SELL, data.getPrice() + MainPage.getProfitPrice(data.getPrice()));
                    if (targetData != null) {
                        // 이미 동일 가격의 order가 있다면 취소 하고 합쳐서 추가
                        if (!orderManager.cancelOrder("OrderListPage_Up_2", targetData)) {
                            Toast.makeText(mainActivity.getApplicationContext(), "High Order 취소 실패", Toast.LENGTH_LONG).show();
                            return;
                        }

                        float newUnits = (float)(Math.round((movingUnits + targetData.getUnits()) * 10000d) / 10000d);
                        JSONObject result = orderManager.addOrder("OrderListPage_Up_3", data.getType(), newUnits, targetData.getPrice());
                        if (!((String)result.get("status")).equals("0000")) {
                            Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "High Order 생성 실패", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } else {
                        // 동일 가격의 order가 없다면 신규만 추가
                        JSONObject result = orderManager.addOrder("OrderListPage_Up_4", data.getType(), movingUnits, data.getPrice() + MainPage.getProfitPrice(data.getPrice()));
                        if (!((String)result.get("status")).equals("0000")) {
                            Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "High Order 생성 실패", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    // 원래 order에 -ONE_TIME_PRICE 만큼 추가
                    if (data.getUnits() > movingUnits) {
                        float newUnits = (float)(Math.round((data.getUnits() - movingUnits) * 10000d) / 10000d);
                        JSONObject result = orderManager.addOrder("OrderListPage_Up_5", data.getType(), newUnits, data.getPrice());
                        if (!((String)result.get("status")).equals("0000")) {
                            Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "Order 마무리 실패", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }).start();
                return true;
            case R.id.down:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    try {
                        down(MainPage.getProfitPrice());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                return true;

            case R.id.down_half:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                // 최소 거래 단위인 1,000원으로 내림한다.
                new Thread(() -> {
                    try {
                        down(floor(MainPage.getProfitPrice() / 2, 1000));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                return true;

            case R.id.merge:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    TradeData data = list.get(g_position).getData();
                    OrderManager orderManager = new OrderManager();

                    List<TradeData> list = placedOrderManager.getByPrice(SELL, data.getPrice());
                    if (list.size() > 1) {
                        // merge
                        float mergedUnits = 0;
                        for (TradeData data2 : list) {
                            mergedUnits += data2.getUnits();

                            // cancel original orders
                            if (!orderManager.cancelOrder("OrderListPage_Down_367", data2)) {
                                Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        // add new merged order
                        float newUnits = (float)(Math.round(mergedUnits * 10000d) / 10000d);
                        JSONObject result = orderManager.addOrder("OrderListPage_Down_375", SELL, newUnits, data.getPrice());
                        if (!((String)result.get("status")).equals("0000")) {
                            Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "Lower Order 생성 실패", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }).start();
                return true;
            case R.id.sell_10000:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    TradeData data = list.get(g_position).getData();
                    OrderManager orderManager = new OrderManager();
                    float sellUnits = (float)((int)((10000.0 / TradeJobService.currentPrice) * 10000) / 10000.0);

                    // 이상하게 큰 값이 나오면 에러로 판단한다.
                    if (sellUnits > 0.1) {
                        log_info("계산된 만원 어치가 너무 큼 : " + sellUnits);
                        return;
                    }

                    // 10,000원보다 적게 남는 경우 모두 판매한다.
                    if (sellUnits > data.getUnits()) {
                        sellUnits = data.getUnits();
                    }

                    // 원래 order 취소
                    if (!orderManager.cancelOrder("sell_10000_1", data)) {
                        Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 10,000원 어치 시장가 판매
                    JSONObject result = orderManager.addOrderWithMarketPrice("sell_10000_2", SELL, sellUnits);
                    if (result == null) {
                        log_info("시장가로 10,000원 어치 매도 실패");
                        return;
                    }

                    // 원래 order에 -10,000원 만큼 다시 판매
                    float newUnits = 0;
                    if (data.getUnits() > sellUnits) {
                        newUnits = (float)(Math.round((data.getUnits() - sellUnits) * 10000d) / 10000d);
                        result = orderManager.addOrder("sell_10000_3", data.getType(), newUnits, data.getPrice());
                        if (!((String)result.get("status")).equals("0000")) {
                            Log.d("KTrader", result.toString());
                            return;
                        }
                    }

                    log_info("시장가 10,000원 어치 매도 성공 : " + data.getUnits() + " -> " + newUnits);
                }).start();
                return true;
            case R.id.sell_50000:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    TradeData data = list.get(g_position).getData();
                    OrderManager orderManager = new OrderManager();
                    float sellUnits = (float)((int)((50000.0 / TradeJobService.currentPrice) * 10000) / 10000.0);

                    // 이상하게 큰 값이 나오면 에러로 판단한다.
                    if (sellUnits > 0.1) {
                        log_info("계산된 만원 어치가 너무 큼 : " + sellUnits);
                        return;
                    }

                    // 10,000원보다 적게 남는 경우 모두 판매한다.
                    if (sellUnits > data.getUnits()) {
                        sellUnits = data.getUnits();
                    }

                    // 원래 order 취소
                    if (!orderManager.cancelOrder("sell_50000_1", data)) {
                        Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 10,000원 어치 시장가 판매
                    JSONObject result = orderManager.addOrderWithMarketPrice("sell_50000_2", SELL, sellUnits);
                    if (result == null) {
                        log_info("시장가로 50,000원 어치 매도 실패");
                        return;
                    }

                    // 원래 order에 -50,000원 만큼 다시 판매
                    float newUnits = 0;
                    if (data.getUnits() > sellUnits) {
                        newUnits = (float)(Math.round((data.getUnits() - sellUnits) * 10000d) / 10000d);
                        result = orderManager.addOrder("sell_50000_3", data.getType(), newUnits, data.getPrice());
                        if (!((String)result.get("status")).equals("0000")) {
                            Log.d("KTrader", result.toString());
                            return;
                        }
                    }

                    log_info("시장가 50,000원 어치 매도 성공 : " + data.getUnits() + " -> " + newUnits);
                }).start();
                return true;
            case R.id.cancel:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread(() -> {
                    TradeData data = list.get(g_position).getData();
                    OrderManager orderManager = new OrderManager();
                    orderManager.cancelOrder("", data);
                }).start();
                return true;
            default:
                return false;
        }
    }

    private void down(int amount) {
        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
        new Thread(() -> {
            TradeData data = list.get(g_position).getData();
            OrderManager orderManager = new OrderManager();
            float movingUnits = (float)((int)(((float)GlobalSettings.getInstance().getUnitPrice() / data.getPrice()) * 10000) / 10000.0);

            // server 정책 체크
            if ((TradeJobService.currentPrice * 2 - 1000000) < (data.getPrice())) {
                Runnable runnable = new Runnable() {
                    public void run() {
                        mainActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(mainActivity.getApplicationContext(), "대상 가격이 현재가 2배 이상이라 Skip", Toast.LENGTH_LONG).show();                                                }
                        });
                    }
                };
                runnable.run();
                return;
            }

            // 옮긴 이후에 애매하게 남을거 같으면 다 옮긴다.
            if (movingUnits * 1.5 > data.getUnits()) {
                movingUnits = data.getUnits();
            }

            // 원래 order 취소
            if (!orderManager.cancelOrder("OrderListPage_Down_1", data)) {
                Toast.makeText(mainActivity.getApplicationContext(), "Order 취소 실패", Toast.LENGTH_LONG).show();
                return;
            }

            // amount 만큼 낮은 가격의 신규 order 추가
            TradeData targetData = placedOrderManager.findByPrice(SELL, data.getPrice() - amount);
            if (targetData != null) {
                // 이미 동일 가격의 order가 있다면 취소 하고 합쳐서 추가
                if (!orderManager.cancelOrder("OrderListPage_Down_2", targetData)) {
                    Toast.makeText(mainActivity.getApplicationContext(), "Lower Order 취소 실패", Toast.LENGTH_LONG).show();
                    return;
                }

                float newUnits = (float)(Math.round((movingUnits + targetData.getUnits()) * 10000d) / 10000d);
                JSONObject result = orderManager.addOrder("OrderListPage_Down_3", data.getType(), newUnits, targetData.getPrice());
                if (!((String)result.get("status")).equals("0000")) {
                    Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "Lower Order 생성 실패", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                // 동일 가격의 order가 없다면 신규만 추가
                JSONObject result = orderManager.addOrder("OrderListPage_Down_4", data.getType(), movingUnits, data.getPrice() - amount);
                if (!((String)result.get("status")).equals("0000")) {
                    Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "Lower Order 생성 실패", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // 원래 order에 -amount 만큼 추가
            if (data.getUnits() > movingUnits) {
                float newUnits = (float)(Math.round((data.getUnits() - movingUnits) * 10000d) / 10000d);
                JSONObject result = orderManager.addOrder("OrderListPage_Down_5", data.getType(), newUnits, data.getPrice());
                if (!((String)result.get("status")).equals("0000")) {
                    Log.d("KTrader", result.toString());
//                                                Toast.makeText(mainActivity.getApplicationContext(), "Order 마무리 실패", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }).start();
    }

    private int floor(int value, int unit) {
        if ((value % unit) != 0)
            return value - (value % unit);

        return value;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    /**
     * ViewModel 관찰자 설정
     */
    private void setupViewModelObservers() {
        // 대기 중인 주문 관찰
        orderManagementViewModel.getPlacedOrders().observe(this, new Observer<java.util.List<com.example.k_trader.domain.model.DomainModels.Trade>>() {
            @Override
            public void onChanged(java.util.List<com.example.k_trader.domain.model.DomainModels.Trade> trades) {
                if (trades != null) {
                    Log.d("KTrader", "[PlacedOrderPage] Placed orders updated: " + trades.size() + " orders");
                    // TODO: UI 업데이트 로직 추가
                }
            }
        });
        
        // 에러 메시지 관찰
        orderManagementViewModel.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String errorMessage) {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Log.e("KTrader", "[PlacedOrderPage] Error from ViewModel: " + errorMessage);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        // 로딩 상태 관찰
        orderManagementViewModel.getIsLoading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading != null) {
                    Log.d("KTrader", "[PlacedOrderPage] Loading state updated: " + isLoading);
                    // TODO: 로딩 UI 업데이트 로직 추가
                }
            }
        });
    }
    
    /**
     * 시장가 매수 실행 (ViewModel 사용)
     */
    public void executeMarketBuyOrder(String coinType, double units) {
        Log.d("KTrader", "[PlacedOrderPage] Executing market buy order via ViewModel");
        orderManagementViewModel.executeMarketBuyOrder(coinType, units);
    }
    
    /**
     * 시장가 매도 실행 (ViewModel 사용)
     */
    public void executeMarketSellOrder(String coinType, double units) {
        Log.d("KTrader", "[PlacedOrderPage] Executing market sell order via ViewModel");
        orderManagementViewModel.executeMarketSellOrder(coinType, units);
    }
    
    /**
     * 주문 취소 (ViewModel 사용)
     */
    public void cancelOrder(String orderId) {
        Log.d("KTrader", "[PlacedOrderPage] Cancelling order via ViewModel: " + orderId);
        orderManagementViewModel.cancelOrder(orderId);
    }
    
    /**
     * 모든 주문 취소 (ViewModel 사용)
     */
    public void cancelAllOrders(String coinType) {
        Log.d("KTrader", "[PlacedOrderPage] Cancelling all orders via ViewModel for: " + coinType);
        orderManagementViewModel.cancelAllOrders(coinType);
    }
}
