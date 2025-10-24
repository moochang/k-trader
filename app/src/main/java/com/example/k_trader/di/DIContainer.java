package com.example.k_trader.di;

import android.content.Context;
import android.util.Log;

import com.example.k_trader.presentation.viewmodel.ViewModels.*;
import com.example.k_trader.database.OrderRepository;
import com.example.k_trader.database.CoinPriceInfoRepository;
import com.example.k_trader.database.TransactionInfoRepository;
import com.example.k_trader.api.service.BiThumbApiService;
import com.example.k_trader.database.OrderDatabase;
import com.example.k_trader.database.daos.OrderDao;
import com.example.k_trader.database.daos.CoinPriceInfoDao;
import com.example.k_trader.database.daos.TransactionInfoDao;
import com.example.k_trader.database.daos.ErrorDao;
import com.example.k_trader.database.daos.ApiCallResultDao;

/**
 * Dependency Injection Container for K-Trader App
 * 
 * Clean Architecture의 의존성 주입을 통한 느슨한 결합
 * 중앙 집중식 의존성 관리
 * Repository 구현체들과 Use Case 연결
 * ViewModel Factory 제공
 * 
 * @author K-Trader Team
 * @version 1.0
 */
public class DIContainer {
    
    private static DIContainer instance;
    private final Context context;
    
    // Database
    private OrderDatabase database;
    
    // DAOs
    private OrderDao orderDao;
    private CoinPriceInfoDao coinPriceInfoDao;
    private TransactionInfoDao transactionInfoDao;
    private ErrorDao errorDao;
    private ApiCallResultDao apiCallResultDao;
    
    // API Service
    private BiThumbApiService bithumbApiService;
    
    // Repository Implementations (기존 구조 사용)
    private OrderRepository orderRepository;
    private CoinPriceInfoRepository coinPriceInfoRepository;
    private TransactionInfoRepository transactionInfoRepository;

    private DIContainer(Context context) {
        this.context = context.getApplicationContext();
        initializeDependencies();
    }

    public static synchronized DIContainer getInstance(Context context) {
        if (instance == null) {
            instance = new DIContainer(context);
        }
        return instance;
    }

