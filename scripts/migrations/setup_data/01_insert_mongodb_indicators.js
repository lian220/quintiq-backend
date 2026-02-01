/**
 * MongoDB 초기 데이터 Insert 스크립트
 * - fred_indicators: FRED 경제 지표
 * - yfinance_indicators: Yahoo Finance 지표 (지수, ETF, 통화, 원자재)
 *
 * 실행 방법:
 * mongosh "mongodb+srv://..." < 01_insert_mongodb_indicators.js
 */

// 데이터베이스 선택
use stock_trading;

print("====================================");
print("MongoDB 초기 데이터 Insert 시작");
print("====================================\n");

// =====================================
// 1. fred_indicators 삭제 및 재생성
// =====================================
print("1. fred_indicators 초기화...");
db.fred_indicators.drop();

const fredIndicators = [
  {
    code: "T10YIE",
    name: "10년 기대 인플레이션율",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "T10Y2Y",
    name: "장단기 금리차",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "FEDFUNDS",
    name: "기준금리",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "UMCSENT",
    name: "미시간대 소비자 심리지수",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "UNRATE",
    name: "실업률",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "DGS2",
    name: "2년 만기 미국 국채 수익률",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "DGS10",
    name: "10년 만기 미국 국채 수익률",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "STLFSI4",
    name: "금융스트레스지수",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "PCE",
    name: "개인 소비 지출",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "CPIAUCSL",
    name: "소비자 물가지수",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "MORTGAGE5US",
    name: "5년 변동금리 모기지",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "DTWEXM",
    name: "미국 달러 환율",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "M2",
    name: "통화 공급량 M2",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "TDSP",
    name: "가계 부채 비율",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "GDPC1",
    name: "GDP 성장률",
    type: "economic",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    code: "NASDAQCOM",
    name: "나스닥 종합지수",
    type: "index",
    is_active: true,
    created_at: new Date(),
    updated_at: new Date()
  }
];

const fredResult = db.fred_indicators.insertMany(fredIndicators);
print(`✅ fred_indicators: ${fredResult.insertedIds.length}개 삽입 완료\n`);

// =====================================
// 2. yfinance_indicators 삭제 및 재생성
// =====================================
print("2. yfinance_indicators 초기화...");
db.yfinance_indicators.drop();

const yfinanceIndicators = [
  { ticker: "^GSPC", name: "S&P 500 지수", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "GC=F", name: "금 가격", type: "commodity", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "DX-Y.NYB", name: "달러 인덱스", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^NDX", name: "나스닥 100", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "SPY", name: "S&P 500 ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "QQQ", name: "QQQ ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "IWM", name: "러셀 2000 ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "DIA", name: "다우 존스 ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^VIX", name: "VIX 지수", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^N225", name: "닛케이 225", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "000001.SS", name: "상해종합", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^HSI", name: "항셍", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^FTSE", name: "영국 FTSE", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^GDAXI", name: "독일 DAX", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "^FCHI", name: "프랑스 CAC 40", type: "index", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "AGG", name: "미국 전체 채권시장 ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "TIP", name: "TIPS ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "LQD", name: "투자등급 회사채 ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "JPY=X", name: "달러/엔", type: "currency", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "CNY=X", name: "달러/위안", type: "currency", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "VNQ", name: "미국 리츠 ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "SOXX", name: "SOXX ETF", type: "etf", is_active: true, created_at: new Date(), updated_at: new Date() }
];

const yfinanceResult = db.yfinance_indicators.insertMany(yfinanceIndicators);
print(`✅ yfinance_indicators: ${yfinanceResult.insertedIds.length}개 삽입 완료\n`);

// =====================================
// 3. 인덱스 생성
// =====================================
print("3. 인덱스 생성...");
db.fred_indicators.createIndex({ code: 1 }, { unique: true });
db.fred_indicators.createIndex({ type: 1 });
db.fred_indicators.createIndex({ is_active: 1 });

db.yfinance_indicators.createIndex({ ticker: 1 }, { unique: true });
db.yfinance_indicators.createIndex({ type: 1 });
db.yfinance_indicators.createIndex({ is_active: 1 });

print("✅ 인덱스 생성 완료\n");

// =====================================
// 4. 검증
// =====================================
print("====================================");
print("검증 결과:");
print("====================================");
print(`fred_indicators: ${db.fred_indicators.countDocuments()}개`);
print(`yfinance_indicators: ${db.yfinance_indicators.countDocuments()}개`);
print("\n초기 데이터 Insert 완료!");
