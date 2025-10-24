package com.example.k_trader.domain.service;

import android.util.Log;
import com.example.k_trader.domain.model.DomainModels.*;
import com.example.k_trader.domain.repository.RepositoryInterfaces.*;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Domain Services for K-Trader App
 * 
 * Clean Architecture의 Domain Layer에 속하는 복잡한 비즈니스 로직과 도메인 규칙 처리
 * 여러 Repository를 조합한 복합적인 비즈니스 로직
 * 거래 분석, 실행, 포트폴리오 관리 서비스
 * 
 * @author K-Trader Team
 * @version 1.0
 */
public class DomainServices {

    /**
     * 거래 분석 서비스
     * 기존 TradeJobService의 분석 로직을 Clean Architecture에 맞게 개선
     */
    public static class TradingAnalysisService {
        private final TradeRepository tradeRepository;
        private final CoinPriceRepository coinPriceRepository;
        private final BalanceRepository balanceRepository;

        public TradingAnalysisService(TradeRepository tradeRepository, CoinPriceRepository coinPriceRepository,
                                    BalanceRepository balanceRepository) {
            this.tradeRepository = tradeRepository;
            this.coinPriceRepository = coinPriceRepository;
            this.balanceRepository = balanceRepository;
        }

        // 가격 분석
        public Single<PriceAnalysisResult> analyzePriceTrend(String coinType, int timeWindowMinutes) {
            Log.d("KTrader", "[TradingAnalysisService] Analyzing price trend for: " + coinType);
            return coinPriceRepository.getPriceChangesInPeriod(coinType, timeWindowMinutes * 60 * 1000L)
                .map(priceChanges -> {
                    if (priceChanges.isEmpty()) {
                        return new PriceAnalysisResult(PriceTrend.STABLE, 0.0, 0.0);
                    }

                    double totalChange = 0.0;
                    double volatility = 0.0;
                    int increasingCount = 0;
                    int decreasingCount = 0;

                    for (CoinPriceInfo price : priceChanges) {
                        totalChange += price.getDailyChangeRate();
                        volatility += Math.abs(price.getDailyChangeRate());
                        
                        if (price.getDailyChangeRate() > 0) {
                            increasingCount++;
                        } else if (price.getDailyChangeRate() < 0) {
                            decreasingCount++;
                        }
                    }

                    double averageChange = totalChange / priceChanges.size();
                    double averageVolatility = volatility / priceChanges.size();

                    PriceTrend trend;
                    if (increasingCount > decreasingCount * 1.5) {
                        trend = PriceTrend.INCREASING;
                    } else if (decreasingCount > increasingCount * 1.5) {
                        trend = PriceTrend.DECREASING;
                    } else {
                        trend = PriceTrend.STABLE;
                    }

                    return new PriceAnalysisResult(trend, averageChange, averageVolatility);
                });
        }

        // 거래 패턴 분석
        public Single<TradingPatternAnalysis> analyzeTradingPattern(String coinType, long periodMillis) {
            Log.d("KTrader", "[TradingAnalysisService] Analyzing trading pattern for: " + coinType);
            return Single.zip(
                tradeRepository.getTradesByCoinType(coinType),
                tradeRepository.getTradingStatisticsByCoinType(coinType, System.currentTimeMillis() - periodMillis, System.currentTimeMillis()),
                (trades, statistics) -> {
                    List<Trade> buyTrades = new ArrayList<>();
                    List<Trade> sellTrades = new ArrayList<>();

                    for (Trade trade : trades) {
                        if (trade.isBuyOrder()) {
                            buyTrades.add(trade);
                        } else if (trade.isSellOrder()) {
                            sellTrades.add(trade);
                        }
                    }

                    return new TradingPatternAnalysis(
                        buyTrades.size(),
                        sellTrades.size(),
                        statistics.getWinRate(),
                        statistics.getNetProfit(),
                        calculateAverageTradeSize(trades),
                        calculateTradeFrequency(trades, periodMillis)
                    );
                }
            );
        }

