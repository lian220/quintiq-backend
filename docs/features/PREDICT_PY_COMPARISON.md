# predict.py 버전 비교 분석

## 🔍 3가지 버전 비교

### 1️⃣ GitLab Repository (https://gitlab.com/banbu3/banbu-stocktrading)

**데이터베이스**: ❌ **Supabase (PostgreSQL)**

**데이터 구조**:
```python
# Supabase 테이블: economic_and_stock_data
{
  "날짜": "2026-01-31",

  # 주식 가격 (27개 컬럼)
  "애플": 150.25,
  "마이크로소프트": 380.50,
  "아마존": 170.30,
  "구글 A": 140.20,
  # ... 23개 더
  "S&P 500 ETF": 450.12,
  "QQQ ETF": 380.45,

  # 경제 지표 (37개 컬럼)
  "GDP": 123.45,
  "실업률": 3.7,
  "인플레이션": 2.5,
  # ... 34개 더
}
```

**특징**:
- ✅ 날짜별 단일 행(row)에 모든 데이터 통합
- ✅ 관계형 DB 구조 (컬럼 기반)
- ❌ MongoDB와 호환 안됨
- ❌ 현재 Quantiq 프로젝트와 DB 타입 자체가 다름

**데이터 조회 방식**:
```python
# Supabase 클라이언트 사용
response = supabase.table("economic_and_stock_data").select("*").order("날짜", desc=False).execute()
df = pd.DataFrame(response.data)
```

---

### 2️⃣ Local Repository (/Users/imdoyeong/Desktop/workSpace/stock-trading)

**데이터베이스**: ✅ **MongoDB**

**데이터 구조**:
```javascript
// MongoDB 컬렉션: daily_stock_data
{
  _id: ObjectId("..."),
  date: "2026-01-31",

  // FRED 경제 지표 (중첩 객체)
  fred_indicators: {
    "GDP": 123.45,
    "Unemployment_Rate": 3.7,
    "CPI": 2.5,
    "Treasury_10Y": 4.2,
    "USD_KRW": 1462.89
  },

  // Yahoo Finance 지수 (중첩 객체)
  yfinance_indicators: {
    "S&P 500 ETF": 127.5,
    "QQQ ETF": 42.0,
    "SOXX ETF": 22.14,
    "SP500": 4500.12,
    "NASDAQ": 15000.34,
    "KOSPI": 2600.0,
    "Gold": 1850.0
  },

  // 개별 주식 (중첩 객체)
  stocks: {
    "AMZN": {
      close_price: 2.24,
      short_interest: {
        sharesShort: 76073227,
        sharesShortPriorMonth: 70637204,
        shortRatio: 1.76,
        shortPercentOfFloat: 0.0079
      }
    },
    "GOOGL": { close_price: 10.82, short_interest: {...} },
    "NVDA": { close_price: 0.37, short_interest: {...} }
    // ... 더 많은 주식
  },

  updated_at: ISODate("2025-12-28T05:20:49.119Z")
}
```

**특징**:
- ✅ 날짜별 단일 문서에 모든 데이터 통합
- ✅ MongoDB 중첩 객체 구조
- ✅ 현재 22,002개 문서 존재
- ✅ `scripts/utils/predict.py` (30K+ 라인)이 이 구조 사용

**데이터 조회 방식**:
```python
# MongoDB 클라이언트 사용
cursor = db.daily_stock_data.find().sort("date", 1)
for doc in cursor:
    fred = doc.get("fred_indicators", {})
    yfinance = doc.get("yfinance_indicators", {})
    stocks = doc.get("stocks", {})
```

---

### 3️⃣ 현재 Quantiq 프로젝트 (❌ 잘못된 구조)

**데이터베이스**: ✅ **MongoDB** (동일)

