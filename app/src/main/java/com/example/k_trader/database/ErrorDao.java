package com.example.k_trader.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.arch.persistence.room.ColumnInfo;
import java.util.List;
import io.reactivex.Flowable;
import io.reactivex.Single;

/**
 * Transaction 에러 데이터 접근 객체
 */
@Dao
public interface ErrorDao {
    
    /**
     * 모든 에러 조회 (최신순)
     */
    @Query("SELECT * FROM transaction_errors ORDER BY error_time DESC")
    Flowable<List<ErrorEntity>> getAllErrors();
    
    /**
     * 해결되지 않은 에러만 조회
     */
    @Query("SELECT * FROM transaction_errors WHERE is_resolved = 0 ORDER BY error_time DESC")
    Flowable<List<ErrorEntity>> getUnresolvedErrors();
    
    /**
     * 특정 에러 타입의 에러들 조회
     */
    @Query("SELECT * FROM transaction_errors WHERE error_type = :errorType ORDER BY error_time DESC")
    Flowable<List<ErrorEntity>> getErrorsByType(String errorType);
    
    /**
     * 최근 24시간 내 에러들 조회
     */
    @Query("SELECT * FROM transaction_errors WHERE error_time > :sinceTime ORDER BY error_time DESC")
    Flowable<List<ErrorEntity>> getRecentErrors(long sinceTime);
    
    /**
     * 특정 기간의 에러들 조회
     */
    @Query("SELECT * FROM transaction_errors WHERE error_time BETWEEN :startTime AND :endTime ORDER BY error_time DESC")
    Flowable<List<ErrorEntity>> getErrorsByTimeRange(long startTime, long endTime);
    
    /**
     * 에러 심각도별 조회
     */
    @Query("SELECT * FROM transaction_errors WHERE error_type IN (:errorTypes) ORDER BY error_time DESC")
    Flowable<List<ErrorEntity>> getErrorsBySeverity(String... errorTypes);
    
    /**
     * 특정 에러 ID로 조회
     */
    @Query("SELECT * FROM transaction_errors WHERE id = :errorId")
    Single<ErrorEntity> getErrorById(long errorId);
    
    /**
     * 에러 통계 조회
     */
    @Query("SELECT error_type, COUNT(*) as count FROM transaction_errors GROUP BY error_type")
    Flowable<List<ErrorStatistics>> getErrorStatistics();
    
    /**
     * 에러 개수 조회
     */
    @Query("SELECT COUNT(*) FROM transaction_errors")
    Single<Integer> getErrorCount();
    
    /**
     * 해결되지 않은 에러 개수 조회
     */
    @Query("SELECT COUNT(*) FROM transaction_errors WHERE is_resolved = 0")
    Single<Integer> getUnresolvedErrorCount();
    
    /**
     * 에러 삽입
     */
    @Insert
    long insertError(ErrorEntity error);
    
    /**
     * 여러 에러 삽입
     */
    @Insert
    List<Long> insertErrors(List<ErrorEntity> errors);
    
    /**
     * 에러 업데이트
     */
    @Update
    int updateError(ErrorEntity error);
    
    /**
     * 에러 해결 상태 업데이트
     */
    @Query("UPDATE transaction_errors SET is_resolved = :isResolved, resolution_note = :resolutionNote, updated_at = :updatedAt WHERE id = :errorId")
    int updateErrorResolution(long errorId, boolean isResolved, String resolutionNote, long updatedAt);
    
    /**
     * 에러 삭제
     */
    @Delete
    int deleteError(ErrorEntity error);
    
    /**
     * 모든 에러 삭제
     */
    @Query("DELETE FROM transaction_errors")
    int deleteAllErrors();
    
    /**
     * 오래된 에러들 삭제 (30일 이상)
     */
    @Query("DELETE FROM transaction_errors WHERE error_time < :cutoffTime")
    int deleteOldErrors(long cutoffTime);
    
    /**
     * 해결된 에러들 삭제
     */
    @Query("DELETE FROM transaction_errors WHERE is_resolved = 1")
    int deleteResolvedErrors();
    
    /**
     * 에러 통계 클래스
     */
    class ErrorStatistics {
        @ColumnInfo(name = "error_type")
        public String errorType;
        @ColumnInfo(name = "count")
        public int count;
        
        public ErrorStatistics(String errorType, int count) {
            this.errorType = errorType;
            this.count = count;
        }
    }
}