        // 수익성 분석
        public Single<ProfitabilityAnalysis> analyzeProfitability(String coinType, long periodMillis) {
            Log.d("KTrader", "[TradingAnalysisService] Analyzing profitability for: " + coinType);
            return tradeRepository.getTradingStatisticsByCoinType(coinType, System.currentTimeMillis() - periodMillis, System.currentTimeMillis())
                .map(statistics -> {
                    double riskScore = calculateRiskScore(statistics);
                    double profitScore = calculateProfitScore(statistics);
                    ProfitabilityLevel level = determineProfitabilityLevel(statistics);

                    return new ProfitabilityAnalysis(
                        statistics.getNetProfit(),
                        statistics.getWinRate(),
                        riskScore,
                        profitScore,
                        level
                    );
                });
        }

        // 시장 상황 분석
        public Single<MarketConditionAnalysis> analyzeMarketCondition(String coinType) {
            Log.d("KTrader", "[TradingAnalysisService] Analyzing market condition for: " + coinType);
            return Single.zip(
                coinPriceRepository.getCurrentPrice(coinType),
                coinPriceRepository.calculatePriceVolatility(coinType, 24 * 60 * 60 * 1000L), // 24시간
                balanceRepository.getBalanceByCoinType(coinType),
                (priceInfo, volatility, balanceInfo) -> {
                    MarketCondition condition;
                    if (volatility > 10.0) {
                        condition = MarketCondition.HIGH_VOLATILITY;
                    } else if (volatility < 2.0) {
                        condition = MarketCondition.LOW_VOLATILITY;
                    } else {
                        condition = MarketCondition.NORMAL;
                    }

                    return new MarketConditionAnalysis(
                        condition,
                        volatility,
                        priceInfo.getCurrentPrice(),
                        balanceInfo.getAvailableBalance(),
                        priceInfo.isPriceIncreased()
                    );
                }
            );
        }

        // 헬퍼 메서드들
        private double calculateAverageTradeSize(List<Trade> trades) {
            if (trades.isEmpty()) return 0.0;
            
            double totalValue = 0.0;
            for (Trade trade : trades) {
                totalValue += trade.calculateTotalValue();
            }
            return totalValue / trades.size();
        }

        private double calculateTradeFrequency(List<Trade> trades, long periodMillis) {
            if (trades.isEmpty()) return 0.0;
            
            long timeSpan = periodMillis;
            return (double) trades.size() / (timeSpan / (60 * 60 * 1000.0)); // 거래/시간
        }

        private double calculateRiskScore(TradingStatistics statistics) {
            if (statistics.getTotalTrades() == 0) return 0.0;
            
            double lossRate = (double) statistics.getFailedTrades() / statistics.getTotalTrades();
            double averageLoss = statistics.getAverageLoss();
            
            return (lossRate * 0.6) + (averageLoss * 0.4);
        }

        private double calculateProfitScore(TradingStatistics statistics) {
            if (statistics.getTotalTrades() == 0) return 0.0;
            
            double winRate = statistics.getWinRate() / 100.0;
            double averageProfit = statistics.getAverageProfit();
            
            return (winRate * 0.7) + (averageProfit * 0.3);
        }

        private ProfitabilityLevel determineProfitabilityLevel(TradingStatistics statistics) {
            if (statistics.getNetProfit() > 10.0 && statistics.getWinRate() > 70.0) {
                return ProfitabilityLevel.HIGH;
            } else if (statistics.getNetProfit() > 0.0 && statistics.getWinRate() > 50.0) {
                return ProfitabilityLevel.MEDIUM;
            } else if (statistics.getNetProfit() > -5.0) {
                return ProfitabilityLevel.LOW;
            } else {
                return ProfitabilityLevel.NEGATIVE;
            }
        }

        // 분석 결과 클래스들
        public static class PriceAnalysisResult {
            private final PriceTrend trend;
            private final double averageChange;
            private final double volatility;

            public PriceAnalysisResult(PriceTrend trend, double averageChange, double volatility) {
                this.trend = trend;
                this.averageChange = averageChange;
                this.volatility = volatility;
            }

            public PriceTrend getTrend() { return trend; }
            public double getAverageChange() { return averageChange; }
            public double getVolatility() { return volatility; }
        }

        public static class TradingPatternAnalysis {
            private final int buyCount;
            private final int sellCount;
            private final double winRate;
            private final double netProfit;
            private final double averageTradeSize;
            private final double tradeFrequency;

