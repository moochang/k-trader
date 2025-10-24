package com.example.k_trader.domain.repository;

import android.util.Log;
import com.example.k_trader.domain.model.DomainModels.*;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;

/**
 * Repository Interfaces for K-Trader App
 * 
 * Clean Architecture의 Domain Layer에 속하는 데이터 접근 추상화 인터페이스들
 * Dependency Inversion Principle을 적용하여 데이터 소스에 독립적인 인터페이스 제공
 * RxJava를 활용한 반응형 프로그래밍 지원
 * 
 * @author K-Trader Team
 * @version 1.0
 */
public class RepositoryInterfaces {

    /**
     * 거래 데이터 Repository 인터페이스
     * 기존 TradeDataManager의 기능을 Clean Architecture에 맞게 개선
     */
    public interface TradeRepository {
        
        // 거래 데이터 조회
        Single<List<Trade>> getAllTrades();
        Single<List<Trade>> getTradesByType(Trade.Type type);
        Single<List<Trade>> getTradesByStatus(Trade.Status status);
        Single<List<Trade>> getTradesByCoinType(String coinType);
        Single<Trade> getTradeById(String tradeId);
        Single<List<Trade>> getTradesByTimeRange(long startTime, long endTime);
        
        // 실시간 관찰
        Observable<List<Trade>> observeAllTrades();
        Observable<List<Trade>> observeTradesByType(Trade.Type type);
        Observable<List<Trade>> observeTradesByStatus(Trade.Status status);
        Observable<List<Trade>> observeTradesByCoinType(String coinType);
        
        // 거래 데이터 저장/수정/삭제
        Completable saveTrade(Trade trade);
        Completable saveTrades(List<Trade> trades);
        Completable updateTrade(Trade trade);
        Completable deleteTrade(String tradeId);
        Completable deleteTradesByType(Trade.Type type);
        Completable clearAllTrades();
        
        // 비즈니스 로직 관련 메서드
        Single<List<Trade>> getActiveOrders();
        Single<List<Trade>> getCompletedTrades();
        Single<List<Trade>> getPendingTrades();
        Single<Trade> getLatestTradeByType(Trade.Type type);
        Single<Integer> getActiveOrderCount(Trade.Type type);
        Single<Double> calculateTotalProfit();
        Single<Double> calculateTotalProfitByCoinType(String coinType);
        
        // 통계 관련 메서드
        Single<TradingStatistics> getTradingStatistics(long startTime, long endTime);
        Single<TradingStatistics> getTradingStatisticsByCoinType(String coinType, long startTime, long endTime);
    }

    /**
     * 코인 가격 정보 Repository 인터페이스
     */
    public interface CoinPriceRepository {
        
        // 가격 정보 조회
        Single<CoinPriceInfo> getCurrentPrice(String coinType);
        Single<List<CoinPriceInfo>> getAllCurrentPrices();
        Single<List<CoinPriceInfo>> getPriceHistory(String coinType, long startTime, long endTime);
        Single<CoinPriceInfo> getLatestPriceByCoinType(String coinType);
        
        // 실시간 관찰
        Observable<CoinPriceInfo> observeCurrentPrice(String coinType);
        Observable<List<CoinPriceInfo>> observeAllCurrentPrices();
        Observable<CoinPriceInfo> observePriceChanges(String coinType);
        
        // 가격 정보 저장/업데이트
        Completable savePriceInfo(CoinPriceInfo priceInfo);
        Completable savePriceInfos(List<CoinPriceInfo> priceInfos);
        Completable updatePriceInfo(CoinPriceInfo priceInfo);
        Completable deletePriceInfo(String coinType);
        Completable clearPriceHistory();
        
        // 비즈니스 로직 관련 메서드
        Single<Boolean> isPriceDataStale(String coinType, long maxAgeMillis);
        Single<List<CoinPriceInfo>> getPriceChangesInPeriod(String coinType, long periodMillis);
        Single<Double> calculatePriceVolatility(String coinType, long periodMillis);
        Single<Boolean> isPriceIncreasing(String coinType, int timeWindowMinutes);
        Single<Boolean> isPriceDecreasing(String coinType, int timeWindowMinutes);
    }

    /**
     * 잔고 정보 Repository 인터페이스
     */
    public interface BalanceRepository {
        
        // 잔고 정보 조회
        Single<BalanceInfo> getBalanceByCoinType(String coinType);
        Single<List<BalanceInfo>> getAllBalances();
        Single<BalanceInfo> getKrwBalance();
        Single<List<BalanceInfo>> getBalanceHistory(String coinType, long startTime, long endTime);
        
        // 실시간 관찰
        Observable<BalanceInfo> observeBalanceByCoinType(String coinType);
        Observable<List<BalanceInfo>> observeAllBalances();
        Observable<BalanceInfo> observeKrwBalance();
        
