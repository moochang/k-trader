package com.example.k_trader.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.TypeConverters;
import com.example.k_trader.database.converters.DateConverter;
import java.util.Date;

/**
 * 빗썸 API 데이터를 저장하기 위한 Room DB 엔티티들
 * Clean Architecture의 Data Layer에 해당
 * SRP 원칙에 따라 각 엔티티는 하나의 책임만 가짐
 */
public class BiThumbApiEntities {

    /**
     * Ticker 데이터 엔티티
     * 실시간 시세 정보를 저장
     */
    @Entity(
        tableName = "bithumb_ticker",
        indices = {
            @Index(value = {"coinPair", "timestamp"}),
            @Index(value = {"timestamp"})
        }
    )
    public static class BithumbTickerEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        
        @ColumnInfo(name = "coin_pair")
        public String coinPair; // BTC_KRW, ETH_KRW 등
        
        @ColumnInfo(name = "opening_price")
        public double openingPrice;
        
        @ColumnInfo(name = "closing_price")
        public double closingPrice;
        
        @ColumnInfo(name = "min_price")
        public double minPrice;
        
        @ColumnInfo(name = "max_price")
        public double maxPrice;
        
        @ColumnInfo(name = "average_price")
        public double averagePrice;
        
        @ColumnInfo(name = "units_traded")
        public double unitsTraded;
        
        @ColumnInfo(name = "volume_1day")
        public double volume1Day;
        
        @ColumnInfo(name = "volume_7day")
        public double volume7Day;
        
        @ColumnInfo(name = "fluctate_24h")
        public double fluctate24H;
        
        @ColumnInfo(name = "fluctate_rate_24h")
        public double fluctateRate24H;
        
        @ColumnInfo(name = "fluctate_rate_1h")
        public double fluctateRate1H;
        
        @ColumnInfo(name = "timestamp")
        @TypeConverters(DateConverter.class)
        public Date timestamp;
        
        @ColumnInfo(name = "created_at")
        @TypeConverters(DateConverter.class)
        public Date createdAt;
        
        // 기본 생성자
        public BithumbTickerEntity() {
            this.createdAt = new Date();
        }
        