            public TradingPatternAnalysis(int buyCount, int sellCount, double winRate, double netProfit,
                                        double averageTradeSize, double tradeFrequency) {
                this.buyCount = buyCount;
                this.sellCount = sellCount;
                this.winRate = winRate;
                this.netProfit = netProfit;
                this.averageTradeSize = averageTradeSize;
                this.tradeFrequency = tradeFrequency;
            }

            public int getBuyCount() { return buyCount; }
            public int getSellCount() { return sellCount; }
            public double getWinRate() { return winRate; }
            public double getNetProfit() { return netProfit; }
            public double getAverageTradeSize() { return averageTradeSize; }
            public double getTradeFrequency() { return tradeFrequency; }
        }

        public static class ProfitabilityAnalysis {
            private final double netProfit;
            private final double winRate;
            private final double riskScore;
            private final double profitScore;
            private final ProfitabilityLevel level;

            public ProfitabilityAnalysis(double netProfit, double winRate, double riskScore, double profitScore, ProfitabilityLevel level) {
                this.netProfit = netProfit;
                this.winRate = winRate;
                this.riskScore = riskScore;
                this.profitScore = profitScore;
                this.level = level;
            }

            public double getNetProfit() { return netProfit; }
            public double getWinRate() { return winRate; }
            public double getRiskScore() { return riskScore; }
            public double getProfitScore() { return profitScore; }
            public ProfitabilityLevel getLevel() { return level; }
        }

        public static class MarketConditionAnalysis {
            private final MarketCondition condition;
            private final double volatility;
            private final int currentPrice;
            private final double availableBalance;
            private final boolean isPriceIncreasing;

            public MarketConditionAnalysis(MarketCondition condition, double volatility, int currentPrice,
                                         double availableBalance, boolean isPriceIncreasing) {
                this.condition = condition;
                this.volatility = volatility;
                this.currentPrice = currentPrice;
                this.availableBalance = availableBalance;
                this.isPriceIncreasing = isPriceIncreasing;
            }

            public MarketCondition getCondition() { return condition; }
            public double getVolatility() { return volatility; }
            public int getCurrentPrice() { return currentPrice; }
            public double getAvailableBalance() { return availableBalance; }
            public boolean isPriceIncreasing() { return isPriceIncreasing; }
        }

        // 열거형들
        public enum PriceTrend {
            INCREASING("상승"),
            DECREASING("하락"),
            STABLE("안정");

            private final String description;

            PriceTrend(String description) {
                this.description = description;
            }

            public String getDescription() { return description; }
        }

        public enum ProfitabilityLevel {
            HIGH("높음"),
            MEDIUM("보통"),
            LOW("낮음"),
            NEGATIVE("음수");

            private final String description;

            ProfitabilityLevel(String description) {
                this.description = description;
            }

            public String getDescription() { return description; }
        }

        public enum MarketCondition {
            HIGH_VOLATILITY("고변동성"),
            LOW_VOLATILITY("저변동성"),
            NORMAL("정상");

            private final String description;

            MarketCondition(String description) {
                this.description = description;
            }

            public String getDescription() { return description; }
        }
    }

    /**
     * 거래 실행 서비스
     * 기존 OrderManager의 기능을 Clean Architecture에 맞게 개선
     */
    public static class TradingExecutionService {
        private final TradeRepository tradeRepository;
        private final BalanceRepository balanceRepository;
        private final SettingsRepository settingsRepository;
        private final NotificationRepository notificationRepository;

        public TradingExecutionService(TradeRepository tradeRepository, BalanceRepository balanceRepository,
                                     SettingsRepository settingsRepository, NotificationRepository notificationRepository) {
            this.tradeRepository = tradeRepository;
            this.balanceRepository = balanceRepository;
            this.settingsRepository = settingsRepository;
            this.notificationRepository = notificationRepository;
        }

        // 매수 주문 실행
        public Single<Trade> executeBuyOrder(String coinType, int price, double units) {
            Log.d("KTrader", "[TradingExecutionService] Executing buy order for: " + coinType);
            return validateBuyOrder(coinType, price, units)
                .flatMap(valid -> {
                    if (!valid) {
                        return Single.error(new TradingException("Invalid buy order parameters"));
                    }
                    return createBuyOrder(coinType, price, units);
                })
                .flatMap(this::processBuyOrder);
        }

