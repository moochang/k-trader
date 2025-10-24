package com.example.k_trader.domain.model;

import android.util.Log;
import java.util.Date;
import java.util.Objects;

/**
 * Domain Models for K-Trader App
 * 
 * Clean Architecture의 Domain Layer에 속하는 순수한 비즈니스 로직 모델들
 * UI와 데이터 저장소에 독립적인 도메인 객체들
 * 
 * @author K-Trader Team
 * @version 1.0
 */
public class DomainModels {

    /**
     * 거래 정보를 나타내는 도메인 모델
     * 기존 TradeData를 Clean Architecture에 맞게 개선
     */
    public static class Trade {
        private String id;
        private Type type;
        private int price;
        private double units;
        private long processedTime;
        private Status status;
        private String coinType;
        private double profitRate;
        private String orderId;

        public enum Type {
            BUY(1, "매수"),
            SELL(2, "매도"),
            WITHDRAW_ONGOING(3, "출금중"),
            DEPOSIT(4, "입금"),
            WITHDRAW(5, "출금"),
            DEPOSIT_KRW(9, "KRW 입금중"),
            NONE(0, "없음");

            private final int code;
            private final String description;

            Type(int code, String description) {
                this.code = code;
                this.description = description;
            }

            public int getCode() { return code; }
            public String getDescription() { return description; }

            public static Type fromCode(int code) {
                for (Type type : values()) {
                    if (type.code == code) return type;
                }
                return NONE;
            }
        }

        public enum Status {
            PLACED("대기중"),
            PROCESSED("완료"),
            CANCELLED("취소됨"),
            FAILED("실패");

            private final String description;

            Status(String description) {
                this.description = description;
            }

            public String getDescription() { return description; }
        }

        // Constructors
        public Trade() {
            this.status = Status.PLACED;
            this.processedTime = System.currentTimeMillis();
        }

        public Trade(String id, Type type, int price, double units, String coinType) {
            this();
            this.id = id;
            this.type = type;
            this.price = price;
            this.units = units;
            this.coinType = coinType;
        }

        // Business Logic Methods
        public boolean isBuyOrder() {
            return type == Type.BUY;
        }

        public boolean isSellOrder() {
            return type == Type.SELL;
        }

        public boolean isCompleted() {
            return status == Status.PROCESSED;
        }

        public boolean isPending() {
            return status == Status.PLACED;
        }

        public double calculateTotalValue() {
            return price * units;
        }

        public double calculateProfit(Trade buyTrade) {
            if (!isSellOrder() || !buyTrade.isBuyOrder()) {
                return 0.0;
            }
            return (price - buyTrade.getPrice()) * units;
        }

        public double calculateProfitRate(Trade buyTrade) {
            if (!isSellOrder() || !buyTrade.isBuyOrder()) {
                return 0.0;
            }
            return ((double)(price - buyTrade.getPrice()) / buyTrade.getPrice()) * 100;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public Type getType() { return type; }
        public void setType(Type type) { this.type = type; }

        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }

        public double getUnits() { return units; }
        public void setUnits(double units) { this.units = units; }

        public long getProcessedTime() { return processedTime; }
        public void setProcessedTime(long processedTime) { this.processedTime = processedTime; }

        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }

        public String getCoinType() { return coinType; }
        public void setCoinType(String coinType) { this.coinType = coinType; }

        public double getProfitRate() { return profitRate; }
        public void setProfitRate(double profitRate) { this.profitRate = profitRate; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Trade trade = (Trade) o;
            return Objects.equals(id, trade.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return String.format("Trade{id='%s', type=%s, price=%d, units=%.8f, status=%s, coinType='%s'}", 
                id, type, price, units, status, coinType);
        }
    }

    /**
     * 코인 가격 정보를 나타내는 도메인 모델
     */
    public static class CoinPriceInfo {
        private String coinType;
        private int currentPrice;
        private double dailyChangeRate;
        private double hourlyChangeRate;
        private long lastUpdated;
        private int high24h;
        private int low24h;
        private double volume24h;

