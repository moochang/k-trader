package com.example.k_trader.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.ColumnInfo;

/**
 * Transaction 정보를 저장하는 Entity
 */
@Entity(tableName = "transaction_info")
public class TransactionInfoEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "transaction_time")
    private String transactionTime;
    
    @ColumnInfo(name = "btc_current_price")
    private String btcCurrentPrice;
    
    @ColumnInfo(name = "hourly_change")
    private String hourlyChange;
    
    @ColumnInfo(name = "daily_change")
    private String dailyChange;
    
    @ColumnInfo(name = "estimated_balance")
    private String estimatedBalance;
    
    @ColumnInfo(name = "last_buy_price")
    private String lastBuyPrice;
    
    @ColumnInfo(name = "last_sell_price")
    private String lastSellPrice;
    
    @ColumnInfo(name = "next_buy_price")
    private String nextBuyPrice;
    
    @ColumnInfo(name = "is_from_server")
    private boolean isFromServer;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    public TransactionInfoEntity() {
        this.createdAt = System.currentTimeMillis();
    }
    
    public TransactionInfoEntity(String transactionTime, String btcCurrentPrice, String hourlyChange, 
                                String dailyChange, String estimatedBalance, String lastBuyPrice, 
                                String lastSellPrice, String nextBuyPrice, boolean isFromServer) {
        this.transactionTime = transactionTime;
        this.btcCurrentPrice = btcCurrentPrice;
        this.hourlyChange = hourlyChange;
        this.dailyChange = dailyChange;
        this.estimatedBalance = estimatedBalance;
        this.lastBuyPrice = lastBuyPrice;
        this.lastSellPrice = lastSellPrice;
        this.nextBuyPrice = nextBuyPrice;
        this.isFromServer = isFromServer;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getTransactionTime() {
        return transactionTime;
    }
    
    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }
    
    public String getBtcCurrentPrice() {
        return btcCurrentPrice;
    }
    
    public void setBtcCurrentPrice(String btcCurrentPrice) {
        this.btcCurrentPrice = btcCurrentPrice;
    }
    
    public String getHourlyChange() {
        return hourlyChange;
    }
    
    public void setHourlyChange(String hourlyChange) {
        this.hourlyChange = hourlyChange;
    }
    
    public String getDailyChange() {
        return dailyChange;
    }
    
    public void setDailyChange(String dailyChange) {
        this.dailyChange = dailyChange;
    }
    
    public String getEstimatedBalance() {
        return estimatedBalance;
    }
    
    public void setEstimatedBalance(String estimatedBalance) {
        this.estimatedBalance = estimatedBalance;
    }
    
    public String getLastBuyPrice() {
        return lastBuyPrice;
    }
    
    public void setLastBuyPrice(String lastBuyPrice) {
        this.lastBuyPrice = lastBuyPrice;
    }
    
    public String getLastSellPrice() {
        return lastSellPrice;
    }
    
    public void setLastSellPrice(String lastSellPrice) {
        this.lastSellPrice = lastSellPrice;
    }
    
    public String getNextBuyPrice() {
        return nextBuyPrice;
    }
    
    public void setNextBuyPrice(String nextBuyPrice) {
        this.nextBuyPrice = nextBuyPrice;
    }
    
    public boolean isFromServer() {
        return isFromServer;
    }
    
    public void setFromServer(boolean fromServer) {
        isFromServer = fromServer;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "TransactionInfoEntity{" +
                "id=" + id +
                ", transactionTime='" + transactionTime + '\'' +
                ", btcCurrentPrice='" + btcCurrentPrice + '\'' +
                ", hourlyChange='" + hourlyChange + '\'' +
                ", dailyChange='" + dailyChange + '\'' +
                ", estimatedBalance='" + estimatedBalance + '\'' +
                ", lastBuyPrice='" + lastBuyPrice + '\'' +
                ", lastSellPrice='" + lastSellPrice + '\'' +
                ", nextBuyPrice='" + nextBuyPrice + '\'' +
                ", isFromServer=" + isFromServer +
                ", createdAt=" + createdAt +
                '}';
    }
}
