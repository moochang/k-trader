package com.example.k_trader_eth;

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
    int buyPrice;

    class DummyApiClient extends Api_Client {
        @Override
        public JSONObject callApi(String method, String endpoint, HashMap<String, String> params) {
            if (endpoint.equals("/info/balance")) { // getBalance
                JSONObject data = new JSONObject();
                data.put("total_krw", "0");
                //data.put("available_btc", "0.00011808");
                data.put("available_eth", "0.00011808");

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            //} else if (endpoint.equals("/public/orderbook/BTC")) { // getCurrentPrice
            } else if (endpoint.equals("/public/orderbook/ETH")) { // getCurrentPrice
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

        GlobalSettings.getInstance().setApiKey(null);
        Mockito.when(sharedPrefs.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)).thenReturn(1*1200*1000);
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_KEY_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_SECRET_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)).thenReturn(60);
        Mockito.when(sharedPrefs.getBoolean(GlobalSettings.FILE_LOG_ENABLED_KEY_NAME, false)).thenReturn(false);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE)).thenReturn(1.0f);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE)).thenReturn(0.5f);

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
                //data.put("available_btc", "0.00001808");
                data.put("available_eth", "0.00001808");

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            //} else if (endpoint.equals("/public/orderbook/BTC")) { // getCurrentPrice
            } else if (endpoint.equals("/public/orderbook/ETH")) { // getCurrentPrice
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

                JSONObject item2 = new JSONObject();
                item2.put("order_id", "0");
                item2.put("type", "bid");
                item2.put("units_remaining", "0.0123");
                item2.put("price", "50,750,000");
                item2.put("order_date", "1000");

                JSONArray data = new JSONArray();
                data.add(item);
                data.add(item2);

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

        GlobalSettings.getInstance().setApiKey(null);
        Mockito.when(sharedPrefs.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)).thenReturn(1*1200*1000);
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_KEY_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_SECRET_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)).thenReturn(60);
        Mockito.when(sharedPrefs.getBoolean(GlobalSettings.FILE_LOG_ENABLED_KEY_NAME, false)).thenReturn(false);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE)).thenReturn(1.0f);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE)).thenReturn(0.5f);

        isOrderAdded = false;

        TradeJobService job = new TradeJobService();
        job.setContext(context);
        job.setOrderManager(new OrderManager(new DummyTradeApiService2()));
        Method method = job.getClass().getDeclaredMethod("tradeBusinessLogic");
        method.setAccessible(true);
        method.invoke(job);

        assertEquals(false, isOrderAdded);
    }

    class DummyApiClient3 extends Api_Client {
        @Override
        public JSONObject callApi(String method, String endpoint, HashMap<String, String> params) {
            if (endpoint.equals("/info/balance")) { // getBalance
                JSONObject data = new JSONObject();
                data.put("total_krw", "0");
                //data.put("available_btc", "0.00001808");
                data.put("available_eth", "0.00001808");

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            //} else if (endpoint.equals("/public/orderbook/BTC")) { // getCurrentPrice
            } else if (endpoint.equals("/public/orderbook/ETH")) { // getCurrentPrice
                JSONObject price = new JSONObject();
                price.put("price", "49390000"); // currentPrice

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
                item.put("price", "49,600,000");
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
                String price = params.get("price");
                buyPrice = Integer.valueOf(price);
            }
            JSONObject obj = new JSONObject();
            obj.put("status", "0000");
            return obj;
        }
    }

    class DummyTradeApiService3 implements OrderManager.TradeApiService {
        @Override
        public Api_Client getApiService() {
            return new DummyApiClient3();
        }
    }

    // 현재 BTC가격이 첫 매도 slot의 가격과 근접한 경우에는 Buy order를 발행할 때 아래로 2 slot 이상을 확인할 필요가 있음
    // 1개 slot만 확인 할 경우 현재가 기준으로 매도 slot값이 이미 있음으로 판단해서 buy order 발행 안 됨
    @Test
    public void tradeBusinessLogic_issuingBuyOrderCheck() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        final Context context = Mockito.mock(Context.class);
        Mockito.when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);

        GlobalSettings.getInstance().setApiKey(null);
        Mockito.when(sharedPrefs.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)).thenReturn(1*1200*1000);
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_KEY_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_SECRET_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)).thenReturn(60);
        Mockito.when(sharedPrefs.getBoolean(GlobalSettings.FILE_LOG_ENABLED_KEY_NAME, false)).thenReturn(false);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE)).thenReturn(1.0f);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE)).thenReturn(0.5f);

        isOrderAdded = false;

        TradeJobService job = new TradeJobService();
        job.setContext(context);
        job.setOrderManager(new OrderManager(new DummyTradeApiService3()));
        Method method = job.getClass().getDeclaredMethod("tradeBusinessLogic");
        method.setAccessible(true);
        method.invoke(job);

        assertEquals(true, isOrderAdded);
        assertEquals(49000000, buyPrice);
    }

    class DummyApiClient4 extends Api_Client {
        @Override
        public JSONObject callApi(String method, String endpoint, HashMap<String, String> params) {
            if (endpoint.equals("/info/balance")) { // getBalance
                JSONObject data = new JSONObject();
                data.put("total_krw", "0");
                //data.put("available_btc", "0.00001808");
                data.put("available_eth", "0.00001808");

                JSONObject obj = new JSONObject();
                obj.put("status", "0000");
                obj.put("data", data);
                return obj;
            //} else if (endpoint.equals("/public/orderbook/BTC")) { // getCurrentPrice
            } else if (endpoint.equals("/public/orderbook/ETH")) { // getCurrentPrice
                JSONObject price = new JSONObject();
                price.put("price", "49390000"); // currentPrice

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
                item.put("price", "49,600,000");
                item.put("order_date", "1000");

                JSONObject item2 = new JSONObject();
                item2.put("order_id", "0");
                item2.put("type", "ask");
                item2.put("units_remaining", "0.0123");
                item2.put("price", "49,700,000");
                item2.put("order_date", "1000");

                JSONArray data = new JSONArray();
                data.add(item);
                data.add(item2);

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
                String price = params.get("price");
                buyPrice = Integer.valueOf(price);
            }
            JSONObject obj = new JSONObject();
            obj.put("status", "0000");
            return obj;
        }
    }

    class DummyTradeApiService4 implements OrderManager.TradeApiService {
        @Override
        public Api_Client getApiService() {
            return new DummyApiClient4();
        }
    }

    // slot interval이 default value(0.5%)가 아니더라도 정상 동작하는지 확인
    @Test
    public void tradeBusinessLogic_issuingBuyOrder_slotIntervalCheck() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        final Context context = Mockito.mock(Context.class);
        Mockito.when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);

        GlobalSettings.getInstance().setApiKey(null);
        Mockito.when(sharedPrefs.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)).thenReturn(1*1200*1000);
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_KEY_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getString(GlobalSettings.API_SECRET_KEY_NAME, "")).thenReturn("");
        Mockito.when(sharedPrefs.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)).thenReturn(60);
        Mockito.when(sharedPrefs.getBoolean(GlobalSettings.FILE_LOG_ENABLED_KEY_NAME, false)).thenReturn(false);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE)).thenReturn(1.0f);
        Mockito.when(sharedPrefs.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE)).thenReturn(0.25f);

        isOrderAdded = false;

        TradeJobService job = new TradeJobService();
        job.setContext(context);
        job.setOrderManager(new OrderManager(new DummyTradeApiService4()));
        Method method = job.getClass().getDeclaredMethod("tradeBusinessLogic");
        method.setAccessible(true);
        method.invoke(job);

        assertEquals(true, isOrderAdded);
        assertEquals(49100000, buyPrice);
    }
}
