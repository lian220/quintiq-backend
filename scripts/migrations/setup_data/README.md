# 초기 데이터 세팅 가이드

**목적**: PostgreSQL 및 MongoDB에 초기 데이터 삽입
**작성일**: 2026-02-01

---

## 📋 초기 데이터 목록

### 1. Stocks (35개 종목)
미국 주식 및 ETF 메타데이터

**데이터**:
- 개별 주식: 32개 (AAPL, MSFT, NVDA, TSLA 등)
- ETF: 3개 (SPY, QQQ, SOXX)

### 2. FRED Indicators (16개)
FRED 경제 지표 설정

**카테고리**:
- 경제 성장: GDP, PCE
- 노동 시장: UNRATE
- 인플레이션: CPI, T10YIE
- 금리: FEDFUNDS, DGS2, DGS10, T10Y2Y
- 금융: STLFSI4
- 통화: M2, DTWEXM
- 부채: TDSP
- 주택: MORTGAGE5US
- 소비자: UMCSENT
- 시장: NASDAQCOM

### 3. Yahoo Finance Indicators (22개)
시장 지표 (지수, ETF, 통화, 원자재)

**카테고리**:
- 미국 지수: S&P 500, NASDAQ 100, VIX, 달러 인덱스
- 글로벌 지수: 닛케이 225, 상해종합, 항셍, FTSE, DAX, CAC 40
- ETF: SPY, QQQ, IWM, DIA, AGG, TIP, LQD, VNQ, SOXX
- 통화: JPY=X, CNY=X
- 원자재: 금 (GC=F)

---

## 🚀 PostgreSQL 초기 데이터 세팅

### 자동 세팅 (Flyway 마이그레이션)

프로젝트 시작 시 자동으로 실행됩니다:

```bash
./start.sh
```

**실행되는 마이그레이션**:
1. `V6__Create_Stocks_Table.sql` - Stocks 테이블 생성
2. `V7__Insert_Initial_Stocks_Data.sql` - **35개 종목 삽입**
3. `V9__Create_Economic_Indicators_Tables.sql` - 경제 지표 테이블 생성
4. `V10__Insert_Initial_Indicators.sql` - **FRED 16개 + Yahoo Finance 22개 삽입**

### 수동 세팅 (필요 시)

이미 실행된 마이그레이션을 재실행하려면:

```bash
# PostgreSQL 접속
docker exec -it quantiq-postgres psql -U quantiq_user -d quantiq

# 특정 테이블만 초기화
TRUNCATE stocks RESTART IDENTITY CASCADE;
TRUNCATE fred_indicators RESTART IDENTITY CASCADE;
TRUNCATE yfinance_indicators RESTART IDENTITY CASCADE;

# SQL 파일 실행
\i /path/to/V7__Insert_Initial_Stocks_Data.sql
\i /path/to/V10__Insert_Initial_Indicators.sql
```

---

## 🗄️ MongoDB 초기 데이터 세팅

### 방법 1: 스크립트 실행 (권장)

```bash
cd scripts/migrations/setup_data

# 1. FRED + Yahoo Finance 지표
mongosh "mongodb+srv://..." < 01_insert_mongodb_indicators.js

# 2. Stocks 종목
mongosh "mongodb+srv://..." < 02_insert_mongodb_stocks.js
```

**MongoDB URI 확인**:
```bash
# .env.prod 파일에서 확인
grep MONGODB_URI .env.prod
```

### 방법 2: mongosh에서 직접 실행

```bash
# MongoDB Atlas 접속
mongosh "mongodb+srv://..."

# 데이터베이스 선택
use stock_trading

# 스크립트 로드
load("01_insert_mongodb_indicators.js")
load("02_insert_mongodb_stocks.js")
```

---

## ✅ 검증

### PostgreSQL 검증

```sql
-- Stocks 확인
SELECT COUNT(*) FROM stocks;
-- 예상: 35개

SELECT ticker, stock_name, is_etf
FROM stocks
ORDER BY ticker
LIMIT 5;

-- FRED Indicators 확인
SELECT COUNT(*) FROM fred_indicators;
-- 예상: 16개 이상

SELECT code, name, frequency
FROM fred_indicators
WHERE is_active = true;

-- Yahoo Finance Indicators 확인
SELECT COUNT(*) FROM yfinance_indicators;
-- 예상: 22개 이상

SELECT ticker, name, indicator_type
FROM yfinance_indicators
WHERE is_active = true;
```

### MongoDB 검증

