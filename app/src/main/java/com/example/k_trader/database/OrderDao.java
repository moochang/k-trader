package com.example.k_trader.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import io.reactivex.Flowable;

/**
 * Order 데이터베이스 접근 객체
 * Room을 사용한 데이터베이스 CRUD 작업 정의
 */
@Dao
public interface OrderDao {

    /**
     * 모든 주문 조회 (Flowable - 실시간 업데이트)
     */
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    Flowable<List<OrderEntity>> getAllOrders();

    /**
     * 여러 주문 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertOrders(List<OrderEntity> orders);

    /**
     * 마킹되지 않은 주문들 삭제
     */
    @Query("DELETE FROM orders WHERE marked = 0")
    int deleteUnmarkedOrders();

    /**
     * 모든 주문의 마킹 상태 해제
     */
    @Query("UPDATE orders SET marked = 0, updatedAt = :currentTime WHERE marked = 1")
    int unmarkAllOrders(long currentTime);
    
    /**
     * 모든 주문 삭제
     */
    @Query("DELETE FROM orders")
    int deleteAllOrders();
    
    /**
     * 활성 거래 수 조회 (미체결 주문)
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED'")
    int getActiveOrdersCount();
    
    /**
     * 활성 SELL 주문 수 조회
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED' AND type = 'SELL'")
    int getActiveSellOrdersCount();
    
    /**
     * 활성 BUY 주문 수 조회
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED' AND type = 'BUY'")
    int getActiveBuyOrdersCount();
    
    /**
     * 디버깅용: 모든 활성 주문 조회
     */
    @Query("SELECT * FROM orders WHERE status = 'PLACED'")
    List<OrderEntity> getAllActiveOrders();
    
    /**
     * 디버깅용: 활성 주문의 타입별 개수 조회
     */
    @Query("SELECT type, COUNT(*) as count FROM orders WHERE status = 'PLACED' GROUP BY type")
    List<OrderTypeCount> getActiveOrdersCountByType();
    
    /**
     * 활성 주문 수 실시간 관찰 (Flowable)
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED'")
    Flowable<Integer> observeActiveOrdersCount();
    
    /**
     * 활성 SELL 주문 수 실시간 관찰 (Flowable)
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED' AND type = 'SELL'")
    Flowable<Integer> observeActiveSellOrdersCount();
    
    /**
     * 활성 BUY 주문 수 실시간 관찰 (Flowable)
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED' AND type = 'BUY'")
    Flowable<Integer> observeActiveBuyOrdersCount();
    
    /**
     * 활성 주문 타입별 개수 실시간 관찰 (Flowable)
     */
    @Query("SELECT type, COUNT(*) as count FROM orders WHERE status = 'PLACED' GROUP BY type")
    Flowable<List<OrderTypeCount>> observeActiveOrdersCountByType();
}