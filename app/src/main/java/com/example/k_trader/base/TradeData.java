package com.example.k_trader.base;

import java.util.Calendar;
import java.util.Locale;
import android.support.annotation.NonNull;

import static com.example.k_trader.base.TradeDataManager.Type.BUY;
import static com.example.k_trader.base.TradeDataManager.Type.SELL;

/**
 * Created by 김무창 on 2017-12-24.
 */

public class TradeData {
    private TradeDataManager.Type type;                          // order type
    private TradeDataManager.Status status;                      // order status
    private String id;                          // order id
    private float units;                       // 비트코인 amount, 소수점 4자리만 허용
    private int price;                         // 매수/매도 당시의 비트코인 원화 가격
    private String feeRaw;                     // 수수료 문자열, 매수시에는 BTC 단위, 매도시에는 원화(KRW) 단위, ex) "0.00000765 BTC", "146 KRW"
    private double feeEvaluated;             // 원화 환산 수수료
    private long placedTimeInMillis;
    private long processedTimeInMillis;
    private boolean marked;

    public TradeDataManager.Type getType() {return type;}
    public TradeDataManager.Status getStatus() {return status;}
    public String getId() {return id;}
    public float getUnits() {return units;}
    public int getPrice() {return price;}
    public String getFeeRaw() {return feeRaw;}
    public double getFeeEvaluated() {return feeEvaluated;}
    public long getPlacedTime() {return placedTimeInMillis;}
    public long getProcessedTime() {return processedTimeInMillis;}
    public boolean getMarked() {return marked;}

    @Override
    @NonNull
    public String toString() {
        Calendar placedCal = Calendar.getInstance();
        Calendar processedCal = Calendar.getInstance();

        placedCal.setTimeInMillis(placedTimeInMillis);
        processedCal.setTimeInMillis(processedTimeInMillis);

        return type.toString() + " : " + status.toString() + " : " + id + " : " + units + " : " + String.format(Locale.getDefault(), "%,d", price) + " : "
                + feeRaw
                + " : "
                + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d"
                , placedCal.get(Calendar.MONTH) + 1, placedCal.get(Calendar.DATE)
                , placedCal.get(Calendar.HOUR_OF_DAY), placedCal.get(Calendar.MINUTE))
                + " : "
                + String.format(Locale.getDefault(), "%02d/%02d %02d:%02d"
                , processedCal.get(Calendar.MONTH) + 1, processedCal.get(Calendar.DATE)
                , processedCal.get(Calendar.HOUR_OF_DAY), processedCal.get(Calendar.MINUTE));
    }

    public TradeData build() {
        return new TradeData();
    }

    public TradeData setType(TradeDataManager.Type type) {
        this.type = type;
        return this;
    }

    public TradeData setStatus(TradeDataManager.Status status) {
        this.status = status;
        return this;
    }

    public TradeData setId(String id) {
        this.id = id;
        return this;
    }

    public TradeData setUnits(float units) {
        this.units = units;
        return this;
    }

    public TradeData setPrice(int price) {
        this.price = price;
        return this;
    }

    public TradeData setFeeRaw(String fee) {
        //Log.d("KTrader", fee);
        this.feeRaw = fee;
        if (fee != null) {
            if (this.getType() == SELL)
                this.feeEvaluated = Double.parseDouble(fee.replaceAll(",", ""));
            else if (this.getType() == BUY)
                this.feeEvaluated = Double.parseDouble(fee.replaceAll(",", ""));
        } else {
            this.feeEvaluated = 0.0;
        }
        return this;
    }

    public TradeData setPlacedTime(long time) {
        this.placedTimeInMillis = time;
        return this;
    }

    public TradeData setProcessedTime(long time) {
        this.processedTimeInMillis = time;
        return this;
    }

    public TradeData setMarked(boolean marked) {
        this.marked = marked;
        return this;
    }
}