        public CoinPriceInfo() {
            this.lastUpdated = System.currentTimeMillis();
        }

        public CoinPriceInfo(String coinType, int currentPrice, double dailyChangeRate, double hourlyChangeRate) {
            this();
            this.coinType = coinType;
            this.currentPrice = currentPrice;
            this.dailyChangeRate = dailyChangeRate;
            this.hourlyChangeRate = hourlyChangeRate;
        }

        // Business Logic Methods
        public boolean isPriceIncreased() {
            return dailyChangeRate > 0;
        }

        public boolean isPriceDecreased() {
            return dailyChangeRate < 0;
        }

        public boolean isPriceStable() {
            return Math.abs(dailyChangeRate) < 0.1;
        }

        public String getFormattedPrice() {
            return String.format("₩%,d", currentPrice);
        }

        public String getFormattedDailyChange() {
            String sign = dailyChangeRate >= 0 ? "+" : "";
            return String.format("%s%.2f%%", sign, dailyChangeRate);
        }

        public String getFormattedHourlyChange() {
            String sign = hourlyChangeRate >= 0 ? "+" : "";
            return String.format("%s%.2f%%", sign, hourlyChangeRate);
        }

        public boolean isDataStale(long maxAgeMillis) {
            return (System.currentTimeMillis() - lastUpdated) > maxAgeMillis;
        }

        // Getters and Setters
        public String getCoinType() { return coinType; }
        public void setCoinType(String coinType) { this.coinType = coinType; }

        public int getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(int currentPrice) { this.currentPrice = currentPrice; }

        public double getDailyChangeRate() { return dailyChangeRate; }
        public void setDailyChangeRate(double dailyChangeRate) { this.dailyChangeRate = dailyChangeRate; }

        public double getHourlyChangeRate() { return hourlyChangeRate; }
        public void setHourlyChangeRate(double hourlyChangeRate) { this.hourlyChangeRate = hourlyChangeRate; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        public int getHigh24h() { return high24h; }
        public void setHigh24h(int high24h) { this.high24h = high24h; }

        public int getLow24h() { return low24h; }
        public void setLow24h(int low24h) { this.low24h = low24h; }

        public double getVolume24h() { return volume24h; }
        public void setVolume24h(double volume24h) { this.volume24h = volume24h; }

        @Override
        public String toString() {
            return String.format("CoinPriceInfo{coinType='%s', currentPrice=%d, dailyChange=%.2f%%, hourlyChange=%.2f%%}", 
                coinType, currentPrice, dailyChangeRate, hourlyChangeRate);
        }
    }

    /**
     * 잔고 정보를 나타내는 도메인 모델
     */
    public static class BalanceInfo {
        private String coinType;
        private double availableBalance;
        private double totalBalance;
        private double inUseBalance;
        private long lastUpdated;

        public BalanceInfo() {
            this.lastUpdated = System.currentTimeMillis();
        }

        public BalanceInfo(String coinType, double availableBalance, double totalBalance) {
            this();
            this.coinType = coinType;
            this.availableBalance = availableBalance;
            this.totalBalance = totalBalance;
            this.inUseBalance = totalBalance - availableBalance;
        }

        // Business Logic Methods
        public boolean hasAvailableBalance() {
            return availableBalance > 0.0001;
        }

        public boolean hasInsufficientBalance(double requiredAmount) {
            return availableBalance < requiredAmount;
        }

        public double getUsageRate() {
            if (totalBalance == 0) return 0.0;
            return (inUseBalance / totalBalance) * 100;
        }

        public String getFormattedAvailableBalance() {
            return String.format("%.8f %s", availableBalance, coinType);
        }

        public String getFormattedTotalBalance() {
            return String.format("%.8f %s", totalBalance, coinType);
        }

        public String getFormattedInUseBalance() {
            return String.format("%.8f %s", inUseBalance, coinType);
        }

