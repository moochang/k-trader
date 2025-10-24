package com.example.k_trader.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.k_trader.database.entities.BiThumbApiEntities.*;
import java.util.Date;
import java.util.List;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.Completable;

/**
 * 빗썸 API 데이터 접근을 위한 DAO들
 * Clean Architecture의 Data Layer에 해당
 * SRP 원칙에 따라 각 DAO는 하나의 엔티티에 대한 데이터 접근만 담당
 */
public class BiThumbApiDao {

    /**
     * Ticker 데이터 접근 객체
     */
    @Dao
    public interface BiThumbTickerDao {
        
        // 단일 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertTicker(BithumbTickerEntity ticker);
        
        // 배치 삽입 (성능 최적화)
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertTickers(List<BithumbTickerEntity> tickers);
        
        // 최신 티커 데이터 조회 (실시간 관찰용)
        @Query("SELECT * FROM bithumb_ticker WHERE coinPair = :coinPair ORDER BY timestamp DESC LIMIT 1")
        Flowable<BithumbTickerEntity> observeLatestTicker(String coinPair);
        
        // 특정 코인의 최신 티커 데이터 조회
        @Query("SELECT * FROM bithumb_ticker WHERE coinPair = :coinPair ORDER BY timestamp DESC LIMIT 1")
        Single<BithumbTickerEntity> getLatestTicker(String coinPair);
        
        // 티커 히스토리 조회 (차트용)
        @Query("SELECT * FROM bithumb_ticker WHERE coinPair = :coinPair AND timestamp >= :fromTime ORDER BY timestamp ASC LIMIT :limit")
        Single<List<BithumbTickerEntity>> getTickerHistory(String coinPair, Date fromTime, int limit);
        
        // 특정 기간의 티커 데이터 조회
        @Query("SELECT * FROM bithumb_ticker WHERE coinPair = :coinPair AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
        Single<List<BithumbTickerEntity>> getTickersByTimeRange(String coinPair, Date startTime, Date endTime);
        
        // 모든 코인의 최신 티커 데이터 조회
        @Query("SELECT * FROM bithumb_ticker WHERE timestamp IN (SELECT MAX(timestamp) FROM bithumb_ticker GROUP BY coinPair)")
        Single<List<BithumbTickerEntity>> getAllLatestTickers();
        
        // 오래된 데이터 삭제 (데이터 정리)
        @Query("DELETE FROM bithumb_ticker WHERE timestamp < :cutoffTime")
        Completable deleteOldTickers(Date cutoffTime);
        
        // 특정 코인의 모든 데이터 삭제
        @Query("DELETE FROM bithumb_ticker WHERE coinPair = :coinPair")
        Completable deleteTickersByCoinPair(String coinPair);
        
        // 데이터베이스 크기 확인
        @Query("SELECT COUNT(*) FROM bithumb_ticker")
        Single<Integer> getTickerCount();
    }

    /**
     * Balance 데이터 접근 객체
     */
    @Dao
    public interface BithumbBalanceDao {
        
        // 단일 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertBalance(BithumbBalanceEntity balance);
        
        // 배치 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertBalances(List<BithumbBalanceEntity> balances);
        
        // 최신 잔고 데이터 조회 (실시간 관찰용)
        @Query("SELECT * FROM bithumb_balance WHERE currency = :currency ORDER BY timestamp DESC LIMIT 1")
        Flowable<BithumbBalanceEntity> observeLatestBalance(String currency);
        
        // 특정 통화의 최신 잔고 조회
        @Query("SELECT * FROM bithumb_balance WHERE currency = :currency ORDER BY timestamp DESC LIMIT 1")
        Single<BithumbBalanceEntity> getLatestBalance(String currency);
        
        // 모든 통화의 최신 잔고 조회
        @Query("SELECT * FROM bithumb_balance WHERE timestamp IN (SELECT MAX(timestamp) FROM bithumb_balance GROUP BY currency)")
        Single<List<BithumbBalanceEntity>> getAllLatestBalances();
        
