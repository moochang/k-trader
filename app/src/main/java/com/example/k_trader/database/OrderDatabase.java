package com.example.k_trader.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Room 데이터베이스 설정
 */
@Database(
    entities = {OrderEntity.class, ErrorEntity.class, ApiCallResultEntity.class},
    version = 3,
    exportSchema = false
)
public abstract class OrderDatabase extends RoomDatabase {

    private static volatile OrderDatabase INSTANCE;

    public abstract OrderDao orderDao();
    public abstract ErrorDao errorDao();
    public abstract ApiCallResultDao apiCallResultDao();

    /**
     * 싱글톤 패턴으로 데이터베이스 인스턴스 반환
     */
    public static OrderDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (OrderDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            OrderDatabase.class,
                            "order_database"
                    )
                    .fallbackToDestructiveMigration() // 스키마 변경 시 데이터 삭제
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
