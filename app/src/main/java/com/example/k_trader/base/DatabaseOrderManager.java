package com.example.k_trader.base;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.example.k_trader.KTraderApplication;
import com.example.k_trader.database.entities.OrderEntity;
import com.example.k_trader.ui.fragment.TransactionLogPage;
import com.example.k_trader.bitthumb.lib.Api_Client;
import com.example.k_trader.database.DatabaseMonitor;
import com.example.k_trader.database.OrderRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.NONE;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;

/**
 * DB 기반 OrderManager
 * 모든 주문을 데이터베이스에 저장하고 DB 변경사항을 모니터링하여 UI 업데이트
 */
public class DatabaseOrderManager {
    
    private static final org.apache.log4j.Logger logger = Log4jHelper.getLogger("DatabaseOrderManager");
    
    private final TradeApiService tradeApiService;
    private final OrderRepository orderRepository;
    private final DatabaseMonitor databaseMonitor;
    private final CompositeDisposable disposables;

    public interface TradeApiService {
        Api_Client getApiService();
    }

    static class DefaultTradeApiService implements TradeApiService {
        @Override
        public Api_Client getApiService() {
            return new Api_Client();
        }
    }

    public DatabaseOrderManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.tradeApiService = new DefaultTradeApiService();
        this.orderRepository = OrderRepository.getInstance(appContext);
        this.databaseMonitor = DatabaseMonitor.getInstance(appContext);
        this.disposables = new CompositeDisposable();
    }








    /**
     * 미체결 주문 목록 조회 및 DB 동기화
     */
    public Completable syncPlacedOrders(String tag) {
        return Completable.fromAction(() -> {
            Api_Client api = tradeApiService.getApiService();
            JSONObject result;

            try {
                HashMap<String, String> param = new HashMap<>();
                param.put("count", "300");
                param.put("order_currency", "BTC");

                result = api.callApi("POST", "/info/orders", param);

                if (result == null) {
                    log_info(tag + " : " + "/info/orders : 1 : null");
                    return;
                }

                if (result.get("status") instanceof Long) {
                    log_info(tag + " : " + "/info/orders : 2 : " + result);
                    return;
                }

                if (result.get("status") != null && String.valueOf(result.get("status")).equals("5600")) {
                    if (result.get("message") != null && String.valueOf(result.get("message")).equals("거래 진행중인 내역이 존재하지 않습니다.")) {
                        // DB에서 미체결 주문들 삭제
                        orderRepository.deleteUnmarkedOrders().blockingAwait();
                        return;
                    }
                }

                if (result.get("status") == null || !String.valueOf(result.get("status")).equals("0000")) {
                    log_info(tag + " : " + "/info/orders : 3 : " + result);
                    return;
                }

                // API 응답을 DB와 동기화
                JSONArray dataArray = (JSONArray) result.get("data");
                if (dataArray != null) {
                    List<TradeData> apiOrders = new java.util.ArrayList<>();
                    
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject item = (JSONObject) dataArray.get(i);
                        String priceStr = (String) item.get("price");
                        int price = 0;
                        if (priceStr != null) {
                            price = Integer.parseInt(priceStr.replaceAll(",", ""));
                        }
                        
                        String typeStr = (String) item.get("type");
                        String unitsStr = (String) item.get("units_remaining");
                        
                        if (typeStr != null && unitsStr != null) {
                            TradeData tradeData = new TradeData()
                                .setType(convertOrderType(typeStr))
                                .setStatus(TradeDataManager.Status.PLACED)
                                .setId((String) item.get("order_id"))
                                .setUnits((float) Double.parseDouble(unitsStr))
                                .setPrice(price)
                                .setPlacedTime(System.currentTimeMillis())
                                .setMarked(true); // API에서 가져온 주문은 마킹
                            
                            apiOrders.add(tradeData);
                        }
                    }
                    
                    // DB에 저장 (결과는 의도적으로 무시함)
                    @SuppressWarnings("unused")
                    List<Long> insertResults = orderRepository.saveOrders(apiOrders).blockingGet();
                    
                    // 마킹되지 않은 주문들 삭제 (API에 없는 주문들)
                    orderRepository.deleteUnmarkedOrders().blockingAwait();
                    
                    // 모든 주문의 마킹 해제
                    orderRepository.unmarkAllOrders().blockingAwait();
                }

            } catch (Exception e) {
                // logger null 체크 추가
                if (logger != null) {
                    logger.error(tag + " : " + "/info/orders : 4 : " + e.getMessage(), e);
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 체결 주문 목록 조회 및 DB 동기화
     */
    public Completable syncProcessedOrders(String tag, int offset, String count) {
        return Completable.fromAction(() -> {
            Api_Client api = tradeApiService.getApiService();
            JSONObject result;

            try {
                HashMap<String, String> rgParams = new HashMap<>();
                rgParams.put("offset", String.valueOf(offset));
                rgParams.put("count", count); // 1~50, default = 20
                rgParams.put("searchGb", "0"); // 0 = all, 1 = buy
                rgParams.put("order_currency", "BTC");
                rgParams.put("payment_currency", "KRW");

                result = api.callApi("POST", "/info/user_transactions", rgParams);

                if (result == null) {
                    log_info(tag + " : " + "/info/user_transactions : null");
                    return;
                }

                if (result.get("status") instanceof Long) {
                    log_info(tag + " : " + "/info/user_transactions : " + result);
                    return;
                }

                if (result.get("status") == null || !String.valueOf(result.get("status")).equals("0000")) {
                    log_info(tag + " : " + "/info/user_transactions : " + result);
                    return;
                }

                // API 응답을 DB와 동기화
                JSONArray dataArray = (JSONArray) result.get("data");
                if (dataArray != null) {
                    List<TradeData> apiOrders = new java.util.ArrayList<>();
                    
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject item = (JSONObject) dataArray.get(i);
                        String priceStr = (String) item.get("price");
                        int price = 0;
                        if (priceStr != null) {
                            price = Integer.parseInt(priceStr.replaceAll(",", ""));
                        }
                        
                        String typeStr = (String) item.get("type");
                        String unitsStr = (String) item.get("units");
                        
                        if (typeStr != null && unitsStr != null) {
                            TradeData tradeData = new TradeData()
                                .setType(convertOrderType(typeStr))
                                .setStatus(TradeDataManager.Status.PROCESSED)
                                .setId((String) item.get("order_id"))
                                .setUnits((float) Double.parseDouble(unitsStr))
                                .setPrice(price)
                                .setFeeRaw((String) item.get("fee"))
                                .setPlacedTime(System.currentTimeMillis())
                                .setProcessedTime(System.currentTimeMillis())
                                .setMarked(true); // API에서 가져온 주문은 마킹
                            
                            apiOrders.add(tradeData);
                        }
                    }
                    
                    // DB에 저장 (결과는 의도적으로 무시함)
                    @SuppressWarnings("unused")
                    List<Long> insertResults = orderRepository.saveOrders(apiOrders).blockingGet();
                }

            } catch (Exception e) {
                // logger null 체크 추가
                if (logger != null) {
                    logger.error(tag + " : " + "/info/user_transactions : " + e.getMessage(), e);
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 주문 타입 변환
     */
    public TradeDataManager.Type convertOrderType(String type) {
        switch(type) {
            case "bid" : return BUY;
            case "ask" : return SELL;
        }
        return NONE;
    }

    /**
     * 앱 시작 시 초기 데이터 로드 및 API 동기화
     */
    public Completable initializeAndSyncData(String tag) {
        return Completable.fromAction(() -> {
            // 1. 먼저 DB에서 기존 데이터 로드
            List<TradeData> existingOrders = orderRepository.loadInitialOrders().blockingGet();
            
            // 기존 데이터를 UI에 표시
            databaseMonitor.notifyOrdersChanged(existingOrders);
            
            // 2. API에서 최신 데이터 가져와서 DB 동기화
            syncPlacedOrders(tag).blockingAwait();
            syncProcessedOrders(tag, 0, "50").blockingAwait();
            
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 주기적인 데이터 동기화 (백그라운드에서 실행)
     */
    public Completable periodicSyncData(String tag) {
        return Completable.fromAction(() -> {
            // 미체결 주문 동기화
            syncPlacedOrders(tag).blockingAwait();
            
            // 최근 체결 주문 동기화 (최근 20개)
            syncProcessedOrders(tag, 0, "20").blockingAwait();
            
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * 활성 거래 수 조회
     */
    public io.reactivex.Single<Integer> getActiveOrdersCount() {
        return orderRepository.getActiveOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 활성 SELL 주문 수 조회
     */
    public io.reactivex.Single<Integer> getActiveSellOrdersCount() {
        return orderRepository.getActiveSellOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 활성 BUY 주문 수 조회
     */
    public io.reactivex.Single<Integer> getActiveBuyOrdersCount() {
        return orderRepository.getActiveBuyOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 디버깅용: 모든 활성 주문 조회
     */
    public io.reactivex.Single<List<OrderEntity>> getAllActiveOrders() {
        return orderRepository.getAllActiveOrders()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 디버깅용: 활성 주문의 타입별 개수 조회
     */
    public io.reactivex.Single<List<com.example.k_trader.database.OrderTypeCount>> getActiveOrdersCountByType() {
        return orderRepository.getActiveOrdersCountByType()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 활성 주문 수 실시간 관찰 (Flowable)
     */
    public io.reactivex.Flowable<Integer> observeActiveOrdersCount() {
        return orderRepository.observeActiveOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 활성 SELL 주문 수 실시간 관찰 (Flowable)
     */
    public io.reactivex.Flowable<Integer> observeActiveSellOrdersCount() {
        return orderRepository.observeActiveSellOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 활성 BUY 주문 수 실시간 관찰 (Flowable)
     */
    public io.reactivex.Flowable<Integer> observeActiveBuyOrdersCount() {
        return orderRepository.observeActiveBuyOrdersCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 활성 주문 타입별 개수 실시간 관찰 (Flowable)
     */
    public io.reactivex.Flowable<List<com.example.k_trader.database.OrderTypeCount>> observeActiveOrdersCountByType() {
        return orderRepository.observeActiveOrdersCountByType()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 리소스 정리
     */
    public void dispose() {
        disposables.clear();
    }

    private void log_info(final String log) {
        if (logger != null)
            logger.info(log);
        Intent intent = new Intent(TransactionLogPage.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        if (KTraderApplication.getAppContext() != null)
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);
    }
    
}