        // Getters and Setters
        public String getCoinType() { return coinType; }
        public void setCoinType(String coinType) { this.coinType = coinType; }

        public double getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(double availableBalance) { 
            this.availableBalance = availableBalance;
            this.inUseBalance = totalBalance - availableBalance;
        }

        public double getTotalBalance() { return totalBalance; }
        public void setTotalBalance(double totalBalance) { 
            this.totalBalance = totalBalance;
            this.inUseBalance = totalBalance - availableBalance;
        }

        public double getInUseBalance() { return inUseBalance; }
        public void setInUseBalance(double inUseBalance) { this.inUseBalance = inUseBalance; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        @Override
        public String toString() {
            return String.format("BalanceInfo{coinType='%s', available=%.8f, total=%.8f, inUse=%.8f}", 
                coinType, availableBalance, totalBalance, inUseBalance);
        }
    }

    /**
     * 거래 통계 정보를 나타내는 도메인 모델
     */
    public static class TradingStatistics {
        private int totalTrades;
        private int successfulTrades;
        private int failedTrades;
        private double totalProfit;
        private double totalLoss;
        private double netProfit;
        private double winRate;
        private double averageProfit;
        private double averageLoss;
        private long periodStart;
        private long periodEnd;

        public TradingStatistics() {
            this.periodStart = System.currentTimeMillis();
            this.periodEnd = System.currentTimeMillis();
        }

        // Business Logic Methods
        public void addTrade(Trade trade) {
            totalTrades++;
            
            if (trade.isCompleted()) {
                successfulTrades++;
                if (trade.getProfitRate() > 0) {
                    totalProfit += trade.getProfitRate();
                } else {
                    totalLoss += Math.abs(trade.getProfitRate());
                }
            } else {
                failedTrades++;
            }
            
            updateCalculatedFields();
        }

        private void updateCalculatedFields() {
            netProfit = totalProfit - totalLoss;
            winRate = totalTrades > 0 ? (double) successfulTrades / totalTrades * 100 : 0.0;
            averageProfit = successfulTrades > 0 ? totalProfit / successfulTrades : 0.0;
            averageLoss = failedTrades > 0 ? totalLoss / failedTrades : 0.0;
        }

        public boolean isProfitable() {
            return netProfit > 0;
        }

        public String getFormattedNetProfit() {
            String sign = netProfit >= 0 ? "+" : "";
            return String.format("%s%.2f%%", sign, netProfit);
        }

        public String getFormattedWinRate() {
            return String.format("%.1f%%", winRate);
        }

        public String getFormattedAverageProfit() {
            return String.format("%.2f%%", averageProfit);
        }

        public String getFormattedAverageLoss() {
            return String.format("%.2f%%", averageLoss);
        }

        // Getters and Setters
        public int getTotalTrades() { return totalTrades; }
        public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }

        public int getSuccessfulTrades() { return successfulTrades; }
        public void setSuccessfulTrades(int successfulTrades) { this.successfulTrades = successfulTrades; }

        public int getFailedTrades() { return failedTrades; }
        public void setFailedTrades(int failedTrades) { this.failedTrades = failedTrades; }

        public double getTotalProfit() { return totalProfit; }
        public void setTotalProfit(double totalProfit) { this.totalProfit = totalProfit; }

        public double getTotalLoss() { return totalLoss; }
        public void setTotalLoss(double totalLoss) { this.totalLoss = totalLoss; }

        public double getNetProfit() { return netProfit; }
        public void setNetProfit(double netProfit) { this.netProfit = netProfit; }

        public double getWinRate() { return winRate; }
        public void setWinRate(double winRate) { this.winRate = winRate; }

        public double getAverageProfit() { return averageProfit; }
        public void setAverageProfit(double averageProfit) { this.averageProfit = averageProfit; }

        public double getAverageLoss() { return averageLoss; }
        public void setAverageLoss(double averageLoss) { this.averageLoss = averageLoss; }

        public long getPeriodStart() { return periodStart; }
        public void setPeriodStart(long periodStart) { this.periodStart = periodStart; }

