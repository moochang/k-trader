package com.example.k_trader.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * 코인 가격 정보를 저장하는 엔티티
 * 현재 가격과 등락률을 실시간으로 관찰하기 위한 테이블
 */
@Entity(tableName = "coin_price_info")
public class CoinPriceInfoEntity {
    @PrimaryKey
    private int id = 1; // 항상 하나의 레코드만 유지
    
    private String coinType;           // 코인 타입 (BTC, ETH 등)
    private String currentPrice;       // 현재 가격 (₩5,944,000)
    private String priceChange;        // 등락률 (+2.5%)
    private long lastUpdated;          // 마지막 업데이트 시간
    
    public CoinPriceInfoEntity() {
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public CoinPriceInfoEntity(String coinType, String currentPrice, String priceChange) {
        this.id = 1;
        this.coinType = coinType;
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCoinType() {
        return coinType;
    }
    
    public void setCoinType(String coinType) {
        this.coinType = coinType;
    }
    
    public String getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(String currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public String getPriceChange() {
        return priceChange;
    }
    
    public void setPriceChange(String priceChange) {
        this.priceChange = priceChange;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    @Override
    public String toString() {
        return "CoinPriceInfoEntity{" +
                "id=" + id +
                ", coinType='" + coinType + '\'' +
                ", currentPrice='" + currentPrice + '\'' +
                ", priceChange='" + priceChange + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}




