package com.example.k_trader_eth;

import android.app.AlertDialog;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.k_trader_eth.base.GlobalSettings;
import com.example.k_trader_eth.base.OrderManager;
import com.example.k_trader_eth.base.TradeData;
import com.example.k_trader_eth.base.TradeDataManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.example.k_trader_eth.base.TradeDataManager.Status.PLACED;
import static com.example.k_trader_eth.base.TradeDataManager.Type.BUY;
import static com.example.k_trader_eth.base.TradeDataManager.Type.NONE;
import static com.example.k_trader_eth.base.TradeDataManager.Type.SELL;

/**
 * Created by 김무창 on 2017-12-20.
 * 현재 Sell / Buy 대기중인 리스트를 보여주는 화면을 관리한다.
 */

public class PlacedOrderPage extends Fragment implements PopupMenu.OnMenuItemClickListener {
    private static String BY_TIME = "by Time";
    private static String BY_PRICE = "by Price";

    MainActivity mainActivity;
    ArrayList<Listviewitem> list;
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
                new Thread() {
                    public void run() {
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
                                        ListviewAdapter adapter = new ListviewAdapter(mainActivity.getApplicationContext(), R.layout.list_item, list);
                                        listView.setAdapter(adapter);

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
                                            adapter.notifyDataSetChanged();
                                        } else if (sortBy.equals(BY_TIME)) {
                                            Comparator<Listviewitem> noAsc = new Comparator<Listviewitem>() {
                                                @Override
                                                public int compare(Listviewitem item1, Listviewitem item2) {
                                                    // 오름 차순 정렬
                                                    return (int)(item1.getData().getPlacedTime() - item2.getData().getPlacedTime());
                                                }
                                            };

                                            Collections.sort(list, noAsc) ;
                                            adapter.notifyDataSetChanged() ;
                                        }
                                    }
                                });
                            }
                        };
                        runnable.run();
                    }
                }.start();
            }
        });

        btnBuyWithMarketPrice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(mainActivity.cur_fragment.getContext(), v);
                popup.setOnMenuItemClickListener(self);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.buy_market_price, popup.getMenu());
                popup.show();
            }
        });

        return layout;
    }

    private void onSellItemClick(int position) {
        g_position = position;
        PopupMenu popup = new PopupMenu(this.getContext(), listView);
        popup.setOnMenuItemClickListener(self);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.placed_order_item_shortcut, popup.getMenu());
        popup.show();
    }

    private void onBuyItemClick(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity.cur_fragment.getContext());
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
        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
        new Thread() {
            public void run() {
                OrderManager orderManager = new OrderManager();
                // ONE_TIME_PRICE 어치 시장가 매수
                float units = (float) ((int) ((GlobalSettings.getInstance().getUnitPrice() / (double)TradeJobService.currentPrice) * 10000) / 10000.0);
                JSONObject result = orderManager.addOrderWithMarketPrice("시장가 수동 매수 +" + profit, BUY, units);
                if (result == null) {
                    log_info("시장가 수동 매수 실패");
                    return;
                }

                // 매수 결과값을 분석해서 바로 매도 요청을 한다.
                if (((String) result.get("status")).equals("0000")) {
                    JSONArray dataArray = (JSONArray) result.get("data");
                    if (dataArray != null) {
                        float earnedUnits = 0;
                        int earnedPrice = 0;
                        for (int i = 0; i < dataArray.size(); i++) {
                            JSONObject item = (JSONObject) dataArray.get(i);
                            earnedUnits += Float.parseFloat((String) item.get("units"));
                            earnedPrice += Long.valueOf((Long) item.get("price"));
                        }
                        earnedUnits = (float) ((int) (earnedUnits * 10000) / 10000.0);
                        earnedPrice = earnedPrice / dataArray.size();

                        // +PROFIT_PRICE 가격에 매도 요청
                        result = orderManager.addOrder("시장가 수동 매수 +" + profit, SELL, earnedUnits, (int) ((earnedPrice + (MainPage.getProfitPrice(earnedPrice) * profit)) / 1000) * 1000);
                        if (result == null) {
                            log_info("시장가 수동 매도 실패");
                            return;
                        }

                        String resultStr = "시장가 수동 매수 +" + profit + "완료 : " + String.format(Locale.getDefault(), "%,d", earnedPrice) + " -> " + String.format(Locale.getDefault(), "%,d", earnedPrice + (MainPage.getProfitPrice(earnedPrice) * profit));
                        log_info(resultStr);

                        // 이미 완료된 시장가 매수에 대해 Noti를 받지 않도록 시간을 업데이트 한다.
                        TradeJobService.lastNotiTimeInMillis = Calendar.getInstance().getTimeInMillis();
                    }
                }
            }
        }.start();
    }

    private void log_info(final String log) {
        Intent intent = new Intent(MainPage.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        LocalBroadcastManager.getInstance(MainPage.context).sendBroadcast(intent);
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
                new Thread() {
                    public void run() {
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
                    }
                }.start();
                return true;
            case R.id.down:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
                        try {
                            down(MainPage.getProfitPrice());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                return true;

            case R.id.down_half:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                // 최소 거래 단위인 1,000원으로 내림한다.
                new Thread() {
                    public void run() {
                        try {
                            down(floor(MainPage.getProfitPrice() / 2, 1000));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                return true;

            case R.id.merge:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
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
                    }
                }.start();
                return true;
            case R.id.sell_10000:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
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
                    }
                }.start();
                return true;
            case R.id.sell_50000:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
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
                    }
                }.start();
                return true;
            case R.id.cancel:
                // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
                new Thread() {
                    public void run() {
                        TradeData data = list.get(g_position).getData();
                        OrderManager orderManager = new OrderManager();
                        orderManager.cancelOrder("", data);
                    }
                }.start();
                return true;
            default:
                return false;
        }
    }

    private void down(int amount) {
        // NetworkOnMainThreadException을 방지하기 위해 thread를 돌린다.
        new Thread() {
            public void run() {
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
            }
        }.start();
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
}
