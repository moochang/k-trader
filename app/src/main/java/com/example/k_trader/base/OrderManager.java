package com.example.k_trader.base;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.k_trader.MainActivity;
import com.example.k_trader.MainPage;
import com.example.k_trader.KTraderApplication;
import com.example.k_trader.TransactionLogFragment;
import com.example.k_trader.bitthumb.lib.Api_Client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.NONE;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;
import static com.example.k_trader.base.ErrorCode.*;

/**
 * Created by 김무창 on 2017-12-23.
 */

public class OrderManager {
    private static long lastRequestTimeInMillis = 0;
    private static final long safeIntervalInSec = 15;
    private static final org.apache.log4j.Logger logger = Log4jHelper.getLogger("OrderManager");
    private final TradeApiService tradeApiService;

    public interface TradeApiService {
        Api_Client getApiService();
    }

    static class DefaultTradeApiService implements TradeApiService {
        @Override
        public Api_Client getApiService() {
            return new Api_Client();
        }
    }

    public OrderManager() {
        tradeApiService = new DefaultTradeApiService();
    }

    public OrderManager(TradeApiService tradeApiService) {
        this.tradeApiService = tradeApiService;
    }

    public boolean cancelOrder(String tag, TradeData data) {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result;

        HashMap<String, String> rgParams = new HashMap<>();
        if (data.getType() == BUY)
            rgParams.put("type", "bid");
        else
            rgParams.put("type", "ask");

        rgParams.put("order_currency", getCurrentCoinType());
        rgParams.put("order_id", data.getId());
        rgParams.put("payment_currency", "KRW");

        log_info(tag + " : " + data.getType().toString() + " 취소 : " + data.getId() + " : " + data.getUnits() + " : " + String.format(Locale.getDefault(), "%,d", data.getPrice()));

        try {
            result = api.callApi("POST", "/trade/cancel", rgParams);

            if (result == null) {
                log_info(tag + " : " + "/trade/cancel : null");
                return false;
            }

            if (result.get("status") instanceof Long) {
                String logMessage = tag + " : " + "/trade/cancel : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_001.getDescription());
                return false;
            }

            if (!((String) result.get("status")).equals("0000")) {
                String logMessage = tag + " : " + "/trade/cancel : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_001.getDescription());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String logMessage = tag + " : " + "/trade/cancel : " + e.getMessage();
            log_info(logMessage);
            sendErrorCard("API Error", ERR_API_001.getDescription());
            return false;
        }

        return true;
    }

    public boolean cancelAllBuyOrders() {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = api.callApi("POST", "/info/orders", null);
        int cancelCount = 0;

        if (result == null) {
            String logMessage = "/info/orders : null";
            sendErrorCard("API Error", ERR_API_002.getDescription());
            return false;
        }

        JSONArray dataArray = (JSONArray) result.get("data");
        if (dataArray != null) {
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject item = (JSONObject) dataArray.get(i);
                String type = (String) item.get("type");
                if (type.equals("bid")) { // buy
                    TradeData data = new TradeData();
                    data.setType(BUY);
                    data.setId((String) item.get("order_id"));
                    data.setUnits((float) Double.parseDouble((String) item.get("units_remaining")));
                    data.setPrice(Integer.parseInt(((String) item.get("price")).replaceAll(",", "")));
                    cancelOrder("전체취소", data);
                    cancelCount++;
                }
            }
            log_info(String.format("취소 결과 : %d개", cancelCount));
        }

        return true;
    }

