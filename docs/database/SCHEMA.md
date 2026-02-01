# Database Schema Documentation

## 개요

QuantiQ 시스템은 **PostgreSQL**과 **MongoDB** 두 가지 데이터베이스를 사용하는 **폴리글롯 퍼시스턴스** 아키텍처를 채택하고 있습니다.

- **PostgreSQL (JPA)**: 트랜잭션 데이터, 사용자 정보, 거래 내역
- **MongoDB**: 분석 데이터, 시계열 데이터, 예측 결과

---

## PostgreSQL (JPA Entities)

### 1. users (사용자)

**테이블명**: `users`
**엔티티**: `UserEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| user_id | VARCHAR(50) | UNIQUE, NOT NULL | 사용자 고유 ID |
| name | VARCHAR(100) | NULL | 사용자 이름 |
| email | VARCHAR(100) | UNIQUE | 이메일 |
| password_hash | VARCHAR(255) | NULL | 비밀번호 해시 |
| status | VARCHAR(20) | NOT NULL | 상태 (ACTIVE, INACTIVE, SUSPENDED) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 연관 관계
- `1:1` → **trading_configs** (TradingConfigEntity)
- `1:1` → **account_balances** (AccountBalanceEntity)
- `1:1` → **user_kis_accounts** (UserKisAccountEntity)
- `1:N` → **trades** (TradeEntity)
- `1:N` → **trade_signals_executed** (TradeSignalExecutedEntity)
- `1:N` → **kis_tokens** (KisTokenEntity)

---

### 2. user_kis_accounts (KIS 계정 정보)

**테이블명**: `user_kis_accounts`
**엔티티**: `UserKisAccountEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | KIS 계정 ID |
| user_id | BIGINT | FK (users), UNIQUE, NOT NULL | 사용자 ID |
| app_key | VARCHAR(100) | NOT NULL | KIS App Key |
| app_secret_encrypted | VARCHAR(500) | NOT NULL | 암호화된 App Secret |
| account_number | VARCHAR(20) | NOT NULL | 계좌번호 (앞 8자리) |
| account_product_code | VARCHAR(2) | NOT NULL | 계좌 상품 코드 (01: 해외주식) |
| account_type | VARCHAR(10) | NOT NULL | 계정 타입 (REAL, MOCK) |
| enabled | BOOLEAN | NOT NULL | 활성화 여부 |
| last_used_at | TIMESTAMP | NULL | 마지막 사용 시간 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 연관 관계
- `N:1` → **users** (UserEntity)

#### 보안
- `app_secret_encrypted`: Jasypt 또는 AES 암호화 저장
- `getDecryptedAppSecret()`: 복호화 메서드 제공

---

### 3. kis_tokens (KIS API Access Token)

**테이블명**: `kis_tokens`
**엔티티**: `KisTokenEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 토큰 ID |
| user_id | BIGINT | FK (users), NOT NULL | 사용자 ID |
| account_type | VARCHAR(10) | NOT NULL | 계정 타입 (REAL, MOCK) |
| access_token | TEXT | NOT NULL | Access Token |
| expiration_time | TIMESTAMP | NOT NULL | 만료 시간 |
| is_active | BOOLEAN | NOT NULL | 활성화 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 인덱스
- **UNIQUE**: `(user_id, account_type)` - 사용자별, 계정 타입별 유니크 제약
- **INDEX**: `idx_kis_tokens_user_account` - 조회 성능 최적화
- **INDEX**: `idx_kis_tokens_expiration` - 만료 토큰 정리 최적화

#### 연관 관계
- `N:1` → **users** (UserEntity)

#### 메서드
- `isExpired()`: 토큰 만료 여부 확인
- `isValid()`: 토큰 유효성 확인 (활성화 + 만료 안 됨)

---

### 4. trading_configs (거래 설정)

**테이블명**: `trading_configs`
**엔티티**: `TradingConfigEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 설정 ID |
| user_id | BIGINT | FK (users), UNIQUE, NOT NULL | 사용자 ID |
| enabled | BOOLEAN | NOT NULL | 활성화 여부 |
| auto_trading_enabled | BOOLEAN | NOT NULL | 자동 거래 활성화 |
| min_composite_score | DECIMAL(5,2) | DEFAULT 2.0 | 최소 종합 점수 |
| max_stocks_to_buy | INT | DEFAULT 5 | 최대 매수 종목 수 |
| max_amount_per_stock | DECIMAL(12,2) | DEFAULT 10000.0 | 종목당 최대 투자 금액 |
| stop_loss_percent | DECIMAL(5,2) | DEFAULT -7.0 | 손절 비율 (%) |
| take_profit_percent | DECIMAL(5,2) | DEFAULT 5.0 | 익절 비율 (%) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 연관 관계
- `1:1` → **users** (UserEntity)