        // 매도 주문 실행
        public Single<Trade> executeSellOrder(String coinType, int price, double units) {
            Log.d("KTrader", "[TradingExecutionService] Executing sell order for: " + coinType);
            return validateSellOrder(coinType, price, units)
                .flatMap(valid -> {
                    if (!valid) {
                        return Single.error(new TradingException("Invalid sell order parameters"));
                    }
                    return createSellOrder(coinType, price, units);
                })
                .flatMap(this::processSellOrder);
        }

        // 시장가 매수
        public Single<Trade> executeMarketBuyOrder(String coinType, double units) {
            Log.d("KTrader", "[TradingExecutionService] Executing market buy order for: " + coinType);
            return getCurrentPrice(coinType)
                .flatMap(priceInfo -> executeBuyOrder(coinType, priceInfo.getCurrentPrice(), units));
        }

        // 시장가 매도
        public Single<Trade> executeMarketSellOrder(String coinType, double units) {
            Log.d("KTrader", "[TradingExecutionService] Executing market sell order for: " + coinType);
            return getCurrentPrice(coinType)
                .flatMap(priceInfo -> executeSellOrder(coinType, priceInfo.getCurrentPrice(), units));
        }

        // 주문 취소
        public Completable cancelOrder(String orderId) {
            Log.d("KTrader", "[TradingExecutionService] Cancelling order: " + orderId);
            return tradeRepository.getTradeById(orderId)
                .flatMapCompletable(trade -> {
                    trade.setStatus(Trade.Status.CANCELLED);
                    return tradeRepository.updateTrade(trade)
                        .andThen(Completable.fromAction(() -> {
                            // TODO: 알림 기능 구현
                            // notificationRepository.sendInfoNotification("주문 취소", "주문이 취소되었습니다.");
                        }));
                });
        }

        // 모든 주문 취소
        public Completable cancelAllOrders(String coinType) {
            Log.d("KTrader", "[TradingExecutionService] Cancelling all orders for: " + coinType);
            return tradeRepository.getTradesByCoinType(coinType)
                .flatMapCompletable(trades -> {
                    List<Completable> cancelTasks = new ArrayList<>();
                    for (Trade trade : trades) {
                        if (trade.isPending()) {
                            trade.setStatus(Trade.Status.CANCELLED);
                            cancelTasks.add(tradeRepository.updateTrade(trade));
                        }
                    }
                    return Completable.merge(cancelTasks);
                });
        }

        // 주문 상태 업데이트
        public Completable updateOrderStatus(String orderId, Trade.Status status) {
            Log.d("KTrader", "[TradingExecutionService] Updating order status: " + orderId);
            return tradeRepository.getTradeById(orderId)
                .flatMapCompletable(trade -> {
                    trade.setStatus(status);
                    return tradeRepository.updateTrade(trade)
                        .andThen(createStatusNotification(trade, status));
                });
        }

        // 비즈니스 로직 검증
        private Single<Boolean> validateBuyOrder(String coinType, int price, double units) {
            return Single.zip(
                balanceRepository.getKrwBalance(),
                settingsRepository.getTradingSettings(),
                (krwBalance, settings) -> {
                    double requiredAmount = price * units;
                    return krwBalance.getAvailableBalance() >= requiredAmount &&
                           settings.isTradeAmountValid(units) &&
                           price > 0 && units > 0;
                }
            );
        }

        private Single<Boolean> validateSellOrder(String coinType, int price, double units) {
            return Single.zip(
                balanceRepository.getBalanceByCoinType(coinType),
                settingsRepository.getTradingSettings(),
                (coinBalance, settings) -> {
                    return coinBalance.getAvailableBalance() >= units &&
                           settings.isTradeAmountValid(units) &&
                           price > 0 && units > 0;
                }
            );
        }

