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
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.Log4jHelper;
import com.example.k_trader.base.OrderManager;
import com.example.k_trader.base.TradeData;
import com.example.k_trader.base.TradeDataManager;
import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;
import static com.example.k_trader.base.ErrorCode.*;

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
import static com.example.k_trader.base.TradeDataManager.Type.NONE;

/**
 * Created by 김무창 on 2017-12-17.
 */

public class TradeJobService extends JobService {

    private static final int PRICE_SAVING_QUEUE_COUNT = 60;  // 1시간 분량의 시장가를 저장해 두고 분석에 사용한다.
    private static final int SELL_SLOT_LOOK_ASIDE_MAX = 3; // 3 단계 위까지 매도점을 찾아본다.
    private static final int BUY_SLOT_LOOK_ASIDE_MAX = 3;
    private static final double TRADING_VALUE_MIN = 0.0001;
    
    // Foreground Service 관련 상수
    private static final int FOREGROUND_SERVICE_ID = 1001;
    private static final String CHANNEL_ID = "k_trader_foreground_channel";

    public static int currentPrice;                  // 현재 코인 시장가
    public static long lastNotiTimeInMillis;        // 마지막 Notification 완료 시점
    public static double availableCoinBalance;      // 현재 판매 가능한 코인 총량 = 현재 보유중인 코인 총량 - 매도 중인 코인 총량

    private final TradeDataManager placedOrderManager = new TradeDataManager();
    private static final TradeDataManager processedOrderManager = new TradeDataManager();

    private static final List<Integer> priceQueue = new ArrayList<>();
    private static org.apache.log4j.Logger logger = Log4jHelper.getLogger("TradeJobService");
    private Context ctx;
    private OrderManager orderManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d("KTrader", "[TradeJobService] onStartJob() 시작 - Job ID: " + jobParameters.getJobId());
        
        // Foreground Service로 시작
        startForegroundService();
        
        new Thread(() -> {
            ctx = TradeJobService.this;
            orderManager = new OrderManager();

            try {
                tradeBusinessLogic();
            } catch (Exception e) {
                // 예외 발생 시 로그만 출력
                log_info("Trade business logic error: " + e.getMessage());
                
                // 에러 카드 전송
                sendErrorCard("Trade Business Logic Error", ERR_BUSINESS_001.getDescription());
            }

            if (jobParameters.getJobId() == MainPage.JOB_ID_REGULAR)
                scheduleRefresh();
            jobFinished(jobParameters, false);
        }).start();

        // return true because new thread started.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void createNotificationChannel() {
        Log.d("KTrader", "[TradeJobService] createNotificationChannel() 시작");
        
        try {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "K-Trader Trading Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("K-Trader 자동 거래 서비스");
            channel.setShowBadge(false);
            // 앱바와 동일한 진한 주황색 파스텔 톤 적용
            channel.setLightColor(Color.parseColor("#FF8C42"));
            
            Log.d("KTrader", "[TradeJobService] NotificationChannel 생성 완료 - ID: " + CHANNEL_ID);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                Log.d("KTrader", "[TradeJobService] NotificationManager 획득 성공");
                
                // 기존 채널이 있는지 확인
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel != null) {
                    Log.d("KTrader", "[TradeJobService] 기존 채널 발견 - 삭제 후 재생성");
                    notificationManager.deleteNotificationChannel(CHANNEL_ID);
                }
                
                notificationManager.createNotificationChannel(channel);
                Log.d("KTrader", "[TradeJobService] NotificationChannel 생성 완료");
                
