package com.example.k_trader.base;

public class GlobalSettings {
    public static int TRADE_INTERVAL_MIN_VALUE = 10;
    public static int TRADE_INTERVAL_DEFAULT_VALUE = 60;
    public static int UNIT_PRICE_MIN_VALUE = 10000;
    public static int UNIT_PRICE_DEFAULT_VALUE = 1*1000*1000;

    private String apiKey;
    private String apiSecret;
    private int tradeInterval = TRADE_INTERVAL_DEFAULT_VALUE;
    private int unitPrice = UNIT_PRICE_DEFAULT_VALUE;
    private boolean isFileLogEnabled = false;

    private static GlobalSettings gSettings = new GlobalSettings();
    private GlobalSettings(){}

    public static GlobalSettings getInstance() {
        return gSettings;
    }

    public String getApiKey() {
        return apiKey;
    }
    public String getApiSecret() {
        return apiSecret;
    }
    public int getTradeInterval() {
        return tradeInterval;
    }
    public int getUnitPrice() {
        return unitPrice;
    }
    public boolean isFileLogEnabled() { return isFileLogEnabled; }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
    public void setTradeInterval(int interval) {
        this.tradeInterval = interval;
    }
    public void setUnitPrice(int price) {
        this.unitPrice = price;
    }
    public void setFileLogEnabled(boolean enabled) { this.isFileLogEnabled = enabled; }

}
