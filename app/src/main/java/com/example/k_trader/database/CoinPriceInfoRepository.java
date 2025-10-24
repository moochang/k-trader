package com.example.k_trader.database;

import android.content.Context;

import com.example.k_trader.database.daos.CoinPriceInfoDao;
import com.example.k_trader.database.entities.CoinPriceInfoEntity;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * 코인 가격 정보 저장소
 */
public class CoinPriceInfoRepository {
    
    private CoinPriceInfoDao coinPriceInfoDao;
    
    public CoinPriceInfoRepository(Context context) {
        OrderDatabase database = OrderDatabase.getInstance(context);
        this.coinPriceInfoDao = database.coinPriceInfoDao();
    }
    
    /**
     * 코인 가격 정보 저장/업데이트
     */
    public Completable savePriceInfo(String coinType, String currentPrice, String priceChange) {
        return Completable.fromAction(() -> {
            CoinPriceInfoEntity entity = new CoinPriceInfoEntity(coinType, currentPrice, priceChange);
            coinPriceInfoDao.insertOrUpdate(entity);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 현재 코인 가격 정보 실시간 관찰 (Flowable)
     */
    public Flowable<CoinPriceInfoEntity> observeCurrentPriceInfo() {
        return coinPriceInfoDao.observeCurrentPriceInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 현재 코인 가격 정보 조회 (Single)
     */
    public Single<CoinPriceInfoEntity> getCurrentPriceInfo() {
        return coinPriceInfoDao.getCurrentPriceInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 모든 코인 가격 정보 삭제
     */
    public Completable deleteAll() {
        return Completable.fromAction(() -> coinPriceInfoDao.deleteAll())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