                // 채널 생성 확인
                NotificationChannel createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (createdChannel != null) {
                    Log.d("KTrader", "[TradeJobService] 채널 생성 확인 성공 - 중요도: " + createdChannel.getImportance());
                } else {
                    Log.e("KTrader", "[TradeJobService] 채널 생성 확인 실패");
                }
            } else {
                Log.e("KTrader", "[TradeJobService] NotificationManager 획득 실패");
            }
        } catch (Exception e) {
            Log.e("KTrader", "[TradeJobService] createNotificationChannel() 오류", e);
        }
    }

    private void startForegroundService() {
        Log.d("KTrader", "[TradeJobService] startForegroundService() 시작");
        
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Log.d("KTrader", "[TradeJobService] PendingIntent 생성 완료");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("K-Trader 자동 거래")
                .setContentText("백그라운드에서 자동 거래가 실행 중입니다")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setColor(getNotificationColorByTheme()); // 테마에 따른 동적 색상 설정

            Log.d("KTrader", "[TradeJobService] NotificationCompat.Builder 생성 완료");

            // 채널 존재 확인
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
                if (channel != null) {
                    Log.d("KTrader", "[TradeJobService] 채널 확인 성공 - 중요도: " + channel.getImportance());
                } else {
                    Log.e("KTrader", "[TradeJobService] 채널 확인 실패 - 채널이 존재하지 않음");
                }
            }

            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14 (API 34) 이상에서는 서비스 타입을 지정해야 함
                Log.d("KTrader", "[TradeJobService] Android 14+ - FOREGROUND_SERVICE_TYPE_DATA_SYNC 사용");
                startForeground(FOREGROUND_SERVICE_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                Log.d("KTrader", "[TradeJobService] Android 13 이하 - 기본 startForeground 사용");
                startForeground(FOREGROUND_SERVICE_ID, builder.build());
            }
            
            Log.d("KTrader", "[TradeJobService] startForeground() 호출 완료");
        } catch (Exception e) {
            Log.e("KTrader", "[TradeJobService] startForegroundService() 오류", e);
        }
    }

    private void scheduleRefresh() {
        JobScheduler mJobScheduler = (JobScheduler)getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder mJobBuilder = new JobInfo.Builder(MainPage.JOB_ID_REGULAR, new ComponentName(getPackageName(), TradeJobService.class.getName()));

        /* For Android N and Upper Versions */
        mJobBuilder
                .setMinimumLatency((long) GlobalSettings.getInstance().getTradeInterval() * 1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        if (mJobScheduler != null && mJobScheduler.schedule(mJobBuilder.build()) <= JobScheduler.RESULT_FAILURE) {
            //Scheduled Failed/LOG or run fail safe measures
            log_info("Unable to schedule trade job!");
        }
    }

    private void log_info(final String log) {
        if (logger != null) {
            logger.info(log);
        }

        Intent intent = new Intent(TransactionLogFragment.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        if (KTraderApplication.getAppContext() != null) {
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(KTraderApplication.getAppContext());
            // LocalBroadcastManager.getInstance()는 null을 반환할 수 있음
            manager.sendBroadcast(intent);
        }
    }


    private void notificationTrade(String title, String text) {
        Log.d("KTrader", "[TradeJobService] notificationTrade() 시작 - title: " + title + ", text: " + text);
        
        try {
            Resources res = getResources();

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Log.d("KTrader", "[TradeJobService] PendingIntent 생성 완료");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id_03");

            builder.setContentTitle(title)
                    .setContentText(text)
                    .setTicker(text)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_ALL);

            builder.setCategory(Notification.CATEGORY_MESSAGE)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);

            Log.d("KTrader", "[TradeJobService] NotificationCompat.Builder 생성 완료");

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (nm != null) {
                Log.d("KTrader", "[TradeJobService] NotificationManager 획득 성공");
                
                // 안드로이드 8.0 이상 노티피케이션을 사용하기 위해서는 하나 이상의 알림 채널을 만들어야한다.
                NotificationChannel notificationChannel = new NotificationChannel("my_channel_id_03", "K-Trader Trade Notifications", NotificationManager.IMPORTANCE_DEFAULT);

                // Configure the notification channel.
                notificationChannel.setDescription("K-Trader 거래 알림 채널");
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.parseColor("#FF8C42")); // 앱 테마와 일치하는 주황색
                // 거래 체결시 진동 소리만으로도 다른 Android noti와 구분할 수 있도록 전용 진동 패턴을 사용한다.
                notificationChannel.setVibrationPattern(new long[]{0, 100, 100, 100, 100, 100});
                notificationChannel.enableVibration(true);
                notificationChannel.setShowBadge(true);
                
                Log.d("KTrader", "[TradeJobService] NotificationChannel 생성 완료 - ID: my_channel_id_03");
                
                nm.createNotificationChannel(notificationChannel);
                Log.d("KTrader", "[TradeJobService] NotificationChannel 등록 완료");
                
                // 채널 생성 확인
                NotificationChannel createdChannel = nm.getNotificationChannel("my_channel_id_03");
                if (createdChannel != null) {
                    Log.d("KTrader", "[TradeJobService] 채널 생성 확인 성공 - 중요도: " + createdChannel.getImportance());
                } else {
                    Log.e("KTrader", "[TradeJobService] 채널 생성 확인 실패");
                }
                
                int notificationId = (int)System.currentTimeMillis();
                nm.notify(notificationId, builder.build());
                Log.d("KTrader", "[TradeJobService] Notification 등록 완료 - ID: " + notificationId);
            } else {
                Log.e("KTrader", "[TradeJobService] NotificationManager 획득 실패");
            }
        } catch (Exception e) {
            Log.e("KTrader", "[TradeJobService] notificationTrade() 오류", e);
        }
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
        if (((oData.getUnits() + pData.getUnits()) * price) <= (GlobalSettings.getInstance().getUnitPrice() + GlobalSettings.getInstance().getUnitPrice() * (GlobalSettings.getInstance().getEarningRate() / 100.0))) {
            log_info("isSameSlotOrder : " + String.format(Locale.getDefault(), "%,d", (int)((oData.getUnits() + pData.getUnits()) * price))
                    + ", " + String.format(Locale.getDefault(), "%,d", (int)(oData.getUnits() * price))
                    + ", " + String.format(Locale.getDefault(), "%,d", (int)(pData.getUnits() * price)));

            return true;
        }

        return false;
    }

    private void tradeBusinessLogic() throws Exception {
        Log.d("KTrader", "[TradeJobService] tradeBusinessLogic() 시작");
        
        // Read settings again if MainActivity has been terminated by Android
        if (GlobalSettings.getInstance().getApiKey() == null) {
            SharedPreferences sharedPreferences = ctx.getSharedPreferences("settings", MODE_PRIVATE);
            GlobalSettings.getInstance().setApiKey(sharedPreferences.getString(GlobalSettings.API_KEY_KEY_NAME, ""))
                                        .setApiSecret(sharedPreferences.getString(GlobalSettings.API_SECRET_KEY_NAME, ""))
                                        .setUnitPrice(sharedPreferences.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE))
                                        .setTradeInterval(sharedPreferences.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE))
                                        .setFileLogEnabled(sharedPreferences.getBoolean(GlobalSettings.FILE_LOG_ENABLED_KEY_NAME, false))
                                        .setEarningRate(sharedPreferences.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE))
                                        .setSlotIntervalRate(sharedPreferences.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE));
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
                double krwBalance;
                {
                    JSONObject dataObj = orderManager.getBalance("");
                    String totalKrw = (String) dataObj.get("total_krw");
                    String availableBtc = (String) dataObj.get("available_btc");
                    
                    if (totalKrw != null && availableBtc != null) {
                        krwBalance = Double.parseDouble(totalKrw);
                        availableCoinBalance = Double.parseDouble(availableBtc);
                    } else {
                        log_info("잔고 정보를 가져올 수 없습니다.");
                        sendErrorCard("Balance Error", ERR_API_003.getDescription());
                        return;
                    }
                }

        // 현재 코인 현재가를 가져온다.
        {
            JSONObject dataObj = orderManager.getCurrentPrice("");
            JSONArray dataArray = (JSONArray) dataObj.get("bids"); // 매수가
            if (dataArray != null && !dataArray.isEmpty()) {
                JSONObject item = (JSONObject) dataArray.get(0); // 기본 5개 아이템 중 첫번째 아이템 사용
                String priceStr = (String) item.get("price");
                if (priceStr != null) {
                    currentPrice = (int)Double.parseDouble(priceStr);
                } else {
                    log_info("현재가 정보를 가져올 수 없습니다.");
                    sendErrorCard("Price Error", ERR_API_004.getDescription());
                    return;
                }
            } else {
                log_info("매수 정보를 가져올 수 없습니다.");
                sendErrorCard("Buy Order Error", ERR_API_002.getDescription());
                return;
            }

            log_info(getCurrentCoinType() + " 현재가 : " + String.format(Locale.getDefault(), "%,d", currentPrice));
            
            // 카드 데이터 전송
            sendCardData(currentPrice, krwBalance);

            // 빗썸은 0.0001 코인이 최소 거래 단위이므로 체크
            String coinType = getCurrentCoinType();
            if (currentPrice / 10000 > GlobalSettings.getInstance().getUnitPrice()) {
                log_info("확인 필요 : 현재 설정 된 1회 거래 금액 설정값(" + String.format(Locale.getDefault(), "%,d원", GlobalSettings.getInstance().getUnitPrice()) +")이 거래소 최소 거래 가능 금액 0.0001" + coinType + String.format(Locale.getDefault(), "(%,d원)", currentPrice / 10000) + " 보다 작습니다.");
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
                
                String typeStr = (String) item.get("type");
                String unitsStr = (String) item.get("units_remaining");
                String priceStr = (String) item.get("price");
                String orderDateStr = (String) item.get("order_date");
                
                if (typeStr != null && unitsStr != null && priceStr != null && orderDateStr != null) {
                    placedOrderManager.add(placedOrderManager.build()
                            .setType(orderManager.convertOrderType(typeStr))
                            .setStatus(PLACED)
                            .setId((String) item.get("order_id"))
                            .setUnits((float) Double.parseDouble(unitsStr))
                            .setPrice(Integer.parseInt(priceStr.replaceAll(",", "")))
                            .setPlacedTime(Long.parseLong(orderDateStr) / 1000));
                }
            }
        }

        // 현재 매도 걸려 있는 order들이 전부 매도 완료되었을 때 예상 잔고
        log_info("예상잔고 : " + String.format(Locale.getDefault(), "%,d"
                , (long)(krwBalance + placedOrderManager.getEstimation()) + (int)(availableCoinBalance * currentPrice))
                + " , 주문가능원화 (" + String.format(Locale.getDefault(), "%,d", (long)(krwBalance)) +")" );
        //log_info("예상잔고 : " + String.format(Locale.getDefault(), "%,d"
        //        , (long)(krwBalance + placedOrderManager.getEstimation()) + (int)(availableCoinBalance * currentPrice))
        //        );
        log_info("매도완료시: " + String.format(Locale.getDefault(), "%,d", (long)(placedOrderManager.getEstimation()))
                + " , 주문잔고: " + String.format(Locale.getDefault(), "%,d", (int)(availableCoinBalance * currentPrice))
                );

        // 매수/매도 완료 이력을 가져온다.
        {
            JSONArray dataArray = orderManager.getProcessedOrderList("", 0, "15");
            for (Object o : dataArray) {
                JSONObject item = (JSONObject)o;
//              Log.d("KTrader", item.toString());

                String searchStr = (String)item.get("search");
                String transferDateStr = (String)item.get("transfer_date");
                
                if (searchStr != null && transferDateStr != null) {
                    int search = Integer.parseInt(searchStr);
                    long processedTimeInMillis;
                    
                    if (transferDateStr.length() == 13)
                        processedTimeInMillis = Long.parseLong(transferDateStr);
                    else // micro second
                        processedTimeInMillis = Long.parseLong(transferDateStr) / 1000;

                if (processedOrderManager.findByProcessedTime(processedTimeInMillis) == null && convertSearchType(search) != NONE) {
//                                Log.d("KTrader", item.toString());
                    String unitsStr = (String) item.get("units");
                    String priceStr = (String) item.get("price");
                    String feeStr = (String) item.get("fee");
                    
                    if (unitsStr != null && priceStr != null) {
                        processedOrderManager.add(processedOrderManager.build()
                                .setType(convertSearchType(search))
                                .setStatus(PROCESSED)
                                .setUnits(((float) Double.parseDouble(unitsStr.replace(" ", "").replace("-", ""))))
                                .setPrice(Math.abs(Integer.parseInt(priceStr)))
                                .setFeeRaw(feeStr)
                                .setProcessedTime(processedTimeInMillis));
                    }
                }
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
                        if ((availableCoinBalance - unit) > 0.0001) {
                            unit = (float)(Math.round(pData.getUnits() * 10000d) / 10000d);
                            log_info(String.format(Locale.getDefault(), "매도 보정1 : %f -> %f", pData.getUnits(), unit));
                        }
                    }

                    // 이전 매수된 BTC 가 소수점 5자리에서 반올림 되는 경우 대비
                    // 남은 잔고보다 계산 값이 큰 경우에는 서버 에러가 발생하므로 잔고만큼만 매도한다. (ex : 0.0047 vs 0.00469..)
                    // 런타임에 availableCoinBalance 값이 변경되므로 조건문은 정상적으로 동작함
                    if (unit > availableCoinBalance) {
                        log_info(String.format(Locale.getDefault(), "매도 보정2 : %f, %f", unit, availableCoinBalance));
                        unit = (float)((int)(availableCoinBalance * 10000) / 10000.0);
                    }

                    // 매수된 내용이 있다면 가능한 상위 slot에 매도하도록 한다.
                    boolean isSold = false;
                    for (int i = 0; i< SELL_SLOT_LOOK_ASIDE_MAX; i++) {
                        // intervalPrice가 바뀌는 경계값일 때 문제를 해결하기 위해서 매도할 때의 interval은 현재가가 아니라 매수가를 기준으로 산정한다.
                        int sellIntervalPrice = MainPage.getSlotIntervalPrice(pData.getPrice());
                        int targetPrice = pData.getPrice() + MainPage.getProfitPrice(pData.getPrice()) + (sellIntervalPrice * (SELL_SLOT_LOOK_ASIDE_MAX - 1 - i));
                        if ((pData.getPrice() % sellIntervalPrice) != 0)
                            targetPrice = (pData.getPrice() - (pData.getPrice() % sellIntervalPrice) + sellIntervalPrice) + MainPage.getProfitPrice(pData.getPrice()) + (sellIntervalPrice * (SELL_SLOT_LOOK_ASIDE_MAX - 1 - i));

                        TradeData oData = placedOrderManager.findByPrice(SELL, targetPrice);
                        // 런타임에 oData 값이 변경되므로 조건문은 정상적으로 동작함
                        @SuppressWarnings("ConstantConditions")
                        boolean oDataCondition = oData == null || // Slot이 비어 있다면 해당 Slot에 매도 주문을 넣는다.
                                (oData != null && isSameSlotOrder(oData, pData, targetPrice)); // 해당 Slot에 이미 Order가 있는 경우라도 분할 매수된 경우라면 동일 가격으로 매도 주문하도록 한다.
                        if (oDataCondition) {
                            Log.d("KTrader", "[TradeJobService] 매도 주문 시도 - 가격: " + targetPrice + ", 수량: " + unit);
                            JSONObject sellResult = orderManager.addOrder("매수 발생 대응 매도", SELL, unit, targetPrice);
                            if (sellResult == null) {
                                Log.e("KTrader", "[TradeJobService] 매도 주문 실패");
                                return;
                            } else {
                                Log.d("KTrader", "[TradeJobService] 매도 주문 성공: " + sellResult.toString());
                            }
                            isSold = true;
                            availableCoinBalance -= unit;

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
                    // BUY, SELL 이외 수수료 쿠폰 구입 등의 항목일 경우에 여기로 올 수 있다.
                    log_info("기타 거래 항목: " + pData.getType());
                }

                // 런타임에 lastNotiTimeInMillis 값이 변경되므로 조건문은 정상적으로 동작함
                if (pData.getProcessedTime() > lastNotiTimeInMillis)
                    lastNotiTimeInMillis = pData.getProcessedTime();
            }
        }

        // 매수건에 대한 매도를 다 처리 했음에도 코인 잔고가 남아 있는 경우에 대한 예외처리, 가능한 slot을 찾아 매도 오더를 발행한다.
        // 예) 매수 발생 후 앱이 종료되었다가 앱이 재실행 된 경우
        if (availableCoinBalance > TRADING_VALUE_MIN) {
            log_info("매도 필요 잔고 : " + String.format(Locale.getDefault(), "%.4f", availableCoinBalance));
            // 현재가보다 상위에 비어 있는 slot 중 하나를 찾아보고 있다면 매도하도록 한다.
            int floorPrice = getFloorPrice(currentPrice);
            double unit = Math.min(getUnitAmount4Price(floorPrice), (availableCoinBalance * 10000) / 10000.0);
            int sellIntervalPrice = MainPage.getSlotIntervalPrice(floorPrice) ;
            for (int i = 0; i< SELL_SLOT_LOOK_ASIDE_MAX; i++) {
                int targetPrice = floorPrice + MainPage.getProfitPrice(floorPrice) + (sellIntervalPrice * (SELL_SLOT_LOOK_ASIDE_MAX - 1 - i));

                TradeData oData = placedOrderManager.findByPrice(SELL, targetPrice);
                // 런타임에 oData 값이 변경되므로 조건문은 정상적으로 동작함
                @SuppressWarnings("ConstantConditions")
                boolean oDataCondition = oData == null || // Slot이 비어 있다면 해당 Slot에 매도 주문을 넣는다.
                        (oData != null && isSameSlotOrder(oData, new TradeData().build().setUnits((float)unit), targetPrice)); // 해당 Slot에 이미 Order가 있는 경우라도 분할 매수된 경우라면 동일 가격으로 매도 주문하도록 한다.
                if (oDataCondition) {
                    if (orderManager.addOrder("이전 실행 매수 발생 대응 매도", SELL, unit, targetPrice) == null) {
                        return;
                    }
                    availableCoinBalance -= unit;

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
            Log.d("KTrader", "[TradeJobService] 매수 주문 로직 시작 - 현재가: " + currentPrice);
            Log.d("KTrader", "[TradeJobService] KRW 잔고: " + krwBalance);
            
            for (int i = 0; i< BUY_SLOT_LOOK_ASIDE_MAX; i++) {
                int targetPrice = getFloorPrice(currentPrice);
                targetPrice -= (i * (MainPage.getSlotIntervalPrice(targetPrice)));
                
                Log.d("KTrader", "[TradeJobService] 매수 슬롯 " + i + " - 목표가격: " + targetPrice);

                // 해당 가격에 이미 대기중인 매수가 있다면 skip
                TradeData existingBuy = placedOrderManager.findByPrice(BUY, targetPrice);
                if (existingBuy != null) {
                    Log.d("KTrader", "[TradeJobService] 이미 대기중인 매수 주문 존재 - 가격: " + targetPrice + ", 수량: " + existingBuy.getUnits());
                    return;
                }

                // 해당 가격에 이미 대기중인 매도가 있다면 skip
                int sellPrice = targetPrice + MainPage.getProfitPrice(targetPrice);
                TradeData existingSell = placedOrderManager.findByPrice(SELL, sellPrice);
                if (existingSell != null) {
                    Log.d("KTrader", "[TradeJobService] 이미 대기중인 매도 주문 존재 - 가격: " + sellPrice + ", 수량: " + existingSell.getUnits());
                    continue;
                }

                log_info("다음 저점 매수가 : " + String.format(Locale.getDefault(), "%,d", targetPrice));

                // 매수 주문 전 잔고 확인
                double requiredAmount = getUnitAmount4Price(targetPrice) * targetPrice;
                Log.d("KTrader", "[TradeJobService] 매수 주문 필요 금액: " + requiredAmount + ", 보유 금액: " + krwBalance);
                
                if (krwBalance < requiredAmount) {
                    log_info("잔고 부족으로 매수 주문을 건너뜁니다. 필요: " + 
                        String.format(Locale.getDefault(), "%,.0f", requiredAmount) + 
                        "원, 보유: " + String.format(Locale.getDefault(), "%,.0f", krwBalance) + "원");
                    Log.d("KTrader", "[TradeJobService] 잔고 부족으로 매수 주문 건너뜀");
                    continue; // 다음 슬롯으로 이동
                }

                // 체결 되기 어려운 낮은 가격 order는 모두 취소한다.
                Log.d("KTrader", "[TradeJobService] 기존 매수 주문 취소 시작");
                for (TradeData tmp : placedOrderManager.getList()) {
                    if (tmp.getType() == BUY) {  // 1000만원 단위 경계에서 buy price가 미세하게 차이나서 data가 null이 되어 들어올 수 있으므로 전체 buy를 취소한다.
                        Log.d("KTrader", "[TradeJobService] 기존 매수 주문 취소 - 가격: " + tmp.getPrice() + ", 수량: " + tmp.getUnits());
                        if (!orderManager.cancelOrder("체결 안 될 오더", tmp)) {
                            Log.e("KTrader", "[TradeJobService] 기존 매수 주문 취소 실패");
                            return;
                        }
                    }
                }

                // add buy request for targt price
                if (orderManager.addOrder("저점", BUY, getUnitAmount4Price(targetPrice), targetPrice) == null) {
                    return;
                } else {
                    Log.d("KTrader", "[TradeJobService] 매수 주문 성공");
                }
                break;
            }
        }
    }

    // 주어진 가격 아래쪽의 첫번째 매수 slot 가격을 구한다.
    private int getFloorPrice(int price) {
        return price - (price % MainPage.getSlotIntervalPrice(price));
    }

    // 주어진 가격 slot에 매수 가능한 코인 개수를 구한다. 소수점 아래 4자리로 절사
    private double getUnitAmount4Price(int price) {
        return (((double)GlobalSettings.getInstance().getUnitPrice() / price) * 10000) / 10000.0;
    }
    
    /**
     * 현재 설정된 코인 타입을 반환
     */
    private String getCurrentCoinType() {
        String coinType = GlobalSettings.getInstance().getCoinType();
        if (GlobalSettings.COIN_TYPE_ETH.equals(coinType)) {
            return "ETH";
        } else {
            return "BTC"; // 기본값
        }
    }
    
    /**
     * API에서 현재 등락률 정보를 가져옴 (TransactionCard용 - 1시간 전 대비)
     */
    private String getCurrentPriceChangeFromApi() {
        try {
            // TransactionDataManager를 통해 최신 등락률 정보 가져오기
            com.example.k_trader.data.TransactionDataManager dataManager = 
                com.example.k_trader.data.TransactionDataManager.getInstance(KTraderApplication.getAppContext());
            
            // 캐시된 데이터에서 1시간 전 대비 등락률 정보 가져오기
            com.example.k_trader.data.TransactionData cachedData = dataManager.getCachedData();
            if (cachedData != null && cachedData.getHourlyChange() != null) {
                String change = cachedData.getHourlyChange();
                Log.d("KTrader", "[TradeJobService] Using cached hourly change (1H): " + change);
                return change;
            }
            
            // 캐시된 데이터가 없으면 기본값 반환
            Log.w("KTrader", "[TradeJobService] No cached hourly change data available");
            return "+0.00%";
            
        } catch (Exception e) {
            Log.e("KTrader", "[TradeJobService] Error getting hourly change from API", e);
            return "+0.00%";
        }
    }
    
    /**
     * API에서 전일 대비 등락률 정보를 가져옴 (CoinInfo용)
     */
    private String getDailyChangeFromApi() {
        try {
            // TransactionDataManager를 통해 최신 등락률 정보 가져오기
            com.example.k_trader.data.TransactionDataManager dataManager = 
                com.example.k_trader.data.TransactionDataManager.getInstance(KTraderApplication.getAppContext());
            
            // 캐시된 데이터에서 전일 대비 등락률 정보 가져오기
            com.example.k_trader.data.TransactionData cachedData = dataManager.getCachedData();
            if (cachedData != null && cachedData.getDailyChange() != null) {
                String change = cachedData.getDailyChange();
                Log.d("KTrader", "[TradeJobService] Using cached daily change (24H): " + change);
                return change;
            }
            
            // 캐시된 데이터가 없으면 기본값 반환
            Log.w("KTrader", "[TradeJobService] No cached daily change data available");
            return "+0.00%";
            
        } catch (Exception e) {
            Log.e("KTrader", "[TradeJobService] Error getting daily change from API", e);
            return "+0.00%";
        }
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    public void setOrderManager(OrderManager orderManager) {
        this.orderManager = orderManager;
    }
    
    private void sendCardData(int currentPrice, double krwBalance) {
        try {
            Calendar currentTime = Calendar.getInstance();
            String transactionTime = String.format(Locale.getDefault(), "%d/%02d/%02d %02d:%02d:%02d",
                currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH) + 1, currentTime.get(Calendar.DATE),
                currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND));
            
            String coinCurrentPrice = String.format(Locale.getDefault(), "₩%,d", currentPrice);
            
            // 시간당 변화율은 API에서 가져온 실제 데이터 사용
            String hourlyChange = getCurrentPriceChangeFromApi();
            
            String estimatedBalance = String.format(Locale.getDefault(), "₩%,.0f", krwBalance);
            
            // 마지막 매수 정보 가져오기
            String lastBuyPrice = "정보 없음";
            TradeData lastBuyData = processedOrderManager.findLatestProcessedTime(BUY);
            if (lastBuyData != null) {
                Calendar lastBuyTime = Calendar.getInstance();
                lastBuyTime.setTimeInMillis(lastBuyData.getProcessedTime());
                lastBuyPrice = String.format(Locale.getDefault(), "₩%,d (%02d/%02d %02d:%02d)",
                    lastBuyData.getPrice(),
                    lastBuyTime.get(Calendar.MONTH) + 1, lastBuyTime.get(Calendar.DATE),
                    lastBuyTime.get(Calendar.HOUR_OF_DAY), lastBuyTime.get(Calendar.MINUTE));
            }
            
            // 마지막 매도 정보 가져오기
            String lastSellPrice = "정보 없음";
            TradeData lastSellData = processedOrderManager.findLatestProcessedTime(SELL);
            if (lastSellData != null) {
                Calendar lastSellTime = Calendar.getInstance();
                lastSellTime.setTimeInMillis(lastSellData.getProcessedTime());
                lastSellPrice = String.format(Locale.getDefault(), "₩%,d (%02d/%02d %02d:%02d)",
                    lastSellData.getPrice(),
                    lastSellTime.get(Calendar.MONTH) + 1, lastSellTime.get(Calendar.DATE),
                    lastSellTime.get(Calendar.HOUR_OF_DAY), lastSellTime.get(Calendar.MINUTE));
            }
            
            // 다음 저점 매수가 계산 (간단한 예시)
            String nextBuyPrice = String.format(Locale.getDefault(), "₩%,d (%02d/%02d %02d:%02d)",
                currentPrice - 100000, // 현재가에서 10만원 낮춘 가격
                currentTime.get(Calendar.MONTH) + 1, currentTime.get(Calendar.DATE),
                currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE) + 5);
            
            // 가격 정보를 데이터베이스에 저장 (실시간 관찰을 위해) - CoinInfo용 전일 대비 등락률 사용
            String dailyChangeForCoinInfo = getDailyChangeFromApi();
            savePriceInfoToDatabase(coinCurrentPrice, dailyChangeForCoinInfo);
            
            Intent intent = new Intent("TRADE_CARD_DATA");
            intent.putExtra("transactionTime", transactionTime);
            intent.putExtra("btcCurrentPrice", coinCurrentPrice);  // MainPage에서 사용하는 키로 변경
            intent.putExtra("hourlyChange", hourlyChange);
            intent.putExtra("estimatedBalance", estimatedBalance);
            intent.putExtra("lastBuyPrice", lastBuyPrice);
            intent.putExtra("lastSellPrice", lastSellPrice);
            intent.putExtra("nextBuyPrice", nextBuyPrice);
            
            Log.d("KTrader", "[TradeJobService] Sending card data - Price: " + coinCurrentPrice + ", Change: " + hourlyChange);
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("[TradeJobService]", "카드 데이터 전송 중 오류 발생", e);
            
            // 에러 카드 전송
            sendErrorCard("Card Data Send Error", ERR_CARD_DATA_001.getDescription());
        }
    }
    
    private void sendErrorCard(String errorType, String errorMessage) {
        try {
            Calendar currentTime = Calendar.getInstance();
            String errorTime = String.format(Locale.getDefault(), "%d/%02d/%02d %02d:%02d:%02d",
                currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH) + 1, currentTime.get(Calendar.DATE),
                currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND));
            
            Intent intent = new Intent("TRADE_ERROR_CARD");
            intent.putExtra("errorTime", errorTime);
            intent.putExtra("errorType", errorType);
            intent.putExtra("errorMessage", errorMessage);
            
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e("[TradeJobService[", "에러 카드 전송 중 오류 발생", e);
        }
    }
    
    /**
     * 현재 테마에 따라 Notification 색상을 반환하는 메서드
     */
    private int getNotificationColorByTheme() {
        // 현재 테마가 Light 테마인지 확인
        boolean isLightTheme = isLightTheme();
        
        if (isLightTheme) {
            return ContextCompat.getColor(this, R.color.notification_light);
        } else {
            return ContextCompat.getColor(this, R.color.notification_dark);
        }
    }
    
    /**
     * 현재 테마가 Light 테마인지 확인하는 메서드
     */
    private boolean isLightTheme() {
        // 현재 앱이 Light 테마를 사용하고 있는지 확인
        // AppTheme의 parent가 Theme.AppCompat.Light.DarkActionBar이므로 Light 테마
        return true; // 현재 앱은 Light 테마 사용
    }
    
    /**
     * 가격 정보를 데이터베이스에 저장
     */
    private void savePriceInfoToDatabase(String currentPrice, String priceChange) {
        try {
            // 현재 코인 타입 가져오기
            String coinType = com.example.k_trader.base.GlobalSettings.getInstance().getCoinType();
            
            // CoinPriceInfoRepository를 사용하여 데이터베이스에 저장
            com.example.k_trader.database.CoinPriceInfoRepository repository = 
                new com.example.k_trader.database.CoinPriceInfoRepository(KTraderApplication.getAppContext());
            
            repository.savePriceInfo(coinType, currentPrice, priceChange)
                .subscribe(
                    () -> Log.d("KTrader", "[TradeJobService] Price info saved to database successfully"),
                    throwable -> Log.e("KTrader", "[TradeJobService] Error saving price info to database", throwable)
                );
                
        } catch (Exception e) {
            Log.e("KTrader", "[TradeJobService] Error in savePriceInfoToDatabase", e);
        }
    }
}
