package com.example.k_trader.repository.impl;

import android.content.Context;
import android.util.Log;
import com.example.k_trader.domain.model.DomainModels.*;
import com.example.k_trader.domain.repository.RepositoryInterfaces.*;
import com.example.k_trader.database.OrderRepository;
import com.example.k_trader.database.CoinPriceInfoRepository;
import com.example.k_trader.base.TradeData;
import com.example.k_trader.base.TradeDataManager;
import com.example.k_trader.database.entities.CoinPriceInfoEntity;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.ArrayList;

/**
 * Repository 구현체들
 * 기존 구조와 연동되는 실제 구현체들
 * Clean Architecture의 Data Layer에 해당
 */
public class RepositoryImplementations {

    /**
     * 거래 데이터 Repository 구현체
     * 기존 OrderRepository와 TradeData를 새로운 Domain Model로 변환
     */
    public static class TradeRepositoryImpl implements TradeRepository {
        
        private final OrderRepository orderRepository;
        private final Context context;
        
        public TradeRepositoryImpl(OrderRepository orderRepository, Context context) {
            this.orderRepository = orderRepository;
            this.context = context;
        }
        
        @Override
        public Single<List<Trade>> getAllTrades() {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting all trades");
            return orderRepository.loadInitialOrders()
                .map(this::convertToDomainModels)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Single<List<Trade>> getTradesByType(Trade.Type type) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trades by type: " + type);
            return getAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (trade.getType() == type) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Single<List<Trade>> getTradesByStatus(Trade.Status status) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trades by status: " + status);
            return getAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (trade.getStatus() == status) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Single<List<Trade>> getTradesByCoinType(String coinType) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trades by coin type: " + coinType);
            return getAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (coinType.equals(trade.getCoinType())) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Single<Trade> getTradeById(String tradeId) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trade by ID: " + tradeId);
            return getAllTrades()
                .map(trades -> {
                    for (Trade trade : trades) {
                        if (tradeId.equals(trade.getId())) {
                            return trade;
                        }
                    }
                    return null;
                });
        }
        
