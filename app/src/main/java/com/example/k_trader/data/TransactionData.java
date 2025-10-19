package com.example.k_trader.data;

import com.example.k_trader.TransactionItemFragment;

import java.util.Date;

/**
 * Transaction 데이터 모델
 * 서버와 DB 간의 데이터 교환을 위한 표준 모델
 */
public class TransactionData {
    private String transactionTime;
    private String btcCurrentPrice;
    private String hourlyChange;
    private String estimatedBalance;
    private String lastBuyPrice;
    private String lastSellPrice;
    private String nextBuyPrice;
    private Date lastUpdated;
    private boolean isFromServer;

    public TransactionData() {
        this.lastUpdated = new Date();
        this.isFromServer = false;
    }

    public TransactionData(String transactionTime, String btcCurrentPrice, String hourlyChange,
                             String estimatedBalance, String lastBuyPrice, String lastSellPrice, String nextBuyPrice) {
        this();
        this.transactionTime = transactionTime;
        this.btcCurrentPrice = btcCurrentPrice;
        this.hourlyChange = hourlyChange;
        this.estimatedBalance = estimatedBalance;
        this.lastBuyPrice = lastBuyPrice;
        this.lastSellPrice = lastSellPrice;
        this.nextBuyPrice = nextBuyPrice;
    }

    // Getters and Setters
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

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isFromServer() {
        return isFromServer;
    }

    public void setFromServer(boolean fromServer) {
        isFromServer = fromServer;
    }

    /**
     * 데이터가 유효한지 확인
     */
    public boolean isValid() {
        return transactionTime != null && !transactionTime.isEmpty() &&
               btcCurrentPrice != null && !btcCurrentPrice.isEmpty();
    }

    /**
     * TransactionItemFragment의 TransactionCard로 변환
     */
    public TransactionItemFragment.CardAdapter.TransactionCard oTransactionCard() {
        return new TransactionItemFragment.CardAdapter.TransactionCard(
            transactionTime, btcCurrentPrice, hourlyChange, estimatedBalance,
            lastBuyPrice, lastSellPrice, nextBuyPrice
        );
    }
}
