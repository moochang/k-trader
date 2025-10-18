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
}