---

### 5. account_balances (계좌 잔액)

**테이블명**: `account_balances`
**엔티티**: `AccountBalanceEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 잔액 ID |
| user_id | BIGINT | FK (users), UNIQUE, NOT NULL | 사용자 ID |
| cash | DECIMAL(15,2) | NOT NULL | 현금 |
| total_value | DECIMAL(15,2) | NOT NULL | 총 자산 가치 |
| locked_cash | DECIMAL(15,2) | DEFAULT 0.0 | 잠긴 현금 (주문 대기 중) |
| version | BIGINT | NOT NULL | 낙관적 락 버전 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 연관 관계
- `1:1` → **users** (UserEntity)

#### 동시성 제어
- `@Version`: 낙관적 락(Optimistic Lock) 사용
- `getAvailableCash()`: 사용 가능한 현금 계산 (cash - lockedCash)

---

### 6. trades (거래 내역)

**테이블명**: `trades`
**엔티티**: `TradeEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 거래 ID |
| user_id | BIGINT | FK (users), NOT NULL | 사용자 ID |
| ticker | VARCHAR(10) | NOT NULL | 종목 티커 |
| side | VARCHAR(10) | NOT NULL | 거래 방향 (BUY, SELL) |
| quantity | INT | NOT NULL | 수량 |
| price | DECIMAL(10,2) | NOT NULL | 단가 |
| total_amount | DECIMAL(15,2) | NOT NULL | 총 거래 금액 |
| commission | DECIMAL(10,2) | DEFAULT 0.0 | 수수료 |
| status | VARCHAR(20) | DEFAULT 'PENDING' | 상태 (PENDING, EXECUTED, FAILED, CANCELLED) |
| kis_order_id | VARCHAR(100) | NULL | KIS 주문 ID |
| executed_at | TIMESTAMP | NULL | 체결 일시 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 연관 관계
- `N:1` → **users** (UserEntity)
- `1:1` ← **trade_signals_executed** (역참조)

---

### 7. trade_signals_executed (거래 신호 실행 기록)

**테이블명**: `trade_signals_executed`
**엔티티**: `TradeSignalExecutedEntity`

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 실행 기록 ID |
| user_id | BIGINT | FK (users), NOT NULL | 사용자 ID |
| recommendation_id | VARCHAR(100) | NOT NULL | 추천 ID (MongoDB 참조) |
| ticker | VARCHAR(10) | NOT NULL | 종목 티커 |
| signal | VARCHAR(20) | NOT NULL | 신호 (BUY, SELL, HOLD) |
| confidence | DECIMAL(3,2) | NOT NULL | 신뢰도 |
| execution_decision | VARCHAR(20) | NOT NULL | 실행 결정 (EXECUTED, SKIPPED, FAILED) |
| skip_reason | VARCHAR(255) | NULL | 스킵 사유 |
| executed_trade_id | BIGINT | FK (trades), NULL | 실행된 거래 ID |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |

#### 연관 관계
- `N:1` → **users** (UserEntity)
- `N:1` → **trades** (TradeEntity) - 실행된 거래 참조

---

### 8. stocks (종목 정보) ✨

