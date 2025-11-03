package com.example.k_trader.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.example.k_trader.R;
import com.example.k_trader.TransactionItemFragment;

/**
 * TransactionCard 상세 정보를 보여주는 다이얼로그
 */
public class TransactionDetailDialog extends Dialog {
    
    private TransactionItemFragment.CardAdapter.TransactionCard transactionCard;
    private Context context;

    public TransactionDetailDialog(@NonNull Context context, 
                                TransactionItemFragment.CardAdapter.TransactionCard transactionCard) {
        super(context);
        this.context = context;
        this.transactionCard = transactionCard;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 커스텀 레이아웃 설정
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_transaction_detail, null);
        setContentView(view);
        
        // 데이터 바인딩
        bindTransactionData(view);
        
        // 다이얼로그 설정
        setTitle("거래 상세 정보");
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    /**
     * Transaction 데이터를 UI에 바인딩
     */
    private void bindTransactionData(View view) {
        TextView textTransactionTime = view.findViewById(R.id.textDetailTransactionTime);
        TextView textBtcCurrentPrice = view.findViewById(R.id.textDetailBtcCurrentPrice);
        TextView textHourlyChange = view.findViewById(R.id.textDetailHourlyChange);
        TextView textEstimatedBalance = view.findViewById(R.id.textDetailEstimatedBalance);
        TextView textLastBuyPrice = view.findViewById(R.id.textDetailLastBuyPrice);
        TextView textLastSellPrice = view.findViewById(R.id.textDetailLastSellPrice);
        TextView textNextBuyPrice = view.findViewById(R.id.textDetailNextBuyPrice);
        
        // 데이터 설정
        textTransactionTime.setText(transactionCard.transactionTime != null ? 
            transactionCard.transactionTime : "정보 없음");
        textBtcCurrentPrice.setText(transactionCard.btcCurrentPrice != null ? 
            transactionCard.btcCurrentPrice + " KRW" : "정보 없음");
        textHourlyChange.setText(transactionCard.hourlyChange != null ? 
            transactionCard.hourlyChange : "정보 없음");
        textEstimatedBalance.setText(transactionCard.estimatedBalance != null ? 
            transactionCard.estimatedBalance + " BTC" : "정보 없음");
        textLastBuyPrice.setText(transactionCard.lastBuyPrice != null ? 
            transactionCard.lastBuyPrice + " KRW" : "정보 없음");
        textLastSellPrice.setText(transactionCard.lastSellPrice != null ? 
            transactionCard.lastSellPrice + " KRW" : "정보 없음");
        textNextBuyPrice.setText(transactionCard.nextBuyPrice != null ? 
            transactionCard.nextBuyPrice + " KRW" : "정보 없음");
        
        // 시간 포맷팅 (타임스탬프인 경우)
        if (transactionCard.transactionTime != null && 
            transactionCard.transactionTime.matches("\\d+")) {
            try {
                long timestamp = Long.parseLong(transactionCard.transactionTime);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                textTransactionTime.setText(sdf.format(new java.util.Date(timestamp)));
            } catch (NumberFormatException e) {
                // 타임스탬프가 아닌 경우 원본 텍스트 유지
            }
        }
        
        // 가격 포맷팅 (숫자인 경우)
        formatPrice(textBtcCurrentPrice, transactionCard.btcCurrentPrice);
        formatPrice(textLastBuyPrice, transactionCard.lastBuyPrice);
        formatPrice(textLastSellPrice, transactionCard.lastSellPrice);
        formatPrice(textNextBuyPrice, transactionCard.nextBuyPrice);
    }

    /**
     * 가격 정보 포맷팅
     */
    private void formatPrice(TextView textView, String price) {
        if (price != null && !price.isEmpty()) {
            try {
                double priceValue = Double.parseDouble(price);
                textView.setText(String.format(java.util.Locale.getDefault(), 
                    "%,.0f KRW", priceValue));
            } catch (NumberFormatException e) {
                textView.setText(price + " KRW");
            }
        }
    }

    /**
     * 정적 팩토리 메서드로 다이얼로그 생성
     */
    public static void show(Context context, 
                          TransactionItemFragment.CardAdapter.TransactionCard transactionCard) {
        TransactionDetailDialog dialog = new TransactionDetailDialog(context, transactionCard);
        dialog.show();
    }
}
