package com.example.k_trader.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Transaction 데이터 캐시 관리 서비스
 * SharedPreferences를 사용하여 로컬에 Transaction 데이터를 캐시
 */
public class TransactionCacheService {
    
    private static final String PREFS_NAME = "transaction_cache";
    private static final String KEY_CACHED_DATA = "cached_transaction_data";
    private static final String KEY_CACHE_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5분
    
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private static volatile TransactionCacheService INSTANCE;

    private TransactionCacheService(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * 싱글톤 패턴으로 인스턴스 반환
     */
    public static TransactionCacheService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TransactionCacheService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TransactionCacheService(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 캐시된 Transaction 데이터 조회
     * @return 캐시된 데이터가 있으면 반환, 없으면 null
     */
    public TransactionData getCachedData() {
        String cachedJson = sharedPreferences.getString(KEY_CACHED_DATA, null);
        if (cachedJson == null) {
            return null;
        }

        try {
            Type type = new TypeToken<TransactionData>() {}.getType();
            TransactionData cachedData = gson.fromJson(cachedJson, type);
            
            // 캐시 만료 확인
            if (isCacheExpired()) {
                clearCache();
                return null;
            }
            
            return cachedData;
        } catch (Exception e) {
            e.printStackTrace();
            clearCache();
            return null;
        }
    }

    /**
     * Transaction 데이터를 캐시에 저장
     */
    public void saveToCache(TransactionData data) {
        try {
            String json = gson.toJson(data);
            sharedPreferences.edit()
                    .putString(KEY_CACHED_DATA, json)
                    .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 캐시가 만료되었는지 확인
     */
    private boolean isCacheExpired() {
        long cacheTimestamp = sharedPreferences.getLong(KEY_CACHE_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - cacheTimestamp) > CACHE_EXPIRY_TIME;
    }

    /**
     * 캐시 클리어
     */
    public void clearCache() {
        sharedPreferences.edit()
                .remove(KEY_CACHED_DATA)
                .remove(KEY_CACHE_TIMESTAMP)
                .apply();
    }

    /**
     * 캐시된 데이터가 있는지 확인
     */
    public boolean hasCachedData() {
        return sharedPreferences.contains(KEY_CACHED_DATA) && !isCacheExpired();
    }

    /**
     * 캐시 마지막 업데이트 시간 반환
     */
    public Date getLastCacheTime() {
        long timestamp = sharedPreferences.getLong(KEY_CACHE_TIMESTAMP, 0);
        return new Date(timestamp);
    }
}
