package com.example.k_trader.database;

import android.arch.persistence.room.ColumnInfo;

/**
 * 주문 타입별 개수를 저장하는 데이터 클래스
 */
public class OrderTypeCount {
    @ColumnInfo(name = "type")
    private String type;
    private int count;
    
    public OrderTypeCount() {
    }
    
    public OrderTypeCount(String type, int count) {
        this.type = type;
        this.count = count;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    @Override
    public String toString() {
        return "OrderTypeCount{" +
                "type='" + type + '\'' +
                ", count=" + count +
                '}';
    }
}