        // 잔고 히스토리 조회
        @Query("SELECT * FROM bithumb_balance WHERE currency = :currency AND timestamp >= :fromTime ORDER BY timestamp ASC LIMIT :limit")
        Single<List<BithumbBalanceEntity>> getBalanceHistory(String currency, Date fromTime, int limit);
        
        // 오래된 데이터 삭제
        @Query("DELETE FROM bithumb_balance WHERE timestamp < :cutoffTime")
        Completable deleteOldBalances(Date cutoffTime);
        
        // 특정 통화의 모든 데이터 삭제
        @Query("DELETE FROM bithumb_balance WHERE currency = :currency")
        Completable deleteBalancesByCurrency(String currency);
    }

    /**
     * Order 데이터 접근 객체
     */
    @Dao
    public interface BithumbOrderDao {
        
        // 단일 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertOrder(BithumbOrderEntity order);
        
        // 배치 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertOrders(List<BithumbOrderEntity> orders);
        
        // 주문 ID로 조회
        @Query("SELECT * FROM bithumb_orders WHERE orderId = :orderId")
        Single<BithumbOrderEntity> getOrderById(String orderId);
        
        // 활성 주문 조회 (실시간 관찰용)
        @Query("SELECT * FROM bithumb_orders WHERE coinPair = :coinPair AND status IN ('placed', 'partially_filled') ORDER BY timestamp DESC")
        Flowable<List<BithumbOrderEntity>> observeActiveOrders(String coinPair);
        
        // 특정 코인의 활성 주문 조회
        @Query("SELECT * FROM bithumb_orders WHERE coinPair = :coinPair AND status IN ('placed', 'partially_filled') ORDER BY timestamp DESC")
        Single<List<BithumbOrderEntity>> getActiveOrders(String coinPair);
        
        // 주문 히스토리 조회
        @Query("SELECT * FROM bithumb_orders WHERE coinPair = :coinPair AND timestamp >= :fromTime ORDER BY timestamp DESC LIMIT :limit")
        Single<List<BithumbOrderEntity>> getOrderHistory(String coinPair, Date fromTime, int limit);
        
        // 특정 타입의 주문 조회 (매수/매도)
        @Query("SELECT * FROM bithumb_orders WHERE coinPair = :coinPair AND type = :type ORDER BY timestamp DESC LIMIT :limit")
        Single<List<BithumbOrderEntity>> getOrdersByType(String coinPair, String type, int limit);
        
        // 특정 상태의 주문 조회
        @Query("SELECT * FROM bithumb_orders WHERE coinPair = :coinPair AND status = :status ORDER BY timestamp DESC LIMIT :limit")
        Single<List<BithumbOrderEntity>> getOrdersByStatus(String coinPair, String status, int limit);
        
        // 주문 상태 업데이트
        @Update
        Completable updateOrder(BithumbOrderEntity order);
        
        // 주문 취소
        @Query("UPDATE bithumb_orders SET status = 'cancelled', cancelDate = :cancelDate WHERE orderId = :orderId")
        Completable cancelOrder(String orderId, Date cancelDate);
        
        // 오래된 데이터 삭제
        @Query("DELETE FROM bithumb_orders WHERE timestamp < :cutoffTime")
        Completable deleteOldOrders(Date cutoffTime);
        
        // 특정 코인의 모든 주문 삭제
        @Query("DELETE FROM bithumb_orders WHERE coinPair = :coinPair")
        Completable deleteOrdersByCoinPair(String coinPair);
        
        // 주문 통계 조회
        @Query("SELECT COUNT(*) FROM bithumb_orders WHERE coinPair = :coinPair AND type = :type AND status = :status")
        Single<Integer> getOrderCount(String coinPair, String type, String status);
    }

    /**
     * Candlestick 데이터 접근 객체
     */
    @Dao
    public interface BithumbCandlestickDao {
        
