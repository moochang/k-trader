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

/**
 * Transaction Log 탭을 담당하는 Fragment
 * Created by K-Trader on 2024-12-25.
 */
public class TransactionLogFragment extends Fragment {

    public static final String BROADCAST_LOG_MESSAGE = "TRADE_LOG";

    private static final int MAX_BUFFER = 10000;

    private EditText editText;
    private ScrollView scrollView;
    private LogReceiver logReceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_log, container, false);
        
        // UI 컴포넌트 초기화
        editText = view.findViewById(R.id.editText);
        scrollView = view.findViewById(R.id.scrollView1);
        
        // BroadcastReceiver 등록
        registerBroadcastReceiver();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // BroadcastReceiver 해제
        if (logReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(logReceiver);
        }
    }

    /**
     * BroadcastReceiver를 등록하는 메서드
     */
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_LOG_MESSAGE);
        
        logReceiver = new LogReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(logReceiver, filter);
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

                    // 항상 scroll to bottom
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
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
            if (intent.getAction().equals(BROADCAST_LOG_MESSAGE)) {
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