**테이블명**: `stocks`
**엔티티**: `StockEntity`
**마이그레이션**: 2026-02-01 MongoDB → PostgreSQL 완료

#### 스키마
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 종목 ID |
| ticker | VARCHAR(20) | UNIQUE, NOT NULL | 종목 티커 (예: AAPL, TSLA) |
| stock_name | VARCHAR(200) | NOT NULL | 한글 종목명 |
| stock_name_en | VARCHAR(200) | NULL | 영문 종목명 |
| is_etf | BOOLEAN | NOT NULL, DEFAULT FALSE | ETF 여부 |
| leverage_ticker | VARCHAR(20) | NULL | 레버리지 상품 티커 (예: TQQQ) |
| exchange | VARCHAR(50) | NULL | 거래소 (NASDAQ, NYSE 등) |
| sector | VARCHAR(100) | NULL | 섹터 (Technology, Healthcare 등) |
| industry | VARCHAR(100) | NULL | 산업 (Consumer Electronics 등) |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 활성화 여부 (거래 가능) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

#### 인덱스
- **UNIQUE**: `uq_stocks_ticker` - ticker 유니크 제약
- **INDEX**: `idx_stocks_ticker` - ticker 조회 최적화
- **INDEX**: `idx_stocks_is_active` (WHERE is_active = TRUE) - 활성 종목 필터링
- **INDEX**: `idx_stocks_sector` (WHERE sector IS NOT NULL) - 섹터별 조회
- **INDEX**: `idx_stocks_industry` (WHERE industry IS NOT NULL) - 산업별 조회
- **INDEX**: `idx_stocks_is_etf` (WHERE is_etf = TRUE) - ETF 필터링

#### Repository 메서드
```kotlin
fun findByTicker(ticker: String): StockEntity?
fun findByIsActive(isActive: Boolean): List<StockEntity>
fun findByIsEtf(isEtf: Boolean): List<StockEntity>
fun findBySector(sector: String): List<StockEntity>
fun findByIndustry(industry: String): List<StockEntity>
fun findAllActiveStocks(): List<StockEntity>
fun findAllActiveNonEtfStocks(): List<StockEntity>
fun findAllActiveEtfs(): List<StockEntity>
```

#### 초기 데이터
- `V7__Insert_Initial_Stocks_Data.sql`로 35개 종목 데이터 관리
- AAPL, TSLA, NVDA, MSFT, QQQ, SPY 등 주요 미국 주식 및 ETF

#### 마이그레이션 히스토리
**2026-02-01**: MongoDB → PostgreSQL 마이그레이션
- **이유**: 정적 메타데이터로 RDB가 더 적합
- **결과**: 35개 stocks 데이터 이전 완료
- **다음 단계**: Dual-write 지원 → MongoDB 제거
- **참조**: [마이그레이션 문서](../../claudedocs/Stock_마이그레이션_MongoDB_to_PostgreSQL.md)

---

## MongoDB Collections

### 1. daily_stock_data (일별 주식 데이터) ⚠️ 컬렉션명 변경 예정

**컬렉션명**: `daily_stock_data` (stocks에서 변경됨)
**도메인 모델**: `DailyStockData`

#### 설명
- 이전에 `stocks` 컬렉션으로 혼용되던 것을 명확히 구분
- 주식 **메타데이터**(ticker, name 등)는 PostgreSQL `stocks` 테이블로 이동
- **시계열 데이터**(일별 가격, 거래량 등)는 MongoDB 유지

---

### 2. economic_data (경제 지표 데이터)

**컬렉션명**: `economic_data`
**도메인 모델**: `EconomicData`

#### 스키마
```javascript
{
  "_id": ObjectId,
  "date": ISODate,
  "indicators": {
    "GDP": Double,
    "CPI": Double,
    "UnemploymentRate": Double,
    "InterestRate": Double,
    // ... 기타 경제 지표
  },
  "created_at": ISODate
}
```

#### 인덱스
- `date` (UNIQUE)

---

### 3. stock_analysis_results (종목 분석 결과)