        // 생성자
        public BithumbTickerEntity(String coinPair, double openingPrice, double closingPrice,
                                  double minPrice, double maxPrice, double averagePrice,
                                  double unitsTraded, double volume1Day, double volume7Day,
                                  double fluctate24H, double fluctateRate24H, double fluctateRate1H,
                                  Date timestamp) {
            this.coinPair = coinPair;
            this.openingPrice = openingPrice;
            this.closingPrice = closingPrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.averagePrice = averagePrice;
            this.unitsTraded = unitsTraded;
            this.volume1Day = volume1Day;
            this.volume7Day = volume7Day;
            this.fluctate24H = fluctate24H;
            this.fluctateRate24H = fluctateRate24H;
            this.fluctateRate1H = fluctateRate1H;
            this.timestamp = timestamp;
            this.createdAt = new Date();
        }
    }

    /**
     * Balance 데이터 엔티티
     * 계좌 잔고 정보를 저장
     */
    @Entity(
        tableName = "bithumb_balance",
        indices = {
            @Index(value = {"timestamp"}),
            @Index(value = {"currency"})
        }
    )
    public static class BithumbBalanceEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        
        @ColumnInfo(name = "currency")
        public String currency; // KRW, BTC, ETH 등
        
        @ColumnInfo(name = "total")
        public double total;
        
        @ColumnInfo(name = "in_use")
        public double inUse;
        
        @ColumnInfo(name = "available")
        public double available;
        
        @ColumnInfo(name = "timestamp")
        @TypeConverters(DateConverter.class)
        public Date timestamp;
        
        @ColumnInfo(name = "created_at")
        @TypeConverters(DateConverter.class)
        public Date createdAt;
        
        // 기본 생성자
        public BithumbBalanceEntity() {
            this.createdAt = new Date();
        }
        
        // 생성자
        public BithumbBalanceEntity(String currency, double total, double inUse, double available, Date timestamp) {
            this.currency = currency;
            this.total = total;
            this.inUse = inUse;
            this.available = available;
            this.timestamp = timestamp;
            this.createdAt = new Date();
        }
    }

    /**
     * Order 데이터 엔티티
     * 주문 내역을 저장
     */
    @Entity(
        tableName = "bithumb_orders",
        indices = {
            @Index(value = {"orderId"}),
            @Index(value = {"coinPair", "timestamp"}),
            @Index(value = {"type", "status"}),
            @Index(value = {"timestamp"})
        }
    )
    public static class BithumbOrderEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        
        @ColumnInfo(name = "order_id")
        public String orderId;
        
        @ColumnInfo(name = "coin_pair")
        public String coinPair; // BTC_KRW, ETH_KRW 등
        
        @ColumnInfo(name = "type")
        public String type; // bid (매수), ask (매도)
        
        @ColumnInfo(name = "status")
        public String status; // placed, filled, cancelled 등
        
        @ColumnInfo(name = "order_price")
        public double orderPrice;
        
        @ColumnInfo(name = "order_qty")
        public double orderQty;
        
        @ColumnInfo(name = "exec_qty")
        public double execQty;
        
        @ColumnInfo(name = "exec_price")
        public double execPrice;
        
        @ColumnInfo(name = "exec_amount")
        public double execAmount;
        
        @ColumnInfo(name = "exec_fee")
        public double execFee;
        
        @ColumnInfo(name = "order_date")
        @TypeConverters(DateConverter.class)
        public Date orderDate;
        
        @ColumnInfo(name = "exec_date")
        @TypeConverters(DateConverter.class)
        public Date execDate;
        
        @ColumnInfo(name = "cancel_date")
        @TypeConverters(DateConverter.class)
        public Date cancelDate;
        
        @ColumnInfo(name = "timestamp")
        @TypeConverters(DateConverter.class)
        public Date timestamp;
        
        @ColumnInfo(name = "created_at")
        @TypeConverters(DateConverter.class)
        public Date createdAt;
        
        // 기본 생성자
        public BithumbOrderEntity() {
            this.createdAt = new Date();
        }
        
        // 생성자
        public BithumbOrderEntity(String orderId, String coinPair, String type, String status,
                                double orderPrice, double orderQty, double execQty, double execPrice,
                                double execAmount, double execFee, Date orderDate, Date execDate,
                                Date cancelDate, Date timestamp) {
            this.orderId = orderId;
            this.coinPair = coinPair;
            this.type = type;
            this.status = status;
            this.orderPrice = orderPrice;
            this.orderQty = orderQty;
            this.execQty = execQty;
            this.execPrice = execPrice;
            this.execAmount = execAmount;
            this.execFee = execFee;
            this.orderDate = orderDate;
            this.execDate = execDate;
            this.cancelDate = cancelDate;
            this.timestamp = timestamp;
            this.createdAt = new Date();
        }
    }

    /**
     * Candlestick 데이터 엔티티
     * 캔들스틱 차트 데이터를 저장
     */
    @Entity(
        tableName = "bithumb_candlestick",
        indices = {
            @Index(value = {"coinPair", "interval", "timestamp"}),
            @Index(value = {"timestamp"}),
            @Index(value = {"coinPair", "interval"})
        }
    )
    public static class BithumbCandlestickEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        
        @ColumnInfo(name = "coin_pair")
        public String coinPair; // BTC_KRW, ETH_KRW 등
        
        @ColumnInfo(name = "interval")
        public String interval; // 1m, 3m, 5m, 10m, 30m, 1h, 6h, 12h, 24h
        
        @ColumnInfo(name = "timestamp")
        @TypeConverters(DateConverter.class)
        public Date timestamp;
        
        @ColumnInfo(name = "open")
        public double open;
        
        @ColumnInfo(name = "close")
        public double close;
        
        @ColumnInfo(name = "high")
        public double high;
        
        @ColumnInfo(name = "low")
        public double low;
        
        @ColumnInfo(name = "volume")
        public double volume;
        
        @ColumnInfo(name = "created_at")
        @TypeConverters(DateConverter.class)
        public Date createdAt;
        
        // 기본 생성자
        public BithumbCandlestickEntity() {
            this.createdAt = new Date();
        }
        
        // 생성자
        public BithumbCandlestickEntity(String coinPair, String interval, Date timestamp,
                                      double open, double close, double high, double low, double volume) {
            this.coinPair = coinPair;
            this.interval = interval;
            this.timestamp = timestamp;
            this.open = open;
            this.close = close;
            this.high = high;
            this.low = low;
            this.volume = volume;
            this.createdAt = new Date();
        }
    }

    /**
     * API 호출 통계 엔티티
     * API 호출 성능 모니터링을 위한 통계 데이터
     */
    @Entity(
        tableName = "api_call_stats",
        indices = {
            @Index(value = {"endpoint", "timestamp"}),
            @Index(value = {"timestamp"})
        }
    )
    public static class ApiCallStatsEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        
        @ColumnInfo(name = "endpoint")
        public String endpoint; // /info/ticker, /info/balance 등
        
        @ColumnInfo(name = "method")
        public String method; // GET, POST
        
        @ColumnInfo(name = "status_code")
        public int statusCode;
        
        @ColumnInfo(name = "response_time_ms")
        public long responseTimeMs;
        
        @ColumnInfo(name = "success")
        public boolean success;
        
        @ColumnInfo(name = "error_message")
        public String errorMessage;
        
        @ColumnInfo(name = "timestamp")
        @TypeConverters(DateConverter.class)
        public Date timestamp;
        
        @ColumnInfo(name = "created_at")
        @TypeConverters(DateConverter.class)
        public Date createdAt;
        
        // 기본 생성자
        public ApiCallStatsEntity() {
            this.createdAt = new Date();
        }
        
        // 생성자
        public ApiCallStatsEntity(String endpoint, String method, int statusCode,
                                long responseTimeMs, boolean success, String errorMessage, Date timestamp) {
            this.endpoint = endpoint;
            this.method = method;
            this.statusCode = statusCode;
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
            this.createdAt = new Date();
        }
    }
}