        // 잔고 정보 저장/업데이트
        Completable saveBalanceInfo(BalanceInfo balanceInfo);
        Completable saveBalanceInfos(List<BalanceInfo> balanceInfos);
        Completable updateBalanceInfo(BalanceInfo balanceInfo);
        Completable deleteBalanceInfo(String coinType);
        Completable clearBalanceHistory();
        
        // 비즈니스 로직 관련 메서드
        Single<Boolean> hasSufficientBalance(String coinType, double requiredAmount);
        Single<Double> getAvailableBalance(String coinType);
        Single<Double> getTotalBalance(String coinType);
        Single<Double> getInUseBalance(String coinType);
        Single<Double> calculateTotalPortfolioValue();
        Single<Boolean> isBalanceDataStale(String coinType, long maxAgeMillis);
    }

    /**
     * 거래 설정 Repository 인터페이스
     */
    public interface SettingsRepository {
        
        // 설정 조회
        Single<TradingSettings> getTradingSettings();
        Single<String> getApiKey();
        Single<String> getApiSecret();
        Single<Integer> getUnitPrice();
        Single<Integer> getTradeInterval();
        Single<Float> getEarningRate();
        Single<Float> getSlotIntervalRate();
        Single<Boolean> isFileLogEnabled();
        Single<String> getCoinType();
        Single<Boolean> isAutoTradingEnabled();
        
        // 설정 저장/업데이트
        Completable saveTradingSettings(TradingSettings settings);
        Completable updateApiCredentials(String apiKey, String apiSecret);
        Completable updateUnitPrice(int unitPrice);
        Completable updateTradeInterval(int tradeInterval);
        Completable updateEarningRate(float earningRate);
        Completable updateSlotIntervalRate(float slotIntervalRate);
        Completable updateFileLogEnabled(boolean enabled);
        Completable updateCoinType(String coinType);
        Completable updateAutoTradingEnabled(boolean enabled);
        
        // 실시간 관찰
        Observable<TradingSettings> observeTradingSettings();
        Observable<Boolean> observeAutoTradingEnabled();
        Observable<String> observeCoinType();
        
        // 비즈니스 로직 관련 메서드
        Single<Boolean> isValidConfiguration();
        Single<Boolean> isAutoTradingReady();
        Single<String> getMaskedApiKey();
        Single<String> getMaskedApiSecret();
        Completable resetToDefaults();
    }

    /**
     * 포트폴리오 Repository 인터페이스
     */
    public interface PortfolioRepository {
        
        // 포트폴리오 조회
        Single<Portfolio> getCurrentPortfolio();
        Single<List<Portfolio>> getPortfolioHistory(long startTime, long endTime);
        Single<Portfolio> getPortfolioSnapshot(long timestamp);
        
        // 실시간 관찰
        Observable<Portfolio> observeCurrentPortfolio();
        Observable<Portfolio> observePortfolioChanges();
        
        // 포트폴리오 저장/업데이트
        Completable savePortfolio(Portfolio portfolio);
        Completable updatePortfolio(Portfolio portfolio);
        Completable deletePortfolioHistory(long beforeTimestamp);
        Completable clearPortfolioHistory();
        
        // 비즈니스 로직 관련 메서드
        Single<Double> calculateTotalValue();
        Single<Double> calculateUnrealizedProfit();
        Single<Double> calculateUnrealizedProfitRate();
        Single<Integer> getActiveOrderCount();
        Single<Integer> getActiveBuyOrderCount();
        Single<Integer> getActiveSellOrderCount();
        Single<Boolean> isPortfolioProfitable();
        Single<Double> calculatePortfolioGrowthRate(long startTime, long endTime);
    }

    /**
     * 통계 Repository 인터페이스
     */
    public interface StatisticsRepository {
        
        // 통계 조회
        Single<TradingStatistics> getTradingStatistics(long startTime, long endTime);
        Single<TradingStatistics> getTradingStatisticsByCoinType(String coinType, long startTime, long endTime);
        Single<TradingStatistics> getTradingStatisticsByPeriod(int days);
        Single<List<TradingStatistics>> getDailyStatistics(long startTime, long endTime);
        
        // 실시간 관찰
        Observable<TradingStatistics> observeTradingStatistics();
        Observable<TradingStatistics> observeDailyStatistics();
        
        // 통계 저장/업데이트
        Completable saveTradingStatistics(TradingStatistics statistics);
        Completable updateTradingStatistics(TradingStatistics statistics);
        Completable deleteStatisticsBefore(long timestamp);
        Completable clearAllStatistics();
        
        // 비즈니스 로직 관련 메서드
        Single<Double> calculateWinRate(long startTime, long endTime);
        Single<Double> calculateAverageProfit(long startTime, long endTime);
        Single<Double> calculateAverageLoss(long startTime, long endTime);
        Single<Double> calculateNetProfit(long startTime, long endTime);
        Single<Integer> getTotalTradeCount(long startTime, long endTime);
        Single<Integer> getSuccessfulTradeCount(long startTime, long endTime);
        Single<Integer> getFailedTradeCount(long startTime, long endTime);
        Single<List<Trade>> getBestTrades(int limit);
        Single<List<Trade>> getWorstTrades(int limit);
    }

