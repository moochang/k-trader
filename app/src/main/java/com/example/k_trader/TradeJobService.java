package com.example.k_trader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.Log4jHelper;
import com.example.k_trader.base.OrderManager;
import com.example.k_trader.base.TradeData;
import com.example.k_trader.base.TradeDataManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.example.k_trader.base.TradeDataManager.Status.PLACED;
import static com.example.k_trader.base.TradeDataManager.Status.PROCESSED;
import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.NONE;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;

/**
 * Created by 김무창 on 2017-12-17.
 */

public class TradeJobService extends JobService {

    private static final int PRICE_SAVING_QUEUE_COUNT = 60;  // 1시간 분량의 시장가를 저장해 두고 분석에 사용한다.
    private static final int SELL_SLOT_LOOK_ASIDE_MAX = 3; // 3 단계 위까지 매도점을 찾아본다.
    private static final int BUY_SLOT_LOOK_ASIDE_MAX = 3;
    private static final double TRADING_VALUE_MIN = 0.0001;

    private double krwBalance;

    public static int currentPrice;                  // 비트코인 현재 시장가
    public static long lastNotiTimeInMillis;        // 마지막 Notification 완료 시점
    public static double availableBtcBalance;       // 현재 판매 가능한 비트코인 총량 = 현재 보유중인 비트코인 총량 - 매도 중인 비트코인 총량

    private TradeDataManager placedOrderManager = new TradeDataManager();
    private static TradeDataManager processedOrderManager = new TradeDataManager();

    private static List<Integer> priceQueue = new ArrayList<>();
    private static org.apache.log4j.Logger logger = Log4jHelper.getLogger("TradeJobService");
    private Context ctx;
    private OrderManager orderManager;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        new Thread() {
            public void run() {
                ctx = TradeJobService.this;
                orderManager = new OrderManager();

                try {
                    tradeBusinessLogic();
                } catch (Exception e) {}

                if (jobParameters.getJobId() == MainPage.JOB_ID_REGULAR)
                    scheduleRefresh();
                jobFinished(jobParameters, false);
            }
        }.start();

        // return true because new thread started.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void log_info(final String log) {
        if (logger != null) {
            logger.info(log);
        }

        Intent intent = new Intent(MainPage.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        if (MainPage.context != null) {
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(MainPage.context);
            if (manager != null)
                manager.sendBroadcast(intent);
        }
    }

