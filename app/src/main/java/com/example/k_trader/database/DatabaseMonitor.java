package com.example.k_trader.database;

import android.content.Context;

import com.example.k_trader.base.TradeData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * DB 변경 모니터링 시스템
 * UI 컴포넌트들이 DB 변경사항을 구독할 수 있도록 하는 중앙 관리자
 */
public class DatabaseMonitor {
    
    private static volatile DatabaseMonitor INSTANCE;
    private final OrderRepository orderRepository;
    private final CopyOnWriteArrayList<DatabaseChangeListener> listeners;
    private final CompositeDisposable disposables;

    private DatabaseMonitor(Context context) {
        this.orderRepository = OrderRepository.getInstance(context);
        this.listeners = new CopyOnWriteArrayList<>();
        this.disposables = new CompositeDisposable();
    }

    /**
     * 싱글톤 패턴으로 Monitor 인스턴스 반환
     */
    public static DatabaseMonitor getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseMonitor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseMonitor(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * DB 변경 리스너 인터페이스
     */
    public interface DatabaseChangeListener {
        void onOrdersChanged(List<TradeData> orders);
    }

    /**
     * 모든 주문 변경사항 구독
     */
    public void subscribeToAllOrders(DatabaseChangeListener listener) {
        listeners.add(listener);
        
        Disposable disposable = orderRepository.observeAllOrders()
                .subscribe(orders -> notifyListeners(orders, listener));
        
        disposables.add(disposable);
    }





    /**
     * 구독 해제
     */
    public void unsubscribe(String subscriberId) {
        listeners.removeIf(listener -> 
            listener.getClass().getName().contains(subscriberId));
    }





    /**
     * 주문 목록 변경 알림 (초기 로드용)
     */
    public void notifyOrdersChanged(List<TradeData> orders) {
        // 모든 구독자에게 주문 목록 변경 알림
        for (DatabaseChangeListener listener : listeners) {
            listener.onOrdersChanged(orders);
        }
    }

    /**
     * 리스너들에게 변경사항 알림
     */
    private void notifyListeners(List<TradeData> orders, DatabaseChangeListener listener) {
        listener.onOrdersChanged(orders);
    }


}
