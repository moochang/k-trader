package com.example.k_trader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;

import com.example.k_trader.base.TradeData;
import com.example.k_trader.database.DatabaseMonitor;

import java.util.List;

/**
 * Transaction Log 탭을 담당하는 Fragment
 * DB 구독 시스템을 통해 실시간으로 주문 데이터 로그를 표시
 * Created by K-Trader on 2024-12-25.
 */
public class TransactionLogFragment extends Fragment implements DatabaseMonitor.DatabaseChangeListener {

    public static final String BROADCAST_LOG_MESSAGE = "TRADE_LOG";

    private static final int MAX_BUFFER = 10000;

    private EditText editText;
    private ScrollView scrollView;
    private LogReceiver logReceiver;
    private DatabaseMonitor databaseMonitor;
    private String subscriberId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_log, container, false);
        
        // UI 컴포넌트 초기화
        editText = view.findViewById(R.id.editText);
        scrollView = view.findViewById(R.id.scrollView1);
        
        // Database Monitor 초기화
        databaseMonitor = DatabaseMonitor.getInstance(getContext());
        subscriberId = "TransactionLogFragment_" + System.currentTimeMillis();
        
        // BroadcastReceiver 등록
        if (getContext() != null) {
            registerBroadcastReceiver();
        }
        
        // DB 구독 시작
        subscribeToDatabase();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // DB 구독 해제
        if (databaseMonitor != null) {
            databaseMonitor.unsubscribe(subscriberId);
        }
        
        // BroadcastReceiver 해제
        if (logReceiver != null && getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(logReceiver);
        }
    }

    /**
     * DB 구독을 시작하는 메서드
     */
    private void subscribeToDatabase() {
        if (databaseMonitor != null && getContext() != null) {
            // 모든 주문 변경사항 구독
            databaseMonitor.subscribeToAllOrders(this);
        }
    }

    /**
     * DB 변경 리스너 구현
     */
    @Override
    public void onOrdersChanged(List<TradeData> orders) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                log_info("=== 주문 목록 업데이트 ===");
                for (TradeData order : orders) {
                    log_info(order.toString());
                }
                log_info("=== 총 " + orders.size() + "개 주문 ===");
            });
        }
    }

    /**
     * BroadcastReceiver를 등록하는 메서드
     */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_LOG_MESSAGE);
        
        logReceiver = new LogReceiver();
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, filter);
        }
    }

    /**
     * 로그 메시지를 처리하는 메서드
     */
    private void log_info(final String log) {
        Runnable runnable = () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    editText.append(log + "\r\n");

                    CharSequence charSequence = editText.getText();
                    if (charSequence.length() > MAX_BUFFER)
                        editText.getEditableText().delete(0, charSequence.length() - MAX_BUFFER);

                    // Auto scroll 설정에 따라 스크롤
                    if (GlobalSettings.getInstance().isAutoScroll()) {
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    }
                });
            }
        };
        runnable.run();
    }

    /**
     * 로그 메시지를 받는 BroadcastReceiver
     */
    private class LogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(BROADCAST_LOG_MESSAGE)) {
                String log = intent.getStringExtra("log");
                log_info(log);
            }
        }
    }

    /**
     * Scroll to bottom 기능을 제공하는 메서드
     */
    public void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
}
