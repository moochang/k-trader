package com.example.k_trader.api;

import com.example.k_trader.bitthumb.lib.Api_Client;
import com.example.k_trader.data.TransactionData;
import com.example.k_trader.base.GlobalSettings;
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
                // Ticker 정보 가져오기
                JSONObject tickerResponse = getTicker();
                if (tickerResponse == null) {
                    future.completeExceptionally(new RuntimeException(getCurrentCoinType() + " Ticker API 호출 실패"));
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
                // Ticker 정보 가져오기
                JSONObject tickerResponse = getTickerWithErrorInfo(result, "/info/ticker");
                
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
     * Ticker 정보 가져오기 (에러 정보 포함)
     */
    private JSONObject getTickerWithErrorInfo(TransactionApiResult result, String endpoint) {
        try {
            JSONObject response = apiClient.callApi("GET", endpoint, null);
            if (response != null && isSuccessResponse(response)) {
                return response;
            } else {
                String errorMessage = extractDetailedErrorMessage(response, endpoint);
                result.addError(endpoint, errorMessage);
                return null;
            }
        } catch (Exception e) {
            String errorMessage = String.format("{\"endpoint\":\"%s\",\"error\":\"Exception\",\"message\":\"%s\"}", 
                endpoint, e.getMessage());
            result.addError(endpoint, errorMessage);
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
                String errorMessage = extractDetailedErrorMessage(response, endpoint);
                result.addError(endpoint, errorMessage);
                return null;
            }
        } catch (Exception e) {
            String errorMessage = String.format("{\"endpoint\":\"%s\",\"error\":\"Exception\",\"message\":\"%s\"}", 
                endpoint, e.getMessage());
            result.addError(endpoint, errorMessage);
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
                String errorMessage = extractDetailedErrorMessage(response, endpoint);
                result.addError(endpoint, errorMessage);
                return null;
            }
        } catch (Exception e) {
            String errorMessage = String.format("{\"endpoint\":\"%s\",\"error\":\"Exception\",\"message\":\"%s\"}", 
                endpoint, e.getMessage());
            result.addError(endpoint, errorMessage);
            return null;
        }
    }
    
    /**
     * Ticker 정보 가져오기
     */
    private JSONObject getTicker() {
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
            String coinType = getCurrentCoinType();
            String coinPair = coinType + "_KRW";
            
            // 현재 가격 추출
            String currentPrice = "0";
            if (tickerResponse.containsKey("data")) {
                JSONObject data = (JSONObject) tickerResponse.get("data");
                if (data.containsKey(coinPair)) {
                    JSONObject coinKrw = (JSONObject) data.get(coinPair);
                    if (coinKrw.containsKey("closing_price")) {
                        currentPrice = coinKrw.get("closing_price").toString();
                    }
                }
            }
            
            // 전일 대비 등락률 추출 (CoinInfo용)
            String dailyChange = "+0.00%";
            // 1시간 전 대비 등락률 추출 (TransactionCard용)
            String hourlyChange = "+0.00%";
            
            if (tickerResponse.containsKey("data")) {
                JSONObject data = (JSONObject) tickerResponse.get("data");
                if (data.containsKey(coinPair)) {
                    JSONObject coinKrw = (JSONObject) data.get(coinPair);
                    
                    // API 응답의 모든 필드 로그 출력
                    android.util.Log.d("[K-TR]", "[TransactionApiService] Available fields in coinKrw: " + coinKrw.keySet());
                    
                    // 전일 대비 등락률 (24시간)
                    if (coinKrw.containsKey("fluctate_rate_24H")) {
                        String rawDailyChange = coinKrw.get("fluctate_rate_24H").toString();
                        android.util.Log.d("[K-TR]", "[TransactionApiService] Raw daily change (24H): " + rawDailyChange);
                        try {
                            double changeValue = Double.parseDouble(rawDailyChange);
                            if (changeValue >= 0) {
                                dailyChange = String.format("+%.2f%%", changeValue);
                            } else {
                                dailyChange = String.format("%.2f%%", changeValue);
                            }
                            android.util.Log.d("[K-TR]", "[TransactionApiService] Formatted daily change: " + dailyChange);
                        } catch (NumberFormatException e) {
                            android.util.Log.e("[K-TR]", "[TransactionApiService] Error parsing daily change: " + rawDailyChange, e);
                            dailyChange = "+0.00%";
                        }
                    } else {
                        android.util.Log.w("[K-TR]", "[TransactionApiService] fluctate_rate_24H not found in coinKrw");
                    }
                    
                    // 1시간 대비 등락률 (1시간)
                    if (coinKrw.containsKey("fluctate_rate_1H")) {
                        String rawHourlyChange = coinKrw.get("fluctate_rate_1H").toString();
                        android.util.Log.d("[K-TR]", "[TransactionApiService] Raw hourly change (1H): " + rawHourlyChange);
                        try {
                            double changeValue = Double.parseDouble(rawHourlyChange);
                            if (changeValue >= 0) {
                                hourlyChange = String.format("+%.2f%%", changeValue);
                            } else {
                                hourlyChange = String.format("%.2f%%", changeValue);
                            }
                            android.util.Log.d("[K-TR]", "[TransactionApiService] Formatted hourly change: " + hourlyChange);
                        } catch (NumberFormatException e) {
                            android.util.Log.e("[K-TR]", "[TransactionApiService] Error parsing hourly change: " + rawHourlyChange, e);
                            hourlyChange = "+0.00%";
                        }
                    } else {
                        android.util.Log.w("[K-TR]", "[TransactionApiService] fluctate_rate_1H not found, using daily change for hourly");
                        // 1시간 대비 등락률이 없으면 전일 대비 등락률을 사용
                        hourlyChange = dailyChange;
                    }
                    
                } else {
                    android.util.Log.w("[K-TR]", "[TransactionApiService] coinPair " + coinPair + " not found in data");
                }
            } else {
                android.util.Log.w("[K-TR]", "[TransactionApiService] data not found in tickerResponse");
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
                double currentPriceValue = Double.parseDouble(currentPrice);
                nextBuyPrice = String.valueOf((long)(currentPriceValue * 0.95));
            } catch (NumberFormatException e) {
                nextBuyPrice = "0";
            }
            
            return new TransactionData(
                String.valueOf(System.currentTimeMillis()),
                currentPrice,
                hourlyChange,
                dailyChange,
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
     * 네트워크 상태 확인
     */
    public boolean isNetworkAvailable() {
        // 실제 네트워크 상태 확인 로직 구현
        return true;
    }
    
    /**
     * 상세한 에러 메시지 추출 (JSON 형태로 포맷)
     */
    private String extractDetailedErrorMessage(JSONObject response, String endpoint) {
        if (response == null) {
            return String.format("{\"endpoint\":\"%s\",\"error\":\"No Response\",\"message\":\"서버 응답이 없습니다\",\"timestamp\":\"%d\"}", 
                endpoint, System.currentTimeMillis());
        }
        
        String status = response.get("status") != null ? response.get("status").toString() : "Unknown";
        String message = response.get("message") != null ? response.get("message").toString() : "No message";
        String fullResponse = response.toString();
        
        // JSON 형태로 상세 정보 포맷팅
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"server_url\": \"https://api.bithumb.com").append(endpoint).append("\",\n");
        jsonBuilder.append("  \"api_endpoint\": \"").append(endpoint).append("\",\n");
        jsonBuilder.append("  \"error_code\": \"").append(status).append("\",\n");
        jsonBuilder.append("  \"server_message\": \"").append(message.replace("\"", "\\\"")).append("\",\n");
        jsonBuilder.append("  \"full_response\": \"").append(fullResponse.replace("\"", "\\\"").replace("\n", "\\n")).append("\",\n");
        jsonBuilder.append("  \"timestamp\": \"").append(System.currentTimeMillis()).append("\"\n");
        jsonBuilder.append("}");
        
        return jsonBuilder.toString();
    }
}
