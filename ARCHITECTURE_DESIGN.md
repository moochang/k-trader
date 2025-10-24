# K-Trader 앱 Clean Architecture 설계 문서

## 📋 개요

빗썸 API v1.2.0과 v1.2.0/candlestick API를 활용한 K-Trader 앱의 Clean Architecture 설계입니다. 단일 책임 원칙(SRP)을 준수하여 각 레이어와 컴포넌트가 명확한 역할을 가지도록 설계했습니다.

## 🏗️ 전체 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
├─────────────────────────────────────────────────────────────────┤
│  Activities/Fragments  │  ViewModels  │  UI Components          │
│  - MainActivity        │  - MainVM    │  - TransactionCard       │
│  - OrderActivity       │  - OrderVM   │  - CoinInfo             │
│  - HistoryActivity     │  - HistoryVM │  - BalanceDisplay        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Domain Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  Use Cases          │  Repository Interfaces  │  Models         │
│  - GetTickerData    │  - TickerRepository     │  - TransactionData│
│  - GetBalanceData   │  - BalanceRepository    │  - OrderData     │
│  - GetOrderData     │  - OrderRepository      │  - BalanceData   │
│  - SaveData         │  - CandlestickRepository│  - ApiError      │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Data Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  Repository Impl    │  API Service    │  Local Database         │
│  - TickerRepoImpl   │  - BithumbApi   │  - Room Database        │
│  - BalanceRepoImpl  │  - HttpClient   │  - Entities             │
│  - OrderRepoImpl    │  - Gson Parser  │  - DAOs                 │
│  - CandlestickRepo  │  - Error Handler│  - Type Converters      │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      External Services                          │
├─────────────────────────────────────────────────────────────────┤
│  Bithumb API v1.2.0  │  Local Storage  │  Network               │
│  - /public/ticker     │  - Room DB      │  - OkHttp              │
│  - /info/balance      │  - SharedPrefs  │  - Retrofit            │
│  - /info/orders       │  - File System  │  - Interceptors         │
│  - /public/candlestick│  - Cache        │  - Logging              │
└─────────────────────────────────────────────────────────────────┘
```

## 📊 데이터 흐름 (Data Flow)

### 1. API 호출 흐름
```
UI Request → ViewModel → UseCase → Repository → API Service → Bithumb API
     ↓                                                              ↓
UI Update ← ViewModel ← UseCase ← Repository ← Data Parser ← API Response
```

### 2. 로컬 저장 흐름
```
API Response → Data Parser → Entity Converter → Room DAO → Local Database
     ↓                                                              ↓
UI Update ← ViewModel ← Repository ← Local Data ← Room Query ← Database
```

## 🎯 레이어별 상세 설계

### Presentation Layer (UI Layer)
**책임**: 사용자 인터페이스 표시 및 사용자 상호작용 처리

**구성 요소**:
- **Activities/Fragments**: UI 컨트롤러
- **ViewModels**: UI 상태 관리 및 비즈니스 로직 호출
- **UI Components**: 재사용 가능한 UI 컴포넌트

**SRP 준수**:
- 각 Activity/Fragment는 하나의 화면만 담당
- ViewModel은 UI 상태 관리만 담당
- UI Component는 특정 기능만 담당

### Domain Layer (비즈니스 로직)
**책임**: 비즈니스 로직 및 도메인 규칙 정의

**구성 요소**:
- **Use Cases**: 비즈니스 로직 캡슐화
- **Repository Interfaces**: 데이터 접근 추상화
- **Models**: 도메인 모델 (순수 Java 객체)

**SRP 준수**:
- 각 UseCase는 하나의 비즈니스 시나리오만 담당
- Repository Interface는 하나의 도메인만 담당
- Model은 특정 도메인 엔티티만 담당

### Data Layer (데이터 관리)
**책임**: 데이터 소스 통합 및 데이터 변환

**구성 요소**:
- **Repository Implementations**: 데이터 소스 통합
- **API Service**: 외부 API 호출 및 파싱
- **Local Database**: Room을 사용한 로컬 저장

**SRP 준수**:
- 각 Repository는 하나의 도메인만 담당
- API Service는 API 호출만 담당
- DAO는 특정 엔티티만 담당

## 🔧 주요 설계 패턴

### 1. Repository Pattern
```java
public interface TickerRepository {
    Flowable<BithumbTickerEntity> observeLatestTicker(String coinPair);
    Completable fetchAndSaveTicker(String coinPair);
    Single<List<BithumbTickerEntity>> getTickerHistory(String coinPair, Date fromTime, int limit);
}
```

**장점**:
- 데이터 소스 추상화
- 테스트 용이성
- 캐싱 전략 구현 가능

### 2. Observer Pattern (RxJava)
```java
// 실시간 데이터 관찰
tickerRepository.observeLatestTicker("BTC_KRW")
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(ticker -> {
        // UI 업데이트
        updateTickerDisplay(ticker);
    });
