package com.example.k_trader.base;

public class GlobalSettings {
    public static String API_KEY_KEY_NAME = "API_KEY";
    public static String API_SECRET_KEY_NAME = "API_SECRET";
    public static String UNIT_PRICE_KEY_NAME = "UNIT_PRICE";
    public static String TRADE_INTERVAL_KEY_NAME = "TRADE_INTERVAL";
    public static String FILE_LOG_ENABLED_KEY_NAME = "FILE_LOG_ENABLED";
    public static String EARNING_RATE_KEY_NAME = "EARNING_RATE";
    public static String SLOT_INTERVAL_RATE_KEY_NAME = "SLOT_INTERVAL_RATE";

    public static int TRADE_INTERVAL_MIN_VALUE = 10;
    public static int TRADE_INTERVAL_DEFAULT_VALUE = 60;
    public static int UNIT_PRICE_MIN_VALUE = 10000;
    public static int UNIT_PRICE_DEFAULT_VALUE = 1*1000*1000;
    public static float EARNING_RATE_DEFAULT_VALUE = 1.0f; // Proportion to unit price
    public static float SLOT_INTERVAL_RATE_DEFAULT_VALUE = 0.5f; // Proportion to unit price

    private String apiKey;
    private String apiSecret;
    private int tradeInterval = TRADE_INTERVAL_DEFAULT_VALUE;
    private int unitPrice = UNIT_PRICE_DEFAULT_VALUE;
    private boolean isFileLogEnabled = false;
    private float earningRate = EARNING_RATE_DEFAULT_VALUE;
    private float slotIntervalRate = SLOT_INTERVAL_RATE_DEFAULT_VALUE;

    private static GlobalSettings gSettings = new GlobalSettings();
    private GlobalSettings() {}

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
    public float getEarningRate() {return earningRate;}
    public float getSlotIntervalRate() {return slotIntervalRate;}

    public GlobalSettings setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
    public GlobalSettings setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }
    public GlobalSettings setTradeInterval(int interval) {
        this.tradeInterval = interval;
        return this;
    }
    public GlobalSettings setUnitPrice(int price) {
        this.unitPrice = price;
        return this;
    }
    public GlobalSettings setFileLogEnabled(boolean enabled) {
        this.isFileLogEnabled = enabled;
        return this;
    }
    public GlobalSettings setEarningRate(float earningRate) {
        this.earningRate = earningRate;
        return this;
    }
    public GlobalSettings setSlotIntervalRate(float buyIntervalRate) {
        this.slotIntervalRate = buyIntervalRate;
        return this;
    }
}
