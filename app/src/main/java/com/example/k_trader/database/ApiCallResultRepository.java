package com.example.k_trader.database;

import android.arch.persistence.room.RoomDatabase;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API 호출 결과 Repository
 */
public class ApiCallResultRepository {
    
    private static volatile ApiCallResultRepository INSTANCE;
    private final ApiCallResultDao apiCallResultDao;
    
    private ApiCallResultRepository(ApiCallResultDao apiCallResultDao) {
        this.apiCallResultDao = apiCallResultDao;
    }
    
    public static ApiCallResultRepository getInstance(ApiCallResultDao apiCallResultDao) {
        if (INSTANCE == null) {
            synchronized (ApiCallResultRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ApiCallResultRepository(apiCallResultDao);
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * API 호출 결과 저장
     */
    public Single<Long> saveApiCallResult(ApiCallResultEntity apiCallResult) {
        return Single.fromCallable(() -> apiCallResultDao.insertApiCallResult(apiCallResult))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 성공한 API 호출 결과 저장
     */
    public Single<Long> saveSuccessfulApiCall(String apiEndpoint, String responseData, String transactionData) {
        long callTime = System.currentTimeMillis();
        ApiCallResultEntity entity = new ApiCallResultEntity();
        entity.setCallTime(callTime);
        entity.setApiEndpoint(apiEndpoint);
        entity.setSuccess(true);
        entity.setResponseData(responseData);
        entity.setTransactionData(transactionData);
        
        return saveApiCallResult(entity);
    }
    
    /**
     * 실패한 API 호출 결과 저장
     */
    public Single<Long> saveFailedApiCall(String apiEndpoint, String errorCode, String errorMessage, String serverErrorMessage) {
        long callTime = System.currentTimeMillis();
        ApiCallResultEntity entity = new ApiCallResultEntity();
        entity.setCallTime(callTime);
        entity.setApiEndpoint(apiEndpoint);
        entity.setSuccess(false);
        entity.setErrorCode(errorCode);
        entity.setErrorMessage(errorMessage);
        entity.setServerErrorMessage(serverErrorMessage);
        // 전체 에러 메시지를 responseData로 저장
        entity.setResponseData(errorMessage);
        
        return saveApiCallResult(entity);
    }
    
    /**
     * 모든 API 호출 결과 조회
     */
    public Flowable<List<ApiCallResultEntity>> getAllApiCallResults() {
        return Flowable.fromCallable(() -> apiCallResultDao.getAllApiCallResults())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 최근 API 호출 결과 조회 (1시간 이내)
     */
    public Flowable<List<ApiCallResultEntity>> getRecentApiCallResults() {
        long oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        return Flowable.fromCallable(() -> apiCallResultDao.getApiCallResultsSince(oneHourAgo))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 성공한 API 호출 결과 조회
     */
    public Flowable<List<ApiCallResultEntity>> getSuccessfulApiCallResults() {
        return Flowable.fromCallable(() -> apiCallResultDao.getSuccessfulApiCallResults())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 실패한 API 호출 결과 조회
     */
    public Flowable<List<ApiCallResultEntity>> getFailedApiCallResults() {
        return Flowable.fromCallable(() -> apiCallResultDao.getFailedApiCallResults())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 특정 엔드포인트의 API 호출 결과 조회
     */
    public Flowable<List<ApiCallResultEntity>> getApiCallResultsByEndpoint(String endpoint) {
        return Flowable.fromCallable(() -> apiCallResultDao.getApiCallResultsByEndpoint(endpoint))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 오래된 API 호출 결과 삭제 (7일 이전)
     */
    public Completable cleanupOldApiCallResults() {
        long sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        return Completable.fromAction(() -> apiCallResultDao.deleteOldApiCallResults(sevenDaysAgo))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * API 호출 결과 통계 조회
     */
    public Single<ApiCallStatistics> getApiCallStatistics() {
        return Single.fromCallable(() -> {
            int totalCount = apiCallResultDao.getApiCallResultCount();
            int successCount = apiCallResultDao.getSuccessfulApiCallResultCount();
            int failedCount = apiCallResultDao.getFailedApiCallResultCount();
            
            return new ApiCallStatistics(totalCount, successCount, failedCount);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * API 호출 통계 클래스
     */
    public static class ApiCallStatistics {
        public int totalCount;
        public int successCount;
        public int failedCount;
        
        public ApiCallStatistics(int totalCount, int successCount, int failedCount) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
        }
        
        public double getSuccessRate() {
            return totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
        }
        
        public double getFailureRate() {
            return totalCount > 0 ? (double) failedCount / totalCount * 100 : 0;
        }
    }
}
