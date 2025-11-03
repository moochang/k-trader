package com.example.k_trader.dialog;

import static java.lang.Math.min;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.k_trader.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 세련된 ProgressDialog를 관리하는 클래스
 */
public class ProgressDialogManager {
    
    private static AlertDialog progressDialog;
    private static Handler progressHandler;
    private static Runnable progressRunnable;
    private static Timer mTimer;
    
    /**
     * 세련된 ProgressDialog를 표시하는 메서드
     */
    public static void show(Context context, int maxSeconds) {
        int adjustMaxSeconds = min(maxSeconds, 10);
        android.util.Log.d("KTrader", "[ProgressDialogManager] show() 시작 - maxSeconds: " + adjustMaxSeconds);
        
        try {
            // 기존 다이얼로그가 있다면 닫기
            dismiss();
            
            // 커스텀 레이아웃 인플레이트
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_progress, null);
            
            // UI 컴포넌트 참조
            TextView timeRemainingText = dialogView.findViewById(R.id.timeRemaining);

            android.util.Log.d("KTrader", "[ProgressDialogManager] UI 컴포넌트 초기화 완료");
            
            // 다이얼로그 생성
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            builder.setView(dialogView);
            builder.setCancelable(false); // 뒤로가기로 닫을 수 없음
            
            progressDialog = builder.create();
            progressDialog.show();
            
            android.util.Log.d("KTrader", "[ProgressDialogManager] AlertDialog 생성 및 표시 완료");
            
            // Handler 초기화
            progressHandler = new Handler();
            
            // 남은 시간 업데이트 Runnable
            progressRunnable = new Runnable() {
                int remainingSeconds = adjustMaxSeconds;
                
                @Override
                public void run() {
                    if (remainingSeconds > 0 && progressDialog != null && progressDialog.isShowing()) {
                        timeRemainingText.setText("남은 시간: " + remainingSeconds + "초");
                        remainingSeconds--;
                        progressHandler.postDelayed(this, 1000); // 1초마다 업데이트
                    } else {
                        // 시간이 끝나면 다이얼로그 닫기
                        dismiss();
                    }
                }
            };
            
            // 타이머 시작
            mTimer = new Timer();
            mTimer.schedule(new ProgressTimerTask(), maxSeconds * 1000); // maxSeconds 후에 자동으로 닫기
            
            // 남은 시간 업데이트 시작
            progressHandler.post(progressRunnable);
            
            android.util.Log.d("KTrader", "[ProgressDialogManager] ProgressDialog 표시 완료");
        } catch (Exception e) {
            android.util.Log.e("KTrader", "[ProgressDialogManager] show() 오류", e);
        }
    }
    
    /**
     * ProgressDialog를 닫는 메서드
     */
    public static void dismiss() {
        android.util.Log.d("KTrader", "[ProgressDialogManager] dismiss() 시작");
        
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
                android.util.Log.d("KTrader", "[ProgressDialogManager] ProgressDialog 닫기 완료");
            }
            
            // Handler와 Runnable 정리
            if (progressHandler != null && progressRunnable != null) {
                progressHandler.removeCallbacks(progressRunnable);
                android.util.Log.d("KTrader", "[ProgressDialogManager] Handler 정리 완료");
            }
            
            // Timer 정리
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
                android.util.Log.d("KTrader", "[ProgressDialogManager] Timer 정리 완료");
            }
        } catch (Exception e) {
            android.util.Log.e("KTrader", "[ProgressDialogManager] dismiss() 오류", e);
        }
    }
    
    /**
     * ProgressDialog가 표시 중인지 확인하는 메서드
     */
    public static boolean isShowing() {
        return progressDialog != null && progressDialog.isShowing();
    }
    
    /**
     * TimerTask를 위한 내부 클래스
     */
    private static class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            android.util.Log.d("KTrader", "[ProgressDialogManager] TimerTask 실행 - 다이얼로그 닫기");
            dismiss();
        }
    }
}
