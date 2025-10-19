package com.example.k_trader.data;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.example.k_trader.KTraderApplication;
import com.example.k_trader.TransactionItemFragment;
import com.example.k_trader.database.ErrorRepository;
import com.example.k_trader.api.TransactionApiService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Transaction 데이터 관리자
 * 캐시된 데이터와 서버 데이터를 통합 관리하는 중앙 관리자
 * 앱 시작 시 즉시 캐시된 데이터를 로드하고, 백그라운드에서 서버 데이터를 동기화
 */
public class TransactionDataManager {
    
    private static final String BROADCAST_TRANSACTION_DATA = "com.example.k_trader.TRANSACTION_DATA_UPDATED";
    
    private final TransactionCacheService cacheService;
    private final TransactionApiService apiService;
    private final ErrorRepository errorRepository;
    private final ExecutorService executorService;
    private static volatile TransactionDataManager INSTANCE;

    private TransactionDataManager(Context context) {
        this.cacheService = TransactionCacheService.getInstance(context);
        this.apiService = TransactionApiService.getInstance();
        this.errorRepository = ErrorRepository.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 싱글톤 패턴으로 인스턴스 반환
     */
    public static TransactionDataManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TransactionDataManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TransactionDataManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 앱 시작 시 Transaction 데이터 로드
     * 1. 즉시 캐시된 데이터 로드 및 UI 업데이트
     * 2. 백그라운드에서 서버 데이터 동기화
     */
    public void loadTransactionData() {
        // 1. 캐시된 데이터 즉시 로드
        loadCachedData();
        
        // 2. 백그라운드에서 서버 데이터 동기화
        syncWithServer();
    }

    /**
     * 캐시된 데이터를 즉시 로드하고 UI 업데이트
     */
    private void loadCachedData() {
        TransactionData cachedData = cacheService.getCachedData();
        if (cachedData != null && cachedData.isValid()) {
            // 캐시된 데이터를 UI에 즉시 표시
            broadcastTransactionData(cachedData, false);
        }
    }

    /**
     * 서버에서 최신 데이터를 가져와서 동기화
     */
    private void syncWithServer() {
        executorService.execute(() -> {
            try {
                CompletableFuture<TransactionData> future = apiService.fetchTransactionData();
                TransactionData serverData = future.get();
                
                if (serverData != null && serverData.isValid()) {
                    // 서버 데이터를 캐시에 저장
                    cacheService.saveToCache(serverData);
                    
                    // 서버 데이터로 UI 업데이트
                    broadcastTransactionData(serverData, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 서버 동기화 실패 시 에러 처리
                handleSyncError(e);
            }
        });
    }

    /**
     * Transaction 데이터를 브로드캐스트로 전송
     */
    private void broadcastTransactionData(TransactionData data, boolean isFromServer) {
        Intent intent = new Intent(BROADCAST_TRANSACTION_DATA);
        intent.putExtra("transactionTime", data.getTransactionTime());
        intent.putExtra("btcCurrentPrice", data.getBtcCurrentPrice());
        intent.putExtra("hourlyChange", data.getHourlyChange());
        intent.putExtra("estimatedBalance", data.getEstimatedBalance());
        intent.putExtra("lastBuyPrice", data.getLastBuyPrice());
        intent.putExtra("lastSellPrice", data.getLastSellPrice());
        intent.putExtra("nextBuyPrice", data.getNextBuyPrice());
        intent.putExtra("isFromServer", isFromServer);
        
        if (KTraderApplication.getAppContext() != null) {
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext())
                    .sendBroadcast(intent);
        }
    }

    /**
     * 서버 동기화 에러 처리
     */
    private void handleSyncError(Exception e) {
        long errorTime = System.currentTimeMillis();
        String errorType = "Network Error";
        String errorMessage = "서버 동기화 실패: " + e.getMessage();
        
        // 에러를 DB에 저장
        saveErrorToDatabase(errorTime, errorType, errorMessage, "TransactionDataManager.syncWithServer()", e);
        
        // UI에 에러 카드 표시
        Intent intent = new Intent(TransactionItemFragment.BROADCAST_ERROR_CARD);
        intent.putExtra("errorTime", String.valueOf(errorTime));
        intent.putExtra("errorType", errorType);
        intent.putExtra("errorMessage", errorMessage);
        
        if (KTraderApplication.getAppContext() != null) {
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext())
                    .sendBroadcast(intent);
        }
    }

    /**
     * 수동으로 서버 동기화 실행
     */
    public void refreshData() {
        syncWithServer();
    }

    /**
     * 캐시 클리어
     */
    public void clearCache() {
        cacheService.clearCache();
    }

    /**
     * 리소스 정리
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 네트워크 상태 확인
     */
    public boolean isNetworkAvailable() {
        return apiService.isNetworkAvailable();
    }

    /**
     * 캐시된 데이터가 있는지 확인
     */
    public boolean hasCachedData() {
        return cacheService.hasCachedData();
    }

    /**
     * 에러를 데이터베이스에 저장
     */
    private void saveErrorToDatabase(long errorTime, String errorType, String errorMessage, 
                                   String transactionContext, Exception exception) {
        executorService.execute(() -> {
            try {
                errorRepository.saveErrorFromException(exception, errorType, transactionContext)
                        .subscribe(
                            errorId -> {
                                // 에러 저장 성공 로그
                                android.util.Log.d("TransactionDataManager", 
                                    "Error saved to database with ID: " + errorId);
                            },
                            throwable -> {
                                // 에러 저장 실패 로그
                                android.util.Log.e("TransactionDataManager", 
                                    "Failed to save error to database", throwable);
                            }
                        );
            } catch (Exception e) {
                android.util.Log.e("TransactionDataManager", 
                    "Exception while saving error to database", e);
            }
        });
    }

    /**
     * 에러를 데이터베이스에 저장 (간단한 버전)
     */
    public void saveError(String errorType, String errorMessage, String transactionContext) {
        long errorTime = System.currentTimeMillis();
        executorService.execute(() -> {
            try {
                errorRepository.saveError(errorTime, errorType, errorMessage, null, transactionContext)
                        .subscribe(
                            errorId -> {
                                android.util.Log.d("TransactionDataManager", 
                                    "Error saved to database with ID: " + errorId);
                            },
                            throwable -> {
                                android.util.Log.e("TransactionDataManager", 
                                    "Failed to save error to database", throwable);
                            }
                        );
            } catch (Exception e) {
                android.util.Log.e("TransactionDataManager", 
                    "Exception while saving error to database", e);
            }
        });
    }

    /**
     * 예외를 데이터베이스에 저장
     */
    public void saveException(Exception exception, String errorType, String transactionContext) {
        executorService.execute(() -> {
            try {
                errorRepository.saveErrorFromException(exception, errorType, transactionContext)
                        .subscribe(
                            errorId -> {
                                android.util.Log.d("TransactionDataManager", 
                                    "Exception saved to database with ID: " + errorId);
                            },
                            throwable -> {
                                android.util.Log.e("TransactionDataManager", 
                                    "Failed to save exception to database", throwable);
                            }
                        );
            } catch (Exception e) {
                android.util.Log.e("TransactionDataManager", 
                    "Exception while saving exception to database", e);
            }
        });
    }

    /**
     * 해결되지 않은 에러들 조회
     */
    public io.reactivex.Flowable<java.util.List<com.example.k_trader.database.ErrorEntity>> getUnresolvedErrors() {
        return errorRepository.getUnresolvedErrors();
    }

    /**
     * 최근 에러들 조회
     */
    public io.reactivex.Flowable<java.util.List<com.example.k_trader.database.ErrorEntity>> getRecentErrors() {
        return errorRepository.getLast24HoursErrors();
    }

    /**
     * 에러 해결 처리
     */
    public void resolveError(long errorId, String resolutionNote) {
        executorService.execute(() -> {
            try {
                errorRepository.resolveError(errorId, resolutionNote)
                        .subscribe(
                            result -> {
                                android.util.Log.d("TransactionDataManager", 
                                    "Error resolved: " + errorId);
                            },
                            throwable -> {
                                android.util.Log.e("TransactionDataManager", 
                                    "Failed to resolve error", throwable);
                            }
                        );
            } catch (Exception e) {
                android.util.Log.e("TransactionDataManager", 
                    "Exception while resolving error", e);
            }
        });
    }

    /**
     * 오래된 에러들 정리
     */
    public void cleanupOldErrors() {
        executorService.execute(() -> {
            try {
                errorRepository.cleanupOldErrors()
                        .subscribe(
                            () -> {
                                android.util.Log.d("TransactionDataManager", 
                                    "Old errors cleaned up successfully");
                            },
                            throwable -> {
                                android.util.Log.e("TransactionDataManager", 
                                    "Failed to cleanup old errors", throwable);
                            }
                        );
            } catch (Exception e) {
                android.util.Log.e("TransactionDataManager", 
                    "Exception while cleaning up old errors", e);
            }
        });
    }
}
