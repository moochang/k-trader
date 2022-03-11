package com.example.k_trader;

import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.k_trader.base.OrderManager;
import com.example.k_trader.bitthumb.lib.Api_Client;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.util.HashMap;

public class OrderManagerTest {
    @Test
    public void addOrder_minValueCheck() throws Exception {
        OrderManager orderManager = new OrderManager();
//        Api_Client api = mock(Api_Client.class);
//        when(api.callApi(anyString(), (HashMap)anyMap())).thenReturn((JSONObject)new JSONParser().parse("{}"));

        JSONObject obj = orderManager.addOrder("저점", BUY, 0.0001, 10000000);;
        assertNotEquals(obj, null);
    }
}
