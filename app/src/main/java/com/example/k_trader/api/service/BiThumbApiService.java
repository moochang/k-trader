package com.example.k_trader.api.service;

import com.example.k_trader.api.models.BiThumbApiModels.*;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.util.Map;
import java.util.HashMap;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import com.google.gson.Gson;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

/**
 * 빗썸 API v1.2.0 서비스
 * Clean Architecture의 Data Layer에 해당
 * SRP 원칙에 따라 API 호출과 데이터 파싱만 담당
 */
public class BiThumbApiService {
    
    private static final String BASE_URL = "https://api.bithumb.com";
    private static final String PUBLIC_API_BASE = BASE_URL + "/public";
    private static final String PRIVATE_API_BASE = BASE_URL + "/info";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final String apiSecret;
    
    public BiThumbApiService(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.gson = new Gson();
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new ApiLoggingInterceptor())
                .addInterceptor(new ApiErrorInterceptor())
                .build();
    }
    
    /**
     * Ticker 정보 조회
     * GET /public/ticker/{order_currency}_{payment_currency}
     */
    public Single<TickerResponse> getTicker(String coinPair) {
        return Single.fromCallable(() -> {
            String url = PUBLIC_API_BASE + "/ticker/" + coinPair;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("KTrader", "[BithumbApiService] Ticker response: " + responseBody);
                    return gson.fromJson(responseBody, TickerResponse.class);
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .doOnError(error -> Log.e("KTrader", "[BithumbApiService] Error getting ticker for " + coinPair, error));
    }
    
    /**
     * 잔고 정보 조회
     * GET /info/balance
     */
    public Single<BalanceResponse> getBalance() {
        return Single.fromCallable(() -> {
            String url = PRIVATE_API_BASE + "/balance";
            Map<String, String> params = new HashMap<>();
            params.put("currency", "ALL");
            
            Request request = createAuthenticatedRequest(url, params);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("KTrader", "[BithumbApiService] Balance response: " + responseBody);
                    return gson.fromJson(responseBody, BalanceResponse.class);
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .doOnError(error -> Log.e("KTrader", "[BithumbApiService] Error getting balance", error));
    }
    
    /**
     * 주문 내역 조회
     * POST /info/orders
     */
    public Single<OrdersResponse> getOrders(String coinType) {
        return Single.fromCallable(() -> {
            String url = PRIVATE_API_BASE + "/orders";
            Map<String, String> params = new HashMap<>();
            params.put("order_currency", coinType);
            params.put("count", "300");
            
            Request request = createAuthenticatedRequest(url, params);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("KTrader", "[BithumbApiService] Orders response: " + responseBody);
                    
                    // JSON 파싱하여 상태 코드 확인
                    JSONObject jsonResponse = new JSONObject();
                    try {
                        jsonResponse = (JSONObject) new org.json.simple.parser.JSONParser().parse(responseBody);
                    } catch (Exception e) {
                        Log.e("KTrader", "[BithumbApiService] JSON parsing error", e);
                        throw new IOException("JSON parsing error: " + e.getMessage());
                    }
                    
                    String status = String.valueOf(jsonResponse.get("status"));
                    
                    // 기존 코드와 동일한 에러 처리
                    if (status.equals("5600")) {
                        String message = String.valueOf(jsonResponse.get("message"));
                        if (message.equals("거래 진행중인 내역이 존재하지 않습니다.")) {
                            Log.d("KTrader", "[BithumbApiService] No pending orders found");
                            // 빈 데이터로 응답 생성
                            OrdersResponse emptyResponse = new OrdersResponse();
                            emptyResponse.status = "0000";
                            emptyResponse.data = null; // 빈 데이터
                            return emptyResponse;
                        }
                    }
                    
                    if (!status.equals("0000")) {
                        Log.e("KTrader", "[BithumbApiService] Orders API error: " + responseBody);
                        throw new IOException("API Error: " + status + " - " + jsonResponse.get("message"));
                    }
                    
                    return gson.fromJson(responseBody, OrdersResponse.class);
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .doOnError(error -> Log.e("KTrader", "[BithumbApiService] Error getting orders for " + coinType, error));
    }
    
    /**
     * 캔들스틱 데이터 조회
     * GET /public/candlestick/{order_currency}_{payment_currency}/{chart_intervals}
     */
    public Single<CandlestickResponse> getCandlesticks(String coinPair, String interval, int limit) {
        return Single.fromCallable(() -> {
            String url = PUBLIC_API_BASE + "/candlestick/" + coinPair + "/" + interval;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("KTrader", "[BithumbApiService] Candlestick response: " + responseBody);
                    return gson.fromJson(responseBody, CandlestickResponse.class);
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .doOnError(error -> Log.e("KTrader", "[BithumbApiService] Error getting candlesticks for " + coinPair + " " + interval, error));
    }
    
    /**
     * 주문 등록
     * POST /trade/place
     */
    public Single<PlaceOrderResponse> placeOrder(String coinPair, String type, String units, String price) {
        return Single.fromCallable(() -> {
            String url = BASE_URL + "/trade/place";
            Map<String, String> params = new HashMap<>();
            params.put("order_currency", coinPair.split("_")[0]);
            params.put("payment_currency", coinPair.split("_")[1]);
            params.put("units", units);
            params.put("price", price);
            params.put("type", type); // bid (매수) or ask (매도)
            
            Request request = createAuthenticatedRequest(url, params);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("KTrader", "[BithumbApiService] Place order response: " + responseBody);
                    return gson.fromJson(responseBody, PlaceOrderResponse.class);
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .doOnError(error -> Log.e("KTrader", "[BithumbApiService] Error placing order", error));
    }
    
    /**
     * 주문 취소
     * POST /trade/cancel
     */
    public Single<String> cancelOrder(String orderId, String coinPair) {
        return Single.fromCallable(() -> {
            String url = BASE_URL + "/trade/cancel";
            Map<String, String> params = new HashMap<>();
            params.put("order_id", orderId);
            params.put("order_currency", coinPair.split("_")[0]);
            params.put("payment_currency", coinPair.split("_")[1]);
            
            Request request = createAuthenticatedRequest(url, params);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d("KTrader", "[BithumbApiService] Cancel order response: " + responseBody);
                    return responseBody;
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .doOnError(error -> Log.e("KTrader", "[BithumbApiService] Error canceling order", error));
    }
    
    /**
     * 인증이 필요한 요청 생성 (빗썸 API v1.2.0 방식)
     */
    private Request createAuthenticatedRequest(String url, Map<String, String> params) {
        // 빗썸 API 인증 로직 구현
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (postData.length() > 0) {
                postData.append("&");
            }
            postData.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        // Nonce 생성 (현재 시간 밀리초)
        String nonce = String.valueOf(System.currentTimeMillis());
        
        // 서명을 위한 데이터 생성: endpoint + ";" + postData + ";" + nonce
        String endpoint = url.substring(url.lastIndexOf("/") + 1);
        String signData = endpoint + ";" + postData.toString() + ";" + nonce;
        
        RequestBody body = RequestBody.create(
                MediaType.parse("application/x-www-form-urlencoded"),
                postData.toString()
        );
        
        return new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Api-Key", apiKey)
                .addHeader("Api-Sign", generateApiSign(signData))
                .addHeader("Api-Nonce", nonce)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
    }
    
    /**
     * API 서명 생성 (빗썸 API v1.2.0 방식)
     * HMAC-SHA512를 사용하여 서명 생성
     */
    private String generateApiSign(String data) {
        try {
            // API Secret을 Base64 디코딩
            byte[] secretBytes = Base64.decode(apiSecret, Base64.DEFAULT);
            
            // HMAC-SHA512로 서명 생성
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA512");
            mac.init(secretKeySpec);
            
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Base64로 인코딩하여 반환
            return Base64.encodeToString(signatureBytes, Base64.NO_WRAP);
            
        } catch (Exception e) {
            Log.e("KTrader", "[BithumbApiService] Error generating API signature", e);
            return "error_signature";
        }
    }
    
    /**
     * API 로깅 인터셉터
     */
    private static class ApiLoggingInterceptor implements okhttp3.Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            
            Log.d("KTrader", "[BithumbApiService] API Request: " + request.method() + " " + request.url());
            
            Response response = chain.proceed(request);
            long endTime = System.currentTimeMillis();
            
            Log.d("KTrader", "[BithumbApiService] API Response: " + response.code() + " (" + (endTime - startTime) + "ms)");
            
            return response;
        }
    }
    
    /**
     * API 에러 인터셉터
     */
    private static class ApiErrorInterceptor implements okhttp3.Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            
            if (!response.isSuccessful()) {
                Log.e("KTrader", "[BithumbApiService] API Error: " + response.code() + " " + response.message());
            }
            
            return response;
        }
    }
}
