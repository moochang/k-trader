package com.example.k_trader.util;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.k_trader.KTraderApplication;
import com.example.k_trader.ui.fragment.TransactionLogPage;
import com.example.k_trader.base.Log4jHelper;

import java.util.Calendar;
import java.util.Locale;

/**
 * 로그 정보를 포맷팅하고 처리하는 유틸리티 클래스
 * log_info에 입력되는 문자열을 생성하고 로깅을 처리하는 함수들을 제공
 */
public class LogInfoFormatter {
    
    private static final String TAG = "KTrader";
    private static org.apache.log4j.Logger logger = Log4jHelper.getLogger("LogInfoFormatter");
    
    /**
     * 현재 시간을 포맷팅하여 반환
     * @return 포맷팅된 현재 시간 문자열
     */
    public static String formatCurrentTime() {
        Calendar currentTime = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%d/%02d/%02d %02d:%02d:%02d",
                currentTime.get(Calendar.YEAR), 
                currentTime.get(Calendar.MONTH) + 1, 
                currentTime.get(Calendar.DATE),
                currentTime.get(Calendar.HOUR_OF_DAY), 
                currentTime.get(Calendar.MINUTE), 
                currentTime.get(Calendar.SECOND));
    }
    
    /**
     * 현재가 정보를 포맷팅하여 반환
     * @param coinType 코인 타입
     * @param currentPrice 현재가
     * @return 포맷팅된 현재가 문자열
     */
    public static String formatCurrentPrice(String coinType, int currentPrice) {
        return coinType + " 현재가 : " + String.format(Locale.getDefault(), "%,d", currentPrice);
    }
    
    /**
     * 가격 변화율을 포맷팅하여 반환
     * @param variationRate 변화율
     * @return 포맷팅된 변화율 문자열
     */
    public static String formatPriceVariationRate(double variationRate) {
        return "최근 한시간 변화폭 : " + String.format(Locale.getDefault(), "(%,.1f%%)", variationRate);
    }
    
    /**
     * 예상 잔고 정보를 포맷팅하여 반환
     * @param estimatedBalance 예상 잔고
     * @param krwBalance KRW 잔고
     * @return 포맷팅된 예상 잔고 문자열
     */
    public static String formatEstimatedBalance(long estimatedBalance, long krwBalance) {
        return "예상잔고 : " + String.format(Locale.getDefault(), "%,d", estimatedBalance)
                + " , 주문가능원화 (" + String.format(Locale.getDefault(), "%,d", krwBalance) + ")";
    }
    
    /**
     * 매도 완료 시 잔고 정보를 포맷팅하여 반환
     * @param sellCompleteBalance 매도 완료 시 잔고
     * @param orderBalance 주문 잔고
     * @return 포맷팅된 매도 완료 잔고 문자열
     */
    public static String formatSellCompleteBalance(long sellCompleteBalance, long orderBalance) {
        return "매도완료시: " + String.format(Locale.getDefault(), "%,d", sellCompleteBalance)
                + " , 주문잔고: " + String.format(Locale.getDefault(), "%,d", orderBalance);
    }
    
    /**
     * 마지막 매수 정보를 포맷팅하여 반환
     * @param price 가격
     * @param processedTime 처리 시간 (밀리초)
     * @return 포맷팅된 마지막 매수 문자열
     */
    public static String formatLastBuyInfo(int price, long processedTime) {
        Calendar lastBuyTime = Calendar.getInstance();
        lastBuyTime.setTimeInMillis(processedTime);
        return "마지막 매수 : " + String.format(Locale.getDefault(), "%,d", price) 
                + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d",
                lastBuyTime.get(Calendar.MONTH) + 1, 
                lastBuyTime.get(Calendar.DATE),
                lastBuyTime.get(Calendar.HOUR_OF_DAY), 
                lastBuyTime.get(Calendar.MINUTE));
    }
    
    /**
     * 마지막 매도 정보를 포맷팅하여 반환
     * @param price 가격
     * @param processedTime 처리 시간 (밀리초)
     * @return 포맷팅된 마지막 매도 문자열
     */
    public static String formatLastSellInfo(int price, long processedTime) {
        Calendar lastSellTime = Calendar.getInstance();
        lastSellTime.setTimeInMillis(processedTime);
        return "마지막 매도 : " + String.format(Locale.getDefault(), "%,d", price) 
                + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d",
                lastSellTime.get(Calendar.MONTH) + 1, 
                lastSellTime.get(Calendar.DATE),
                lastSellTime.get(Calendar.HOUR_OF_DAY), 
                lastSellTime.get(Calendar.MINUTE));
    }
    
    /**
     * 매수 발생 정보를 포맷팅하여 반환
     * @param price 가격
     * @param processedTime 처리 시간 (밀리초)
     * @return 포맷팅된 매수 발생 문자열
     */
    public static String formatBuyOccurred(int price, long processedTime) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(processedTime);
        return "매수 발생 : " + String.format(Locale.getDefault(), "%,d", price) 
                + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d",
                time.get(Calendar.MONTH) + 1, 
                time.get(Calendar.DATE),
                time.get(Calendar.HOUR_OF_DAY), 
                time.get(Calendar.MINUTE));
    }
    
    /**
     * 매도 발생 정보를 포맷팅하여 반환
     * @param price 가격
     * @param processedTime 처리 시간 (밀리초)
     * @return 포맷팅된 매도 발생 문자열
     */
    public static String formatSellOccurred(int price, long processedTime) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(processedTime);
        return "매도 발생 : " + String.format(Locale.getDefault(), "%,d", price) 
                + ", " + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d",
                time.get(Calendar.MONTH) + 1, 
                time.get(Calendar.DATE),
                time.get(Calendar.HOUR_OF_DAY), 
                time.get(Calendar.MINUTE));
    }
    
    /**
     * 매도 보정 정보를 포맷팅하여 반환
     * @param originalUnits 원래 단위
     * @param correctedUnits 보정된 단위
     * @return 포맷팅된 매도 보정 문자열
     */
    public static String formatSellCorrection(float originalUnits, float correctedUnits) {
        return String.format(Locale.getDefault(), "매도 보정1 : %f -> %f", originalUnits, correctedUnits);
    }
    
    /**
     * 매도 보정 정보를 포맷팅하여 반환 (잔고 부족)
     * @param unit 단위
     * @param availableBalance 사용 가능한 잔고
     * @return 포맷팅된 매도 보정 문자열
     */
    public static String formatSellCorrection2(float unit, double availableBalance) {
        return String.format(Locale.getDefault(), "매도 보정2 : %f, %f", unit, availableBalance);
    }
    
    /**
     * 매도 필요 잔고 정보를 포맷팅하여 반환
     * @param availableBalance 사용 가능한 잔고
     * @return 포맷팅된 매도 필요 잔고 문자열
     */
    public static String formatSellRequiredBalance(double availableBalance) {
        return "매도 필요 잔고 : " + String.format(Locale.getDefault(), "%.4f", availableBalance);
    }
    
    /**
     * 다음 저점 매수가 정보를 포맷팅하여 반환
     * @param targetPrice 목표 가격
     * @return 포맷팅된 다음 저점 매수가 문자열
     */
    public static String formatNextLowBuyPrice(int targetPrice) {
        return "다음 저점 매수가 : " + String.format(Locale.getDefault(), "%,d", targetPrice);
    }
    
    /**
     * 거래 금액 설정값 경고 메시지를 포맷팅하여 반환
     * @param unitPrice 단위 가격
     * @param coinType 코인 타입
     * @param minTradingPrice 최소 거래 가격
     * @return 포맷팅된 경고 메시지
     */
    public static String formatTradingAmountWarning(int unitPrice, String coinType, int minTradingPrice) {
        return "확인 필요 : 현재 설정 된 1회 거래 금액 설정값(" 
                + String.format(Locale.getDefault(), "%,d원", unitPrice) + ")이 거래소 최소 거래 가능 금액 0.0001" 
                + coinType + String.format(Locale.getDefault(), "(%,d원)", minTradingPrice) + " 보다 작습니다.";
    }
    
    /**
     * 같은 슬롯 주문 정보를 포맷팅하여 반환
     * @param totalAmount 총 금액
     * @param orderAmount 주문 금액
     * @param processedAmount 처리된 금액
     * @return 포맷팅된 같은 슬롯 주문 문자열
     */
    public static String formatSameSlotOrder(int totalAmount, int orderAmount, int processedAmount) {
        return "isSameSlotOrder : " + String.format(Locale.getDefault(), "%,d", totalAmount)
                + ", " + String.format(Locale.getDefault(), "%,d", orderAmount)
                + ", " + String.format(Locale.getDefault(), "%,d", processedAmount);
    }
    
    /**
     * 기타 거래 항목 정보를 포맷팅하여 반환
     * @param tradeType 거래 타입
     * @return 포맷팅된 기타 거래 항목 문자열
     */
    public static String formatOtherTradeItem(String tradeType) {
        return "기타 거래 항목: " + tradeType;
    }
    
    /**
     * 앱 종료 메시지를 포맷팅하여 반환
     * @return 포맷팅된 앱 종료 메시지
     */
    public static String formatAppTerminated() {
        return "App has been terminated by Android";
    }
    
    /**
     * 작업 스케줄 실패 메시지를 포맷팅하여 반환
     * @return 포맷팅된 작업 스케줄 실패 메시지
     */
    public static String formatJobScheduleFailed() {
        return "Unable to schedule trade job!";
    }
    
    /**
     * 비즈니스 로직 에러 메시지를 포맷팅하여 반환
     * @param errorMessage 에러 메시지
     * @return 포맷팅된 비즈니스 로직 에러 메시지
     */
    public static String formatBusinessLogicError(String errorMessage) {
        return "Trade business logic error: " + errorMessage;
    }
    
    /**
     * 잔고 정보 에러 메시지를 포맷팅하여 반환
     * @return 포맷팅된 잔고 정보 에러 메시지
     */
    public static String formatBalanceError() {
        return "잔고 정보를 가져올 수 없습니다.";
    }
    
    /**
     * 현재가 정보 에러 메시지를 포맷팅하여 반환
     * @return 포맷팅된 현재가 정보 에러 메시지
     */
    public static String formatPriceError() {
        return "현재가 정보를 가져올 수 없습니다.";
    }
    
    /**
     * 매수 정보 에러 메시지를 포맷팅하여 반환
     * @return 포맷팅된 매수 정보 에러 메시지
     */
    public static String formatBuyOrderError() {
        return "매수 정보를 가져올 수 없습니다.";
    }
    
    /**
     * 구분선을 포맷팅하여 반환
     * @return 포맷팅된 구분선
     */
    public static String formatSeparator() {
        return "============================================";
    }
    
    /**
     * 로그 정보를 처리하는 메인 함수
     * 내부 Log4j 로깅과 브로드캐스트 전송을 담당
     * @param log 로그 메시지
     */
    public static void log_info(String log) {
        // Log4j 로깅
        if (logger != null) {
            try {
                logger.info(log);
            } catch (Exception e) {
                Log.e(TAG, "[LogInfoFormatter] Error logging with Log4j", e);
            }
        }
        
        // 브로드캐스트 전송
        try {
            Intent intent = new Intent(TransactionLogPage.BROADCAST_LOG_MESSAGE);
            intent.putExtra("log", log);
            if (KTraderApplication.getAppContext() != null) {
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(KTraderApplication.getAppContext());
                if (manager != null) {
                    manager.sendBroadcast(intent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[LogInfoFormatter] Error sending broadcast", e);
        }
    }
    
    /**
     * 로그 정보를 처리하는 메인 함수 (외부 logger 사용)
     * Log4j 로깅과 브로드캐스트 전송을 담당
     * @param externalLogger 외부 Log4j Logger 인스턴스
     * @param log 로그 메시지
     */
    public static void log_info(Object externalLogger, String log) {
        // Log4j 로깅
        if (externalLogger != null) {
            try {
                // logger가 org.apache.log4j.Logger 타입인지 확인
                Class<?> loggerClass = externalLogger.getClass();
                if (loggerClass.getName().equals("org.apache.log4j.Logger")) {
                    java.lang.reflect.Method infoMethod = loggerClass.getMethod("info", String.class);
                    infoMethod.invoke(externalLogger, log);
                }
            } catch (Exception e) {
                Log.e(TAG, "[LogInfoFormatter] Error logging with external Log4j", e);
            }
        }
        
        // 브로드캐스트 전송
        try {
            Intent intent = new Intent(TransactionLogPage.BROADCAST_LOG_MESSAGE);
            intent.putExtra("log", log);
            if (KTraderApplication.getAppContext() != null) {
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(KTraderApplication.getAppContext());
                if (manager != null) {
                    manager.sendBroadcast(intent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[LogInfoFormatter] Error sending broadcast", e);
        }
    }
    
}
