/**
 * MongoDB Stocks 초기 데이터 Insert 스크립트
 * - 35개 미국 주식 종목
 *
 * 실행 방법:
 * mongosh "mongodb+srv://..." < 02_insert_mongodb_stocks.js
 */

// 데이터베이스 선택
use stock_trading;

print("====================================");
print("MongoDB Stocks 초기 데이터 Insert");
print("====================================\n");

// stocks 컬렉션 초기화 (선택 사항)
// db.stocks.drop();

// 35개 주식 종목 데이터
const stocks = [
  { ticker: "AAPL", stock_name: "애플", stock_name_en: "Apple Inc.", is_etf: false, leverage_ticker: "AAPU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "AMAT", stock_name: "어플라이드 머티리얼즈", stock_name_en: "Applied Materials", is_etf: false, leverage_ticker: null, is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "AMD", stock_name: "AMD", stock_name_en: "Advanced Micro Devices", is_etf: false, leverage_ticker: "AMDL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "AMZN", stock_name: "아마존", stock_name_en: "Amazon.com Inc.", is_etf: false, leverage_ticker: "AMZU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "APP", stock_name: "앱플로빈", stock_name_en: "AppLovin", is_etf: false, leverage_ticker: "APPX", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "AVGO", stock_name: "브로드컴", stock_name_en: "Broadcom Inc.", is_etf: false, leverage_ticker: "AVGG", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "BE", stock_name: "블룸에너지", stock_name_en: "Bloom Energy", is_etf: false, leverage_ticker: "BEX", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "CLS", stock_name: "셀레스티카", stock_name_en: "Celestica", is_etf: false, leverage_ticker: null, is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "CRDO", stock_name: "크리도 테크놀로지 그룹 홀딩", stock_name_en: "Credo Technology", is_etf: false, leverage_ticker: "CRDU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "CRM", stock_name: "세일즈포스", stock_name_en: "Salesforce Inc.", is_etf: false, leverage_ticker: "CRMG", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "CRWD", stock_name: "크라우드 스트라이크", stock_name_en: "CrowdStrike", is_etf: false, leverage_ticker: "CRWL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "GOOGL", stock_name: "구글 A", stock_name_en: "Alphabet Inc. Class A", is_etf: false, leverage_ticker: "GGLL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "HOOD", stock_name: "로빈후드", stock_name_en: "Robinhood", is_etf: false, leverage_ticker: "HODU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "INTC", stock_name: "인텔", stock_name_en: "Intel Corporation", is_etf: false, leverage_ticker: "INTW", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "JNJ", stock_name: "존슨앤존슨", stock_name_en: "Johnson & Johnson", is_etf: false, leverage_ticker: null, is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "LLY", stock_name: "일라이릴리", stock_name_en: "Eli Lilly", is_etf: false, leverage_ticker: "ELIL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "META", stock_name: "메타", stock_name_en: "Meta Platforms Inc.", is_etf: false, leverage_ticker: "FBL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "MSFT", stock_name: "마이크로소프트", stock_name_en: "Microsoft Corporation", is_etf: false, leverage_ticker: "MSFU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "MU", stock_name: "마이크론", stock_name_en: "Micron Technology", is_etf: false, leverage_ticker: "MUU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "NBIS", stock_name: "네비우스 그룹", stock_name_en: "Nebius Group", is_etf: false, leverage_ticker: "NEBX", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "NVDA", stock_name: "엔비디아", stock_name_en: "NVIDIA Corporation", is_etf: false, leverage_ticker: "NVDL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "OKLO", stock_name: "오클로", stock_name_en: "Oklo Inc.", is_etf: false, leverage_ticker: "OKLL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "ORCL", stock_name: "오라클", stock_name_en: "Oracle Corporation", is_etf: false, leverage_ticker: "ORCX", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "PANW", stock_name: "팔로알토 네트웍스", stock_name_en: "Palo Alto Networks", is_etf: false, leverage_ticker: "PALU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "PLTR", stock_name: "팔란티어", stock_name_en: "Palantir Technologies", is_etf: false, leverage_ticker: "PTIR", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "QQQ", stock_name: "QQQ ETF", stock_name_en: "Invesco QQQ Trust", is_etf: true, leverage_ticker: "TQQQ", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "SNOW", stock_name: "스노우플레이크", stock_name_en: "Snowflake Inc.", is_etf: false, leverage_ticker: "SNOU", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "SOXX", stock_name: "SOXX ETF", stock_name_en: "iShares Semiconductor ETF", is_etf: true, leverage_ticker: "SOXL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "SPY", stock_name: "S&P 500 ETF", stock_name_en: "SPDR S&P 500 ETF Trust", is_etf: true, leverage_ticker: "UPRO", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "TSLA", stock_name: "테슬라", stock_name_en: "Tesla Inc.", is_etf: false, leverage_ticker: "TSLL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "TSM", stock_name: "TSMC", stock_name_en: "Taiwan Semiconductor", is_etf: false, leverage_ticker: "TSMG", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "TXN", stock_name: "텍사스 인스트루먼트", stock_name_en: "Texas Instruments", is_etf: false, leverage_ticker: null, is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "VRT", stock_name: "버티브 홀딩스", stock_name_en: "Vertiv Holdings", is_etf: false, leverage_ticker: "VRTL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "VST", stock_name: "비스트라 에너지", stock_name_en: "Vistra Energy", is_etf: false, leverage_ticker: "VSTL", is_active: true, created_at: new Date(), updated_at: new Date() },
  { ticker: "WMT", stock_name: "월마트", stock_name_en: "Walmart Inc.", is_etf: false, leverage_ticker: null, is_active: true, created_at: new Date(), updated_at: new Date() }
];

// Bulk Insert
print("Stocks 데이터 삽입 중...");

try {
  const result = db.stocks.insertMany(stocks, { ordered: false });
  print(`✅ ${Object.keys(result.insertedIds).length}개 종목 삽입 완료`);
} catch (e) {
  if (e.code === 11000) {
    // 중복 키 에러 (이미 존재하는 ticker)
    print("⚠️  일부 ticker가 이미 존재합니다. 중복은 건너뜁니다.");
  } else {
    print(`❌ 에러 발생: ${e.message}`);
  }
}

// 인덱스 생성
print("\n인덱스 생성 중...");
db.stocks.createIndex({ ticker: 1 }, { unique: true });
db.stocks.createIndex({ is_active: 1 });
db.stocks.createIndex({ is_etf: 1 });
print("✅ 인덱스 생성 완료");

// 검증
print("\n====================================");
print("검증 결과:");
print("====================================");
const count = db.stocks.countDocuments();
const activeCount = db.stocks.countDocuments({ is_active: true });
const etfCount = db.stocks.countDocuments({ is_etf: true });

print(`총 종목 수: ${count}개`);
print(`활성 종목: ${activeCount}개`);
print(`ETF: ${etfCount}개`);
print(`개별 주식: ${count - etfCount}개`);
print("\n✅ MongoDB Stocks 초기 데이터 Insert 완료!");
