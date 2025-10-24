package com.example.k_trader.presentation.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;
import com.example.k_trader.domain.model.DomainModels.*;
import com.example.k_trader.domain.usecase.UseCases.*;
import com.example.k_trader.domain.service.DomainServices.*;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;

/**
 * ViewModels for K-Trader App
 * 
 * Clean Architecture의 Presentation Layer에 속하는 ViewModel들
 * UI와 Domain Layer 연결을 위한 ViewModel들
 * Android Architecture Components 패턴 적용
 * LiveData를 활용한 반응형 UI 업데이트
 * 
 * @author K-Trader Team
 * @version 1.0
 */
public class ViewModels {

    /**
     * 메인 화면 ViewModel
     * 기존 MainActivity와 MainPage의 기능을 Clean Architecture에 맞게 개선
     */
    public static class MainViewModel extends ViewModel {
        private final ManageTradingDataUseCase manageTradingDataUseCase;
        private final MonitorCoinPriceUseCase monitorCoinPriceUseCase;
        private final AutoTradingUseCase autoTradingUseCase;
        private final ManageSettingsUseCase manageSettingsUseCase;
        private final TradingAnalysisService tradingAnalysisService;
        
        private final CompositeDisposable disposables = new CompositeDisposable();
        
        // LiveData for UI
        private final MutableLiveData<CoinPriceInfo> currentPrice = new MutableLiveData<>();
        private final MutableLiveData<List<Trade>> activeOrders = new MutableLiveData<>();
        private final MutableLiveData<Portfolio> portfolio = new MutableLiveData<>();
        private final MutableLiveData<TradingSettings> settings = new MutableLiveData<>();
        private final MutableLiveData<Boolean> isAutoTradingEnabled = new MutableLiveData<>();
        private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
        private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

        public MainViewModel(ManageTradingDataUseCase manageTradingDataUseCase,
                           MonitorCoinPriceUseCase monitorCoinPriceUseCase,
                           AutoTradingUseCase autoTradingUseCase,
                           ManageSettingsUseCase manageSettingsUseCase,
                           TradingAnalysisService tradingAnalysisService) {
            this.manageTradingDataUseCase = manageTradingDataUseCase;
            this.monitorCoinPriceUseCase = monitorCoinPriceUseCase;
            this.autoTradingUseCase = autoTradingUseCase;
            this.manageSettingsUseCase = manageSettingsUseCase;
            this.tradingAnalysisService = tradingAnalysisService;
            
            initializeData();
        }

        private void initializeData() {
            Log.d("KTrader", "[MainViewModel] Initializing data");
            
            // null 체크 추가
            if (manageSettingsUseCase == null) {
                Log.w("KTrader", "[MainViewModel] manageSettingsUseCase is null, skipping settings initialization");
                return;
            }
            
            if (autoTradingUseCase == null) {
                Log.w("KTrader", "[MainViewModel] autoTradingUseCase is null, skipping auto trading initialization");
                return;
            }
            
            // 설정 로드
            disposables.add(
                manageSettingsUseCase.getTradingSettings()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        settings::setValue,
                        error -> {
                            Log.e("KTrader", "[MainViewModel] Error loading settings", error);
                            errorMessage.setValue("설정을 불러오는데 실패했습니다.");
                        }
                    )
            );