        public long getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(long periodEnd) { this.periodEnd = periodEnd; }

        @Override
        public String toString() {
            return String.format("TradingStatistics{totalTrades=%d, successful=%d, failed=%d, netProfit=%.2f%%, winRate=%.1f%%}", 
                totalTrades, successfulTrades, failedTrades, netProfit, winRate);
        }
    }

    /**
     * 거래 설정을 나타내는 도메인 모델
     */
    public static class TradingSettings {
        private String apiKey;
        private String apiSecret;
        private int unitPrice;
        private int tradeInterval;
        private float earningRate;
        private float slotIntervalRate;
        private boolean fileLogEnabled;
        private String coinType;
        private boolean autoTradingEnabled;
        private int maxConcurrentOrders;
        private double minTradeAmount;
        private double maxTradeAmount;

        public TradingSettings() {
            this.coinType = "BTC";
            this.autoTradingEnabled = false;
            this.maxConcurrentOrders = 5;
            this.minTradeAmount = 0.0001;
            this.maxTradeAmount = 1.0;
        }

        // Business Logic Methods
        public boolean isValidConfiguration() {
            return apiKey != null && !apiKey.isEmpty() && 
                   apiSecret != null && !apiSecret.isEmpty() &&
                   unitPrice > 0 && tradeInterval > 0 &&
                   earningRate > 0 && slotIntervalRate > 0;
        }

        public boolean isAutoTradingReady() {
            return isValidConfiguration() && autoTradingEnabled;
        }

        public double calculateSlotPrice(int slotNumber) {
            return unitPrice * (1 + (slotNumber * slotIntervalRate / 100.0));
        }

        public double calculateRequiredAmount(int price) {
            return (double) unitPrice / price;
        }

        public boolean isTradeAmountValid(double amount) {
            return amount >= minTradeAmount && amount <= maxTradeAmount;
        }

        public String getMaskedApiKey() {
            if (apiKey == null || apiKey.length() < 8) return "****";
            return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
        }

        public String getMaskedApiSecret() {
            if (apiSecret == null || apiSecret.length() < 8) return "****";
            return apiSecret.substring(0, 4) + "****" + apiSecret.substring(apiSecret.length() - 4);
        }

        // Getters and Setters
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

        public int getUnitPrice() { return unitPrice; }
        public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }

        public int getTradeInterval() { return tradeInterval; }
        public void setTradeInterval(int tradeInterval) { this.tradeInterval = tradeInterval; }

        public float getEarningRate() { return earningRate; }
        public void setEarningRate(float earningRate) { this.earningRate = earningRate; }

        public float getSlotIntervalRate() { return slotIntervalRate; }
        public void setSlotIntervalRate(float slotIntervalRate) { this.slotIntervalRate = slotIntervalRate; }

        public boolean isFileLogEnabled() { return fileLogEnabled; }
        public void setFileLogEnabled(boolean fileLogEnabled) { this.fileLogEnabled = fileLogEnabled; }

        public String getCoinType() { return coinType; }
        public void setCoinType(String coinType) { this.coinType = coinType; }

        public boolean isAutoTradingEnabled() { return autoTradingEnabled; }
        public void setAutoTradingEnabled(boolean autoTradingEnabled) { this.autoTradingEnabled = autoTradingEnabled; }

        public int getMaxConcurrentOrders() { return maxConcurrentOrders; }
        public void setMaxConcurrentOrders(int maxConcurrentOrders) { this.maxConcurrentOrders = maxConcurrentOrders; }

        public double getMinTradeAmount() { return minTradeAmount; }
        public void setMinTradeAmount(double minTradeAmount) { this.minTradeAmount = minTradeAmount; }

        public double getMaxTradeAmount() { return maxTradeAmount; }
        public void setMaxTradeAmount(double maxTradeAmount) { this.maxTradeAmount = maxTradeAmount; }