    /**
     * 알림 Repository 인터페이스
     */
    public interface NotificationRepository {
        
        // 알림 조회
        Single<List<NotificationInfo>> getAllNotifications();
        Single<List<NotificationInfo>> getNotificationsByType(NotificationInfo.NotificationType type);
        Single<List<NotificationInfo>> getUnreadNotifications();
        Single<NotificationInfo> getNotificationById(String notificationId);
        
        // 실시간 관찰
        Observable<List<NotificationInfo>> observeAllNotifications();
        Observable<List<NotificationInfo>> observeUnreadNotifications();
        Observable<NotificationInfo> observeNewNotifications();
        
        // 알림 저장/업데이트
        Completable saveNotification(NotificationInfo notification);
        Completable markAsRead(String notificationId);
        Completable markAllAsRead();
        Completable deleteNotification(String notificationId);
        Completable deleteOldNotifications(long beforeTimestamp);
        Completable clearAllNotifications();
        
        // 비즈니스 로직 관련 메서드
        Single<Integer> getUnreadCount();
        Single<Boolean> hasUnreadNotifications();
        Completable sendTradeNotification(Trade trade);
        Completable sendErrorNotification(String errorMessage);
        Completable sendSuccessNotification(String message);
    }

    /**
     * 알림 정보 도메인 모델
     */
    public static class NotificationInfo {
        private String id;
        private NotificationType type;
        private String title;
        private String message;
        private boolean isRead;
        private long timestamp;
        private String tradeId;
        private String coinType;

        public enum NotificationType {
            TRADE_SUCCESS("거래 성공"),
            TRADE_FAILURE("거래 실패"),
            TRADE_PENDING("거래 대기"),
            ERROR("오류"),
            INFO("정보"),
            WARNING("경고");

            private final String description;

            NotificationType(String description) {
                this.description = description;
            }

            public String getDescription() { return description; }
        }

        public NotificationInfo() {
            this.timestamp = System.currentTimeMillis();
            this.isRead = false;
        }

        public NotificationInfo(NotificationType type, String title, String message) {
            this();
            this.type = type;
            this.title = title;
            this.message = message;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public NotificationType getType() { return type; }
        public void setType(NotificationType type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getTradeId() { return tradeId; }
        public void setTradeId(String tradeId) { this.tradeId = tradeId; }

        public String getCoinType() { return coinType; }
        public void setCoinType(String coinType) { this.coinType = coinType; }

        @Override
        public String toString() {
            return String.format("NotificationInfo{id='%s', type=%s, title='%s', isRead=%s}", 
                id, type, title, isRead);
        }
    }

    /**
     * 데이터 동기화 Repository 인터페이스
     */
    public interface SyncRepository {
        
        // 동기화 실행
        Completable syncAllData();
        Completable syncTrades();
        Completable syncPrices();
        Completable syncBalances();
        Completable syncSettings();
        Completable syncPortfolio();
        
        // 동기화 상태 조회
        Single<SyncStatus> getSyncStatus();
        Single<Boolean> isSyncInProgress();
        Single<Long> getLastSyncTime();
        
        // 실시간 관찰
        Observable<SyncStatus> observeSyncStatus();
        Observable<Boolean> observeSyncInProgress();
        
        // 비즈니스 로직 관련 메서드
        Single<Boolean> isDataStale(long maxAgeMillis);
        Completable forceSync();
        Completable cancelSync();
    }

    /**
     * 동기화 상태 도메인 모델
     */
    public static class SyncStatus {
        private boolean isInProgress;
        private long lastSyncTime;
        private SyncResult lastSyncResult;
        private String lastErrorMessage;
        private int syncProgress;

        public enum SyncResult {
            SUCCESS("성공"),
            FAILURE("실패"),
            PARTIAL_SUCCESS("부분 성공"),
            CANCELLED("취소됨");

            private final String description;

            SyncResult(String description) {
                this.description = description;
            }

            public String getDescription() { return description; }
        }

        public SyncStatus() {
            this.isInProgress = false;
            this.lastSyncTime = 0;
            this.lastSyncResult = SyncResult.SUCCESS;
            this.syncProgress = 0;
        }

        // Getters and Setters
        public boolean isInProgress() { return isInProgress; }
        public void setInProgress(boolean inProgress) { isInProgress = inProgress; }

        public long getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }

        public SyncResult getLastSyncResult() { return lastSyncResult; }
        public void setLastSyncResult(SyncResult lastSyncResult) { this.lastSyncResult = lastSyncResult; }

        public String getLastErrorMessage() { return lastErrorMessage; }
        public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

        public int getSyncProgress() { return syncProgress; }
        public void setSyncProgress(int syncProgress) { this.syncProgress = syncProgress; }

        @Override
        public String toString() {
            return String.format("SyncStatus{inProgress=%s, lastSyncTime=%d, result=%s, progress=%d%%}", 
                isInProgress, lastSyncTime, lastSyncResult, syncProgress);
        }
    }
}