```

**장점**:
- 반응형 프로그래밍
- 비동기 처리
- 메모리 효율성

### 3. Dependency Injection
```java
// 의존성 주입을 통한 느슨한 결합
public class MainViewModel {
    private final TickerRepository tickerRepository;
    private final BalanceRepository balanceRepository;
    
    public MainViewModel(TickerRepository tickerRepo, BalanceRepository balanceRepo) {
        this.tickerRepository = tickerRepo;
        this.balanceRepository = balanceRepo;
    }
}
```

**장점**:
- 느슨한 결합
- 테스트 용이성
- 코드 재사용성

## 📱 API 엔드포인트 설계

### 빗썸 API v1.2.0 엔드포인트

#### 1. Public API (인증 불필요)
- **`GET /public/ticker/{coinPair}`**: 실시간 시세 정보
- **`GET /public/candlestick/{coinPair}/{interval}`**: 캔들스틱 차트 데이터

#### 2. Private API (인증 필요)
- **`GET /info/balance`**: 계좌 잔고 정보
- **`GET /info/orders`**: 주문 내역 조회
- **`POST /trade/place`**: 주문 등록
- **`POST /trade/cancel`**: 주문 취소

### 데이터 모델 설계

#### API 응답 모델
```java
public static class TickerResponse {
    public String status;
    public Map<String, TickerData> data;
    public String message;
}

