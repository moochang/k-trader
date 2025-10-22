package com.example.k_trader.database;

import android.support.annotation.NonNull;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.example.k_trader.base.TradeDataManager;

/**
 * Order 데이터베이스 엔티티
 * Room을 사용하여 로컬 데이터베이스에 저장되는 Order 정보
 */
@Entity(tableName = "orders")
@TypeConverters({OrderTypeConverter.class, OrderStatusConverter.class})
public class OrderEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String orderId;                    // 거래소에서 제공하는 주문 ID
    private TradeDataManager.Type type;        // 주문 타입 (BUY, SELL 등)
    private TradeDataManager.Status status;    // 주문 상태 (PLACED, PROCESSED)
    private float units;                       // 비트코인 수량
    private int price;                         // 주문 가격 (원화)
    private String feeRaw;                     // 수수료 문자열
    private double feeEvaluated;              // 원화 환산 수수료
    private long placedTimeInMillis;          // 주문 등록 시간
    private long processedTimeInMillis;      // 주문 체결 시간
    private boolean marked;                   // 마킹 상태
    private long createdAt;                   // DB 생성 시간
    private long updatedAt;                   // DB 수정 시간

    // 기본 생성자
    public OrderEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public TradeDataManager.Type getType() {
        return type;
    }

    public void setType(TradeDataManager.Type type) {
        this.type = type;
    }

    public TradeDataManager.Status getStatus() {
        return status;
    }

    public void setStatus(TradeDataManager.Status status) {
        this.status = status;
    }

    public float getUnits() {
        return units;
    }

    public void setUnits(float units) {
        this.units = units;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getFeeRaw() {
        return feeRaw;
    }

    public void setFeeRaw(String feeRaw) {
        this.feeRaw = feeRaw;
    }

    public double getFeeEvaluated() {
        return feeEvaluated;
    }

    public void setFeeEvaluated(double feeEvaluated) {
        this.feeEvaluated = feeEvaluated;
    }

    public long getPlacedTimeInMillis() {
        return placedTimeInMillis;
    }

    public void setPlacedTimeInMillis(long placedTimeInMillis) {
        this.placedTimeInMillis = placedTimeInMillis;
    }

    public long getProcessedTimeInMillis() {
        return processedTimeInMillis;
    }

    public void setProcessedTimeInMillis(long processedTimeInMillis) {
        this.processedTimeInMillis = processedTimeInMillis;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * TradeData로부터 OrderEntity 생성
     */
    public static OrderEntity fromTradeData(com.example.k_trader.base.TradeData tradeData) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(tradeData.getId());
        entity.setType(tradeData.getType());
        entity.setStatus(tradeData.getStatus());
        entity.setUnits(tradeData.getUnits());
        entity.setPrice(tradeData.getPrice());
        entity.setFeeRaw(tradeData.getFeeRaw());
        entity.setFeeEvaluated(tradeData.getFeeEvaluated());
        entity.setPlacedTimeInMillis(tradeData.getPlacedTime());
        entity.setProcessedTimeInMillis(tradeData.getProcessedTime());
        entity.setMarked(tradeData.getMarked());
        return entity;
    }

    /**
     * OrderEntity를 TradeData로 변환
     */
    public com.example.k_trader.base.TradeData toTradeData() {
        return new com.example.k_trader.base.TradeData()
                .setId(this.orderId)
                .setType(this.type)
                .setStatus(this.status)
                .setUnits(this.units)
                .setPrice(this.price)
                .setFeeRaw(this.feeRaw)
                .setPlacedTime(this.placedTimeInMillis)
                .setProcessedTime(this.processedTimeInMillis)
                .setMarked(this.marked);
    }

    @Override
    @NonNull
    public String toString() {
        return "OrderEntity{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", units=" + units +
                ", price=" + price +
                ", feeRaw='" + feeRaw + '\'' +
                ", feeEvaluated=" + feeEvaluated +
                ", placedTimeInMillis=" + placedTimeInMillis +
                ", processedTimeInMillis=" + processedTimeInMillis +
                ", marked=" + marked +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