    public JSONObject addOrder(String tag, TradeDataManager.Type type, double units, int price) {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result;
        long requestTime = Calendar.getInstance().getTimeInMillis();

        if (units < 0.0001) {
            String logMessage = tag + " : " + type.toString() + " 발행 취소 : " + String.format("%.4f", units) + " : " + "최소 수량 미달";
            log_info(logMessage);
            sendErrorCard("Validation Error", ERR_VALIDATION_001.getDescription());
            Log.d("KTrader", "Order " + "Validation Error");
            return null;
        }

        // 마지막 요청으로부터 15초 이내에 신규 요청이 온 경우에는 delay 시킨다.
        // {"message":"Please try again","status":"5600"} 에러 방지 목적
        while ((requestTime - lastRequestTimeInMillis) < safeIntervalInSec * 1000) {
            Intent intent = new Intent(MainActivity.BROADCAST_PROGRESS_MESSAGE);

            Log.d("KTrader", "Order sending progress : " + String.valueOf((15 * 1000) - (requestTime - lastRequestTimeInMillis)));

            intent.putExtra("progress", (int)((safeIntervalInSec) - (requestTime - lastRequestTimeInMillis)/1000));
            if (KTraderApplication.getAppContext() != null)
                LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);

            try {
                Thread.sleep((safeIntervalInSec * 1000) - (requestTime - lastRequestTimeInMillis));
            } catch (InterruptedException e) {
                // 에러처리
            }

            requestTime = Calendar.getInstance().getTimeInMillis();
        }

        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("order_currency", getCurrentCoinType());
        rgParams.put("Payment_currency", "KRW");
        rgParams.put("units", String.format("%.4f", units));
        rgParams.put("price", String.valueOf(price));
        rgParams.put("payment_currency", "KRW");

        if (type == BUY)
            rgParams.put("type", "bid");
        else
            rgParams.put("type", "ask");

        // 매수 주문인 경우 잔고 확인
        if (type == BUY) {
            try {
                JSONObject balanceData = getBalance("매수 전 잔고 확인");
                if (balanceData != null) {
                    String totalKrw = (String) balanceData.get("total_krw");
                    if (totalKrw != null) {
                        double krwBalance = Double.parseDouble(totalKrw);
                        double requiredAmount = units * price;
                        
                        if (krwBalance < requiredAmount) {
                            log_info(tag + " : 잔고 부족으로 매수 주문을 건너뜁니다. 필요: " + 
                                String.format(Locale.getDefault(), "%,.0f", requiredAmount) + 
                                "원, 보유: " + String.format(Locale.getDefault(), "%,.0f", krwBalance) + "원");
                            return null;
                        }
                    }
                }
            } catch (Exception e) {
                log_info(tag + " : 잔고 확인 중 오류 발생: " + e.getMessage());
                return null;
            }
        }

        log_info(tag + " : " + type.toString() + " 발행 시도 : " + String.format("%.4f", units) + " : " + String.format(Locale.getDefault(), "%,d", price));
        log_info(tag + " : API Key 설정 상태: " + (GlobalSettings.getInstance().getApiKey().isEmpty() ? "비어있음" : "설정됨"));
        log_info(tag + " : API Secret 설정 상태: " + (GlobalSettings.getInstance().getApiSecret().isEmpty() ? "비어있음" : "설정됨"));
        log_info(tag + " : 코인 타입: " + getCurrentCoinType());

        try {
            result = api.callApi("POST", "/trade/place", rgParams);

            if (result == null) {
                log_info(tag + " : " + "/trade/place : null");
                Log.d("KTrader", "Order " + "/trade/place : null");
                return null;
            }

            if (result.get("status") instanceof Long) {
                String logMessage = tag + " : " + "/trade/place : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_005.getDescription());
                Log.d("KTrader", "Order " + logMessage);
                return null;
            }

            if (!((String) result.get("status")).equals("0000")) {
                String logMessage = tag + " : " + "/trade/place : " + result.toString();
                log_info(logMessage);
                log_info(tag + " : API 오류 상세 - Status: " + result.get("status") + ", Message: " + result.get("message"));
                sendErrorCard("API Error", ERR_API_005.getDescription());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String logMessage = tag + " : " + "/trade/place : " + e.getMessage();
            log_info(logMessage);
            sendErrorCard("API Error", ERR_API_005.getDescription());
            Log.d("KTrader", "Order " + logMessage);
            return null;
        }

        lastRequestTimeInMillis = Calendar.getInstance().getTimeInMillis();
        Log.d("KTrader", "Order : " + result);
        return result;
    }

