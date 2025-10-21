package com.example.k_trader.data;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.example.k_trader.KTraderApplication;
import com.example.k_trader.TransactionItemFragment;
import com.example.k_trader.database.ErrorRepository;
import com.example.k_trader.database.ApiCallResultRepository;
import com.example.k_trader.database.TransactionInfoRepository;
import com.example.k_trader.database.TransactionInfoEntity;
import com.example.k_trader.api.TransactionApiService;
import com.example.k_trader.api.TransactionApiResult;
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
    private final ApiCallResultRepository apiCallResultRepository;
    private final TransactionInfoRepository transactionInfoRepository;
    private final ExecutorService executorService;
    private TransactionDataUpdateCallback updateCallback;
    
    /**
     * UI 업데이트를 위한 콜백 인터페이스
     */
    public interface TransactionDataUpdateCallback {
        void onTransactionDataUpdated(TransactionData data);
        void onActiveOrdersUpdated(int sellCount, int buyCount);
    }
    private static volatile TransactionDataManager INSTANCE;

    private TransactionDataManager(Context context) {
        this.cacheService = TransactionCacheService.getInstance(context);
        this.apiService = TransactionApiService.getInstance();
        this.errorRepository = ErrorRepository.getInstance(context);
        this.apiCallResultRepository = ApiCallResultRepository.getInstance(
            com.example.k_trader.database.OrderDatabase.getInstance(context).apiCallResultDao());
        this.transactionInfoRepository = new TransactionInfoRepository(context);
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
     * 캐시된 데이터를 외부에서 조회할 수 있도록 제공
     */
    public TransactionData getCachedData() {
        return cacheService.getCachedData();
    }
    
    /**
     * UI 업데이트 콜백 설정
     */
    public void setUpdateCallback(TransactionDataUpdateCallback callback) {
        this.updateCallback = callback;
    }

    /**
     * 서버에서 최신 데이터를 가져와서 동기화
     */
    private void syncWithServer() {
        // executorService가 종료된 경우 새로 생성
        if (executorService.isShutdown()) {
            // 새로운 executorService를 생성할 수 없으므로 현재 스레드에서 실행
            try {
                TransactionApiResult result = apiService.fetchTransactionDataWithErrorInfo().get();
                
                // API 호출 결과를 DB에 저장 (성공/실패 관계없이)
                saveApiCallResultsToDatabase(result);
                
                if (result.getTransactionData() != null && result.getTransactionData().isValid()) {
                    // 서버 데이터를 캐시에 저장
                    cacheService.saveToCache(result.getTransactionData());
                    
                    // 서버 데이터를 DB에 저장
                    saveTransactionDataToDatabase(result.getTransactionData(), true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                TransactionApiResult result = apiService.fetchTransactionDataWithErrorInfo().get();
                
                // API 호출 결과를 DB에 저장 (성공/실패 관계없이)
                saveApiCallResultsToDatabase(result);
                
                if (result.getTransactionData() != null && result.getTransactionData().isValid()) {
                    // 서버 데이터를 캐시에 저장
                    cacheService.saveToCache(result.getTransactionData());
                    
                    // 서버 데이터를 DB에 저장
                    saveTransactionDataToDatabase(result.getTransactionData(), true);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                // 서버 동기화 실패 시 에러 처리
                handleSyncError(e);
            }
        });
    }
    
    /**
     * Transaction 데이터를 DB에 저장
     */
    private void saveTransactionDataToDatabase(TransactionData data, boolean isFromServer) {
        try {
            TransactionInfoEntity entity = new TransactionInfoEntity(
                data.getTransactionTime(),
                data.getBtcCurrentPrice(),
                data.getHourlyChange(),
                data.getDailyChange(),
                data.getEstimatedBalance(),
                data.getLastBuyPrice(),
                data.getLastSellPrice(),
                data.getNextBuyPrice(),
                isFromServer
            );
            
            transactionInfoRepository.saveTransactionInfo(entity)
                .subscribe(
                    () -> android.util.Log.d("[K-TR]", "[TransactionDataManager] Transaction data saved to DB successfully"),
                    throwable -> android.util.Log.e("[K-TR]", "[TransactionDataManager] Error saving transaction data to DB", throwable)
                );
                
        } catch (Exception e) {
            android.util.Log.e("[K-TR]", "[TransactionDataManager] Error creating TransactionInfoEntity", e);
        }
    }

    /**
     * API 호출 결과를 DB에 저장
     */
    private void saveApiCallResultsToDatabase(TransactionApiResult result) {
        try {
            if (result.getTransactionData() != null && result.getTransactionData().isValid()) {
                // 성공한 경우 - TransactionData 저장
                String transactionDataJson = convertTransactionDataToJson(result.getTransactionData());
                apiCallResultRepository.saveSuccessfulApiCall("TransactionData", 
                    "Success", transactionDataJson)
                    .subscribe(
                        id -> android.util.Log.d("TransactionDataManager", "API call result saved with ID: " + id),
                        throwable -> android.util.Log.e("TransactionDataManager", "Failed to save API call result", throwable)
                    );
            }
            
            // 에러가 있는 경우 각 에러별로 저장
            if (result.hasErrors()) {
                for (TransactionApiResult.ApiError apiError : result.getErrors()) {
                    String errorCode = extractErrorCode(apiError.getErrorMessage());
                    String serverMessage = extractServerMessage(apiError.getErrorMessage());
                    
                    // 전체 에러 메시지를 JSON 형태로 포맷팅 (더 상세한 정보 포함)
                    String fullErrorMessage = formatDetailedErrorMessage(apiError.getApiEndpoint(), 
                        errorCode, serverMessage, apiError.getErrorMessage());
                    
                    apiCallResultRepository.saveFailedApiCall(apiError.getApiEndpoint(), 
                        errorCode, fullErrorMessage, serverMessage)
                        .subscribe(
                            id -> android.util.Log.d("TransactionDataManager", "API error saved with ID: " + id),
                            throwable -> android.util.Log.e("TransactionDataManager", "Failed to save API error", throwable)
                        );
                }
            }
            
        } catch (Exception e) {
            android.util.Log.e("TransactionDataManager", "Exception while saving API call results", e);
        }
    }

    /**
     * TransactionData를 JSON 문자열로 변환
     */
    private String convertTransactionDataToJson(TransactionData data) {
        try {
            return String.format("{\"transactionTime\":\"%s\",\"btcCurrentPrice\":\"%s\",\"hourlyChange\":\"%s\",\"estimatedBalance\":\"%s\",\"lastBuyPrice\":\"%s\",\"lastSellPrice\":\"%s\",\"nextBuyPrice\":\"%s\"}",
                data.getTransactionTime(), data.getBtcCurrentPrice(), data.getHourlyChange(),
                data.getEstimatedBalance(), data.getLastBuyPrice(), data.getLastSellPrice(), data.getNextBuyPrice());
        } catch (Exception e) {
            return "{\"error\":\"Failed to convert TransactionData to JSON\"}";
        }
    }

    /**
     * Transaction 데이터를 브로드캐스트로 전송
     */
    private void broadcastTransactionData(TransactionData data, boolean isFromServer) {
        android.util.Log.d("[K-TR]", "[TransactionDataManager] Broadcasting transaction data - hourlyChange: " + data.getHourlyChange() + ", dailyChange: " + data.getDailyChange());
        
        Intent intent = new Intent(BROADCAST_TRANSACTION_DATA);
        intent.putExtra("transactionTime", data.getTransactionTime());
        intent.putExtra("btcCurrentPrice", data.getBtcCurrentPrice());
        intent.putExtra("hourlyChange", data.getHourlyChange());
        intent.putExtra("dailyChange", data.getDailyChange());
        intent.putExtra("estimatedBalance", data.getEstimatedBalance());
        intent.putExtra("lastBuyPrice", data.getLastBuyPrice());
        intent.putExtra("lastSellPrice", data.getLastSellPrice());
        intent.putExtra("nextBuyPrice", data.getNextBuyPrice());
        intent.putExtra("isFromServer", isFromServer);
        
        if (KTraderApplication.getAppContext() != null) {
            LocalBroadcastManager.getInstance(KTraderApplication.getAppContext())
                    .sendBroadcast(intent);
            android.util.Log.d("[K-TR]", "[TransactionDataManager] Broadcast sent successfully");
        }
    }

    /**
     * API 에러들 처리
     */
    private void handleApiErrors(TransactionApiResult result) {
        long errorTime = System.currentTimeMillis();
        String errorType = "API Error";
        String errorMessage = "API 호출 중 에러 발생";
        String apiErrorDetails = result.getErrorSummary();
        
        // 에러를 DB에 저장 (API 상세 정보 포함)
        saveErrorToDatabaseWithApiDetails(errorTime, errorType, errorMessage, "TransactionDataManager.syncWithServer()", null, apiErrorDetails);
    }

    /**
     * 에러 메시지에서 에러 코드 추출
     */
    private String extractErrorCode(String errorMessage) {
        if (errorMessage == null) return "Unknown";
        
        // "Status: 5001, Message: ..." 형태에서 에러 코드 추출
        if (errorMessage.contains("Status:")) {
            String[] parts = errorMessage.split(",");
            if (parts.length > 0) {
                return parts[0].replace("Status:", "").trim();
            }
        }
        
        return "Unknown";
    }

    /**
     * 에러 메시지에서 서버 메시지 추출
     */
    private String extractServerMessage(String errorMessage) {
        if (errorMessage == null) return "No message";
        
        // "Status: 5001, Message: Invalid API Key" 형태에서 메시지 추출
        if (errorMessage.contains("Message:")) {
            String[] parts = errorMessage.split("Message:");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        
        return errorMessage;
    }

    /**
     * 상세한 에러 메시지를 JSON 형태로 포맷팅 (더 많은 정보 포함)
     */
    private String formatDetailedErrorMessage(String apiEndpoint, String errorCode, String serverMessage, String originalMessage) {
        try {
            return "{\n" +
                    "  \"server_url\": \"https://api.bithumb.com" + apiEndpoint + "\",\n" +
                    "  \"api_endpoint\": \"" + apiEndpoint + "\",\n" +
                    "  \"error_code\": \"" + (errorCode != null ? errorCode : "Unknown") + "\",\n" +
                    "  \"server_message\": \"" + (serverMessage != null ? serverMessage.replace("\"", "\\\"") : "No message") + "\",\n" +
                    "  \"original_error\": \"" + (originalMessage != null ? originalMessage.replace("\"", "\\\"").replace("\n", "\\n") : "No error message") + "\",\n" +
                    "  \"timestamp\": \"" + System.currentTimeMillis() + "\",\n" +
                    "  \"error_type\": \"API_CALL_FAILURE\",\n" +
                    "  \"api_provider\": \"Bithumb\",\n" +
                    "  \"request_method\": \"GET\"\n" +
                    "}";
        } catch (Exception e) {
            return "{\"error\":\"Failed to format error message\",\"original\":\"" + 
                (originalMessage != null ? originalMessage.replace("\"", "\\\"") : "No message") + "\"}";
        }
    }

    /**
     * 전체 에러 메시지를 JSON 형태로 포맷팅
     */
    private String formatFullErrorMessage(String apiEndpoint, String errorCode, String serverMessage, String originalMessage) {
        try {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n");
            jsonBuilder.append("  \"server_url\": \"https://api.bithumb.com").append(apiEndpoint).append("\",\n");
            jsonBuilder.append("  \"api_endpoint\": \"").append(apiEndpoint).append("\",\n");
            jsonBuilder.append("  \"error_code\": \"").append(errorCode != null ? errorCode : "Unknown").append("\",\n");
            jsonBuilder.append("  \"server_message\": \"").append(serverMessage != null ? serverMessage : "No message").append("\",\n");
            jsonBuilder.append("  \"original_error\": \"").append(originalMessage != null ? originalMessage.replace("\"", "\\\"") : "No error message").append("\",\n");
            jsonBuilder.append("  \"timestamp\": \"").append(System.currentTimeMillis()).append("\"\n");
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        } catch (Exception e) {
            return "{\"error\":\"Failed to format error message\",\"original\":\"" + 
                (originalMessage != null ? originalMessage.replace("\"", "\\\"") : "No message") + "\"}";
        }
    }

    /**
     * API 상세 정보와 함께 에러를 DB에 저장
     */
    private void saveErrorToDatabaseWithApiDetails(long errorTime, String errorType, String errorMessage,
                                                   String transactionContext, Exception exception, String apiErrorDetails) {
        executorService.execute(() -> {
            try {
                errorRepository.saveErrorWithApiDetails(errorTime, errorType, errorMessage,
                    transactionContext, exception, apiErrorDetails)
                    .subscribe(
                        errorId -> {
                            android.util.Log.d("TransactionDataManager",
                                "Error with API details saved to database with ID: " + errorId);
                        },
                        throwable -> {
                            android.util.Log.e("TransactionDataManager",
                                "Failed to save error with API details to database", throwable);
                        }
                    );
            } catch (Exception e) {
                android.util.Log.e("TransactionDataManager",
                    "Exception while saving error with API details to database", e);
            }
        });
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