**컬렉션명**: `stock_analysis_results`
**도메인 모델**: `StockAnalysis`

#### 스키마
```javascript
{
  "_id": ObjectId,
  "ticker": String,
  "date": ISODate,
  "metrics": {
    "mae": Double,       // Mean Absolute Error
    "rmse": Double,      // Root Mean Square Error
    "accuracy": Double   // 정확도
  },
  "predictions": {
    "last_actual_price": Double,
    "predicted_future_price": Double,
    "predicted_rise": Boolean,
    "rise_probability": Double
  },
  "recommendation": String,  // 추천 의견
  "analysis": String,        // 분석 내용
  "created_at": ISODate
}
```

#### 인덱스
- `ticker`, `date`
- `created_at`

---

### 4. stock_recommendations (종목 추천)

**컬렉션명**: `stock_recommendations`
**도메인 모델**: `StockRecommendation`

#### 스키마
```javascript
{
  "_id": ObjectId,
  "ticker": String,
  "date": String,              // YYYY-MM-DD
  "stock_name": String,
  "current_price": Double,
  "composite_score": Double,   // 종합 점수
  "technical_indicators": {
    "sma20": Double,
    "sma50": Double,
    "sma200": Double,
    "rsi": Double,
    "macd": Double,
    "signal": Double,
    "macd_histogram": Double,
    "bollinger_upper": Double,
    "bollinger_lower": Double,
    "volume": Long,
    "avg_volume": Long
  },
  "sentiment_score": Double,
  "recommendation_reason": String,
  "is_recommended": Boolean,
  "updated_at": ISODate
}
```

#### 인덱스
- `ticker`, `date` (UNIQUE)
- `is_recommended`
- `composite_score` (DESC)

---

### 5. sentiment_analysis (감정 분석)

**컬렉션명**: `sentiment_analysis`
**도메인 모델**: `SentimentAnalysis`

#### 스키마
```javascript
{
  "_id": ObjectId,
  "ticker": String,
  "date": String,                    // YYYY-MM-DD
  "average_sentiment_score": Double, // 평균 감정 점수
  "article_count": Int,              // 뉴스 기사 수
  "updated_at": ISODate
}
```

#### 인덱스
- `ticker`, `date` (UNIQUE)
- `updated_at`

---

### 6. prediction_results (Vertex AI 예측 결과)

**컬렉션명**: `prediction_results`
**도메인 모델**: `PredictionResult`

#### 스키마
```javascript
{
  "_id": ObjectId,
  "symbol": String,                      // 종목 심볼
  "date": ISODate,                       // 예측 날짜
  "predicted_price": Double,             // 예측 가격
  "confidence": Double,                  // 신뢰도
  "signal": String,                      // BUY, SELL, HOLD
  "predicted_change_percent": Double,    // 예측 변동률 (%)
  "technical_score": Double,             // 기술적 점수
  "sentiment_score": Double,             // 감정 점수
  "model_version": String,               // 모델 버전
  "created_at": ISODate,
  "vertex_ai_job_id": String,            // Vertex AI Job ID
  "metadata": Object                     // 추가 메타데이터
}
```

#### 인덱스
- `symbol`, `date`
- `signal`
- `confidence` (DESC)
- `created_at` (DESC)

---

## 데이터베이스 간 연관 관계

### PostgreSQL ↔ MongoDB 참조

#### 1. 사용자 거래 → 종목 정보
```
UserEntity.trades (PostgreSQL)
  ↓ ticker 참조
Stock (MongoDB)
```

#### 2. 거래 신호 실행 → 추천 정보
```
TradeSignalExecutedEntity.recommendationId (PostgreSQL)
  ↓ MongoDB ObjectId 참조
StockRecommendation._id (MongoDB)
```

#### 3. 거래 신호 → 예측 결과
```
TradeSignalExecutedEntity.ticker (PostgreSQL)
  ↓ symbol 참조
PredictionResult.symbol (MongoDB)
```

---

