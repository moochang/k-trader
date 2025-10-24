package com.example.k_trader.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.example.k_trader.database.entities.CoinPriceInfoEntity;

import io.reactivex.Flowable;

/**
 * 코인 가격 정보 데이터베이스 접근 객체
 */
@Dao
public interface CoinPriceInfoDao {
    
    /**
     * 코인 가격 정보 삽입/업데이트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(CoinPriceInfoEntity coinPriceInfo);
    
    /**
     * 현재 코인 가격 정보 조회 (실시간 관찰)
     */
    @Query("SELECT * FROM coin_price_info WHERE id = 1")
    Flowable<CoinPriceInfoEntity> observeCurrentPriceInfo();
    
    /**
     * 현재 코인 가격 정보 조회 (Single)
     */
    @Query("SELECT * FROM coin_price_info WHERE id = 1")
    io.reactivex.Single<CoinPriceInfoEntity> getCurrentPriceInfo();
    
    /**
     * 모든 코인 가격 정보 삭제
     */
    @Query("DELETE FROM coin_price_info")
    int deleteAll();
}





