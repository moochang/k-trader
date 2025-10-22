package com.example.k_trader.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Delete;

import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;

/**
 * Transaction 정보 데이터베이스 접근 객체
 */
@Dao
public interface TransactionInfoDao {
    
    /**
     * Transaction 정보 삽입/업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(TransactionInfoEntity transactionInfo);
    
    /**
     * 최신 Transaction 정보 조회 (실시간 관찰)
     */
    @Query("SELECT * FROM transaction_info ORDER BY created_at DESC LIMIT 1")
    Flowable<TransactionInfoEntity> observeLatestTransactionInfo();
    
    /**
     * 최신 Transaction 정보 조회 (Single)
     */
    @Query("SELECT * FROM transaction_info ORDER BY created_at DESC LIMIT 1")
    Single<TransactionInfoEntity> getLatestTransactionInfo();
    
    /**
     * 모든 Transaction 정보 조회 (실시간 관찰)
     */
    @Query("SELECT * FROM transaction_info ORDER BY created_at DESC")
    Flowable<List<TransactionInfoEntity>> observeAllTransactionInfo();
    
    /**
     * 모든 Transaction 정보 조회 (Single)
     */
    @Query("SELECT * FROM transaction_info ORDER BY created_at DESC")
    Single<List<TransactionInfoEntity>> getAllTransactionInfo();
    
    /**
     * 서버에서 온 최신 Transaction 정보 조회 (실시간 관찰)
     */
    @Query("SELECT * FROM transaction_info WHERE is_from_server = 1 ORDER BY created_at DESC LIMIT 1")
    Flowable<TransactionInfoEntity> observeLatestServerTransactionInfo();
    
    /**
     * 서버에서 온 최신 Transaction 정보 조회 (Single)
     */
    @Query("SELECT * FROM transaction_info WHERE is_from_server = 1 ORDER BY created_at DESC LIMIT 1")
    Single<TransactionInfoEntity> getLatestServerTransactionInfo();
    
    /**
     * 오래된 Transaction 정보 삭제 (최근 100개만 유지)
     */
    @Query("DELETE FROM transaction_info WHERE id NOT IN (SELECT id FROM transaction_info ORDER BY created_at DESC LIMIT 100)")
    int deleteOldTransactionInfo();
    
    /**
     * 모든 Transaction 정보 삭제
     */
    @Query("DELETE FROM transaction_info")
    int deleteAll();
}