## 마이그레이션 히스토리

### MongoDB → PostgreSQL 마이그레이션

| 컬렉션 | 테이블 | 상태 | 사유 |
|--------|--------|------|------|
| access_tokens | kis_tokens | ✅ 완료 | 트랜잭션 일관성, 인덱스 성능 |
| users (MongoDB) | users (PostgreSQL) | 🔄 병행 | 점진적 마이그레이션 |

---

## 성능 최적화

### PostgreSQL

#### 인덱스 전략
- **kis_tokens**: `(user_id, account_type)` UNIQUE, `expiration_time` INDEX
- **trades**: `user_id`, `ticker`, `created_at` INDEX
- **trade_signals_executed**: `user_id`, `ticker`, `created_at` INDEX

#### 동시성 제어
- **account_balances**: 낙관적 락 (`@Version`) 사용으로 잔액 동시 수정 방지

### MongoDB

#### 인덱스 전략
- **stocks**: `ticker` UNIQUE, `is_active`
- **stock_recommendations**: `(ticker, date)` UNIQUE, `composite_score` DESC
- **prediction_results**: `(symbol, date)`, `confidence` DESC, `created_at` DESC

#### 샤딩 전략 (미래)
- **stocks**: `ticker` 기준 샤딩
- **prediction_results**: `date` 기준 샤딩 (시계열 데이터)

---

## 보안 고려사항

### 민감 정보 암호화
1. **UserKisAccountEntity.appSecretEncrypted**
   - Jasypt 또는 AES-256 암호화
   - 복호화 메서드: `getDecryptedAppSecret()`

2. **KisTokenEntity.accessToken**
   - TEXT 타입으로 저장 (길이 제한 없음)
   - 만료 시간 관리로 보안 강화

### 접근 제어
- 사용자 데이터는 `user_id` 기준 격리
- API 레벨에서 사용자 인증 및 권한 확인

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────┐
│     users       │
│─────────────────│
│ id (PK)        │
│ user_id (UK)   │
│ email (UK)     │
│ status         │
└────────┬────────┘
         │
         │ 1:1
         ├──────────────┐
         │              │
         ▼              ▼
┌──────────────┐  ┌───────────────────┐
│trading_configs│  │account_balances  │
│──────────────│  │───────────────────│
│id (PK)       │  │id (PK)           │
│user_id (FK)  │  │user_id (FK)      │
│enabled       │  │cash              │
│max_stocks    │  │total_value       │
└──────────────┘  │locked_cash       │
                  │version (@Version)│
                  └───────────────────┘
         │
         │ 1:1
         ▼
┌─────────────────────┐
│user_kis_accounts    │
│─────────────────────│
│id (PK)             │
│user_id (FK, UK)    │
│app_key             │
│app_secret_encrypted│
│account_number      │
│account_type        │
└─────────────────────┘
         │
         │ 1:N
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌───────────────┐  ┌──────────────────────┐
│   trades      │  │trade_signals_executed│
│───────────────│  │──────────────────────│
│id (PK)        │  │id (PK)              │
│user_id (FK)   │  │user_id (FK)         │
│ticker         │  │recommendation_id    │◄─── MongoDB Reference
│side (BUY/SELL)│  │ticker               │
│quantity       │  │signal (BUY/SELL)    │
│price          │  │confidence           │
│status         │  │execution_decision   │
│kis_order_id   │  │executed_trade_id(FK)│
└───────────────┘  └──────────────────────┘
         │
         │ 1:N
         ▼
┌───────────────┐
│  kis_tokens   │
│───────────────│
│id (PK)        │
│user_id (FK)   │
│account_type   │
│access_token   │
│expiration_time│
│is_active      │
└───────────────┘
```

---

## 참고 문서
- [Flyway Migration Scripts](/src/main/resources/db/migration)
- [JPA Entity Package](/src/main/kotlin/com/quantiq/core/adapter/output/persistence/jpa)
- [MongoDB Domain Package](/src/main/kotlin/com/quantiq/core/domain)
