package com.example.k_trader.database.converters;

import android.arch.persistence.room.TypeConverter;
import java.util.Date;

/**
 * Room Database를 위한 타입 컨버터들
 * Date 객체와 Long 타임스탬프 간의 변환을 담당
 */
public class DateConverter {
    
    /**
     * Long 타임스탬프를 Date 객체로 변환
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    /**
     * Date 객체를 Long 타임스탬프로 변환
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}

