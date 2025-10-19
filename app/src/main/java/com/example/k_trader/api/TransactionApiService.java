package com.example.k_trader.api;

import com.example.k_trader.bitthumb.lib.Api_Client;
import com.example.k_trader.data.TransactionData;
import org.json.simple.JSONObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Transaction API 서비스
 * SRP: API 호출과 데이터 변환만 담당
 */
public class TransactionApiService {
    
    private static volatile TransactionApiService INSTANCE;
    private final Api_Client apiClient;
    private final ExecutorService executorService;
    
    private TransactionApiService() {
        this.apiClient = new Api_Client();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    public static TransactionApiService getInstance() {
        if (INSTANCE == null) {
            synchronized (TransactionApiService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TransactionApiService();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Transaction 데이터를 서버에서 가져오기
     */
    public CompletableFuture<TransactionData> fetchTransactionData() {
        CompletableFuture<TransactionData> future = new CompletableFuture<>();
        
        executorService.execute(() -> {
            try {
                // BTC Ticker 정보 가져오기
                JSONObject tickerResponse = getBtcTicker();
                if (tickerResponse == null) {
                    future.completeExceptionally(new RuntimeException("BTC Ticker API 호출 실패"));
                    return;
                }
                
                // Balance 정보 가져오기
                JSONObject balanceResponse = getBalance();
                if (balanceResponse == null) {
                    future.completeExceptionally(new RuntimeException("Balance API 호출 실패"));
                    return;
                }
                
                // Recent Orders 정보 가져오기
                JSONObject orderResponse = getRecentOrders();
                if (orderResponse == null) {
                    future.completeExceptionally(new RuntimeException("Recent Orders API 호출 실패"));
                    return;
                }
                
                // TransactionData 생성
                TransactionData transactionData = parseTransactionData(tickerResponse, balanceResponse, orderResponse);
                future.complete(transactionData);
                
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * API 호출 실패 시 상세 에러 정보와 함께 Transaction 데이터 가져오기
     */
    public CompletableFuture<TransactionApiResult> fetchTransactionDataWithErrorInfo() {
        CompletableFuture<TransactionApiResult> future = new CompletableFuture<>();
        
        executorService.execute(() -> {
            TransactionApiResult result = new TransactionApiResult();
            
            try {
                // BTC Ticker 정보 가져오기
                JSONObject tickerResponse = getBtcTickerWithErrorInfo(result, "/info/ticker");
                
                // Balance 정보 가져오기
                JSONObject balanceResponse = getBalanceWithErrorInfo(result, "/info/balance");
                
                // Recent Orders 정보 가져오기
                JSONObject orderResponse = getRecentOrdersWithErrorInfo(result, "/info/orders");
                
                // 모든 API가 성공한 경우에만 TransactionData 생성
                if (tickerResponse != null && balanceResponse != null && orderResponse != null) {
                    TransactionData transactionData = parseTransactionData(tickerResponse, balanceResponse, orderResponse);
                    result.setTransactionData(transactionData);
                }
                
                future.complete(result);
                
            } catch (Exception e) {
                result.addError("General", "API 호출 중 예외 발생: " + e.getMessage());
                future.complete(result);
            }
        });
        
        return future;
    }
    
    /**
     * BTC Ticker 정보 가져오기 (에러 정보 포함)
     */
    private JSONObject getBtcTickerWithErrorInfo(TransactionApiResult result, String endpoint) {
        try {
            JSONObject response = apiClient.callApi("GET", endpoint, null);
            if (response != null && isSuccessResponse(response)) {
                return response;
            } else {
                String errorMessage = extractErrorMessage(response);
                result.addError(endpoint, errorMessage);
                return null;
            }
        } catch (Exception e) {
            result.addError(endpoint, "Exception: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Balance 정보 가져오기 (에러 정보 포함)
     */
    private JSONObject getBalanceWithErrorInfo(TransactionApiResult result, String endpoint) {
        try {
            JSONObject response = apiClient.callApi("GET", endpoint, null);
            if (response != null && isSuccessResponse(response)) {
                return response;
            } else {
                String errorMessage = extractErrorMessage(response);
                result.addError(endpoint, errorMessage);
                return null;
            }
        } catch (Exception e) {
            result.addError(endpoint, "Exception: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Recent Orders 정보 가져오기 (에러 정보 포함)
     */
    private JSONObject getRecentOrdersWithErrorInfo(TransactionApiResult result, String endpoint) {
        try {
            JSONObject response = apiClient.callApi("GET", endpoint, null);
            if (response != null && isSuccessResponse(response)) {
                return response;
            } else {
                String errorMessage = extractErrorMessage(response);
                result.addError(endpoint, errorMessage);
                return null;
            }
        } catch (Exception e) {
            result.addError(endpoint, "Exception: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * BTC Ticker 정보 가져오기
     */
    private JSONObject getBtcTicker() {
        try {
            return apiClient.callApi("GET", "/info/ticker", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Balance 정보 가져오기
     */
    private JSONObject getBalance() {
        try {
            return apiClient.callApi("GET", "/info/balance", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Recent Orders 정보 가져오기
     */
    private JSONObject getRecentOrders() {
        try {
            return apiClient.callApi("GET", "/info/orders", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * API 응답이 성공인지 확인
     */
    private boolean isSuccessResponse(JSONObject response) {
        if (response == null) return false;
        
        // status 필드가 있는 경우
        if (response.containsKey("status")) {
            String status = response.get("status").toString();
            return "0000".equals(status) || "success".equals(status.toLowerCase());
        }
        
        // data 필드가 있는 경우
        if (response.containsKey("data")) {
            return response.get("data") != null;
        }
        
        // 기본적으로 응답이 있으면 성공으로 간주
        return true;
    }
    
    /**
     * API 응답에서 에러 메시지 추출
     */
    private String extractErrorMessage(JSONObject response) {
        if (response == null) {
            return "API 응답이 null입니다.";
        }
        
        // status와 message가 있는 경우
        if (response.containsKey("status") && response.containsKey("message")) {
            String status = response.get("status").toString();
            String message = response.get("message").toString();
            return String.format("Status: %s, Message: %s", status, message);
        }
        
        // message만 있는 경우
        if (response.containsKey("message")) {
            return "Message: " + response.get("message").toString();
        }
        
        // status만 있는 경우
        if (response.containsKey("status")) {
            return "Status: " + response.get("status").toString();
        }
        
        // 전체 응답을 문자열로 반환
        return "Response: " + response.toString();
    }
    
    /**
     * API 응답들을 TransactionData로 변환
     */
    private TransactionData parseTransactionData(JSONObject tickerResponse, 
                                               JSONObject balanceResponse, 
                                               JSONObject orderResponse) {
        try {
            // BTC 현재 가격 추출
            String btcCurrentPrice = "0";
            if (tickerResponse.containsKey("data")) {
                JSONObject data = (JSONObject) tickerResponse.get("data");
                if (data.containsKey("BTC_KRW")) {
                    JSONObject btcKrw = (JSONObject) data.get("BTC_KRW");
                    if (btcKrw.containsKey("closing_price")) {
                        btcCurrentPrice = btcKrw.get("closing_price").toString();
                    }
                }
            }
            
            // 시간당 변화율 추출
            String hourlyChange = "0";
            if (tickerResponse.containsKey("data")) {
                JSONObject data = (JSONObject) tickerResponse.get("data");
                if (data.containsKey("BTC_KRW")) {
                    JSONObject btcKrw = (JSONObject) data.get("BTC_KRW");
                    if (btcKrw.containsKey("fluctate_rate_24H")) {
                        hourlyChange = btcKrw.get("fluctate_rate_24H").toString();
                    }
                }
            }
            
            // 예상 잔고 추출
            String estimatedBalance = "0";
            if (balanceResponse.containsKey("data")) {
                JSONObject data = (JSONObject) balanceResponse.get("data");
                if (data.containsKey("total_krw")) {
                    estimatedBalance = data.get("total_krw").toString();
                }
            }
            
            // 마지막 매수/매도 가격 추출
            String lastBuyPrice = "0";
            String lastSellPrice = "0";
            String nextBuyPrice = "0";
            
            if (orderResponse.containsKey("data")) {
                org.json.simple.JSONArray orders = (org.json.simple.JSONArray) orderResponse.get("data");
                if (orders != null && orders.size() > 0) {
                    // 최근 주문에서 가격 정보 추출
                    JSONObject recentOrder = (JSONObject) orders.get(0);
                    if (recentOrder.containsKey("price")) {
                        String price = recentOrder.get("price").toString();
                        String side = recentOrder.get("side").toString();
                        
                        if ("bid".equals(side)) {
                            lastBuyPrice = price;
                        } else if ("ask".equals(side)) {
                            lastSellPrice = price;
                        }
                    }
                }
            }
            
            // 다음 매수 가격 계산 (현재 가격의 95%)
            try {
                double currentPrice = Double.parseDouble(btcCurrentPrice);
                nextBuyPrice = String.valueOf((long)(currentPrice * 0.95));
            } catch (NumberFormatException e) {
                nextBuyPrice = "0";
            }
            
            return new TransactionData(
                String.valueOf(System.currentTimeMillis()),
                btcCurrentPrice,
                hourlyChange,
                estimatedBalance,
                lastBuyPrice,
                lastSellPrice,
                nextBuyPrice
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 네트워크 상태 확인
     */
    public boolean isNetworkAvailable() {
        // 실제 네트워크 상태 확인 로직 구현
        return true;
    }
}
