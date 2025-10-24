package com.example.k_trader.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.List;

/**
 * 빗썸 API v1.2.0 응답을 위한 타입 안전한 모델 클래스들
 * Clean Architecture의 Data Layer에 해당
 */
public class BiThumbApiModels {

    /**
     * 공통 API 응답 구조
     */
    public static class ApiResponse<T> {
        @SerializedName("status")
        public String status;
        
        @SerializedName("data")
        public T data;
        
        @SerializedName("message")
        public String message;
        
        public boolean isSuccess() {
            return "0000".equals(status);
        }
    }

    /**
     * API 에러 응답
     */
    public static class ApiError {
        @SerializedName("status")
        public String status;
        
        @SerializedName("message")
        public String message;
        
        public String getErrorCode() {
            return status;
        }
        
        public String getErrorMessage() {
            return message;
        }
    }

    /**
     * Ticker API 응답 모델
     * GET /info/ticker/{order_currency}_{payment_currency}
     */
    public static class TickerResponse {
        @SerializedName("status")
        public String status;
        
        @SerializedName("data")
        public Map<String, TickerData> data;
        
        @SerializedName("message")
        public String message;
        
        public boolean isSuccess() {
            return "0000".equals(status);
        }
    }

    public static class TickerData {
        @SerializedName("opening_price")
        public String openingPrice;
        
        @SerializedName("closing_price")
        public String closingPrice;
        
        @SerializedName("min_price")
        public String minPrice;
        
        @SerializedName("max_price")
        public String maxPrice;
        
        @SerializedName("average_price")
        public String averagePrice;
        
        @SerializedName("units_traded")
        public String unitsTraded;
        
        @SerializedName("volume_1day")
        public String volume1Day;
        
        @SerializedName("volume_7day")
        public String volume7Day;
        
        @SerializedName("fluctate_24H")
        public String fluctate24H;
        
        @SerializedName("fluctate_rate_24H")
        public String fluctateRate24H;
        
        @SerializedName("fluctate_rate_1H")
        public String fluctateRate1H;
        
        @SerializedName("date")
        public String date;
    }

    /**
     * Balance API 응답 모델
     * GET /info/balance
     */
    public static class BalanceResponse {
        @SerializedName("status")
        public String status;
        
        @SerializedName("data")
        public BalanceData data;
        
        @SerializedName("message")
        public String message;
        
        public boolean isSuccess() {
            return "0000".equals(status);
        }
    }

    public static class BalanceData {
        @SerializedName("total_krw")
        public String totalKrw;
        
        @SerializedName("in_use_krw")
        public String inUseKrw;
        
        @SerializedName("available_krw")
        public String availableKrw;
        
        @SerializedName("total_btc")
        public String totalBtc;
        
        @SerializedName("in_use_btc")
        public String inUseBtc;
        
        @SerializedName("available_btc")
        public String availableBtc;
        
        @SerializedName("total_eth")
        public String totalEth;
        
        @SerializedName("in_use_eth")
        public String inUseEth;
        
        @SerializedName("available_eth")
        public String availableEth;
        
        @SerializedName("xcoin_last")
        public String xcoinLast;
    }

    /**
     * Orders API 응답 모델
     * GET /info/orders
     */
    public static class OrdersResponse {
        @SerializedName("status")
        public String status;
        
        @SerializedName("data")
        public OrdersData data;
        
        @SerializedName("message")
        public String message;
        
        public boolean isSuccess() {
            return "0000".equals(status);
        }
    }

    public static class OrdersData {
        @SerializedName("order_currency")
        public String orderCurrency;
        
        @SerializedName("payment_currency")
        public String paymentCurrency;
        
        @SerializedName("order_id")
        public String orderId;
        
        @SerializedName("order_date")
        public String orderDate;
        
        @SerializedName("type")
        public String type;
        
        @SerializedName("status")
        public String status;
        
        @SerializedName("order_price")
        public String orderPrice;
        
        @SerializedName("order_qty")
        public String orderQty;
        
        @SerializedName("cancel_date")
        public String cancelDate;
        
        @SerializedName("cancel_type")
        public String cancelType;
        
        @SerializedName("exec_qty")
        public String execQty;
        
        @SerializedName("exec_price")
        public String execPrice;
        
        @SerializedName("exec_amount")
        public String execAmount;
        
        @SerializedName("exec_fee")
        public String execFee;
        
        @SerializedName("exec_date")
        public String execDate;
    }

    /**
     * Candlestick API 응답 모델
     * GET /candlestick/{order_currency}_{payment_currency}/{chart_intervals}
     */
    public static class CandlestickResponse {
        @SerializedName("status")
        public String status;
        
        @SerializedName("data")
        public List<List<String>> data;
        
        @SerializedName("message")
        public String message;
        
        public boolean isSuccess() {
            return "0000".equals(status);
        }
    }

    /**
     * 캔들스틱 데이터 모델
     * data 배열의 각 요소: [timestamp, open, close, high, low, volume]
     */
    public static class CandlestickData {
        public long timestamp;
        public double open;
        public double close;
        public double high;
        public double low;
        public double volume;
        
        public CandlestickData(List<String> rawData) {
            if (rawData.size() >= 6) {
                this.timestamp = Long.parseLong(rawData.get(0));
                this.open = Double.parseDouble(rawData.get(1));
                this.close = Double.parseDouble(rawData.get(2));
                this.high = Double.parseDouble(rawData.get(3));
                this.low = Double.parseDouble(rawData.get(4));
                this.volume = Double.parseDouble(rawData.get(5));
            }
        }
    }

    /**
     * Place Order API 요청 모델
     * POST /trade/place
     */
    public static class PlaceOrderRequest {
        @SerializedName("order_currency")
        public String orderCurrency;
        
        @SerializedName("payment_currency")
        public String paymentCurrency;
        
        @SerializedName("units")
        public String units;
        
        @SerializedName("price")
        public String price;
        
        @SerializedName("type")
        public String type; // bid (매수) or ask (매도)
    }

    /**
     * Place Order API 응답 모델
     */
    public static class PlaceOrderResponse {
        @SerializedName("status")
        public String status;
        
        @SerializedName("data")
        public PlaceOrderData data;
        
        @SerializedName("message")
        public String message;
    }

    public static class PlaceOrderData {
        @SerializedName("order_id")
        public String orderId;
        
        @SerializedName("order_currency")
        public String orderCurrency;
        
        @SerializedName("payment_currency")
        public String paymentCurrency;
        
        @SerializedName("units")
        public String units;
        
        @SerializedName("price")
        public String price;
        
        @SerializedName("type")
        public String type;
        
        @SerializedName("status")
        public String status;
        
        @SerializedName("order_date")
        public String orderDate;
    }
}
