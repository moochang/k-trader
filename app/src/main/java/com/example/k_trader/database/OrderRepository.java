package com.example.k_trader.database;

import android.content.Context;

import com.example.k_trader.base.TradeData;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Order 데이터베이스 Repository
 * 데이터베이스 접근을 추상화하고 비즈니스 로직을 제공
 */
public class OrderRepository {
    
    private final OrderDao orderDao;
    private static volatile OrderRepository INSTANCE;

    private OrderRepository(Context context) {
        OrderDatabase database = OrderDatabase.getInstance(context);
        this.orderDao = database.orderDao();
    }

    /**
     * 싱글톤 패턴으로 Repository 인스턴스 반환
     */
    public static OrderRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (OrderRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OrderRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 모든 주문을 실시간으로 관찰
     */
    public Flowable<List<TradeData>> observeAllOrders() {
        return orderDao.getAllOrders()
                .map(this::convertToTradeDataList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 앱 시작 시 기존 주문 데이터를 한 번만 로드
     */
    public Single<List<TradeData>> loadInitialOrders() {
        return orderDao.getAllOrders()
                .firstOrError()
                .map(this::convertToTradeDataList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }








    /**
     * 여러 주문 저장
     */
    public Single<List<Long>> saveOrders(List<TradeData> tradeDataList) {
        List<OrderEntity> entities = new java.util.ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (TradeData tradeData : tradeDataList) {
            OrderEntity entity = OrderEntity.fromTradeData(tradeData);
            entity.setUpdatedAt(currentTime);
            entities.add(entity);
        }
        
        return Single.fromCallable(() -> orderDao.insertOrders(entities))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }




    /**
     * 마킹되지 않은 주문들 삭제
     */
    public Completable deleteUnmarkedOrders() {
        return Single.fromCallable(orderDao::deleteUnmarkedOrders)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement();
    }

    /**
     * 모든 주문의 마킹 상태 해제
     */
    public Completable unmarkAllOrders() {
        return Single.fromCallable(() -> orderDao.unmarkAllOrders(System.currentTimeMillis()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement();
    }
    
    /**
     * 모든 주문 삭제
     */
    public Completable deleteAllOrders() {
        return Single.fromCallable(orderDao::deleteAllOrders)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .ignoreElement();
    }

    /**
     * 활성 거래 수 조회
     */
    public io.reactivex.Single<Integer> getActiveOrdersCount() {
        return Single.fromCallable(() -> orderDao.getActiveOrdersCount())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * OrderEntity 리스트를 TradeData 리스트로 변환
     */
    private List<TradeData> convertToTradeDataList(List<OrderEntity> entities) {
        List<TradeData> tradeDataList = new java.util.ArrayList<>();
        for (OrderEntity entity : entities) {
            tradeDataList.add(entity.toTradeData());
        }
        return tradeDataList;
    }
}
