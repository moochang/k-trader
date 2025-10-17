package com.example.k_trader_eth.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.example.k_trader_eth.base.TradeDataManager.Type.SELL;

/**
 * Created by 김무창 on 2017-12-23.
 */

public class TradeDataManager {
    public enum Type {
        ALL  // 전체 (0)
        , BUY  // 구매완료 (1)
        , SELL  // 판매완료 (2)
        , WITHDRAW_ONGOING  // 출금중 (3)
        , DEPOSIT  // 입금 (4)
        , WITHDRAW  // 출금 (5)
        , DEPOSIT_KRW  // KRW 입금중 (9)
        , NONE
    }

    public enum Status {
        PLACED, PROCESSED
    }

    private List<TradeData> list = new ArrayList<>();

    public List<TradeData> getList() {
        return list;
    }

    public void clear() {
        list.clear();
    }
    public void add(TradeData data) {
        list.add(data);
    }

    public TradeData build() {
        TradeData data = new TradeData();
        return data;
    }

    public TradeData findById(String orderId) {
        for (TradeData data : list) {
            if (data.getId() != null && data.getId().equals(orderId))
                return data;
        }

        return null;
    }

    public TradeData findByProcessedTime(long time) {
        for (TradeData data : list) {
            if (data.getProcessedTime() == time)
                return data;
        }

        return null;
    }

    public TradeData findByPrice(Type type, int price) {
        for (TradeData data : list) {
            if (data.getType() == type && data.getPrice() == price)
                return data;
        }

        return null;
    }

    public List<TradeData> getByPrice(Type type, int price) {
        List<TradeData> result = new ArrayList<>();
        for (TradeData data : list) {
            if (data.getType() == type && data.getPrice() == price)
                result.add(data);
        }

        return result;
    }

    @Override
    public String toString() {
        String result = "";
        int i = 1;
        for (TradeData data : list) {
            result += (i++ + " : ");
            result += data.toString();
            result += "\n";
        }

        return result;
    }

    public void unmarkAll() {
        for (TradeData data : list) {
            data.setMarked(false);
        }
    }

    public void removeUnmarked() {
        Iterator<TradeData> i = list.iterator();
        while (i.hasNext()) {
            TradeData o = i.next();
            if (!o.getMarked())
                i.remove();
        }
    }

    public int getEstimation() {
        double result = 0;
        for (TradeData data : list) {
            if (data.getType() == SELL)
                result += (data.getPrice() * data.getUnits());
        }

        return (int)result;
    }

    public TradeData findLatestProcessedTime(Type type) {
        TradeData result = null;
        for (TradeData data : list) {
            if (data.getType() == type) {
                if (result == null)
                    result = data;
                else {
                    if (result.getProcessedTime() < data.getProcessedTime())
                        result = data;
                }
            }
        }
        return result;
    }

    public int getSellCount() {
        int result = 0;
        for (TradeData data : list) {
            if (data.getType() == SELL) {
                result++;
            }
        }
        return result;
    }
}