    private void log_info(final String log) {
        if (logger != null)
            logger.info(log);
        Intent intent = new Intent(TransactionLogFragment.BROADCAST_LOG_MESSAGE);
        intent.putExtra("log", log);
        if (KTraderApplication.getAppContext() != null)
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);
    }

    public JSONObject addOrderWithMarketPrice(String tag, TradeDataManager.Type type, float units) {
        Log.d("KTrader", "[OrderManager] addOrderWithMarketPrice() 시작 - tag: " + tag + ", type: " + type + ", units: " + units);
        log_info(tag + " : 시장가 주문 시작 - " + type.toString() + " " + String.format("%.4f", units));
        
        Api_Client api = tradeApiService.getApiService();
        JSONObject result;
        long requestTime = Calendar.getInstance().getTimeInMillis();

        // 마지막 요청으로부터 15초 이내에 신규 요청이 온 경우에는 delay 시킨다.
        // {"message":"Please try again","status":"5600"} 에러 방지 목적
        while ((requestTime - lastRequestTimeInMillis) < safeIntervalInSec * 1000) {
            Intent intent = new Intent(MainActivity.BROADCAST_PROGRESS_MESSAGE);

//            Log.d("KTrader", "sending progress : " + String.valueOf((15 * 1000) - (requestTime - lastRequestTimeInMillis)));

            intent.putExtra("progress", (int)((safeIntervalInSec * 1000) - (requestTime - lastRequestTimeInMillis)));
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext()).sendBroadcast(intent);

            try {
                Thread.sleep((safeIntervalInSec * 1000) - (requestTime - lastRequestTimeInMillis));
            } catch (InterruptedException e) {
                // 에러처리
            }

            requestTime = Calendar.getInstance().getTimeInMillis();
        }

        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("order_currency", getCurrentCoinType());
        rgParams.put("units", String.format("%.4f", units));
        rgParams.put("payment_currency", "KRW");

        // 매수 주문인 경우 잔고 확인 (PlacedOrderPage에서 이미 확인했지만 추가 안전장치)
        if (type == BUY) {
            Log.d("KTrader", "[OrderManager] 매수 주문 - 추가 잔고 확인");
            try {
                JSONObject balanceData = getBalance("시장가 매수 전 잔고 확인");
                Log.d("KTrader", "[OrderManager] 잔고 조회 결과: " + (balanceData != null ? "성공" : "실패"));
                
                if (balanceData != null) {
                    String totalKrw = (String) balanceData.get("total_krw");
                    Log.d("KTrader", "[OrderManager] KRW 잔고: " + totalKrw);
                    
                    if (totalKrw != null) {
                        double krwBalance = Double.parseDouble(totalKrw);
                        // 시장가 매수이므로 현재가를 가져와서 계산
                        JSONObject tickerData = getTicker("시장가 매수 현재가 확인");
                        Log.d("KTrader", "[OrderManager] ticker 조회 결과: " + (tickerData != null ? "성공" : "실패"));
                        
                        if (tickerData != null) {
                            JSONObject data = (JSONObject) tickerData.get("data");
                            if (data != null) {
                                String currentPriceStr = (String) data.get("closing_price");
                                Log.d("KTrader", "[OrderManager] 현재가: " + currentPriceStr);
                                
                                if (currentPriceStr != null) {
                                    double currentPrice = Double.parseDouble(currentPriceStr);
                                    double requiredAmount = units * currentPrice;
                                    Log.d("KTrader", "[OrderManager] 필요 금액: " + requiredAmount + ", 보유 금액: " + krwBalance);
                                    
                                    if (krwBalance < requiredAmount) {
                                        String message = tag + " : 잔고 부족으로 시장가 매수 주문을 건너뜁니다. 필요: " + 
                                            String.format(Locale.getDefault(), "%,.0f", requiredAmount) + 
                                            "원, 보유: " + String.format(Locale.getDefault(), "%,.0f", krwBalance) + "원";
                                        Log.w("KTrader", "[OrderManager] " + message);
                                        log_info(message);
                                        return null;
                                    }
                                    Log.d("KTrader", "[OrderManager] 잔고 확인 완료 - 시장가 매수 가능");
                                } else {
                                    Log.e("KTrader", "[OrderManager] closing_price 정보가 null");
                                    log_info(tag + " : 현재가 정보를 가져올 수 없습니다");
                                    return null;
                                }
                            } else {
                                Log.e("KTrader", "[OrderManager] ticker data가 null");
                                log_info(tag + " : 현재가 데이터를 가져올 수 없습니다");
                                return null;
                            }
                        } else {
                            Log.e("KTrader", "[OrderManager] ticker 조회 실패");
                            log_info(tag + " : 현재가 조회에 실패했습니다");
                            return null;
                        }
                    } else {
                        Log.e("KTrader", "[OrderManager] KRW 잔고 정보가 null");
                        log_info(tag + " : KRW 잔고 정보를 가져올 수 없습니다");
                        return null;
                    }
                } else {
                    Log.e("KTrader", "[OrderManager] 잔고 조회 실패");
                    log_info(tag + " : 잔고 조회에 실패했습니다");
                    return null;
                }
            } catch (Exception e) {
                Log.e("KTrader", "[OrderManager] 잔고 확인 중 오류 발생", e);
                log_info(tag + " : 시장가 매수 잔고 확인 중 오류 발생: " + e.getMessage());
                return null;
            }
        }

        log_info(tag + " : " + type.toString() + " 시장가 발행 : " + String.format("%.4f", units) + " : ");

        try {
            String endpoint = type == BUY ? "/trade/market_buy" : "/trade/market_sell";
            Log.d("KTrader", "[OrderManager] API 호출 시작 - endpoint: " + endpoint);
            
            if (type == BUY)
                result = api.callApi("POST", "/trade/market_buy", rgParams);
            else
                result = api.callApi("POST", "/trade/market_sell", rgParams);
                
            Log.d("KTrader", "[OrderManager] API 호출 완료 - 결과: " + (result != null ? "성공" : "실패"));

            if (result == null) {
                Log.e("KTrader", "[OrderManager] API 응답이 null");
                log_info(tag + " : " + "/trade/market_(buy/sell) : null");
                return null;
            }
            
            Log.d("KTrader", "[OrderManager] API 응답: " + result.toString());

            if (result.get("status") instanceof Long) {
                String logMessage = tag + " : " + "/trade/market_(buy/sell)1 : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_006.getDescription());
                return null;
            }

            // {"message":"잠시 후 이용해 주십시오.[9900]","status":"5600"}
            if (!((String) result.get("status")).equals("0000")) {
                String logMessage = tag + " : " + "/trade/market_(buy/sell)2 : " + result.toString();
                Log.e("KTrader", "[OrderManager] " + logMessage);
                log_info(logMessage);
                log_info(tag + " : API 오류 상세 - Status: " + result.get("status") + ", Message: " + result.get("message"));
                sendErrorCard("API Error", ERR_API_006.getDescription());
                return null;
            }
            
            Log.d("KTrader", "[OrderManager] 시장가 주문 성공");
        } catch (Exception e) {
            e.printStackTrace();
            String logMessage = tag + " : " + "/trade/market_(buy/sell)3 : " + e.getMessage();
            log_info(logMessage);
            sendErrorCard("API Error", ERR_API_006.getDescription());
            return null;
        }

        lastRequestTimeInMillis = Calendar.getInstance().getTimeInMillis();
        Log.d("KTrader", "[OrderManager] addOrderWithMarketPrice() 완료 - 결과: " + result.toString());

        return result;
    }

    public JSONObject getBalance(String tag) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            result = api.callApi("POST", "/info/balance", null);

            if (result == null) {
                log_info(tag + " : " + "/info/balance : null");
                throw new Exception("returns null");
            }

            if (result.get("status") instanceof Long) {
                log_info(tag + " : " + "/info/balance : " + result.toString());
                throw new Exception("returns null");
            }

            if (!((String) result.get("status")).equals("0000")) {
                log_info(tag + " : " + "/info/balance : " + result.toString());
                throw new Exception("returns null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log_info(tag + " : " + "/info/balance : " + e.getMessage());
            throw new Exception("returns null");
        }

        return (JSONObject)result.get("data");
    }

    public JSONObject getCurrentPrice(String tag) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            result = api.callApi("GET", "/public/orderbook/" + getCurrentCoinType(), null);

            if (result == null) {
                log_info(tag + " : " + "/public/orderbook/" + getCurrentCoinType() + " : null");
                throw new Exception("returns null");
            }

            if (result.get("status") instanceof Long) {
                log_info(tag + " : " + "/public/orderbook/" + getCurrentCoinType() + " : " + result.toString());
                throw new Exception("returns null");
            }

            if (!((String) result.get("status")).equals("0000")) {
                // ex ) {"message":"Database Fail","status":"5400"}
                log_info(tag + " : " + "/public/orderbook/" + getCurrentCoinType() + " : " + result.toString());
                throw new Exception("returns null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log_info(tag + " : " + "/public/orderbook/" + getCurrentCoinType() + " : " + e.getMessage());
            throw new Exception("returns null");
        }

        return (JSONObject)result.get("data");
    }

    public JSONObject getTicker(String tag) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            log_info(tag + " : Calling /public/ticker API...");
            result = api.callApi("GET", "/public/ticker/" + getCurrentCoinType(), null);
            log_info(tag + " : Raw API response: " + (result != null ? result.toString() : "null"));

            if (result == null) {
                log_info(tag + " : " + "/public/ticker : null");
                throw new Exception("returns null");
            }

            if (result.get("status") instanceof Long) {
                log_info(tag + " : " + "/public/ticker : " + result.toString());
                throw new Exception("returns null");
            }

            String status = (String) result.get("status");
            log_info(tag + " : API status: " + status);
            
            if (!status.equals("0000")) {
                log_info(tag + " : " + "/public/ticker : " + result.toString());
                throw new Exception("returns null");
            }
            
            log_info(tag + " : Ticker API call successful");
        } catch (Exception e) {
            e.printStackTrace();
            log_info(tag + " : " + "/public/ticker : " + e.getMessage());
            throw new Exception("returns null");
        }

        return result;
    }

    public JSONArray getPlacedOrderList(String tag) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            HashMap param = new HashMap();
            param.put("count", "300");
            param.put("order_currency", getCurrentCoinType());

            result = api.callApi("POST", "/info/orders", param);

            if (result == null) {
                log_info(tag + " : " + "/info/orders : 1 : null");
                throw new Exception("returns null");
            }

            if (result.get("status") instanceof Long) {
                log_info(tag + " : " + "/info/orders : 2 : " + result.toString());
                throw new Exception("returns null");
            }

            if (((String) result.get("status")).equals("5600")) {
                if (((String) result.get("message")).equals("거래 진행중인 내역이 존재하지 않습니다.")) {
                    // workaround
                    throw new Exception("returns null");
                }
            }

            if (!((String) result.get("status")).equals("0000")) {
                log_info(tag + " : " + "/info/orders : 3 : " + result.toString());
                throw new Exception("returns null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log_info(tag + " : " + "/info/orders : 4 : " + e.getMessage());
            JSONArray jarr = new JSONArray();
            return jarr;
        }

        return (JSONArray)result.get("data");
    }

    public JSONArray getProcessedOrderList(String tag, int offset, String count) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            HashMap<String, String> rgParams = new HashMap<>();
            rgParams.put("offset", String.valueOf(offset));
            rgParams.put("count", count); // 1~50, default = 20
            rgParams.put("searchGb", "0"); // 0 = all, 1 = buy
            rgParams.put("order_currency", getCurrentCoinType());
            rgParams.put("payment_currency", "KRW");

            result = api.callApi("POST", "/info/user_transactions", rgParams);

            if (result == null) {
                log_info(tag + " : " + "/info/user_transactions : null");
                throw new Exception("returns null");
            }

            if (result.get("status") instanceof Long) {
                log_info(tag + " : " + "/info/user_transactions : " + result.toString());
                throw new Exception("returns null");
            }

            if (!((String) result.get("status")).equals("0000")) {
                log_info(tag + " : " + "/info/user_transactions : " + result.toString());
                throw new Exception("returns null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log_info(tag + " : " + "/info/user_transactions : " + e.getMessage());
            throw new Exception("returns null");
        }

        return (JSONArray) result.get("data");
    }

    public TradeDataManager.Type convertOrderType(String type) {
        switch(type) {
            case "bid" : return BUY;
            case "ask" : return SELL;
        }
        return NONE;
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
            Log.e("OrderManager", "에러 카드 전송 중 오류 발생", e);
        }
    }
}