        @Override
        public String toString() {
            return String.format("TradingSettings{coinType='%s', unitPrice=%d, tradeInterval=%d, earningRate=%.2f%%, autoTrading=%s}", 
                coinType, unitPrice, tradeInterval, earningRate, autoTradingEnabled);
        }
    }

    /**
     * 포트폴리오 정보를 나타내는 도메인 모델
     */
    public static class Portfolio {
        private double totalKrwBalance;
        private double totalCoinBalance;
        private double totalCoinValue;
        private double totalPortfolioValue;
        private double unrealizedProfit;
        private double unrealizedProfitRate;
        private int activeBuyOrders;
        private int activeSellOrders;
        private long lastUpdated;

        public Portfolio() {
            this.lastUpdated = System.currentTimeMillis();
        }

        // Business Logic Methods
        public void updatePortfolio(double krwBalance, double coinBalance, int currentPrice) {
            this.totalKrwBalance = krwBalance;
            this.totalCoinBalance = coinBalance;
            this.totalCoinValue = coinBalance * currentPrice;
            this.totalPortfolioValue = krwBalance + totalCoinValue;
            this.lastUpdated = System.currentTimeMillis();
        }

        public boolean isProfitable() {
            return unrealizedProfit > 0;
        }

        public String getFormattedTotalKrwBalance() {
            return String.format("₩%,.0f", totalKrwBalance);
        }

        public String getFormattedTotalCoinBalance() {
            return String.format("%.8f BTC", totalCoinBalance);
        }

        public String getFormattedTotalCoinValue() {
            return String.format("₩%,.0f", totalCoinValue);
        }

        public String getFormattedTotalPortfolioValue() {
            return String.format("₩%,.0f", totalPortfolioValue);
        }

        public String getFormattedUnrealizedProfit() {
            String sign = unrealizedProfit >= 0 ? "+" : "";
            return String.format("%s₩%,.0f", sign, unrealizedProfit);
        }

        public String getFormattedUnrealizedProfitRate() {
            String sign = unrealizedProfitRate >= 0 ? "+" : "";
            return String.format("%s%.2f%%", sign, unrealizedProfitRate);
        }

        // Getters and Setters
        public double getTotalKrwBalance() { return totalKrwBalance; }
        public void setTotalKrwBalance(double totalKrwBalance) { this.totalKrwBalance = totalKrwBalance; }

        public double getTotalCoinBalance() { return totalCoinBalance; }
        public void setTotalCoinBalance(double totalCoinBalance) { this.totalCoinBalance = totalCoinBalance; }

        public double getTotalCoinValue() { return totalCoinValue; }
        public void setTotalCoinValue(double totalCoinValue) { this.totalCoinValue = totalCoinValue; }

        public double getTotalPortfolioValue() { return totalPortfolioValue; }
        public void setTotalPortfolioValue(double totalPortfolioValue) { this.totalPortfolioValue = totalPortfolioValue; }

        public double getUnrealizedProfit() { return unrealizedProfit; }
        public void setUnrealizedProfit(double unrealizedProfit) { this.unrealizedProfit = unrealizedProfit; }

        public double getUnrealizedProfitRate() { return unrealizedProfitRate; }
        public void setUnrealizedProfitRate(double unrealizedProfitRate) { this.unrealizedProfitRate = unrealizedProfitRate; }

        public int getActiveBuyOrders() { return activeBuyOrders; }
        public void setActiveBuyOrders(int activeBuyOrders) { this.activeBuyOrders = activeBuyOrders; }

        public int getActiveSellOrders() { return activeSellOrders; }
        public void setActiveSellOrders(int activeSellOrders) { this.activeSellOrders = activeSellOrders; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        @Override
        public String toString() {
            return String.format("Portfolio{totalValue=₩%,.0f, unrealizedProfit=%s₩%,.0f (%.2f%%), activeOrders=%d/%d}", 
                totalPortfolioValue, unrealizedProfit >= 0 ? "+" : "", unrealizedProfit, unrealizedProfitRate, activeBuyOrders, activeSellOrders);
        }
    }
}