**데이터 구조** (❌ 잘못됨):
```javascript
// fred_data 컬렉션 - 지표별, 날짜별 개별 문서
{
  _id: ObjectId("..."),
  date: "2026-01-23",
  code: "DEXKOUS",
  name: "USD_KRW",
  value: 1462.89,
  updated_at: "2026-01-31T15:52:10"
}

// yfinance_data 컬렉션 - 티커별, 날짜별 개별 문서
{
  _id: ObjectId("..."),
  date: "2026-01-30",
  ticker: "GC=F",
  name: "Gold",
  close: 4713.90,
  volume: 23709,
  updated_at: "2026-01-31T15:52:11"
}
```

**문제점**:
- ❌ 데이터가 날짜별로 분산됨
- ❌ `daily_stock_data` 컬렉션 사용 안함
- ❌ Local stock-trading의 predict.py와 호환 안됨
- ❌ 날짜별 통합 조회가 비효율적

---

## 📊 비교 요약표

| 항목 | GitLab (Supabase) | Local (MongoDB) | Quantiq (현재) |
|------|-------------------|-----------------|---------------|
| **데이터베이스** | PostgreSQL | MongoDB | MongoDB |
| **주요 테이블/컬렉션** | `economic_and_stock_data` | `daily_stock_data` | `fred_data`, `yfinance_data` |
| **데이터 저장 방식** | 날짜별 단일 행 | 날짜별 단일 문서 | 지표별 개별 문서 |
| **FRED 지표** | 컬럼 (37개) | `fred_indicators` 객체 | 별도 문서 |
| **Yahoo Finance** | 컬럼 (주식 포함) | `yfinance_indicators` 객체 | 별도 문서 |
| **주식 데이터** | 컬럼 (27개) | `stocks` 객체 | ❌ 없음 |
| **predict.py 호환** | ❌ DB 타입 다름 | ✅ 완벽 호환 | ❌ 구조 다름 |
| **현재 사용 가능** | ❌ | ✅ | ❌ |

---

## 🎯 결론 및 권장사항

### Quantiq가 사용해야 하는 구조

**✅ Local stock-trading 방식 (MongoDB - daily_stock_data)**

**이유**:
1. ✅ 동일한 MongoDB 사용 중
2. ✅ Local predict.py (30K+ 라인)와 호환
3. ✅ 이미 22,002개 문서로 검증된 구조
4. ✅ 날짜별 통합 조회 성능 우수

### GitLab 버전은?

**❌ 사용 불가**

**이유**:
1. ❌ Supabase (PostgreSQL) vs MongoDB (다른 DB)
2. ❌ 완전히 다른 데이터 구조
3. ❌ 마이그레이션 비용 높음
4. ❌ 현재 인프라와 맞지 않음

---

## 🚀 다음 단계

### 현재 작업 계속 진행 (옵션 1)

Quantiq의 데이터 저장 방식을 **Local stock-trading 구조**로 변경:

```python
# 변경 전 (현재)
db.fred_data.insert_one({"date": "2026-01-31", "code": "GDP", "value": 123.45})
db.yfinance_data.insert_one({"date": "2026-01-31", "ticker": "^GSPC", "close": 4500.12})

# 변경 후 (목표)
db.daily_stock_data.update_one(
    {"date": "2026-01-31"},
    {
        "$set": {
            "fred_indicators.GDP": 123.45,
            "yfinance_indicators.SP500": 4500.12,
            "updated_at": datetime.now()
        }
    },
    upsert=True
)
```

### 구현 작업

1. ✅ Repository에 `upsert_daily_data` 메서드 추가 (시작함)
2. ⏳ Service 로직 수정
3. ⏳ 테스트 및 검증
4. ⏳ 기존 데이터 마이그레이션

---

## 📌 핵심 포인트

1. **GitLab 버전은 참고용**: Supabase 기반이라 직접 사용 불가
2. **Local stock-trading이 정답**: MongoDB + 검증된 구조
3. **Quantiq 수정 필요**: `daily_stock_data` 구조로 변경
4. **현재 작업 계속**: 옵션 1 구현 진행

계속 진행할까요? 🚀