public static class TickerData {
    public String openingPrice;
    public String closingPrice;
    public String minPrice;
    public String maxPrice;
    public String fluctateRate24H;
    public String fluctateRate1H;
    // ... 기타 필드들
}
```

## 🗄️ Room Database 설계

### 엔티티 설계
```java
@Entity(tableName = "bithumb_ticker")
public static class BithumbTickerEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    @ColumnInfo(name = "coin_pair")
    public String coinPair;
    
    @ColumnInfo(name = "closing_price")
    public double closingPrice;
    
    @ColumnInfo(name = "fluctate_rate_1h")
    public double fluctateRate1H;
    
    @ColumnInfo(name = "timestamp")
    @TypeConverters(DateConverter.class)
    public Date timestamp;
}
```

### DAO 설계
```java
@Dao
public interface BithumbTickerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertTicker(BithumbTickerEntity ticker);
    
    @Query("SELECT * FROM bithumb_ticker WHERE coinPair = :coinPair ORDER BY timestamp DESC LIMIT 1")
    Flowable<BithumbTickerEntity> observeLatestTicker(String coinPair);
    
    @Query("SELECT * FROM bithumb_ticker WHERE coinPair = :coinPair AND timestamp >= :fromTime ORDER BY timestamp ASC LIMIT :limit")
    Single<List<BithumbTickerEntity>> getTickerHistory(String coinPair, Date fromTime, int limit);
}
```

### 성능 최적화
- **인덱스 활용**: `coinPair`, `timestamp` 필드에 인덱스 생성
- **배치 처리**: 여러 데이터를 한 번에 삽입
- **데이터 정리**: 오래된 데이터 자동 삭제

## 🛡️ 에러 처리 전략

### 1. API 에러 처리
```java
public class ApiErrorHandler {
    public static void handleApiError(BithumbApiError error) {
        switch (error.getErrorCode()) {
            case "5100": // 서버 점검
                showMaintenanceMessage();
                break;
            case "5300": // API 호출 제한
                showRateLimitMessage();
                break;
            default:
                showGenericErrorMessage(error.getMessage());
        }
    }
}
```

### 2. 네트워크 에러 처리
```java
public class NetworkErrorHandler {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
```

### 3. 데이터베이스 에러 처리
```java
public class DatabaseErrorHandler {
    public static void handleDatabaseError(Throwable error) {
        if (error instanceof SQLiteException) {
            Log.e("DatabaseError", "SQLite error occurred", error);
            // 데이터베이스 복구 로직
        } else if (error instanceof RoomException) {
            Log.e("DatabaseError", "Room database error occurred", error);
            // Room 특화 에러 처리
        }
    }
}
```

## ⚡ 성능 최적화 전략

### 1. 데이터베이스 최적화
- **인덱스 활용**: 자주 조회되는 필드에 인덱스 생성
- **배치 삽입**: 여러 레코드를 한 번에 삽입
- **오래된 데이터 정리**: 주기적으로 오래된 데이터 삭제
- **쿼리 최적화**: 필요한 데이터만 조회

### 2. 네트워크 최적화
- **HTTP 캐싱**: 적절한 캐시 헤더 설정
- **요청 배치 처리**: 여러 API 호출을 배치로 처리
- **백그라운드 동기화**: 백그라운드에서 데이터 동기화
- **연결 풀링**: HTTP 연결 재사용

### 3. 메모리 최적화
- **이미지 캐싱**: 이미지 메모리 캐싱
- **객체 재사용**: ViewHolder 패턴 활용
- **메모리 누수 방지**: 적절한 생명주기 관리
- **WeakReference 사용**: 순환 참조 방지

## 🔒 보안 고려사항

### 1. API 키 관리
- **SharedPreferences 암호화**: 민감한 데이터 암호화 저장
- **ProGuard 난독화**: 코드 난독화로 역공학 방지
- **키 로테이션**: 주기적인 API 키 교체

### 2. 데이터 보호
- **민감한 데이터 암호화**: 잔고, 주문 정보 암호화
- **로컬 DB 암호화**: Room 데이터베이스 암호화
- **네트워크 통신 암호화**: HTTPS 사용

### 3. 인증 및 권한
- **API 서명 검증**: HMAC-SHA512 서명 검증
- **Nonce 사용**: 재사용 공격 방지
- **타임스탬프 검증**: 시간 기반 공격 방지

## 🧪 테스트 전략

### 1. 단위 테스트
```java
@Test
public void testTickerRepository() {
    // Given
    TickerRepository repository = new TickerRepositoryImpl(mockDao, mockApiService, mockStatsRepo);
    
    // When
    Single<BithumbTickerEntity> result = repository.getLatestTicker("BTC_KRW");
    
    // Then
    result.test()
        .assertComplete()
        .assertValue(ticker -> "BTC_KRW".equals(ticker.coinPair));
}
```

### 2. 통합 테스트
- **API 통합 테스트**: 실제 API 호출 테스트
- **데이터베이스 통합 테스트**: Room DB 동작 테스트
- **UI 통합 테스트**: Fragment/Activity 테스트

### 3. E2E 테스트
- **전체 사용자 시나리오 테스트**: 완전한 사용자 플로우 테스트
- **성능 테스트**: API 응답 시간, 메모리 사용량 테스트
- **보안 테스트**: 보안 취약점 테스트

## 📈 모니터링 및 로깅

### 1. API 호출 모니터링
```java
@Entity(tableName = "api_call_stats")
public static class ApiCallStatsEntity {
    public String endpoint;
    public String method;
    public int statusCode;
    public long responseTimeMs;
    public boolean success;
    public String errorMessage;
    public Date timestamp;
}
```

### 2. 성능 모니터링
- **API 응답 시간**: 각 엔드포인트별 응답 시간 측정
- **성공률**: API 호출 성공률 추적
- **에러율**: 에러 발생 빈도 모니터링

### 3. 로깅 전략
- **구조화된 로깅**: JSON 형태의 구조화된 로그
- **로그 레벨**: DEBUG, INFO, WARN, ERROR 레벨 구분
- **민감 정보 제외**: API 키, 개인정보 등 민감 정보 로깅 제외

## 🚀 마이그레이션 계획

### 1. 단계별 마이그레이션
1. **Phase 1**: 새로운 API 모델 및 서비스 구현
2. **Phase 2**: Repository 패턴 적용
3. **Phase 3**: Room Database 마이그레이션
4. **Phase 4**: UI 레이어 업데이트

### 2. 호환성 유지
- **기존 API와 병행 운영**: 점진적 전환
- **데이터 마이그레이션**: 기존 데이터 보존
- **롤백 계획**: 문제 발생 시 이전 버전으로 복구

### 3. 테스트 전략
- **A/B 테스트**: 새/구 시스템 비교 테스트
- **성능 테스트**: 성능 개선 효과 측정
- **사용자 피드백**: 사용자 경험 개선

## 📋 체크리스트

### 설계 완료 항목
- ✅ 빗썸 API v1.2.0 스펙 분석
- ✅ 타입 안전한 데이터 모델 설계
- ✅ Room DB 엔티티 및 DAO 설계
- ✅ Repository 패턴 적용
- ✅ API 서비스 레이어 설계
- ✅ Clean Architecture 구조 설계
- ✅ SRP 원칙 준수
- ✅ 에러 처리 전략 수립
- ✅ 성능 최적화 방안 수립
- ✅ 보안 고려사항 정의
- ✅ 테스트 전략 수립
- ✅ 모니터링 및 로깅 계획

### 구현 예정 항목
- ⏳ UseCase 구현
- ⏳ ViewModel 구현
- ⏳ UI 컴포넌트 업데이트
- ⏳ 의존성 주입 설정
- ⏳ 단위 테스트 작성
- ⏳ 통합 테스트 작성
- ⏳ 성능 테스트 실행
- ⏳ 보안 테스트 실행

## 🎯 결론

이 설계는 Clean Architecture와 SRP 원칙을 준수하여 K-Trader 앱의 확장성, 유지보수성, 테스트 용이성을 크게 향상시킵니다. 각 레이어와 컴포넌트가 명확한 책임을 가지며, 느슨한 결합을 통해 시스템의 유연성을 확보했습니다.

빗썸 API v1.2.0의 모든 주요 엔드포인트를 지원하며, Room Database를 활용한 효율적인 로컬 데이터 관리와 RxJava를 통한 반응형 프로그래밍으로 사용자에게 최적의 경험을 제공할 수 있습니다.
