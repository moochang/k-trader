package com.example.k_trader.domain.usecase;

import android.util.Log;
import com.example.k_trader.domain.model.DomainModels.*;
import com.example.k_trader.domain.repository.RepositoryInterfaces.*;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;

/**
 * Use Cases for K-Trader App
 * 
 * Clean Architecture의 Domain Layer에 속하는 비즈니스 로직을 캡슐화한 Use Case들
 * 단일 책임 원칙을 따르는 각각의 비즈니스 기능
 * UI와 데이터 레이어 사이의 중재 역할
 * 
 * @author K-Trader Team
 * @version 1.0
 */
public class UseCases {

    /**
     * 거래 데이터 관리 Use Case
     * 기존 TradeDataManager의 기능을 Clean Architecture에 맞게 개선
     */
    public static class ManageTradingDataUseCase {
        private final TradeRepository tradeRepository;
        private final NotificationRepository notificationRepository;

        public ManageTradingDataUseCase(TradeRepository tradeRepository, NotificationRepository notificationRepository) {
            this.tradeRepository = tradeRepository;
            this.notificationRepository = notificationRepository;
        }

        // 거래 데이터 조회
        public Single<List<Trade>> getAllTrades() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting all trades");
            return tradeRepository.getAllTrades();
        }

        public Single<List<Trade>> getTradesByType(Trade.Type type) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting trades by type: " + type);
            return tradeRepository.getTradesByType(type);
        }

        public Single<List<Trade>> getActiveOrders() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting active orders");
            return tradeRepository.getActiveOrders();
        }

        public Single<List<Trade>> getCompletedTrades() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting completed trades");
            return tradeRepository.getCompletedTrades();
        }

        // 실시간 관찰
        public Observable<List<Trade>> observeAllTrades() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Observing all trades");
            return tradeRepository.observeAllTrades();
        }

        public Observable<List<Trade>> observeActiveOrders() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Observing active orders");
            return tradeRepository.observeTradesByStatus(Trade.Status.PLACED);
        }

        // 거래 데이터 저장
        public Completable saveTrade(Trade trade) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Saving trade: " + trade.getId());
            return tradeRepository.saveTrade(trade)
                .andThen(createTradeNotification(trade));
        }

        public Completable saveTrades(List<Trade> trades) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Saving " + trades.size() + " trades");
            return tradeRepository.saveTrades(trades)
                .andThen(createTradeNotifications(trades));
        }

        // 거래 데이터 업데이트
        public Completable updateTrade(Trade trade) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Updating trade: " + trade.getId());
            return tradeRepository.updateTrade(trade);
        }

        // 거래 데이터 삭제
        public Completable deleteTrade(String tradeId) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Deleting trade: " + tradeId);
            return tradeRepository.deleteTrade(tradeId);
        }

        public Completable clearAllTrades() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Clearing all trades");
            return tradeRepository.clearAllTrades();
        }

        // 비즈니스 로직
        public Single<Trade> getLatestTradeByType(Trade.Type type) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting latest trade by type: " + type);
            return tradeRepository.getLatestTradeByType(type);
        }

        public Single<Integer> getActiveOrderCount(Trade.Type type) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting active order count for type: " + type);
            return tradeRepository.getActiveOrderCount(type);
        }

        public Single<Double> calculateTotalProfit() {
            Log.d("KTrader", "[ManageTradingDataUseCase] Calculating total profit");
            return tradeRepository.calculateTotalProfit();
        }

        public Single<TradingStatistics> getTradingStatistics(long startTime, long endTime) {
            Log.d("KTrader", "[ManageTradingDataUseCase] Getting trading statistics");
            return tradeRepository.getTradingStatistics(startTime, endTime);
        }

        // 알림 생성
        private Completable createTradeNotification(Trade trade) {
            return Completable.fromAction(() -> {
                // TODO: 알림 기능 구현
                Log.d("KTrader", "[ManageTradingDataUseCase] Trade notification: " + trade.getId());
            });
        }

        private Completable createTradeNotifications(List<Trade> trades) {
            return Completable.fromAction(() -> {
                for (Trade trade : trades) {
                    createTradeNotification(trade).subscribe();
                }
            });
        }
    }

    /**
     * 코인 가격 모니터링 Use Case
     */
    public static class MonitorCoinPriceUseCase {
        private final CoinPriceRepository coinPriceRepository;
        private final NotificationRepository notificationRepository;

        public MonitorCoinPriceUseCase(CoinPriceRepository coinPriceRepository, NotificationRepository notificationRepository) {
            this.coinPriceRepository = coinPriceRepository;
            this.notificationRepository = notificationRepository;
        }

        // 가격 정보 조회
        public Single<CoinPriceInfo> getCurrentPrice(String coinType) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Getting current price for: " + coinType);
            return coinPriceRepository.getCurrentPrice(coinType);
        }

        public Single<List<CoinPriceInfo>> getAllCurrentPrices() {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Getting all current prices");
            return coinPriceRepository.getAllCurrentPrices();
        }

        // 실시간 관찰
        public Observable<CoinPriceInfo> observeCurrentPrice(String coinType) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Observing current price for: " + coinType);
            return coinPriceRepository.observeCurrentPrice(coinType);
        }

        public Observable<List<CoinPriceInfo>> observeAllCurrentPrices() {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Observing all current prices");
            return coinPriceRepository.observeAllCurrentPrices();
        }

        // 가격 정보 저장
        public Completable savePriceInfo(CoinPriceInfo priceInfo) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Saving price info for: " + priceInfo.getCoinType());
            return coinPriceRepository.savePriceInfo(priceInfo);
        }

        public Completable savePriceInfos(List<CoinPriceInfo> priceInfos) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Saving " + priceInfos.size() + " price infos");
            return coinPriceRepository.savePriceInfos(priceInfos);
        }

        // 비즈니스 로직
        public Single<Boolean> isPriceDataStale(String coinType, long maxAgeMillis) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Checking if price data is stale for: " + coinType);
            return coinPriceRepository.isPriceDataStale(coinType, maxAgeMillis);
        }

        public Single<Double> calculatePriceVolatility(String coinType, long periodMillis) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Calculating price volatility for: " + coinType);
            return coinPriceRepository.calculatePriceVolatility(coinType, periodMillis);
        }

        public Single<Boolean> isPriceIncreasing(String coinType, int timeWindowMinutes) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Checking if price is increasing for: " + coinType);
            return coinPriceRepository.isPriceIncreasing(coinType, timeWindowMinutes);
        }

        public Single<Boolean> isPriceDecreasing(String coinType, int timeWindowMinutes) {
            Log.d("KTrader", "[MonitorCoinPriceUseCase] Checking if price is decreasing for: " + coinType);
            return coinPriceRepository.isPriceDecreasing(coinType, timeWindowMinutes);
        }

        // 가격 변화 알림
        public Completable notifyPriceChange(CoinPriceInfo oldPrice, CoinPriceInfo newPrice) {
            return Completable.fromAction(() -> {
                if (oldPrice != null && newPrice != null) {
                    double changeRate = ((double)(newPrice.getCurrentPrice() - oldPrice.getCurrentPrice()) / oldPrice.getCurrentPrice()) * 100;
                    if (Math.abs(changeRate) > 5.0) { // 5% 이상 변화 시 알림
                        String message = String.format("%s 가격이 %.2f%% 변화했습니다.", newPrice.getCoinType(), changeRate);
                        // TODO: 알림 기능 구현
                        // notificationRepository.sendInfoNotification("가격 변화 알림", message).subscribe();
                    }
                }
            });
        }
    }

    /**
     * 자동 거래 Use Case
     * 기존 TradeJobService의 핵심 로직을 Clean Architecture에 맞게 개선
     */
    public static class AutoTradingUseCase {
        private final TradeRepository tradeRepository;
        private final CoinPriceRepository coinPriceRepository;
        private final BalanceRepository balanceRepository;
        private final SettingsRepository settingsRepository;
        private final NotificationRepository notificationRepository;

        public AutoTradingUseCase(TradeRepository tradeRepository, CoinPriceRepository coinPriceRepository,
                                BalanceRepository balanceRepository, SettingsRepository settingsRepository,
                                NotificationRepository notificationRepository) {
            this.tradeRepository = tradeRepository;
            this.coinPriceRepository = coinPriceRepository;
            this.balanceRepository = balanceRepository;
            this.settingsRepository = settingsRepository;
            this.notificationRepository = notificationRepository;
        }

        // 자동 거래 실행
        public Completable executeAutoTrading() {
            Log.d("KTrader", "[AutoTradingUseCase] Executing auto trading");
            return settingsRepository.getTradingSettings()
                .flatMapCompletable(settings -> {
                    if (!settings.isAutoTradingReady()) {
                        Log.w("KTrader", "[AutoTradingUseCase] Auto trading is not ready");
                        return Completable.complete();
                    }
                    return executeTradingLogic(settings);
                });
        }

        private Completable executeTradingLogic(TradingSettings settings) {
            return Completable.fromAction(() -> {
                Log.d("KTrader", "[AutoTradingUseCase] Executing trading logic for coin: " + settings.getCoinType());
                
                // TODO: 기존 TradeJobService의 로직을 여기로 이전
            });
        }

        // 거래 상태 확인
        public Single<Boolean> isAutoTradingEnabled() {
            Log.d("KTrader", "[AutoTradingUseCase] Checking if auto trading is enabled");
            return settingsRepository.isAutoTradingEnabled();
        }

        public Single<Boolean> isAutoTradingReady() {
            Log.d("KTrader", "[AutoTradingUseCase] Checking if auto trading is ready");
            return settingsRepository.isAutoTradingReady();
        }

        // 거래 설정 업데이트
        public Completable updateAutoTradingEnabled(boolean enabled) {
            Log.d("KTrader", "[AutoTradingUseCase] Updating auto trading enabled: " + enabled);
            return settingsRepository.updateAutoTradingEnabled(enabled);
        }

        // 실시간 관찰
        public Observable<Boolean> observeAutoTradingEnabled() {
            Log.d("KTrader", "[AutoTradingUseCase] Observing auto trading enabled");
            return settingsRepository.observeAutoTradingEnabled();
        }
    }

    /**
     * 설정 관리 Use Case
     */
    public static class ManageSettingsUseCase {
        private final SettingsRepository settingsRepository;

        public ManageSettingsUseCase(SettingsRepository settingsRepository) {
            this.settingsRepository = settingsRepository;
        }

        // 설정 조회
        public Single<TradingSettings> getTradingSettings() {
            Log.d("KTrader", "[ManageSettingsUseCase] Getting trading settings");
            return settingsRepository.getTradingSettings();
        }

        public Single<String> getApiKey() {
            Log.d("KTrader", "[ManageSettingsUseCase] Getting API key");
            return settingsRepository.getApiKey();
        }

        public Single<String> getApiSecret() {
            Log.d("KTrader", "[ManageSettingsUseCase] Getting API secret");
            return settingsRepository.getApiSecret();
        }

        // 설정 저장/업데이트
        public Completable saveTradingSettings(TradingSettings settings) {
            Log.d("KTrader", "[ManageSettingsUseCase] Saving trading settings");
            return settingsRepository.saveTradingSettings(settings);
        }

        public Completable updateApiCredentials(String apiKey, String apiSecret) {
            Log.d("KTrader", "[ManageSettingsUseCase] Updating API credentials");
            return settingsRepository.updateApiCredentials(apiKey, apiSecret);
        }

        public Completable updateUnitPrice(int unitPrice) {
            Log.d("KTrader", "[ManageSettingsUseCase] Updating unit price: " + unitPrice);
            return settingsRepository.updateUnitPrice(unitPrice);
        }

        public Completable updateTradeInterval(int tradeInterval) {
            Log.d("KTrader", "[ManageSettingsUseCase] Updating trade interval: " + tradeInterval);
            return settingsRepository.updateTradeInterval(tradeInterval);
        }

        public Completable updateEarningRate(float earningRate) {
            Log.d("KTrader", "[ManageSettingsUseCase] Updating earning rate: " + earningRate);
            return settingsRepository.updateEarningRate(earningRate);
        }

        public Completable updateSlotIntervalRate(float slotIntervalRate) {
            Log.d("KTrader", "[ManageSettingsUseCase] Updating slot interval rate: " + slotIntervalRate);
            return settingsRepository.updateSlotIntervalRate(slotIntervalRate);
        }

        public Completable updateCoinType(String coinType) {
            Log.d("KTrader", "[ManageSettingsUseCase] Updating coin type: " + coinType);
            return settingsRepository.updateCoinType(coinType);
        }

        // 비즈니스 로직
        public Single<Boolean> isValidConfiguration() {
            Log.d("KTrader", "[ManageSettingsUseCase] Checking if configuration is valid");
            return settingsRepository.isValidConfiguration();
        }

        public Single<String> getMaskedApiKey() {
            Log.d("KTrader", "[ManageSettingsUseCase] Getting masked API key");
            return settingsRepository.getMaskedApiKey();
        }

        public Completable resetToDefaults() {
            Log.d("KTrader", "[ManageSettingsUseCase] Resetting to defaults");
            return settingsRepository.resetToDefaults();
        }

        // 실시간 관찰
        public Observable<TradingSettings> observeTradingSettings() {
            Log.d("KTrader", "[ManageSettingsUseCase] Observing trading settings");
            return settingsRepository.observeTradingSettings();
        }
    }

    /**
     * 데이터 동기화 Use Case
     */
    public static class SyncDataUseCase {
        private final SyncRepository syncRepository;
        private final TradeRepository tradeRepository;
        private final CoinPriceRepository coinPriceRepository;
        private final BalanceRepository balanceRepository;
        private final SettingsRepository settingsRepository;

        public SyncDataUseCase(SyncRepository syncRepository, TradeRepository tradeRepository,
                             CoinPriceRepository coinPriceRepository, BalanceRepository balanceRepository,
                             SettingsRepository settingsRepository) {
            this.syncRepository = syncRepository;
            this.tradeRepository = tradeRepository;
            this.coinPriceRepository = coinPriceRepository;
            this.balanceRepository = balanceRepository;
            this.settingsRepository = settingsRepository;
        }

        // 전체 데이터 동기화
        public Completable syncAllData() {
            Log.d("KTrader", "[SyncDataUseCase] Syncing all data");
            return syncRepository.syncAllData();
        }

        // 개별 데이터 동기화
        public Completable syncTrades() {
            Log.d("KTrader", "[SyncDataUseCase] Syncing trades");
            return syncRepository.syncTrades();
        }

        public Completable syncPrices() {
            Log.d("KTrader", "[SyncDataUseCase] Syncing prices");
            return syncRepository.syncPrices();
        }

        public Completable syncBalances() {
            Log.d("KTrader", "[SyncDataUseCase] Syncing balances");
            return syncRepository.syncBalances();
        }

        public Completable syncSettings() {
            Log.d("KTrader", "[SyncDataUseCase] Syncing settings");
            return syncRepository.syncSettings();
        }

        // 동기화 상태 조회
        public Single<SyncStatus> getSyncStatus() {
            Log.d("KTrader", "[SyncDataUseCase] Getting sync status");
            return syncRepository.getSyncStatus();
        }

        public Single<Boolean> isSyncInProgress() {
            Log.d("KTrader", "[SyncDataUseCase] Checking if sync is in progress");
            return syncRepository.isSyncInProgress();
        }

        public Single<Long> getLastSyncTime() {
            Log.d("KTrader", "[SyncDataUseCase] Getting last sync time");
            return syncRepository.getLastSyncTime();
        }

        // 비즈니스 로직
        public Single<Boolean> isDataStale(long maxAgeMillis) {
            Log.d("KTrader", "[SyncDataUseCase] Checking if data is stale");
            return syncRepository.isDataStale(maxAgeMillis);
        }

        public Completable forceSync() {
            Log.d("KTrader", "[SyncDataUseCase] Force syncing");
            return syncRepository.forceSync();
        }

        public Completable cancelSync() {
            Log.d("KTrader", "[SyncDataUseCase] Cancelling sync");
            return syncRepository.cancelSync();
        }

        // 실시간 관찰
        public Observable<SyncStatus> observeSyncStatus() {
            Log.d("KTrader", "[SyncDataUseCase] Observing sync status");
            return syncRepository.observeSyncStatus();
        }

        public Observable<Boolean> observeSyncInProgress() {
            Log.d("KTrader", "[SyncDataUseCase] Observing sync in progress");
            return syncRepository.observeSyncInProgress();
        }
    }

    /**
     * 포트폴리오 관리 Use Case
     */
    public static class ManagePortfolioUseCase {
        private final PortfolioRepository portfolioRepository;
        private final TradeRepository tradeRepository;
        private final BalanceRepository balanceRepository;
        private final CoinPriceRepository coinPriceRepository;

        public ManagePortfolioUseCase(PortfolioRepository portfolioRepository, TradeRepository tradeRepository,
                                    BalanceRepository balanceRepository, CoinPriceRepository coinPriceRepository) {
            this.portfolioRepository = portfolioRepository;
            this.tradeRepository = tradeRepository;
            this.balanceRepository = balanceRepository;
            this.coinPriceRepository = coinPriceRepository;
        }

        // 포트폴리오 조회
        public Single<Portfolio> getCurrentPortfolio() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Getting current portfolio");
            return portfolioRepository.getCurrentPortfolio();
        }

        public Single<List<Portfolio>> getPortfolioHistory(long startTime, long endTime) {
            Log.d("KTrader", "[ManagePortfolioUseCase] Getting portfolio history");
            return portfolioRepository.getPortfolioHistory(startTime, endTime);
        }

        // 포트폴리오 업데이트
        public Completable updatePortfolio(String coinType) {
            Log.d("KTrader", "[ManagePortfolioUseCase] Updating portfolio for: " + coinType);
            return Single.zip(
                balanceRepository.getKrwBalance(),
                balanceRepository.getBalanceByCoinType(coinType),
                coinPriceRepository.getCurrentPrice(coinType),
                (krwBalance, coinBalance, priceInfo) -> {
                    Portfolio portfolio = new Portfolio();
                    portfolio.updatePortfolio(krwBalance.getAvailableBalance(), coinBalance.getAvailableBalance(), priceInfo.getCurrentPrice());
                    return portfolio;
                }
            ).flatMapCompletable(portfolioRepository::updatePortfolio);
        }

        // 비즈니스 로직
        public Single<Double> calculateTotalValue() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Calculating total value");
            return portfolioRepository.calculateTotalValue();
        }

        public Single<Double> calculateUnrealizedProfit() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Calculating unrealized profit");
            return portfolioRepository.calculateUnrealizedProfit();
        }

        public Single<Double> calculateUnrealizedProfitRate() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Calculating unrealized profit rate");
            return portfolioRepository.calculateUnrealizedProfitRate();
        }

        public Single<Integer> getActiveOrderCount() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Getting active order count");
            return portfolioRepository.getActiveOrderCount();
        }

        public Single<Boolean> isPortfolioProfitable() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Checking if portfolio is profitable");
            return portfolioRepository.isPortfolioProfitable();
        }

        // 실시간 관찰
        public Observable<Portfolio> observeCurrentPortfolio() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Observing current portfolio");
            return portfolioRepository.observeCurrentPortfolio();
        }

        public Observable<Portfolio> observePortfolioChanges() {
            Log.d("KTrader", "[ManagePortfolioUseCase] Observing portfolio changes");
            return portfolioRepository.observePortfolioChanges();
        }
    }
}
