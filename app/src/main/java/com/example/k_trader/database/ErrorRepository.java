package com.example.k_trader.database;

import android.content.Context;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Transaction 에러 데이터 Repository
 * 에러 데이터의 비즈니스 로직을 처리하고 데이터베이스 접근을 추상화
 */
public class ErrorRepository {
    
    private final ErrorDao errorDao;
    private static volatile ErrorRepository INSTANCE;

    private ErrorRepository(Context context) {
        OrderDatabase database = OrderDatabase.getInstance(context);
        this.errorDao = database.errorDao();
    }

    /**
     * 싱글톤 패턴으로 Repository 인스턴스 반환
     */
    public static ErrorRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ErrorRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ErrorRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 에러 저장
     */
    public Single<Long> saveError(ErrorEntity error) {
        return Single.fromCallable(() -> errorDao.insertError(error))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 여러 에러 저장
     */
    public Single<List<Long>> saveErrors(List<ErrorEntity> errors) {
        return Single.fromCallable(() -> errorDao.insertErrors(errors))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 에러 저장 (편의 메서드)
     */
    public Single<Long> saveError(long errorTime, String errorType, String errorMessage) {
        ErrorEntity error = new ErrorEntity(errorTime, errorType, errorMessage);
        return saveError(error);
    }

    /**
     * 에러 저장 (상세 정보 포함)
     */
    public Single<Long> saveError(long errorTime, String errorType, String errorMessage, 
                                String errorCode, String transactionContext) {
        ErrorEntity error = new ErrorEntity(errorTime, errorType, errorMessage, errorCode, transactionContext);
        return saveError(error);
    }

    /**
     * 예외 정보로 에러 저장
     */
    public Single<Long> saveErrorFromException(Exception exception, String errorType, String transactionContext) {
        long errorTime = System.currentTimeMillis();
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Unknown error";
        String stackTrace = getStackTrace(exception);
        
        ErrorEntity error = new ErrorEntity(errorTime, errorType, errorMessage);
        error.setStackTrace(stackTrace);
        error.setTransactionContext(transactionContext);
        
        return saveError(error);
    }

    /**
     * API 상세 정보와 함께 에러 저장
     */
    public Single<Long> saveErrorWithApiDetails(long errorTime, String errorType, String errorMessage,
                                               String transactionContext, Exception exception, String apiErrorDetails) {
        String stackTrace = exception != null ? getStackTrace(exception) : null;
        
        ErrorEntity error = new ErrorEntity(errorTime, errorType, errorMessage);
        error.setStackTrace(stackTrace);
        error.setTransactionContext(transactionContext);
        error.setApiErrorDetails(apiErrorDetails);
        
        return saveError(error);
    }

    /**
     * 모든 에러 조회
     */
    public Flowable<List<ErrorEntity>> getAllErrors() {
        return errorDao.getAllErrors()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 해결되지 않은 에러들 조회
     */
    public Flowable<List<ErrorEntity>> getUnresolvedErrors() {
        return errorDao.getUnresolvedErrors()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 특정 타입의 에러들 조회
     */
    public Flowable<List<ErrorEntity>> getErrorsByType(String errorType) {
        return errorDao.getErrorsByType(errorType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 최근 에러들 조회 (지정된 시간 이후)
     */
    public Flowable<List<ErrorEntity>> getRecentErrors(long sinceTime) {
        return errorDao.getRecentErrors(sinceTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 최근 24시간 에러들 조회
     */
    public Flowable<List<ErrorEntity>> getLast24HoursErrors() {
        long sinceTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        return getRecentErrors(sinceTime);
    }

    /**
     * 에러 해결 상태 업데이트
     */
    public Single<Integer> resolveError(long errorId, String resolutionNote) {
        long updatedAt = System.currentTimeMillis();
        return Single.fromCallable(() -> errorDao.updateErrorResolution(errorId, true, resolutionNote, updatedAt))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 에러 해결 취소
     */
    public Single<Integer> unresolveError(long errorId) {
        long updatedAt = System.currentTimeMillis();
        return Single.fromCallable(() -> errorDao.updateErrorResolution(errorId, false, null, updatedAt))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 에러 삭제
     */
    public Single<Integer> deleteError(ErrorEntity error) {
        return Single.fromCallable(() -> errorDao.deleteError(error))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 오래된 에러들 삭제 (30일 이상)
     */
    public Completable cleanupOldErrors() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        return Single.fromCallable(() -> errorDao.deleteOldErrors(cutoffTime))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement();
    }

    /**
     * 해결된 에러들 삭제
     */
    public Completable cleanupResolvedErrors() {
        return Single.fromCallable(() -> errorDao.deleteResolvedErrors())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement();
    }

    /**
     * 에러 통계 조회
     */
    public Flowable<List<ErrorDao.ErrorStatistics>> getErrorStatistics() {
        return errorDao.getErrorStatistics()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 해결되지 않은 에러 개수 조회
     */
    public Single<Integer> getUnresolvedErrorCount() {
        return errorDao.getUnresolvedErrorCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 예외의 스택 트레이스 문자열 생성
     */
    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 에러 중복 체크 (같은 에러가 최근에 발생했는지 확인)
     */
    public Single<Boolean> isDuplicateError(String errorType, String errorMessage) {
        long recentTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5); // 5분 이내
        return errorDao.getRecentErrors(recentTime)
                .map(errors -> {
                    for (ErrorEntity error : errors) {
                        if (errorType.equals(error.getErrorType()) && 
                            errorMessage.equals(error.getErrorMessage())) {
                            return true;
                        }
                    }
                    return false;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .firstOrError();
    }
}
