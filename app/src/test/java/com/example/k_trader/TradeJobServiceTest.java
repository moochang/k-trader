package com.example.k_trader;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.OrderManager;
import com.example.k_trader.bitthumb.lib.Api_Client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class TradeJobServiceTest {
    boolean isOrderAdded;

    class DummyApiClient extends Api_Client {
        @Override
        public JSONObject callApi(String method, String endpoint, HashMap<String, String> params) {
            if (endpoint.equals("/info/balance")) { // getBalance
                JSONObject data = new JSONObject();
                data.put("total_krw", "0");
                data.put("available_btc", "0.00011808");

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/public/orderbook/BTC")) { // getCurrentPrice
                JSONObject price = new JSONObject();
                price.put("price", "51234567"); // currentPrice

                JSONArray bids = new JSONArray();
                bids.add(price);

                JSONObject data = new JSONObject();
                data.put("bids", bids);

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/info/orders")) { // getPlacedOrderList
                JSONArray data = new JSONArray();

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/info/user_transactions")) { // getProcessedOrderList
//                JSONObject item = new JSONObject();
//                price.put("search", "0");

                JSONArray data = new JSONArray();
//                data.add(item);

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/trade/place")) { // addOrder
                isOrderAdded = true;
            }
            JSONObject obj = new JSONObject();
            obj.put("status", "0000");
            return obj;
        }
    }

    class DummyTradeApiService implements OrderManager.TradeApiService {
        @Override
        public Api_Client getApiService() {
            return new DummyApiClient();
        }
    }

    // 이전 앱 실행에서 넘어온 BTC 잔고가 존재할 경우 이에 대응하는 매도가 정상 생성되는지 확인한다.
    @Test
    public void tradeBusinessLogic_carriedForwardBalanceCheck() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        final Context context = Mockito.mock(Context.class);
        Mockito.when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);

        Mockito.when(sharedPrefs.getInt("UNIT_PRICE", GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)).thenReturn(1*1200*1000);
        Mockito.when(sharedPrefs.getString("API_KEY", "")).thenReturn("");
        Mockito.when(sharedPrefs.getString("API_SECRET", "")).thenReturn("");
        Mockito.when(sharedPrefs.getInt("TRADE_INTERVAL", GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)).thenReturn(60);
        Mockito.when(sharedPrefs.getBoolean("FILE_LOG_ENABLED", false)).thenReturn(false);

        isOrderAdded = false;

        TradeJobService job = new TradeJobService();
        job.setContext(context);
        job.setOrderManager(new OrderManager(new DummyTradeApiService()));
        Method method = job.getClass().getDeclaredMethod("tradeBusinessLogic");
        method.setAccessible(true);
        method.invoke(job);

        assertEquals(true, isOrderAdded);
    }

    @Test
    public void checkGetFloorPrice()  throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        TradeJobService job = new TradeJobService();
        Method method = job.getClass().getDeclaredMethod("getFloorPrice", int.class);
        method.setAccessible(true);

        int result = (int)method.invoke(job, 50000000);
        assertEquals(50000000, result);
    }

    class DummyApiClient2 extends Api_Client {
        @Override
        public JSONObject callApi(String method, String endpoint, HashMap<String, String> params) {
            if (endpoint.equals("/info/balance")) { // getBalance
                JSONObject data = new JSONObject();
                data.put("total_krw", "0");
                data.put("available_btc", "0.00001808");

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/public/orderbook/BTC")) { // getCurrentPrice
                JSONObject price = new JSONObject();
                price.put("price", "51234567"); // currentPrice

                JSONArray bids = new JSONArray();
                bids.add(price);

                JSONObject data = new JSONObject();
                data.put("bids", bids);

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/info/orders")) { // getPlacedOrderList
                JSONObject item = new JSONObject();
                item.put("order_id", "0");
                item.put("type", "ask");
                item.put("units_remaining", "0.0123");
                item.put("price", "51,500,000");
                item.put("order_date", "1000");

                JSONArray data = new JSONArray();
                data.add(item);

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/info/user_transactions")) { // getProcessedOrderList
//                JSONObject item = new JSONObject();
//                price.put("search", "0");

                JSONArray data = new JSONArray();
//                data.add(item);

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            } else if (endpoint.equals("/trade/place")) { // addOrder
                isOrderAdded = true;
            }
            JSONObject obj = new JSONObject();
            obj.put("status", "0000");
            return obj;
        }
    }

    class DummyTradeApiService2 implements OrderManager.TradeApiService {
        @Override
        public Api_Client getApiService() {
            return new DummyApiClient2();
        }
    }

    // 여러 턴에 걸쳐 현재 가격이 변하지 않고 동일하게 유지 되는 경우에 무한 매수되면 안됨
    @Test
    public void tradeBusinessLogic_notUpdatingPriceCheck() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        final Context context = Mockito.mock(Context.class);
        Mockito.when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);

        Mockito.when(sharedPrefs.getInt("UNIT_PRICE", GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)).thenReturn(1*1200*1000);
        Mockito.when(sharedPrefs.getString("API_KEY", "")).thenReturn("");
        Mockito.when(sharedPrefs.getString("API_SECRET", "")).thenReturn("");
        Mockito.when(sharedPrefs.getInt("TRADE_INTERVAL", GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)).thenReturn(60);
        Mockito.when(sharedPrefs.getBoolean("FILE_LOG_ENABLED", false)).thenReturn(false);

        isOrderAdded = false;

        TradeJobService job = new TradeJobService();
        job.setContext(context);
        job.setOrderManager(new OrderManager(new DummyTradeApiService2()));
        Method method = job.getClass().getDeclaredMethod("tradeBusinessLogic");
        method.setAccessible(true);
        method.invoke(job);

        assertEquals(false, isOrderAdded);
    }
}