    private void scheduleRefresh() {
        JobScheduler mJobScheduler = (JobScheduler)getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
        JobInfo.Builder mJobBuilder = new JobInfo.Builder(MainPage.JOB_ID_REGULAR, new ComponentName(getPackageName(), TradeJobService.class.getName()));

        /* For Android N and Upper Versions */
        mJobBuilder
                .setMinimumLatency(GlobalSettings.getInstance().getTradeInterval() * 1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        if (mJobScheduler != null && mJobScheduler.schedule(mJobBuilder.build()) <= JobScheduler.RESULT_FAILURE) {
            //Scheduled Failed/LOG or run fail safe measures
            log_info("Unable to schedule trade job!");
        }
    }

    private void notificationTrade(String title, String text) {
        Resources res = getResources();

        Intent notificationIntent = new Intent(this, MainActivity.class);
//        notificationIntent.putExtra("notificationId", "1212"); //전달할 값
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id_03");

        builder.setContentTitle(title)
                .setContentText(text)
                .setTicker(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL);

        builder.setCategory(Notification.CATEGORY_MESSAGE)
//                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 안드로이드 8.0 이상 노티피케이션을 사용하기 위해서는 하나 이상의 알림 채널을 만들어야한다.
        NotificationChannel notificationChannel = new NotificationChannel("my_channel_id_03", "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

        // Configure the notification channel.
        notificationChannel.setDescription("Channel description");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        // 거래 체결시 진동 소리만으로도 다른 Android noti와 구분할 수 있도록 전용 진동 패턴을 사용한다.
        notificationChannel.setVibrationPattern(new long[]{0, 100, 100, 100, 100, 100});
        notificationChannel.enableVibration(true);
        nm.createNotificationChannel(notificationChannel);

        if (nm != null)
            nm.notify((int)System.currentTimeMillis(), builder.build());
    }

    private TradeDataManager.Type convertSearchType(int search) {
        switch(search) {
            case 1 : return BUY;
            case 2 : return SELL;
        }
        return NONE;
    }

    // 1시간 동안 시장가 변동폭을 구해 리턴한다.
    private float getPriceVariationRate() {
        int maxPrice = Collections.max(priceQueue);
        int minPrice = Collections.min(priceQueue);

        int minIndex = priceQueue.indexOf(minPrice);
        int maxIndex = priceQueue.indexOf(maxPrice);

        if (minIndex < maxIndex) {
            // 상승
            return ((maxPrice / (float)minPrice ) - 1) * 100;
        } else {
            // 하락
            return ((minPrice / (float)maxPrice ) - 1) * 100;
        }
    }

    private List<TradeData> mergeSamePrice(List<TradeData> list) {
        Iterator<TradeData> i = list.iterator();
        List<TradeData> newList = new ArrayList<>();

        while (i.hasNext()) {
            TradeData outer = i.next();
            boolean skip = false;

            for (TradeData inner : newList) {
                if (inner.getPrice() == outer.getPrice()) {
                    inner.setUnits(outer.getUnits() + inner.getUnits());
                    if (outer.getProcessedTime() > inner.getProcessedTime())
                        inner.setProcessedTime(outer.getProcessedTime());
                    skip = true;
                }
            }

            if (!skip)
                newList.add(outer);
        }

        return newList;
    }

    private boolean isSameSlotOrder(TradeData oData, TradeData pData, int price) {
        if (((oData.getUnits() + pData.getUnits()) * price) <= (GlobalSettings.getInstance().getUnitPrice() + GlobalSettings.getInstance().getUnitPrice() * MainActivity.EARNINGS_RATIO)) {
            log_info("isSameSlotOrder : " + String.format(Locale.getDefault(), "%,d", (int)((oData.getUnits() + pData.getUnits()) * price))
                    + ", " + String.format(Locale.getDefault(), "%,d", (int)(oData.getUnits() * price))
                    + ", " + String.format(Locale.getDefault(), "%,d", (int)(pData.getUnits() * price)));

            return true;
        }

        return false;
    }

    private void tradeBusinessLogic() throws Exception {
        // Read settings again if MainActivity has been terminated by Android
        if (GlobalSettings.getInstance().getApiKey() == null) {
            SharedPreferences sharedPreferences = ctx.getSharedPreferences("settings", MODE_PRIVATE);
            GlobalSettings.getInstance().setUnitPrice(sharedPreferences.getInt("UNIT_PRICE", GlobalSettings.UNIT_PRICE_DEFAULT_VALUE));
            GlobalSettings.getInstance().setApiKey(sharedPreferences.getString("API_KEY", ""));
            GlobalSettings.getInstance().setApiSecret(sharedPreferences.getString("API_SECRET", ""));
            GlobalSettings.getInstance().setTradeInterval(sharedPreferences.getInt("TRADE_INTERVAL", GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE));
            GlobalSettings.getInstance().setFileLogEnabled(sharedPreferences.getBoolean("FILE_LOG_ENABLED", false));
            logger = Log4jHelper.getLogger("TradeJobService");
            log_info("App has been terminated by Android");
        }

        // static 변수 초기화
        if (lastNotiTimeInMillis == 0)
            lastNotiTimeInMillis = Calendar.getInstance().getTimeInMillis();

        Calendar currentTime = Calendar.getInstance();
        log_info("============================================");
        log_info(String.format(Locale.getDefault(), "%d/%02d/%02d %02d:%02d:%02d"
                , currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH) + 1, currentTime.get(Calendar.DATE)
                , currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND)));

        // 잔고를 가져와 업데이트 한다.
        {
            JSONObject dataObj = orderManager.getBalance("");
            krwBalance = Double.parseDouble((String)dataObj.get("total_krw"));
            availableBtcBalance = Double.parseDouble((String)dataObj.get("available_btc"));
        }

        // 비트코인 현재가를 가져온다.
        {
            JSONObject dataObj = orderManager.getCurrentPrice("");
            JSONArray dataArray = (JSONArray) dataObj.get("bids"); // 매수가
            if (dataArray != null) {
                JSONObject item = (JSONObject) dataArray.get(0); // 기본 5개 아이템 중 첫번째 아이템 사용
                currentPrice = (int)Double.parseDouble((String)item.get("price"));
            }

            log_info("BTC 현재가 : " + String.format(Locale.getDefault(), "%,d", currentPrice));

            // 빗썸은 0.0001 BTC가 최소 거래 단위이므로 체크
            if (currentPrice / 10000 > GlobalSettings.getInstance().getUnitPrice()) {
                log_info("확인 필요 : 현재 설정 된 1회 거래 금액 설정값(" + String.format(Locale.getDefault(), "%,d원", GlobalSettings.getInstance().getUnitPrice()) +")이 거래소 최소 거래 가능 금액 0.0001BTC" + String.format(Locale.getDefault(), "(%,d원)", currentPrice / 10000) + " 보다 작습니다.");
                return;
            }

            priceQueue.add(currentPrice);
            while (priceQueue.size() > PRICE_SAVING_QUEUE_COUNT) {
                // 가장 오래된 시장가를 밀어낸다.
                priceQueue.remove(0);
            }

            log_info("최근 한시간 변화폭 : " + String.format(Locale.getDefault(), "(%,.1f%%)", getPriceVariationRate()));
        }

        // 현재 걸려 있는 매도 리스트를 가져온다.
        {
            JSONArray dataArray = orderManager.getPlacedOrderList("");
            Log.d("KTrader", "placed order item count : " +  dataArray.size());

            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject item = (JSONObject) dataArray.get(i);
                placedOrderManager.add(placedOrderManager.build()
                        .setType(orderManager.convertOrderType((String) item.get("type")))
                        .setStatus(PLACED)
                        .setId((String) item.get("order_id"))
                        .setUnits((float) Double.parseDouble((String) item.get("units_remaining")))
                        .setPrice(Integer.parseInt(((String) item.get("price")).replaceAll(",", "")))
                        .setPlacedTime(Long.parseLong((String) item.get("order_date")) / 1000));
            }
        }