            // 자동 거래 상태 관찰
            disposables.add(
                autoTradingUseCase.observeAutoTradingEnabled()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        isAutoTradingEnabled::setValue,
                        error -> Log.e("KTrader", "[MainViewModel] Error observing auto trading", error)
                    )
            );
        }

        // 코인 가격 관찰
        public void observeCoinPrice(String coinType) {
            Log.d("KTrader", "[MainViewModel] Observing coin price for: " + coinType);
            disposables.add(
                monitorCoinPriceUseCase.observeCurrentPrice(coinType)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        currentPrice::setValue,
                        error -> {
                            Log.e("KTrader", "[MainViewModel] Error observing coin price", error);
                            errorMessage.setValue("가격 정보를 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // 활성 주문 관찰
        public void observeActiveOrders() {
            Log.d("KTrader", "[MainViewModel] Observing active orders");
            disposables.add(
                manageTradingDataUseCase.observeActiveOrders()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        activeOrders::setValue,
                        error -> {
                            Log.e("KTrader", "[MainViewModel] Error observing active orders", error);
                            errorMessage.setValue("주문 정보를 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // 자동 거래 토글
        public void toggleAutoTrading() {
            Log.d("KTrader", "[MainViewModel] Toggling auto trading");
            Boolean currentState = isAutoTradingEnabled.getValue();
            if (currentState != null) {
                disposables.add(
                    autoTradingUseCase.updateAutoTradingEnabled(!currentState)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            () -> Log.d("KTrader", "[MainViewModel] Auto trading toggled successfully"),
                            error -> {
                                Log.e("KTrader", "[MainViewModel] Error toggling auto trading", error);
                                errorMessage.setValue("자동 거래 설정 변경에 실패했습니다.");
                            }
                        )
                );
            }
        }

        // 데이터 새로고침
        public void refreshData() {
            Log.d("KTrader", "[MainViewModel] Refreshing data");
            isLoading.setValue(true);
            
            disposables.add(
                manageTradingDataUseCase.getAllTrades()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        trades -> {
                            isLoading.setValue(false);
                            Log.d("KTrader", "[MainViewModel] Data refreshed successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[MainViewModel] Error refreshing data", error);
                            errorMessage.setValue("데이터 새로고침에 실패했습니다.");
                        }
                    )
            );
        }

        // Getters for LiveData
        public LiveData<CoinPriceInfo> getCurrentPrice() { return currentPrice; }
        public LiveData<List<Trade>> getActiveOrders() { return activeOrders; }
        public LiveData<Portfolio> getPortfolio() { return portfolio; }
        public LiveData<TradingSettings> getSettings() { return settings; }
        public LiveData<Boolean> getIsAutoTradingEnabled() { return isAutoTradingEnabled; }
        public LiveData<String> getErrorMessage() { return errorMessage; }
        public LiveData<Boolean> getIsLoading() { return isLoading; }

        @Override
        protected void onCleared() {
            super.onCleared();
            disposables.clear();
            Log.d("KTrader", "[MainViewModel] ViewModel cleared");
        }
    }

    /**
     * 주문 관리 ViewModel
     * 기존 PlacedOrderPage와 ProcessedOrderPage의 기능을 Clean Architecture에 맞게 개선
     */
    public static class OrderManagementViewModel extends ViewModel {
        private final ManageTradingDataUseCase manageTradingDataUseCase;
        private final TradingExecutionService tradingExecutionService;
        private final TradingAnalysisService tradingAnalysisService;
        
        private final CompositeDisposable disposables = new CompositeDisposable();
        
        // LiveData for UI
        private final MutableLiveData<List<Trade>> placedOrders = new MutableLiveData<>();
        private final MutableLiveData<List<Trade>> processedOrders = new MutableLiveData<>();
        private final MutableLiveData<List<Trade>> buyOrders = new MutableLiveData<>();
        private final MutableLiveData<List<Trade>> sellOrders = new MutableLiveData<>();
        private final MutableLiveData<TradingStatistics> statistics = new MutableLiveData<>();
        private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
        private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

        public OrderManagementViewModel(ManageTradingDataUseCase manageTradingDataUseCase,
                                      TradingExecutionService tradingExecutionService,
                                      TradingAnalysisService tradingAnalysisService) {
            this.manageTradingDataUseCase = manageTradingDataUseCase;
            this.tradingExecutionService = tradingExecutionService;
            this.tradingAnalysisService = tradingAnalysisService;
            
            initializeData();
        }

        private void initializeData() {
            Log.d("KTrader", "[OrderManagementViewModel] Initializing data");
            
            // null 체크 추가
            if (manageTradingDataUseCase == null) {
                Log.w("KTrader", "[OrderManagementViewModel] manageTradingDataUseCase is null, skipping initialization");
                return;
            }
            
            // 대기 중인 주문 관찰
            disposables.add(
                manageTradingDataUseCase.observeAllTrades()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        trades -> {
                            List<Trade> placed = new java.util.ArrayList<>();
                            List<Trade> processed = new java.util.ArrayList<>();
                            List<Trade> buy = new java.util.ArrayList<>();
                            List<Trade> sell = new java.util.ArrayList<>();
                            
                            for (Trade trade : trades) {
                                if (trade.isPending()) {
                                    placed.add(trade);
                                } else if (trade.isCompleted()) {
                                    processed.add(trade);
                                }
                                
                                if (trade.isBuyOrder()) {
                                    buy.add(trade);
                                } else if (trade.isSellOrder()) {
                                    sell.add(trade);
                                }
                            }
                            
                            placedOrders.setValue(placed);
                            processedOrders.setValue(processed);
                            buyOrders.setValue(buy);
                            sellOrders.setValue(sell);
                        },
                        error -> {
                            Log.e("KTrader", "[OrderManagementViewModel] Error observing trades", error);
                            errorMessage.setValue("주문 정보를 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // 매수 주문 실행
        public void executeBuyOrder(String coinType, int price, double units) {
            Log.d("KTrader", "[OrderManagementViewModel] Executing buy order");
            isLoading.setValue(true);
            
            disposables.add(
                tradingExecutionService.executeBuyOrder(coinType, price, units)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        trade -> {
                            isLoading.setValue(false);
                            Log.d("KTrader", "[OrderManagementViewModel] Buy order executed successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[OrderManagementViewModel] Error executing buy order", error);
                            errorMessage.setValue("매수 주문 실행에 실패했습니다.");
                        }
                    )
            );
        }

        // 매도 주문 실행
        public void executeSellOrder(String coinType, int price, double units) {
            Log.d("KTrader", "[OrderManagementViewModel] Executing sell order");
            isLoading.setValue(true);
            
            disposables.add(
                tradingExecutionService.executeSellOrder(coinType, price, units)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        trade -> {
                            isLoading.setValue(false);
                            Log.d("KTrader", "[OrderManagementViewModel] Sell order executed successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[OrderManagementViewModel] Error executing sell order", error);
                            errorMessage.setValue("매도 주문 실행에 실패했습니다.");
                        }
                    )
            );
        }

        // 시장가 매수
        public void executeMarketBuyOrder(String coinType, double units) {
            Log.d("KTrader", "[OrderManagementViewModel] Executing market buy order");
            isLoading.setValue(true);
            
            disposables.add(
                tradingExecutionService.executeMarketBuyOrder(coinType, units)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        trade -> {
                            isLoading.setValue(false);
                            Log.d("KTrader", "[OrderManagementViewModel] Market buy order executed successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[OrderManagementViewModel] Error executing market buy order", error);
                            errorMessage.setValue("시장가 매수 실행에 실패했습니다.");
                        }
                    )
            );
        }

        // 시장가 매도
        public void executeMarketSellOrder(String coinType, double units) {
            Log.d("KTrader", "[OrderManagementViewModel] Executing market sell order");
            isLoading.setValue(true);
            
            disposables.add(
                tradingExecutionService.executeMarketSellOrder(coinType, units)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        trade -> {
                            isLoading.setValue(false);
                            Log.d("KTrader", "[OrderManagementViewModel] Market sell order executed successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[OrderManagementViewModel] Error executing market sell order", error);
                            errorMessage.setValue("시장가 매도 실행에 실패했습니다.");
                        }
                    )
            );
        }

        // 주문 취소
        public void cancelOrder(String orderId) {
            Log.d("KTrader", "[OrderManagementViewModel] Cancelling order: " + orderId);
            
            disposables.add(
                tradingExecutionService.cancelOrder(orderId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> Log.d("KTrader", "[OrderManagementViewModel] Order cancelled successfully"),
                        error -> {
                            Log.e("KTrader", "[OrderManagementViewModel] Error cancelling order", error);
                            errorMessage.setValue("주문 취소에 실패했습니다.");
                        }
                    )
            );
        }

        // 모든 주문 취소
        public void cancelAllOrders(String coinType) {
            Log.d("KTrader", "[OrderManagementViewModel] Cancelling all orders for: " + coinType);
            
            disposables.add(
                tradingExecutionService.cancelAllOrders(coinType)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> Log.d("KTrader", "[OrderManagementViewModel] All orders cancelled successfully"),
                        error -> {
                            Log.e("KTrader", "[OrderManagementViewModel] Error cancelling all orders", error);
                            errorMessage.setValue("모든 주문 취소에 실패했습니다.");
                        }
                    )
            );
        }

        // 거래 통계 로드
        public void loadTradingStatistics(long startTime, long endTime) {
            Log.d("KTrader", "[OrderManagementViewModel] Loading trading statistics");
            isLoading.setValue(true);
            
            disposables.add(
                manageTradingDataUseCase.getTradingStatistics(startTime, endTime)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        stats -> {
                            statistics.setValue(stats);
                            isLoading.setValue(false);
                            Log.d("KTrader", "[OrderManagementViewModel] Trading statistics loaded successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[OrderManagementViewModel] Error loading trading statistics", error);
                            errorMessage.setValue("거래 통계를 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // Getters for LiveData
        public LiveData<List<Trade>> getPlacedOrders() { return placedOrders; }
        public LiveData<List<Trade>> getProcessedOrders() { return processedOrders; }
        public LiveData<List<Trade>> getBuyOrders() { return buyOrders; }
        public LiveData<List<Trade>> getSellOrders() { return sellOrders; }
        public LiveData<TradingStatistics> getStatistics() { return statistics; }
        public LiveData<String> getErrorMessage() { return errorMessage; }
        public LiveData<Boolean> getIsLoading() { return isLoading; }

        @Override
        protected void onCleared() {
            super.onCleared();
            disposables.clear();
            Log.d("KTrader", "[OrderManagementViewModel] ViewModel cleared");
        }
    }

    /**
     * 포트폴리오 ViewModel
     */
    public static class PortfolioViewModel extends ViewModel {
        private final ManagePortfolioUseCase managePortfolioUseCase;
        private final TradingAnalysisService tradingAnalysisService;
        
        private final CompositeDisposable disposables = new CompositeDisposable();
        
        // LiveData for UI
        private final MutableLiveData<Portfolio> currentPortfolio = new MutableLiveData<>();
        private final MutableLiveData<List<Portfolio>> portfolioHistory = new MutableLiveData<>();
        private final MutableLiveData<PortfolioManagementService.PortfolioAnalysis> portfolioAnalysis = new MutableLiveData<>();
        private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
        private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

        public PortfolioViewModel(ManagePortfolioUseCase managePortfolioUseCase,
                                TradingAnalysisService tradingAnalysisService) {
            this.managePortfolioUseCase = managePortfolioUseCase;
            this.tradingAnalysisService = tradingAnalysisService;
            
            initializeData();
        }

        private void initializeData() {
            Log.d("KTrader", "[PortfolioViewModel] Initializing data");
            
            // null 체크 추가
            if (managePortfolioUseCase == null) {
                Log.w("KTrader", "[PortfolioViewModel] managePortfolioUseCase is null, skipping initialization");
                return;
            }
            
            // 현재 포트폴리오 관찰
            disposables.add(
                managePortfolioUseCase.observeCurrentPortfolio()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        currentPortfolio::setValue,
                        error -> {
                            Log.e("KTrader", "[PortfolioViewModel] Error observing portfolio", error);
                            errorMessage.setValue("포트폴리오 정보를 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // 포트폴리오 업데이트
        public void updatePortfolio(String coinType) {
            Log.d("KTrader", "[PortfolioViewModel] Updating portfolio for: " + coinType);
            isLoading.setValue(true);
            
            disposables.add(
                managePortfolioUseCase.updatePortfolio(coinType)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isLoading.setValue(false);
                            Log.d("KTrader", "[PortfolioViewModel] Portfolio updated successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[PortfolioViewModel] Error updating portfolio", error);
                            errorMessage.setValue("포트폴리오 업데이트에 실패했습니다.");
                        }
                    )
            );
        }

        // 포트폴리오 분석
        public void analyzePortfolio(String coinType) {
            Log.d("KTrader", "[PortfolioViewModel] Analyzing portfolio for: " + coinType);
            isLoading.setValue(true);
            
            // TODO: 기존 구조와 연동 후 구현
            isLoading.setValue(false);
            Log.d("KTrader", "[PortfolioViewModel] Portfolio analysis completed");
        }

        // 포트폴리오 히스토리 로드
        public void loadPortfolioHistory(long startTime, long endTime) {
            Log.d("KTrader", "[PortfolioViewModel] Loading portfolio history");
            isLoading.setValue(true);
            
            disposables.add(
                managePortfolioUseCase.getPortfolioHistory(startTime, endTime)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        history -> {
                            portfolioHistory.setValue(history);
                            isLoading.setValue(false);
                            Log.d("KTrader", "[PortfolioViewModel] Portfolio history loaded successfully");
                        },
                        error -> {
                            isLoading.setValue(false);
                            Log.e("KTrader", "[PortfolioViewModel] Error loading portfolio history", error);
                            errorMessage.setValue("포트폴리오 히스토리를 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // Getters for LiveData
        public LiveData<Portfolio> getCurrentPortfolio() { return currentPortfolio; }
        public LiveData<List<Portfolio>> getPortfolioHistory() { return portfolioHistory; }
        public LiveData<PortfolioManagementService.PortfolioAnalysis> getPortfolioAnalysis() { return portfolioAnalysis; }
        public LiveData<String> getErrorMessage() { return errorMessage; }
        public LiveData<Boolean> getIsLoading() { return isLoading; }

        @Override
        protected void onCleared() {
            super.onCleared();
            disposables.clear();
            Log.d("KTrader", "[PortfolioViewModel] ViewModel cleared");
        }
    }

    /**
     * 설정 관리 ViewModel
     * 기존 SettingsActivity의 기능을 Clean Architecture에 맞게 개선
     */
    public static class SettingsViewModel extends ViewModel {
        private final ManageSettingsUseCase manageSettingsUseCase;
        
        private final CompositeDisposable disposables = new CompositeDisposable();
        
        // LiveData for UI
        private final MutableLiveData<TradingSettings> settings = new MutableLiveData<>();
        private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
        private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
        private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>();

        public SettingsViewModel(ManageSettingsUseCase manageSettingsUseCase) {
            this.manageSettingsUseCase = manageSettingsUseCase;
            
            initializeData();
        }

        private void initializeData() {
            Log.d("KTrader", "[SettingsViewModel] Initializing data");
            
            // null 체크 추가
            if (manageSettingsUseCase == null) {
                Log.w("KTrader", "[SettingsViewModel] manageSettingsUseCase is null, skipping settings initialization");
                return;
            }
            
            // 설정 관찰
            disposables.add(
                manageSettingsUseCase.observeTradingSettings()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        settings::setValue,
                        error -> {
                            Log.e("KTrader", "[SettingsViewModel] Error observing settings", error);
                            errorMessage.setValue("설정을 불러오는데 실패했습니다.");
                        }
                    )
            );
        }

        // 설정 저장
        public void saveSettings(TradingSettings settings) {
            Log.d("KTrader", "[SettingsViewModel] Saving settings");
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.saveTradingSettings(settings)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] Settings saved successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error saving settings", error);
                            errorMessage.setValue("설정 저장에 실패했습니다.");
                        }
                    )
            );
        }

        // API 자격 증명 업데이트
        public void updateApiCredentials(String apiKey, String apiSecret) {
            Log.d("KTrader", "[SettingsViewModel] Updating API credentials");
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.updateApiCredentials(apiKey, apiSecret)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] API credentials updated successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error updating API credentials", error);
                            errorMessage.setValue("API 자격 증명 업데이트에 실패했습니다.");
                        }
                    )
            );
        }

        // 단위 가격 업데이트
        public void updateUnitPrice(int unitPrice) {
            Log.d("KTrader", "[SettingsViewModel] Updating unit price: " + unitPrice);
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.updateUnitPrice(unitPrice)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] Unit price updated successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error updating unit price", error);
                            errorMessage.setValue("단위 가격 업데이트에 실패했습니다.");
                        }
                    )
            );
        }

        // 거래 간격 업데이트
        public void updateTradeInterval(int tradeInterval) {
            Log.d("KTrader", "[SettingsViewModel] Updating trade interval: " + tradeInterval);
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.updateTradeInterval(tradeInterval)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] Trade interval updated successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error updating trade interval", error);
                            errorMessage.setValue("거래 간격 업데이트에 실패했습니다.");
                        }
                    )
            );
        }

        // 수익률 업데이트
        public void updateEarningRate(float earningRate) {
            Log.d("KTrader", "[SettingsViewModel] Updating earning rate: " + earningRate);
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.updateEarningRate(earningRate)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] Earning rate updated successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error updating earning rate", error);
                            errorMessage.setValue("수익률 업데이트에 실패했습니다.");
                        }
                    )
            );
        }

        // 코인 타입 업데이트
        public void updateCoinType(String coinType) {
            Log.d("KTrader", "[SettingsViewModel] Updating coin type: " + coinType);
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.updateCoinType(coinType)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] Coin type updated successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error updating coin type", error);
                            errorMessage.setValue("코인 타입 업데이트에 실패했습니다.");
                        }
                    )
            );
        }

        // 설정 초기화
        public void resetToDefaults() {
            Log.d("KTrader", "[SettingsViewModel] Resetting to defaults");
            isSaving.setValue(true);
            
            disposables.add(
                manageSettingsUseCase.resetToDefaults()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> {
                            isSaving.setValue(false);
                            Log.d("KTrader", "[SettingsViewModel] Settings reset to defaults successfully");
                        },
                        error -> {
                            isSaving.setValue(false);
                            Log.e("KTrader", "[SettingsViewModel] Error resetting to defaults", error);
                            errorMessage.setValue("설정 초기화에 실패했습니다.");
                        }
                    )
            );
        }

        // 설정 유효성 검사
        public void validateConfiguration() {
            Log.d("KTrader", "[SettingsViewModel] Validating configuration");
            
            disposables.add(
                manageSettingsUseCase.isValidConfiguration()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        isValid -> {
                            if (!isValid) {
                                errorMessage.setValue("설정이 유효하지 않습니다. 모든 필드를 올바르게 입력해주세요.");
                            }
                        },
                        error -> {
                            Log.e("KTrader", "[SettingsViewModel] Error validating configuration", error);
                            errorMessage.setValue("설정 유효성 검사에 실패했습니다.");
                        }
                    )
            );
        }

        // Getters for LiveData
        public LiveData<TradingSettings> getSettings() { return settings; }
        public LiveData<String> getErrorMessage() { return errorMessage; }
        public LiveData<Boolean> getIsLoading() { return isLoading; }
        public LiveData<Boolean> getIsSaving() { return isSaving; }

        @Override
        protected void onCleared() {
            super.onCleared();
            disposables.clear();
            Log.d("KTrader", "[SettingsViewModel] ViewModel cleared");
        }
    }
}