        // 주문 생성
        private Single<Trade> createBuyOrder(String coinType, int price, double units) {
            return Single.fromCallable(() -> {
                Trade trade = new Trade();
                trade.setType(Trade.Type.BUY);
                trade.setCoinType(coinType);
                trade.setPrice(price);
                trade.setUnits(units);
                trade.setStatus(Trade.Status.PLACED);
                return trade;
            });
        }

        private Single<Trade> createSellOrder(String coinType, int price, double units) {
            return Single.fromCallable(() -> {
                Trade trade = new Trade();
                trade.setType(Trade.Type.SELL);
                trade.setCoinType(coinType);
                trade.setPrice(price);
                trade.setUnits(units);
                trade.setStatus(Trade.Status.PLACED);
                return trade;
            });
        }

        // 주문 처리
        private Single<Trade> processBuyOrder(Trade trade) {
            return tradeRepository.saveTrade(trade)
                .andThen(Single.just(trade))
                .doOnSuccess(savedTrade -> {
                    Log.d("KTrader", "[TradingExecutionService] Buy order processed: " + savedTrade.getId());
                    notificationRepository.sendTradeNotification(savedTrade).subscribe();
                });
        }

        private Single<Trade> processSellOrder(Trade trade) {
            return tradeRepository.saveTrade(trade)
                .andThen(Single.just(trade))
                .doOnSuccess(savedTrade -> {
                    Log.d("KTrader", "[TradingExecutionService] Sell order processed: " + savedTrade.getId());
                    notificationRepository.sendTradeNotification(savedTrade).subscribe();
                });
        }

        // 헬퍼 메서드들
        private Single<CoinPriceInfo> getCurrentPrice(String coinType) {
            // TODO: 기존 구조와 연동 후 구현
            return Single.just(new CoinPriceInfo(coinType, 0, 0.0, 0.0));
        }

        private Completable createStatusNotification(Trade trade, Trade.Status status) {
            String message = String.format("%s 주문이 %s 상태로 변경되었습니다.", 
                trade.getType().getDescription(), status.getDescription());
            // TODO: 알림 기능 구현
            return Completable.complete();
        }

        // 커스텀 예외
        public static class TradingException extends Exception {
            public TradingException(String message) {
                super(message);
            }
        }
    }

    /**
     * 포트폴리오 관리 서비스
     */
    public static class PortfolioManagementService {
        private final PortfolioRepository portfolioRepository;
        private final TradeRepository tradeRepository;
        private final BalanceRepository balanceRepository;
        private final CoinPriceRepository coinPriceRepository;

        public PortfolioManagementService(PortfolioRepository portfolioRepository, TradeRepository tradeRepository,
                                       BalanceRepository balanceRepository, CoinPriceRepository coinPriceRepository) {
            this.portfolioRepository = portfolioRepository;
            this.tradeRepository = tradeRepository;
            this.balanceRepository = balanceRepository;
            this.coinPriceRepository = coinPriceRepository;
        }

        // 포트폴리오 업데이트
        public Completable updatePortfolio(String coinType) {
            Log.d("KTrader", "[PortfolioManagementService] Updating portfolio for: " + coinType);
            return Single.zip(
                balanceRepository.getKrwBalance(),
                balanceRepository.getBalanceByCoinType(coinType),
                coinPriceRepository.getCurrentPrice(coinType),
                tradeRepository.getActiveOrderCount(Trade.Type.BUY),
                tradeRepository.getActiveOrderCount(Trade.Type.SELL),
                (krwBalance, coinBalance, priceInfo, buyOrders, sellOrders) -> {
                    Portfolio portfolio = new Portfolio();
                    portfolio.updatePortfolio(
                        krwBalance.getAvailableBalance(),
                        coinBalance.getAvailableBalance(),
                        priceInfo.getCurrentPrice()
                    );
                    portfolio.setActiveBuyOrders(buyOrders);
                    portfolio.setActiveSellOrders(sellOrders);
                    
                    // 미실현 손익 계산
                    calculateUnrealizedProfit(portfolio, coinType);
                    
                    return portfolio;
                }
            ).flatMapCompletable(portfolioRepository::updatePortfolio);
        }

