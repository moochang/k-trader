package com.example.k_trader.database;

import android.arch.persistence.room.TypeConverter;

import com.example.k_trader.base.TradeDataManager;

/**
 * OrderEntity에서 사용하는 Status Converter
 */
public class OrderStatusConverter {

    @TypeConverter
    public static TradeDataManager.Status fromStatusString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TradeDataManager.Status.valueOf(value);
        } catch (IllegalArgumentException e) {
            return TradeDataManager.Status.PLACED;
        }
    }

    @TypeConverter
    public static String statusToString(TradeDataManager.Status status) {
        return status == null ? null : status.name();
    }
}