        @Override
        public Single<List<Trade>> getTradesByTimeRange(long startTime, long endTime) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trades by time range");
            return getAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (trade.getProcessedTime() >= startTime && trade.getProcessedTime() <= endTime) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Observable<List<Trade>> observeAllTrades() {
            Log.d("KTrader", "[TradeRepositoryImpl] Observing all trades");
            return orderRepository.observeAllOrders()
                .toObservable()
                .map(this::convertToDomainModels)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Observable<List<Trade>> observeTradesByType(Trade.Type type) {
            Log.d("KTrader", "[TradeRepositoryImpl] Observing trades by type: " + type);
            return observeAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (trade.getType() == type) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Observable<List<Trade>> observeTradesByStatus(Trade.Status status) {
            Log.d("KTrader", "[TradeRepositoryImpl] Observing trades by status: " + status);
            return observeAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (trade.getStatus() == status) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Observable<List<Trade>> observeTradesByCoinType(String coinType) {
            Log.d("KTrader", "[TradeRepositoryImpl] Observing trades by coin type: " + coinType);
            return observeAllTrades()
                .map(trades -> {
                    List<Trade> filteredTrades = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (coinType.equals(trade.getCoinType())) {
                            filteredTrades.add(trade);
                        }
                    }
                    return filteredTrades;
                });
        }
        
        @Override
        public Completable saveTrade(Trade trade) {
            Log.d("KTrader", "[TradeRepositoryImpl] Saving trade: " + trade.getId());
            return Completable.fromAction(() -> {
                TradeData tradeData = convertToTradeData(trade);
                List<TradeData> tradeDataList = new ArrayList<>();
                tradeDataList.add(tradeData);
                orderRepository.saveOrders(tradeDataList).subscribe();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable saveTrades(List<Trade> trades) {
            Log.d("KTrader", "[TradeRepositoryImpl] Saving " + trades.size() + " trades");
            return Completable.fromAction(() -> {
                List<TradeData> tradeDataList = new ArrayList<>();
                for (Trade trade : trades) {
                    tradeDataList.add(convertToTradeData(trade));
                }
                orderRepository.saveOrders(tradeDataList).subscribe();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateTrade(Trade trade) {
            Log.d("KTrader", "[TradeRepositoryImpl] Updating trade: " + trade.getId());
            return saveTrade(trade); // 기존 구조에서는 save와 update가 동일
        }
        
        @Override
        public Completable deleteTrade(String tradeId) {
            Log.d("KTrader", "[TradeRepositoryImpl] Deleting trade: " + tradeId);
            return Completable.fromAction(() -> {
                // TODO: 기존 구조에서 개별 삭제 기능 구현 필요
                Log.d("KTrader", "[TradeRepositoryImpl] Delete trade not implemented yet");
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable deleteTradesByType(Trade.Type type) {
            Log.d("KTrader", "[TradeRepositoryImpl] Deleting trades by type: " + type);
            return Completable.fromAction(() -> {
                // TODO: 기존 구조에서 타입별 삭제 기능 구현 필요
                Log.d("KTrader", "[TradeRepositoryImpl] Delete trades by type not implemented yet");
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable clearAllTrades() {
            Log.d("KTrader", "[TradeRepositoryImpl] Clearing all trades");
            return Completable.fromAction(() -> {
                orderRepository.deleteAllOrders();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Single<List<Trade>> getActiveOrders() {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting active orders");
            return getTradesByStatus(Trade.Status.PLACED);
        }
        
        @Override
        public Single<List<Trade>> getCompletedTrades() {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting completed trades");
            return getTradesByStatus(Trade.Status.PROCESSED);
        }
        
        @Override
        public Single<List<Trade>> getPendingTrades() {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting pending trades");
            return getTradesByStatus(Trade.Status.PLACED);
        }
        
        @Override
        public Single<Trade> getLatestTradeByType(Trade.Type type) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting latest trade by type: " + type);
            return getTradesByType(type)
                .map(trades -> {
                    if (trades.isEmpty()) return null;
                    
                    Trade latest = trades.get(0);
                    for (Trade trade : trades) {
                        if (trade.getProcessedTime() > latest.getProcessedTime()) {
                            latest = trade;
                        }
                    }
                    return latest;
                });
        }
        
        @Override
        public Single<Integer> getActiveOrderCount(Trade.Type type) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting active order count for type: " + type);
            return getTradesByType(type)
                .map(trades -> {
                    int count = 0;
                    for (Trade trade : trades) {
                        if (trade.isPending()) {
                            count++;
                        }
                    }
                    return count;
                });
        }
        
        @Override
        public Single<Double> calculateTotalProfit() {
            Log.d("KTrader", "[TradeRepositoryImpl] Calculating total profit");
            return getCompletedTrades()
                .map(trades -> {
                    double totalProfit = 0.0;
                    for (Trade trade : trades) {
                        if (trade.isSellOrder()) {
                            totalProfit += trade.getProfitRate();
                        }
                    }
                    return totalProfit;
                });
        }
        
        @Override
        public Single<Double> calculateTotalProfitByCoinType(String coinType) {
            Log.d("KTrader", "[TradeRepositoryImpl] Calculating total profit by coin type: " + coinType);
            return getTradesByCoinType(coinType)
                .map(trades -> {
                    double totalProfit = 0.0;
                    for (Trade trade : trades) {
                        if (trade.isSellOrder() && trade.isCompleted()) {
                            totalProfit += trade.getProfitRate();
                        }
                    }
                    return totalProfit;
                });
        }
        
        @Override
        public Single<TradingStatistics> getTradingStatistics(long startTime, long endTime) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trading statistics");
            return getTradesByTimeRange(startTime, endTime)
                .map(trades -> {
                    TradingStatistics stats = new TradingStatistics();
                    stats.setPeriodStart(startTime);
                    stats.setPeriodEnd(endTime);
                    
                    for (Trade trade : trades) {
                        stats.addTrade(trade);
                    }
                    
                    return stats;
                });
        }
        
        @Override
        public Single<TradingStatistics> getTradingStatisticsByCoinType(String coinType, long startTime, long endTime) {
            Log.d("KTrader", "[TradeRepositoryImpl] Getting trading statistics by coin type: " + coinType);
            return getTradesByCoinType(coinType)
                .map(trades -> {
                    TradingStatistics stats = new TradingStatistics();
                    stats.setPeriodStart(startTime);
                    stats.setPeriodEnd(endTime);
                    
                    for (Trade trade : trades) {
                        if (trade.getProcessedTime() >= startTime && trade.getProcessedTime() <= endTime) {
                            stats.addTrade(trade);
                        }
                    }
                    
                    return stats;
                });
        }
        
        // 변환 메서드들
        private List<Trade> convertToDomainModels(List<TradeData> tradeDataList) {
            List<Trade> trades = new ArrayList<>();
            for (TradeData tradeData : tradeDataList) {
                trades.add(convertToDomainModel(tradeData));
            }
            return trades;
        }
        
        private Trade convertToDomainModel(TradeData tradeData) {
            Trade trade = new Trade();
            trade.setId(tradeData.getId());
            trade.setType(Trade.Type.fromCode(tradeData.getType().ordinal()));
            trade.setPrice(tradeData.getPrice());
            trade.setUnits(tradeData.getUnits());
            trade.setProcessedTime(tradeData.getProcessedTime());
            trade.setStatus(tradeData.getStatus() == TradeDataManager.Status.PROCESSED ? Trade.Status.PROCESSED : Trade.Status.PLACED);
            trade.setCoinType("BTC"); // 기본값, 실제로는 설정에서 가져와야 함
            trade.setProfitRate(0.0); // TODO: 실제 수익률 계산
            trade.setOrderId(tradeData.getId());
            return trade;
        }
        
        private TradeData convertToTradeData(Trade trade) {
            TradeData tradeData = new TradeData();
            tradeData.setId(trade.getId());
            tradeData.setType(TradeDataManager.Type.values()[trade.getType().getCode()]);
            tradeData.setPrice(trade.getPrice());
            tradeData.setUnits((float) trade.getUnits());
            tradeData.setProcessedTime(trade.getProcessedTime());
            tradeData.setStatus(trade.isCompleted() ? TradeDataManager.Status.PROCESSED : TradeDataManager.Status.PLACED);
            // TODO: profitRate는 TradeData에 없으므로 별도 처리 필요
            return tradeData;
        }
    }

    /**
     * 코인 가격 정보 Repository 구현체
     * 기존 CoinPriceInfoRepository와 연동
     */
    public static class CoinPriceRepositoryImpl implements CoinPriceRepository {
        
        private final CoinPriceInfoRepository coinPriceInfoRepository;
        private final Context context;
        
        public CoinPriceRepositoryImpl(CoinPriceInfoRepository coinPriceInfoRepository, Context context) {
            this.coinPriceInfoRepository = coinPriceInfoRepository;
            this.context = context;
        }
        
        @Override
        public Single<CoinPriceInfo> getCurrentPrice(String coinType) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Getting current price for: " + coinType);
            return coinPriceInfoRepository.getCurrentPriceInfo()
                .map(this::convertToDomainModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Single<List<CoinPriceInfo>> getAllCurrentPrices() {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Getting all current prices");
            return getCurrentPrice("BTC")
                .map(priceInfo -> {
                    List<CoinPriceInfo> prices = new ArrayList<>();
                    prices.add(priceInfo);
                    return prices;
                });
        }
        
        @Override
        public Single<List<CoinPriceInfo>> getPriceHistory(String coinType, long startTime, long endTime) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Getting price history for: " + coinType);
            // TODO: 기존 구조에서 히스토리 기능 구현 필요
            return Single.just(new ArrayList<>());
        }
        
        @Override
        public Single<CoinPriceInfo> getLatestPriceByCoinType(String coinType) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Getting latest price by coin type: " + coinType);
            return getCurrentPrice(coinType);
        }
        
        @Override
        public Observable<CoinPriceInfo> observeCurrentPrice(String coinType) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Observing current price for: " + coinType);
            return coinPriceInfoRepository.observeCurrentPriceInfo()
                .toObservable()
                .map(this::convertToDomainModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Observable<List<CoinPriceInfo>> observeAllCurrentPrices() {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Observing all current prices");
            return observeCurrentPrice("BTC")
                .map(priceInfo -> {
                    List<CoinPriceInfo> prices = new ArrayList<>();
                    prices.add(priceInfo);
                    return prices;
                });
        }
        
        @Override
        public Observable<CoinPriceInfo> observePriceChanges(String coinType) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Observing price changes for: " + coinType);
            return observeCurrentPrice(coinType);
        }
        
        @Override
        public Completable savePriceInfo(CoinPriceInfo priceInfo) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Saving price info for: " + priceInfo.getCoinType());
            return coinPriceInfoRepository.savePriceInfo(
                priceInfo.getCoinType(),
                String.valueOf(priceInfo.getCurrentPrice()),
                String.valueOf(priceInfo.getDailyChangeRate())
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable savePriceInfos(List<CoinPriceInfo> priceInfos) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Saving " + priceInfos.size() + " price infos");
            return Completable.fromAction(() -> {
                for (CoinPriceInfo priceInfo : priceInfos) {
                    savePriceInfo(priceInfo).subscribe();
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updatePriceInfo(CoinPriceInfo priceInfo) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Updating price info for: " + priceInfo.getCoinType());
            return savePriceInfo(priceInfo);
        }
        
        @Override
        public Completable deletePriceInfo(String coinType) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Deleting price info for: " + coinType);
            return Completable.fromAction(() -> {
                // TODO: 기존 구조에서 개별 삭제 기능 구현 필요
                Log.d("KTrader", "[CoinPriceRepositoryImpl] Delete price info not implemented yet");
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable clearPriceHistory() {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Clearing price history");
            return coinPriceInfoRepository.deleteAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Single<Boolean> isPriceDataStale(String coinType, long maxAgeMillis) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Checking if price data is stale for: " + coinType);
            return getCurrentPrice(coinType)
                .map(priceInfo -> priceInfo.isDataStale(maxAgeMillis));
        }
        
        @Override
        public Single<List<CoinPriceInfo>> getPriceChangesInPeriod(String coinType, long periodMillis) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Getting price changes in period for: " + coinType);
            // TODO: 기존 구조에서 기간별 변화 기능 구현 필요
            return Single.just(new ArrayList<>());
        }
        
        @Override
        public Single<Double> calculatePriceVolatility(String coinType, long periodMillis) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Calculating price volatility for: " + coinType);
            // TODO: 기존 구조에서 변동성 계산 기능 구현 필요
            return Single.just(0.0);
        }
        
        @Override
        public Single<Boolean> isPriceIncreasing(String coinType, int timeWindowMinutes) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Checking if price is increasing for: " + coinType);
            return getCurrentPrice(coinType)
                .map(CoinPriceInfo::isPriceIncreased);
        }
        
        @Override
        public Single<Boolean> isPriceDecreasing(String coinType, int timeWindowMinutes) {
            Log.d("KTrader", "[CoinPriceRepositoryImpl] Checking if price is decreasing for: " + coinType);
            return getCurrentPrice(coinType)
                .map(CoinPriceInfo::isPriceDecreased);
        }
        
        // 변환 메서드
        private CoinPriceInfo convertToDomainModel(CoinPriceInfoEntity entity) {
            CoinPriceInfo priceInfo = new CoinPriceInfo();
            priceInfo.setCoinType(entity.getCoinType());
            priceInfo.setCurrentPrice(Integer.parseInt(entity.getCurrentPrice()));
            priceInfo.setDailyChangeRate(Double.parseDouble(entity.getPriceChange()));
            priceInfo.setHourlyChangeRate(0.0); // 기본값
            priceInfo.setLastUpdated(System.currentTimeMillis());
            return priceInfo;
        }
    }

    /**
     * 설정 Repository 구현체
     * SharedPreferences와 연동
     */
    public static class SettingsRepositoryImpl implements SettingsRepository {
        
        private final Context context;
        private final String PREFS_NAME = "trading_settings";
        
        public SettingsRepositoryImpl(Context context) {
            this.context = context;
        }
        
        @Override
        public Single<TradingSettings> getTradingSettings() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting trading settings");
            return Single.fromCallable(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                TradingSettings settings = new TradingSettings();
                settings.setApiKey(prefs.getString("api_key", ""));
                settings.setApiSecret(prefs.getString("api_secret", ""));
                settings.setUnitPrice(prefs.getInt("unit_price", 100000));
                settings.setTradeInterval(prefs.getInt("trade_interval", 60));
                settings.setEarningRate(prefs.getFloat("earning_rate", 1.0f));
                settings.setSlotIntervalRate(prefs.getFloat("slot_interval_rate", 0.5f));
                settings.setFileLogEnabled(prefs.getBoolean("file_log_enabled", false));
                settings.setCoinType(prefs.getString("coin_type", "BTC"));
                settings.setAutoTradingEnabled(prefs.getBoolean("auto_trading_enabled", false));
                return settings;
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Single<String> getApiKey() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting API key");
            return getTradingSettings().map(TradingSettings::getApiKey);
        }
        
        @Override
        public Single<String> getApiSecret() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting API secret");
            return getTradingSettings().map(TradingSettings::getApiSecret);
        }
        
        @Override
        public Single<Integer> getUnitPrice() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting unit price");
            return getTradingSettings().map(TradingSettings::getUnitPrice);
        }
        
        @Override
        public Single<Integer> getTradeInterval() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting trade interval");
            return getTradingSettings().map(TradingSettings::getTradeInterval);
        }
        
        @Override
        public Single<Float> getEarningRate() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting earning rate");
            return getTradingSettings().map(TradingSettings::getEarningRate);
        }
        
        @Override
        public Single<Float> getSlotIntervalRate() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting slot interval rate");
            return getTradingSettings().map(TradingSettings::getSlotIntervalRate);
        }
        
        @Override
        public Single<Boolean> isFileLogEnabled() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Checking if file log is enabled");
            return getTradingSettings().map(TradingSettings::isFileLogEnabled);
        }
        
        @Override
        public Single<String> getCoinType() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting coin type");
            return getTradingSettings().map(TradingSettings::getCoinType);
        }
        
        @Override
        public Single<Boolean> isAutoTradingEnabled() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Checking if auto trading is enabled");
            return getTradingSettings().map(TradingSettings::isAutoTradingEnabled);
        }
        
        @Override
        public Completable saveTradingSettings(TradingSettings settings) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Saving trading settings");
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("api_key", settings.getApiKey());
                editor.putString("api_secret", settings.getApiSecret());
                editor.putInt("unit_price", settings.getUnitPrice());
                editor.putInt("trade_interval", settings.getTradeInterval());
                editor.putFloat("earning_rate", settings.getEarningRate());
                editor.putFloat("slot_interval_rate", settings.getSlotIntervalRate());
                editor.putBoolean("file_log_enabled", settings.isFileLogEnabled());
                editor.putString("coin_type", settings.getCoinType());
                editor.putBoolean("auto_trading_enabled", settings.isAutoTradingEnabled());
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateApiCredentials(String apiKey, String apiSecret) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating API credentials");
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("api_key", apiKey);
                editor.putString("api_secret", apiSecret);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateUnitPrice(int unitPrice) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating unit price: " + unitPrice);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("unit_price", unitPrice);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateTradeInterval(int tradeInterval) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating trade interval: " + tradeInterval);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("trade_interval", tradeInterval);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateEarningRate(float earningRate) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating earning rate: " + earningRate);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("earning_rate", earningRate);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateSlotIntervalRate(float slotIntervalRate) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating slot interval rate: " + slotIntervalRate);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("slot_interval_rate", slotIntervalRate);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateFileLogEnabled(boolean enabled) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating file log enabled: " + enabled);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("file_log_enabled", enabled);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateCoinType(String coinType) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating coin type: " + coinType);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("coin_type", coinType);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Completable updateAutoTradingEnabled(boolean enabled) {
            Log.d("KTrader", "[SettingsRepositoryImpl] Updating auto trading enabled: " + enabled);
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("auto_trading_enabled", enabled);
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
        
        @Override
        public Observable<TradingSettings> observeTradingSettings() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Observing trading settings");
            // TODO: SharedPreferences 변경 감지 구현 필요
            return Observable.just(new TradingSettings());
        }
        
        @Override
        public Observable<Boolean> observeAutoTradingEnabled() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Observing auto trading enabled");
            return observeTradingSettings().map(TradingSettings::isAutoTradingEnabled);
        }
        
        @Override
        public Observable<String> observeCoinType() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Observing coin type");
            return observeTradingSettings().map(TradingSettings::getCoinType);
        }
        
        @Override
        public Single<Boolean> isValidConfiguration() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Checking if configuration is valid");
            return getTradingSettings().map(TradingSettings::isValidConfiguration);
        }
        
        @Override
        public Single<Boolean> isAutoTradingReady() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Checking if auto trading is ready");
            return getTradingSettings().map(TradingSettings::isAutoTradingReady);
        }
        
        @Override
        public Single<String> getMaskedApiKey() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting masked API key");
            return getTradingSettings().map(TradingSettings::getMaskedApiKey);
        }
        
        @Override
        public Single<String> getMaskedApiSecret() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Getting masked API secret");
            return getTradingSettings().map(TradingSettings::getMaskedApiSecret);
        }
        
        @Override
        public Completable resetToDefaults() {
            Log.d("KTrader", "[SettingsRepositoryImpl] Resetting to defaults");
            return Completable.fromAction(() -> {
                android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
        }
    }
}