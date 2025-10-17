package com.example.k_trader;

import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static org.junit.Assert.assertNotEquals;

import com.example.k_trader.base.OrderManager;
import com.example.k_trader.bitthumb.lib.Api_Client;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.HashMap;

public class OrderManagerTest {
    class DummyApiClient extends Api_Client {
        @Override
        public JSONObject callApi(String method, String endpoint, HashMap<String, String> params) {
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

    // 최소 거래 단위인 0.0001에 대한 매도 주문이 정상 처리 되는지 확인한다.
    @Test
    public void addOrder_minValueCheck() throws Exception {
        OrderManager orderManager = new OrderManager(new DummyTradeApiService());
        JSONObject obj = orderManager.addOrder("저점", BUY, 0.0001, 10000000);;
        
        // 결과값이 null이 아니어야 정상
        assertNotEquals(null, obj);
    }
}