        // 포트폴리오 분석
        public Single<PortfolioAnalysis> analyzePortfolio(String coinType) {
            Log.d("KTrader", "[PortfolioManagementService] Analyzing portfolio for: " + coinType);
            return Single.zip(
                portfolioRepository.getCurrentPortfolio(),
                tradeRepository.getTradingStatistics(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L), System.currentTimeMillis()),
                (portfolio, statistics) -> {
                    double diversificationScore = calculateDiversificationScore(portfolio);
                    double riskScore = calculatePortfolioRiskScore(portfolio, statistics);
                    double performanceScore = calculatePerformanceScore(portfolio, statistics);
                    
                    return new PortfolioAnalysis(
                        portfolio,
                        diversificationScore,
                        riskScore,
                        performanceScore,
                        generateRecommendations(portfolio, statistics)
                    );
                }
            );
        }

        // 포트폴리오 최적화
        public Single<PortfolioOptimization> optimizePortfolio(String coinType) {
            Log.d("KTrader", "[PortfolioManagementService] Optimizing portfolio for: " + coinType);
            return analyzePortfolio(coinType)
                .map(analysis -> {
                    List<String> recommendations = analysis.getRecommendations();
                    List<String> optimizations = new ArrayList<>();
                    
                    if (analysis.getRiskScore() > 0.7) {
                        optimizations.add("리스크를 줄이기 위해 포지션 크기를 줄이세요.");
                    }
                    
                    if (analysis.getPerformanceScore() < 0.3) {
                        optimizations.add("성과를 개선하기 위해 거래 전략을 재검토하세요.");
                    }
                    
                    if (analysis.getDiversificationScore() < 0.5) {
                        optimizations.add("다양화를 위해 다른 코인도 고려해보세요.");
                    }
                    
                    return new PortfolioOptimization(analysis.getPortfolio(), optimizations);
                });
        }

        // 헬퍼 메서드들
        private void calculateUnrealizedProfit(Portfolio portfolio, String coinType) {
            // 미실현 손익 계산 로직
            // 실제 구현에서는 현재 보유 코인과 평균 매수가를 비교
        }

        private double calculateDiversificationScore(Portfolio portfolio) {
            // 다양화 점수 계산 로직
            return 0.5; // 임시 값
        }

        private double calculatePortfolioRiskScore(Portfolio portfolio, TradingStatistics statistics) {
            // 포트폴리오 리스크 점수 계산 로직
            return 0.3; // 임시 값
        }

        private double calculatePerformanceScore(Portfolio portfolio, TradingStatistics statistics) {
            // 성과 점수 계산 로직
            return 0.7; // 임시 값
        }

        private List<String> generateRecommendations(Portfolio portfolio, TradingStatistics statistics) {
            List<String> recommendations = new ArrayList<>();
            
            if (statistics.getWinRate() < 50.0) {
                recommendations.add("승률을 높이기 위해 거래 전략을 개선하세요.");
            }
            
            if (statistics.getNetProfit() < 0) {
                recommendations.add("손실을 줄이기 위해 리스크 관리를 강화하세요.");
            }
            
            return recommendations;
        }

        // 분석 결과 클래스들
        public static class PortfolioAnalysis {
            private final Portfolio portfolio;
            private final double diversificationScore;
            private final double riskScore;
            private final double performanceScore;
            private final List<String> recommendations;

            public PortfolioAnalysis(Portfolio portfolio, double diversificationScore, double riskScore,
                                   double performanceScore, List<String> recommendations) {
                this.portfolio = portfolio;
                this.diversificationScore = diversificationScore;
                this.riskScore = riskScore;
                this.performanceScore = performanceScore;
                this.recommendations = recommendations;
            }

            public Portfolio getPortfolio() { return portfolio; }
            public double getDiversificationScore() { return diversificationScore; }
            public double getRiskScore() { return riskScore; }
            public double getPerformanceScore() { return performanceScore; }
            public List<String> getRecommendations() { return recommendations; }
        }

        public static class PortfolioOptimization {
            private final Portfolio portfolio;
            private final List<String> optimizations;

            public PortfolioOptimization(Portfolio portfolio, List<String> optimizations) {
                this.portfolio = portfolio;
                this.optimizations = optimizations;
            }

            public Portfolio getPortfolio() { return portfolio; }
            public List<String> getOptimizations() { return optimizations; }
        }
    }
}