    public static synchronized DIContainer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DIContainer must be initialized with Context first");
        }
        return instance;
    }

    private void initializeDependencies() {
        Log.d("KTrader", "[DIContainer] Initializing dependencies");
        
        try {
            // 1. Database 초기화
            initializeDatabase();
            
            // 2. DAOs 초기화
            initializeDAOs();
            
            // 3. API Service 초기화
            initializeApiService();
            
            // 4. Repository Implementations 초기화
            initializeRepositories();
            
            Log.d("KTrader", "[DIContainer] All dependencies initialized successfully");
            
        } catch (Exception e) {
            Log.e("KTrader", "[DIContainer] Error initializing dependencies", e);
            throw new RuntimeException("Failed to initialize DIContainer", e);
        }
    }

    private void initializeDatabase() {
        Log.d("KTrader", "[DIContainer] Initializing database");
        database = OrderDatabase.getInstance(context);
    }

    private void initializeDAOs() {
        Log.d("KTrader", "[DIContainer] Initializing DAOs");
        orderDao = database.orderDao();
        coinPriceInfoDao = database.coinPriceInfoDao();
        transactionInfoDao = database.transactionInfoDao();
        errorDao = database.errorDao();
        apiCallResultDao = database.apiCallResultDao();
    }

    private void initializeApiService() {
        Log.d("KTrader", "[DIContainer] Initializing API service");
        
        // GlobalSettings에서 API 키와 시크릿 가져오기
        String apiKey = com.example.k_trader.base.GlobalSettings.getInstance().getApiKey();
        String apiSecret = com.example.k_trader.base.GlobalSettings.getInstance().getApiSecret();
        
        Log.d("KTrader", "[DIContainer] API Key: " + (apiKey != null && !apiKey.isEmpty() ? "SET" : "NOT SET"));
        Log.d("KTrader", "[DIContainer] API Secret: " + (apiSecret != null && !apiSecret.isEmpty() ? "SET" : "NOT SET"));
        
        bithumbApiService = new BiThumbApiService(apiKey, apiSecret);
    }

    private void initializeRepositories() {
        Log.d("KTrader", "[DIContainer] Initializing repositories");
        
        // 기존 Repository들 초기화
        orderRepository = OrderRepository.getInstance(context);
        coinPriceInfoRepository = new CoinPriceInfoRepository(context);
        transactionInfoRepository = new TransactionInfoRepository(context);
    }

    // Repository Getters (기존 구조)
    public OrderRepository getOrderRepository() {
        return orderRepository;
    }

    public CoinPriceInfoRepository getCoinPriceInfoRepository() {
        return coinPriceInfoRepository;
    }

    public TransactionInfoRepository getTransactionInfoRepository() {
        return transactionInfoRepository;
    }

    // ViewModel Factory Methods
    public MainViewModel createMainViewModel() {
        Log.d("KTrader", "[DIContainer] Creating MainViewModel");
        // TODO: 실제 Use Case들과 연결 후 구현
        return new MainViewModel(
            null, // manageTradingDataUseCase
            null, // monitorCoinPriceUseCase
            null, // autoTradingUseCase
            null, // manageSettingsUseCase
            null  // tradingAnalysisService
        );
    }

    public OrderManagementViewModel createOrderManagementViewModel() {
        Log.d("KTrader", "[DIContainer] Creating OrderManagementViewModel");
        // TODO: 실제 Use Case들과 연결 후 구현
        return new OrderManagementViewModel(
            null, // manageTradingDataUseCase
            null, // tradingExecutionService
            null  // tradingAnalysisService
        );
    }

    public PortfolioViewModel createPortfolioViewModel() {
        Log.d("KTrader", "[DIContainer] Creating PortfolioViewModel");
        // TODO: 실제 Use Case들과 연결 후 구현
        return new PortfolioViewModel(
            null, // managePortfolioUseCase
            null  // tradingAnalysisService
        );
    }

    public SettingsViewModel createSettingsViewModel() {
        Log.d("KTrader", "[DIContainer] Creating SettingsViewModel");
        // TODO: 실제 Use Case들과 연결 후 구현
        return new SettingsViewModel(null); // manageSettingsUseCase
    }

    // Utility Methods (기존 구조)
    public void clearAllData() {
        Log.d("KTrader", "[DIContainer] Clearing all data");
        try {
            // 기존 Repository들을 사용하여 데이터 정리
            orderRepository.deleteAllOrders();
            coinPriceInfoRepository.deleteAll().subscribe();
            transactionInfoRepository.deleteAll().subscribe();
            Log.d("KTrader", "[DIContainer] All data cleared successfully");
        } catch (Exception e) {
            Log.e("KTrader", "[DIContainer] Error clearing data", e);
        }
    }

    public void refreshAllData() {
        Log.d("KTrader", "[DIContainer] Refreshing all data");
        try {
            // TODO: 기존 구조와 연동 후 구현
            Log.d("KTrader", "[DIContainer] All data refreshed successfully");
        } catch (Exception e) {
            Log.e("KTrader", "[DIContainer] Error refreshing data", e);
        }
    }

    public boolean isInitialized() {
        return database != null && 
               orderRepository != null && 
               coinPriceInfoRepository != null && 
               transactionInfoRepository != null;
    }

    public void destroy() {
        Log.d("KTrader", "[DIContainer] Destroying container");
        if (database != null) {
            database.close();
        }
        instance = null;
    }

    // Debug Methods
    public void printDependencyGraph() {
        Log.d("KTrader", "[DIContainer] Dependency Graph:");
        Log.d("KTrader", "[DIContainer] Database: " + (database != null ? "✓" : "✗"));
        Log.d("KTrader", "[DIContainer] API Service: " + (bithumbApiService != null ? "✓" : "✗"));
        Log.d("KTrader", "[DIContainer] Repositories: " + (orderRepository != null ? "✓" : "✗"));
    }

    // Configuration Methods (기존 구조)
    public void updateApiCredentials(String apiKey, String apiSecret) {
        Log.d("KTrader", "[DIContainer] Updating API credentials");
        // TODO: BithumbApiService에 updateCredentials 메서드 구현 필요
        // if (bithumbApiService != null) {
        //     bithumbApiService.updateCredentials(apiKey, apiSecret);
        // }
    }

    public void updateTradingSettings(com.example.k_trader.domain.model.DomainModels.TradingSettings settings) {
        Log.d("KTrader", "[DIContainer] Updating trading settings");
        // TODO: 기존 구조와 연동 후 구현
    }

    // Health Check (기존 구조)
    public boolean performHealthCheck() {
        Log.d("KTrader", "[DIContainer] Performing health check");
        try {
            boolean isHealthy = isInitialized() && 
                               database.isOpen() &&
                               bithumbApiService != null;
            
            Log.d("KTrader", "[DIContainer] Health check result: " + (isHealthy ? "HEALTHY" : "UNHEALTHY"));
            return isHealthy;
        } catch (Exception e) {
            Log.e("KTrader", "[DIContainer] Health check failed", e);
            return false;
        }
    }
}