        // 현재 매도 걸려 있는 order들이 전부 매도 완료되었을 때 예상 잔고
        log_info("예상잔고 : " + String.format(Locale.getDefault(), "%,d"
                , (long)(krwBalance + placedOrderManager.getEstimation()) + (int)(availableBtcBalance * currentPrice)));

        // 매수/매도 완료 이력을 가져온다.
        {
            JSONArray dataArray = orderManager.getProcessedOrderList("", 0, "15");
            for (Object o : dataArray) {
                JSONObject item = (JSONObject)o;
//              Log.d("KTrader", item.toString());

                int search = Integer.parseInt((String)item.get("search"));
                long processedTimeInMillis;
                String date_string = (String)item.get("transfer_date");

                if (date_string.length() == 13)
                    processedTimeInMillis = Long.parseLong((String) item.get("transfer_date"));
                else // micro second
                    processedTimeInMillis = Long.parseLong((String) item.get("transfer_date")) / 1000;

                if (processedOrderManager.findByProcessedTime(processedTimeInMillis) == null && convertSearchType(search) != NONE) {
//                                Log.d("KTrader", item.toString());
                    processedOrderManager.add(processedOrderManager.build()
                            .setType(convertSearchType(search))
                            .setStatus(PROCESSED)
                            .setUnits(((float) Double.parseDouble(((String) item.get("units")).replace(" ", "").replace("-", ""))))
                            .setPrice(Math.abs(Integer.parseInt((String)item.get("price"))))
                            .setFeeRaw((String) item.get("fee"))
                            .setProcessedTime(processedTimeInMillis));
                }
            }
        }