        // 단일 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertCandlestick(BithumbCandlestickEntity candlestick);
        
        // 배치 삽입 (성능 최적화)
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertCandlesticks(List<BithumbCandlestickEntity> candlesticks);
        
        // 캔들스틱 데이터 조회 (차트용)
        @Query("SELECT * FROM bithumb_candlestick WHERE coinPair = :coinPair AND interval = :interval AND timestamp >= :fromTime ORDER BY timestamp ASC LIMIT :limit")
        Single<List<BithumbCandlestickEntity>> getCandlesticks(String coinPair, String interval, Date fromTime, int limit);
        
        // 특정 기간의 캔들스틱 데이터 조회
        @Query("SELECT * FROM bithumb_candlestick WHERE coinPair = :coinPair AND interval = :interval AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
        Single<List<BithumbCandlestickEntity>> getCandlesticksByTimeRange(String coinPair, String interval, Date startTime, Date endTime);
        
        // 최신 캔들스틱 데이터 조회
        @Query("SELECT * FROM bithumb_candlestick WHERE coinPair = :coinPair AND interval = :interval ORDER BY timestamp DESC LIMIT 1")
        Single<BithumbCandlestickEntity> getLatestCandlestick(String coinPair, String interval);
        
        // 캔들스틱 데이터 실시간 관찰
        @Query("SELECT * FROM bithumb_candlestick WHERE coinPair = :coinPair AND interval = :interval ORDER BY timestamp DESC LIMIT 1")
        Flowable<BithumbCandlestickEntity> observeLatestCandlestick(String coinPair, String interval);
        
        // 오래된 데이터 삭제
        @Query("DELETE FROM bithumb_candlestick WHERE timestamp < :cutoffTime")
        Completable deleteOldCandlesticks(Date cutoffTime);
        
        // 특정 코인과 인터벌의 모든 데이터 삭제
        @Query("DELETE FROM bithumb_candlestick WHERE coinPair = :coinPair AND interval = :interval")
        Completable deleteCandlesticksByCoinPairAndInterval(String coinPair, String interval);
        
        // 캔들스틱 데이터 개수 조회
        @Query("SELECT COUNT(*) FROM bithumb_candlestick WHERE coinPair = :coinPair AND interval = :interval")
        Single<Integer> getCandlestickCount(String coinPair, String interval);
    }

    /**
     * API 호출 통계 데이터 접근 객체
     */
    @Dao
    public interface ApiCallStatsDao {
        
        // 통계 데이터 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertStats(ApiCallStatsEntity stats);
        
        // 배치 삽입
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        Completable insertStats(List<ApiCallStatsEntity> statsList);
        
        // 특정 엔드포인트의 통계 조회
        @Query("SELECT * FROM api_call_stats WHERE endpoint = :endpoint AND timestamp >= :fromTime ORDER BY timestamp DESC LIMIT :limit")
        Single<List<ApiCallStatsEntity>> getStatsByEndpoint(String endpoint, Date fromTime, int limit);
        
        // 성공률 통계 조회
        @Query("SELECT COUNT(*) as total, SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success FROM api_call_stats WHERE endpoint = :endpoint AND timestamp >= :fromTime")
        Single<SuccessRateResult> getSuccessRate(String endpoint, Date fromTime);
        
        // 평균 응답 시간 조회
        @Query("SELECT AVG(responseTimeMs) FROM api_call_stats WHERE endpoint = :endpoint AND timestamp >= :fromTime AND success = 1")
        Single<Double> getAverageResponseTime(String endpoint, Date fromTime);
        
        // 오래된 통계 데이터 삭제
        @Query("DELETE FROM api_call_stats WHERE timestamp < :cutoffTime")
        Completable deleteOldStats(Date cutoffTime);
        
        // 성공률 결과를 위한 내부 클래스
        class SuccessRateResult {
            public int total;
            public int success;
            
            public double getSuccessRate() {
                return total > 0 ? (double) success / total * 100 : 0.0;
            }
        }
    }
}
