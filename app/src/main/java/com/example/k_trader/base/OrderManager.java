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

        //rgParams.put("order_currency", "BTC");
        rgParams.put("order_currency", "ETH");
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

        //if (units < 0.0001) {
        if (units < 0.001) { // for eth
            String logMessage = tag + " : " + type.toString() + " 발행 취소 : " + String.format("%.4f", units) + " : " + "최소 수량 미달";
            log_info(logMessage);
            sendErrorCard("Validation Error", ERR_VALIDATION_001.getDescription());
            return null;
        }

        // 마지막 요청으로부터 15초 이내에 신규 요청이 온 경우에는 delay 시킨다.
        // {"message":"Please try again","status":"5600"} 에러 방지 목적
        while ((requestTime - lastRequestTimeInMillis) < safeIntervalInSec * 1000) {
            Intent intent = new Intent(MainActivity.BROADCAST_PROGRESS_MESSAGE);

//            Log.d("KTrader", "sending progress : " + String.valueOf((15 * 1000) - (requestTime - lastRequestTimeInMillis)));

            intent.putExtra("progress", (int)((safeIntervalInSec * 1000) - (requestTime - lastRequestTimeInMillis)));
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
        //rgParams.put("order_currency", "BTC");
        rgParams.put("order_currency", "ETH");
        rgParams.put("Payment_currency", "KRW");
        rgParams.put("units", String.format("%.4f", units));
        rgParams.put("price", String.valueOf(price));
        rgParams.put("payment_currency", "KRW");

        if (type == BUY)
            rgParams.put("type", "bid");
        else
            rgParams.put("type", "ask");

        log_info(tag + " : " + type.toString() + " 발행 : " + String.format("%.4f", units) + " : " + String.format(Locale.getDefault(), "%,d", price));

        try {
            result = api.callApi("POST", "/trade/place", rgParams);

            if (result == null) {
                log_info(tag + " : " + "/trade/place : null");
                return null;
            }

            if (result.get("status") instanceof Long) {
                String logMessage = tag + " : " + "/trade/place : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_005.getDescription());
                return null;
            }

            if (!((String) result.get("status")).equals("0000")) {
                String logMessage = tag + " : " + "/trade/place : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_005.getDescription());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String logMessage = tag + " : " + "/trade/place : " + e.getMessage();
            log_info(logMessage);
            sendErrorCard("API Error", ERR_API_005.getDescription());
            return null;
        }

        lastRequestTimeInMillis = Calendar.getInstance().getTimeInMillis();

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
        //rgParams.put("order_currency", "BTC");
        rgParams.put("order_currency", "ETH");
        rgParams.put("units", String.format("%.4f", units));
        rgParams.put("payment_currency", "KRW");

        log_info(tag + " : " + type.toString() + " 시장가 발행 : " + String.format("%.4f", units) + " : ");

        try {
            if (type == BUY)
                result = api.callApi("POST", "/trade/market_buy", rgParams);
            else
                result = api.callApi("POST", "/trade/market_sell", rgParams);

            if (result == null) {
                log_info(tag + " : " + "/trade/market_(buy/sell) : null");
                return null;
            }

            if (result.get("status") instanceof Long) {
                String logMessage = tag + " : " + "/trade/market_(buy/sell)1 : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_006.getDescription());
                return null;
            }

            // {"message":"잠시 후 이용해 주십시오.[9900]","status":"5600"}
            if (!((String) result.get("status")).equals("0000")) {
                String logMessage = tag + " : " + "/trade/market_(buy/sell)2 : " + result.toString();
                log_info(logMessage);
                sendErrorCard("API Error", ERR_API_006.getDescription());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            String logMessage = tag + " : " + "/trade/market_(buy/sell)3 : " + e.getMessage();
            log_info(logMessage);
            sendErrorCard("API Error", ERR_API_006.getDescription());
            return null;
        }

        lastRequestTimeInMillis = Calendar.getInstance().getTimeInMillis();

        return result;
    }

    public JSONObject getBalance(String tag) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            /*
            HashMap<String, String> requestParams = new HashMap<>();
            requestParams.put("currency", "---");
            // 필요한 다른 파라미터들도 추가할 수 있습니다.
            // requestParams.put("key", "value");
            // callApi 함수 호출
            JSONObject response = callApi("GET", "/some/endpoint", requestParams);
            */
            HashMap<String, String> requestParams = new HashMap<>();
            requestParams.put("currency", "ETH");

            //result = api.callApi("POST", "/info/balance", null);
            result = api.callApi("POST", "/info/balance",requestParams);

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
            //result = api.callApi("GET", "/public/orderbook/BTC", null);
            result = api.callApi("GET", "/public/orderbook/ETH", null);

            if (result == null) {
                //log_info(tag + " : " + "/public/orderbook/BTC : null");
                log_info(tag + " : " + "/public/orderbook/ETH : null");
                throw new Exception("returns null");
            }

            if (result.get("status") instanceof Long) {
                //log_info(tag + " : " + "/public/orderbook/BTC : " + result.toString());
                log_info(tag + " : " + "/public/orderbook/ETH : " + result.toString());
                throw new Exception("returns null");
            }

            if (!((String) result.get("status")).equals("0000")) {
                // ex ) {"message":"Database Fail","status":"5400"}
                //log_info(tag + " : " + "/public/orderbook/BTC : " + result.toString());
                log_info(tag + " : " + "/public/orderbook/ETH : " + result.toString());
                throw new Exception("returns null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //log_info(tag + " : " + "/public/orderbook/BTC : " + e.getMessage());
            log_info(tag + " : " + "/public/orderbook/ETH : " + e.getMessage());
            throw new Exception("returns null");
        }

        return (JSONObject)result.get("data");
    }

    public JSONArray getPlacedOrderList(String tag) throws Exception {
        Api_Client api = tradeApiService.getApiService();
        JSONObject result = null;

        try {
            HashMap param = new HashMap();
            param.put("count", "300");
            //param.put("order_currency", "BTC");
            param.put("order_currency", "ETH");

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
            //rgParams.put("order_currency", "BTC");
            rgParams.put("order_currency", "ETH");
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
