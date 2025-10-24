package com.example.k_trader.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Update;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Query;

import com.example.k_trader.database.entities.ApiCallResultEntity;

import java.util.List;

/**
 * API 호출 결과 DAO
 */
@Dao
public interface ApiCallResultDao {
    
    @Insert
    long insertApiCallResult(ApiCallResultEntity apiCallResult);
    
    @Insert
    List<Long> insertApiCallResults(List<ApiCallResultEntity> apiCallResults);
    
    @Update
    int updateApiCallResult(ApiCallResultEntity apiCallResult);
    
    @Delete
    int deleteApiCallResult(ApiCallResultEntity apiCallResult);
    
    @Query("SELECT * FROM api_call_results ORDER BY call_time DESC")
    List<ApiCallResultEntity> getAllApiCallResults();
    
    @Query("SELECT * FROM api_call_results WHERE call_time >= :sinceTime ORDER BY call_time DESC")
    List<ApiCallResultEntity> getApiCallResultsSince(long sinceTime);
    
    @Query("SELECT * FROM api_call_results WHERE is_success = 1 ORDER BY call_time DESC")
    List<ApiCallResultEntity> getSuccessfulApiCallResults();
    
    @Query("SELECT * FROM api_call_results WHERE is_success = 0 ORDER BY call_time DESC")
    List<ApiCallResultEntity> getFailedApiCallResults();
    
    @Query("SELECT * FROM api_call_results WHERE api_endpoint = :endpoint ORDER BY call_time DESC")
    List<ApiCallResultEntity> getApiCallResultsByEndpoint(String endpoint);
    
    @Query("SELECT * FROM api_call_results WHERE call_time >= :sinceTime AND call_time <= :untilTime ORDER BY call_time DESC")
    List<ApiCallResultEntity> getApiCallResultsInRange(long sinceTime, long untilTime);
    
    @Query("DELETE FROM api_call_results WHERE call_time < :beforeTime")
    int deleteOldApiCallResults(long beforeTime);
    
    @Query("SELECT COUNT(*) FROM api_call_results")
    int getApiCallResultCount();
    
    @Query("SELECT COUNT(*) FROM api_call_results WHERE is_success = 1")
    int getSuccessfulApiCallResultCount();
    
    @Query("SELECT COUNT(*) FROM api_call_results WHERE is_success = 0")
    int getFailedApiCallResultCount();
}
