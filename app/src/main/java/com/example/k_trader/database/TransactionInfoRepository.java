package com.example.k_trader.database;

import android.content.Context;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;

/**
 * Transaction 정보 저장소
 */
public class TransactionInfoRepository {
    
    private TransactionInfoDao transactionInfoDao;
    
    public TransactionInfoRepository(Context context) {
        OrderDatabase database = OrderDatabase.getInstance(context);
        this.transactionInfoDao = database.transactionInfoDao();
    }
    
    /**
     * Transaction 정보 저장/업데이트
     */
    public Completable saveTransactionInfo(TransactionInfoEntity transactionInfo) {
        return Completable.fromAction(() -> {
            transactionInfoDao.insertOrUpdate(transactionInfo);
            // 오래된 데이터 정리
            transactionInfoDao.deleteOldTransactionInfo();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 최신 Transaction 정보 실시간 관찰 (Flowable)
     */
    public Flowable<TransactionInfoEntity> observeLatestTransactionInfo() {
        return transactionInfoDao.observeLatestTransactionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 최신 Transaction 정보 조회 (Single)
     */
    public Single<TransactionInfoEntity> getLatestTransactionInfo() {
        return transactionInfoDao.getLatestTransactionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 모든 Transaction 정보 실시간 관찰 (Flowable)
     */
    public Flowable<List<TransactionInfoEntity>> observeAllTransactionInfo() {
        return transactionInfoDao.observeAllTransactionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 모든 Transaction 정보 조회 (Single)
     */
    public Single<List<TransactionInfoEntity>> getAllTransactionInfo() {
        return transactionInfoDao.getAllTransactionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 서버에서 온 최신 Transaction 정보 실시간 관찰 (Flowable)
     */
    public Flowable<TransactionInfoEntity> observeLatestServerTransactionInfo() {
        return transactionInfoDao.observeLatestServerTransactionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 서버에서 온 최신 Transaction 정보 조회 (Single)
     */
    public Single<TransactionInfoEntity> getLatestServerTransactionInfo() {
        return transactionInfoDao.getLatestServerTransactionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * 모든 Transaction 정보 삭제
     */
    public Completable deleteAll() {
        return Completable.fromAction(() -> transactionInfoDao.deleteAll())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
