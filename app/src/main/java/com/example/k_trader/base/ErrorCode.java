package com.example.k_trader.base;

import android.support.annotation.NonNull;

/**
 * API 에러 코드를 정의하는 enum 클래스
 * Created by K-Trader on 2024-12-25.
 */
public enum ErrorCode {
    // API 에러 코드
    ERR_API_001("ERR_API_001", "거래 취소 실패", "/trade/cancel"),
    ERR_API_002("ERR_API_002", "주문 조회 실패", "/info/orders"),
    ERR_API_003("ERR_API_003", "잔고 조회 실패", "/info/balance"),
    ERR_API_004("ERR_API_004", "호가 조회 실패", "/public/orderbook/BTC"),
    ERR_API_005("ERR_API_005", "주문 발행 실패", "/trade/place"),
    ERR_API_006("ERR_API_006", "시장가 거래 실패", "/trade/market_buy/sell"),
    ERR_API_007("ERR_API_007", "주문 목록 조회 실패", "/info/orders"),
    ERR_API_008("ERR_API_008", "거래 내역 조회 실패", "/info/user_transactions"),
    
    // 비즈니스 로직 에러 코드
    ERR_BUSINESS_001("ERR_BUSINESS_001", "거래 비즈니스 로직 에러", "Trade Business Logic"),
    ERR_CARD_DATA_001("ERR_CARD_DATA_001", "카드 데이터 전송 에러", "Card Data Send"),
    
    // 검증 에러 코드
    ERR_VALIDATION_001("ERR_VALIDATION_001", "최소 수량 미달", "Validation Error"),
    
    // 기타 에러 코드
    ERR_UNKNOWN("ERR_UNKNOWN", "알 수 없는 에러", "Unknown");

    private final String code;
    private final String description;
    private final String apiEndpoint;

    ErrorCode(String code, String description, String apiEndpoint) {
        this.code = code;
        this.description = description;
        this.apiEndpoint = apiEndpoint;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    @Override
    @NonNull
    public String toString() {
        return code;
    }
}