```javascript
use stock_trading

// Stocks 확인
db.stocks.countDocuments()
// 예상: 35개

db.stocks.find({ is_active: true }).limit(5)

// FRED Indicators 확인
db.fred_indicators.countDocuments()
// 예상: 16개

db.fred_indicators.find({ is_active: true })

// Yahoo Finance Indicators 확인
db.yfinance_indicators.countDocuments()
// 예상: 22개

db.yfinance_indicators.find({ is_active: true })
```

---

## 📂 파일 구조

```
scripts/migrations/setup_data/
├── README.md                               # 이 파일
├── 01_insert_mongodb_indicators.js         # MongoDB: FRED + Yahoo Finance
└── 02_insert_mongodb_stocks.js             # MongoDB: Stocks
```

```
quantiq-core/src/main/resources/db/migration/
├── V6__Create_Stocks_Table.sql             # Stocks 테이블 생성
├── V7__Insert_Initial_Stocks_Data.sql      # Stocks 초기 데이터 (35개)
├── V9__Create_Economic_Indicators_Tables.sql  # 경제 지표 테이블
└── V10__Insert_Initial_Indicators.sql      # FRED(16개) + Yahoo Finance(22개)
```

---

## 🔄 데이터 업데이트

### 새로운 종목 추가

#### PostgreSQL
```sql
INSERT INTO stocks (ticker, stock_name, stock_name_en, is_etf, is_active)
VALUES ('NEW', '새 종목', 'New Stock', FALSE, TRUE)
ON CONFLICT (ticker) DO NOTHING;
```

#### MongoDB
```javascript
db.stocks.insertOne({
  ticker: "NEW",
  stock_name: "새 종목",
  stock_name_en: "New Stock",
  is_etf: false,
  is_active: true,
  created_at: new Date(),
  updated_at: new Date()
})
```

### 새로운 경제 지표 추가

#### PostgreSQL
```sql
-- FRED
INSERT INTO fred_indicators (code, name, description, category, frequency)
VALUES ('NEWCODE', '새 지표', 'New Economic Indicator', 'Economic', 'daily')
ON CONFLICT (code) DO NOTHING;

-- Yahoo Finance
INSERT INTO yfinance_indicators (ticker, name, description, indicator_type)
VALUES ('NEW', '새 지표', 'New Market Indicator', 'INDEX')
ON CONFLICT (ticker) DO NOTHING;
```

#### MongoDB
```javascript
// FRED
db.fred_indicators.insertOne({
  code: "NEWCODE",
  name: "새 지표",
  type: "economic",
  is_active: true,
  created_at: new Date(),
  updated_at: new Date()
})

// Yahoo Finance
db.yfinance_indicators.insertOne({
  ticker: "NEW",
  name: "새 지표",
  type: "index",
  is_active: true,
  created_at: new Date(),
  updated_at: new Date()
})
```

---

## ⚠️ 주의사항

1. **중복 방지**: 모든 insert 스크립트는 중복을 자동으로 방지합니다
   - PostgreSQL: `ON CONFLICT DO NOTHING`
   - MongoDB: `insertMany({ ordered: false })` + unique index

2. **데이터 일관성**: PostgreSQL과 MongoDB의 stocks 데이터는 ticker 기준으로 동기화해야 합니다
   - PostgreSQL: Source of Truth (메타데이터)
   - MongoDB: 참고용 (분석 데이터와 함께 사용)

3. **백업**: 초기 데이터 변경 전 반드시 백업하세요
   ```bash
   # PostgreSQL 백업
   pg_dump -U quantiq_user -d quantiq -t stocks > stocks_backup.sql

   # MongoDB 백업
   mongoexport --uri="..." --db stock_trading --collection stocks -o stocks_backup.json
   ```

---

## 📞 문제 해결

### PostgreSQL 연결 실패
```bash
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs quantiq-postgres
```

### MongoDB 연결 실패
```bash
# MongoDB URI 확인
echo $MONGODB_URI

# .env.prod 파일 확인
cat .env.prod | grep MONGODB
```

### 마이그레이션 실패
```bash
# Flyway 히스토리 확인
docker exec -it quantiq-postgres psql -U quantiq_user -d quantiq \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;"

# 실패한 마이그레이션 삭제 (주의!)
docker exec -it quantiq-postgres psql -U quantiq_user -d quantiq \
  -c "DELETE FROM flyway_schema_history WHERE success = false;"
```

---

**마지막 업데이트**: 2026-02-01
**작성자**: Quantiq Development Team