        // 마지막 매수 관련 정보를 초기화 한다.
        {
            TradeData data = processedOrderManager.findLatestProcessedTime(BUY);
            if (data != null) {
                Calendar lastBuyTime;
                lastBuyTime = Calendar.getInstance();
                lastBuyTime.setTimeInMillis(data.getProcessedTime());
                log_info("마지막 매수 : " + String.format(Locale.getDefault(), "%,d", data.getPrice()) + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d"
                        , lastBuyTime.get(Calendar.MONTH) + 1, lastBuyTime.get(Calendar.DATE)
                        , lastBuyTime.get(Calendar.HOUR_OF_DAY), lastBuyTime.get(Calendar.MINUTE)));
            }
        }

        // 마지막 매도 관련 정보를 초기화 환다.
        {
            TradeData data = processedOrderManager.findLatestProcessedTime(SELL);
            if (data != null) {
                Calendar lastSellTime;
                lastSellTime = Calendar.getInstance();
                lastSellTime.setTimeInMillis(data.getProcessedTime());
                log_info("마지막 매도 : " + String.format(Locale.getDefault(), "%,d", data.getPrice()) + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d"
                        , lastSellTime.get(Calendar.MONTH) + 1, lastSellTime.get(Calendar.DATE)
                        , lastSellTime.get(Calendar.HOUR_OF_DAY), lastSellTime.get(Calendar.MINUTE)));
            }
        }

        // 마지막 Noti 이후 발생한 매도/매수에 대해서 Noti를 발송하고, 매수건에 대해서는 이익금을 더해 매도 오더를 발행한다.
        {
            // 마지막 Noti 이후 발생한 매도/매수만 필터링 한 결과를 얻는다.
            List<TradeData> list = processedOrderManager.getList().stream()
                    .filter(T -> T.getProcessedTime() > lastNotiTimeInMillis)
                    .collect(Collectors.toList());

            // 동일 가격이 여러개로 나눠져 있으면 합친다. (따로 매도 등록 되지 않도록 방지)
            List<TradeData> newList = mergeSamePrice(list);

            // 각 항목에 대해 Noti 처리한다.
            for (TradeData pData : newList) {
                Calendar time = Calendar.getInstance();
                time.setTimeInMillis(pData.getProcessedTime());

                if (pData.getType() == BUY) {
                    log_info("매수 발생 : " + String.format(Locale.getDefault(), "%,d", pData.getPrice()));
                    notificationTrade("매수 발생", "매수 : " + String.format(Locale.getDefault(), "%,d", pData.getPrice()) + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d"
                            , time.get(Calendar.MONTH) + 1, time.get(Calendar.DATE)
                            , time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));

                    // 매도 오더 발행 : 마지막 매수 오더가 완료되었다면 +INTERVAL_PRICE 가격에 매도 오더를 발행한다.
                    // 매수되었던 unit이 소수점 4자리 이하 일수도 있으니 다시 4자리로 절사 한다.
                    float unit = (float)((int)(pData.getUnits() * 10000) / 10000.0);

                    // 0.00005~9 만큼 남는다면 반올림한다.
                    if ((pData.getUnits() - unit) > 0.00005) {
                        if ((availableBtcBalance - unit) > 0.0001) {
                            unit = (float)(Math.round(pData.getUnits() * 10000d) / 10000d);
                            log_info(String.format(Locale.getDefault(), "매도 보정1 : %f -> %f", pData.getUnits(), unit));
                        }
                    }

                    // 이전 매수된 BTC 가 소수점 5자리에서 반올림 되는 경우 대비
                    // 남은 잔고보다 계산 값이 큰 경우에는 서버 에러가 발생하므로 잔고만큼만 매도한다. (ex : 0.0047 vs 0.00469..)
                    if (unit > availableBtcBalance) {
                        log_info(String.format(Locale.getDefault(), "매도 보정2 : %f, %f", unit, availableBtcBalance));
                        unit = (float)((int)(availableBtcBalance * 10000) / 10000.0);
                    }

                    // 매수된 내용이 있다면 가능한 상위 slot에 매도하도록 한다.
                    boolean isSold = false;
                    for (int i = 0; i< SELL_SLOT_LOOK_ASIDE_MAX; i++) {
                        // intervalPrice가 바뀌는 경계값일 때 문제를 해결하기 위해서 매도할 때의 interval은 현재가가 아니라 매수가를 기준으로 산정한다.
                        int sellIntervalPrice = MainPage.getProfitPrice(pData.getPrice()) / 2;
                        int targetPrice = pData.getPrice() + MainPage.getProfitPrice(pData.getPrice()) + (sellIntervalPrice * (SELL_SLOT_LOOK_ASIDE_MAX - 1 - i));
                        if ((pData.getPrice() % sellIntervalPrice) != 0)
                            targetPrice = (pData.getPrice() - (pData.getPrice() % sellIntervalPrice) + sellIntervalPrice) + MainPage.getProfitPrice(pData.getPrice()) + (sellIntervalPrice * (SELL_SLOT_LOOK_ASIDE_MAX - 1 - i));

                        TradeData oData = placedOrderManager.findByPrice(SELL, targetPrice);
                        if (oData == null || // Slot이 비어 있다면 해당 Slot에 매도 주문을 넣는다.
                                (oData != null && isSameSlotOrder(oData, pData, targetPrice))) { // 해당 Slot에 이미 Order가 있는 경우라도 분할 매수된 경우라면 동일 가격으로 매도 주문하도록 한다.
                            if (orderManager.addOrder("매수 발생 대응 매도", SELL, unit, targetPrice) == null) {
                                return;
                            }
                            isSold = true;
                            availableBtcBalance -= unit;

                            // 뒤쪽에서 매수 주문 낼 때 위에서 매도낸 금액이랑 똑같은 매수 다시 내지 않도록 리스트에 넣어둔다. (리스트 전체를 다시 갱신하려면 REST API를 한번 더 호출 해야 하니 경제적)
                            placedOrderManager.add(placedOrderManager.build()
                                    .setType(SELL)
                                    .setStatus(PLACED)
                                    .setUnits(unit)
                                    .setPrice(targetPrice));
                            break;
                        }
                    }
                    if (!isSold) {
                        notificationTrade("매도 실패", "매도시도 : "
                                + String.format(Locale.getDefault(), "%,d", pData.getPrice()));
                    }
                } else if (pData.getType() == SELL) {
                    log_info("매도 발생 : " + String.format(Locale.getDefault(), "%,d", pData.getPrice()));
                    notificationTrade("매도 발생", "매도 : " + String.format(Locale.getDefault(), "%,d", pData.getPrice()) + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d"
                            , time.get(Calendar.MONTH) + 1, time.get(Calendar.DATE)
                            , time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));
                } else {
                    // BUY, SELL  이외 수수료 쿠폰 구입 등의 항목일 경우에 여기로 올 수 있다.
                }

                if (pData.getProcessedTime() > lastNotiTimeInMillis)
                    lastNotiTimeInMillis = pData.getProcessedTime();
            }
        }

        // 매수건에 대한 매도를 다 처리 했음에도 BTC 잔고가 남아 있는 경우에 대한 예외처리, 가능한 slot을 찾아 매도 오더를 발행한다.
        // 예) 매수 발생 후 앱이 종료되었다가 앱이 재실행 된 경우
        if (availableBtcBalance > TRADING_VALUE_MIN) {
            log_info("매도 필요 잔고 : " + String.format("%.4f", availableBtcBalance));
            // 현재가보다 상위에 비어 있는 slot 중 하나를 찾아보고 있다면 매도하도록 한다.
            int floorPrice = getFloorPrice(currentPrice);
            double unit = Math.min(getUnitAmount4Price(floorPrice), (availableBtcBalance * 10000) / 10000.0);
            int sellIntervalPrice = MainPage.getProfitPrice(floorPrice) / 2; // 0.5%
            for (int i = 0; i< SELL_SLOT_LOOK_ASIDE_MAX; i++) {
                int targetPrice = floorPrice + MainPage.getProfitPrice(floorPrice) + (sellIntervalPrice * (SELL_SLOT_LOOK_ASIDE_MAX - 1 - i));

                TradeData oData = placedOrderManager.findByPrice(SELL, targetPrice);
                if (oData == null || // Slot이 비어 있다면 해당 Slot에 매도 주문을 넣는다.
                        (oData != null && isSameSlotOrder(oData, new TradeData().build().setUnits((float)unit), targetPrice))) { // 해당 Slot에 이미 Order가 있는 경우라도 분할 매수된 경우라면 동일 가격으로 매도 주문하도록 한다.
                    if (orderManager.addOrder("이전 실행 매수 발생 대응 매도", SELL, unit, targetPrice) == null) {
                        return;
                    }
                    availableBtcBalance -= unit;

                    // 뒤쪽에서 매수 주문 낼 때 위에서 매도낸 금액이랑 똑같은 매수 다시 내지 않도록 리스트에 넣어둔다. (리스트 전체를 다시 갱신하려면 REST API를 한번 더 호출 해야 하니 경제적)
                    placedOrderManager.add(placedOrderManager.build()
                            .setType(SELL)
                            .setStatus(PLACED)
                            .setUnits((float)unit)
                            .setPrice(targetPrice));
                    break;
                }
            }
        }

        // 매수 요청 발행, 어느 시점에서나 active한 매수 오더는 1개만 유지하도록 한다.
        {
            for (int i = 0; i< BUY_SLOT_LOOK_ASIDE_MAX; i++) {
                int targetPrice = getFloorPrice(currentPrice);
                targetPrice -= (i * (MainPage.getProfitPrice(targetPrice) / 2)); // 0.5%

                // 해당 가격에 이미 대기중인 매수가 있다면 skip
                if (placedOrderManager.findByPrice(BUY, targetPrice) != null)
                    return;

                // 해당 가격에 이미 대기중인 매도가 있다면 skip
                if (placedOrderManager.findByPrice(SELL, targetPrice + MainPage.getProfitPrice(targetPrice)) != null)
                    continue;

                log_info("다음 저점 매수가 : " + String.format(Locale.getDefault(), "%,d", targetPrice));

                // 체결 되기 어려운 낮은 가격 order는 모두 취소한다.
                for (TradeData tmp : placedOrderManager.getList()) {
                    if (tmp.getType() == BUY) {  // 1000만원 단위 경계에서 buy price가 미세하게 차이나서 data가 null이 되어 들어올 수 있으므로 전체 buy를 취소한다.
                        if (!orderManager.cancelOrder("체결 안 될 오더", tmp)) {
                            return;
                        }
                    }
                }

                // add buy request for targt price
                if (orderManager.addOrder("저점", BUY, getUnitAmount4Price(targetPrice), targetPrice) == null) {
                    return;
                }
                break;
            }
        }
    }

    // 주어진 가격 위쪽의 첫번째 매도 slot 가격을 구한다.
    private int getCeilingPrice(int price) {
        int floor = getFloorPrice(price);
        return floor + MainPage.getProfitPrice(floor);
    }

    // 주어진 가격 아래쪽의 첫번째 매수 slot 가격을 구한다. (0.5% 단위)
    private int getFloorPrice(int price) {
        return price - (price % (MainPage.getProfitPrice(price) / 2));
    }

    // 주어진 가격 slot에 매수 가능한 BTC 개수를 구한다. 소수점 아래 4자리로 절사
    private double getUnitAmount4Price(int price) {
        return (((double)GlobalSettings.getInstance().getUnitPrice() / price) * 10000) / 10000.0;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    public void setOrderManager(OrderManager orderManager) {
        this.orderManager = orderManager;
    }
}
