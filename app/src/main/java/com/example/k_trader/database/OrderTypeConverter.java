package com.example.k_trader.database;

import android.arch.persistence.room.TypeConverter;

import com.example.k_trader.base.TradeDataManager;

/**
 * OrderEntity에서 사용하는 Type Converter
 */
public class OrderTypeConverter {

    @TypeConverter
    public static TradeDataManager.Type fromTypeString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TradeDataManager.Type.valueOf(value);
        } catch (IllegalArgumentException e) {
            return TradeDataManager.Type.NONE;
        }
    }

    @TypeConverter
    public static String typeToString(TradeDataManager.Type type) {
        return type == null ? null : type.name();
    }
}
