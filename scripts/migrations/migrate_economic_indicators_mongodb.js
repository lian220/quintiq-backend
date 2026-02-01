/**
 * MongoDB 경제 지표 메타데이터 마이그레이션 스크립트
 *
 * 사용법 (로컬 환경):
 *   mongosh -u quantiq_user -p quantiq_password --authenticationDatabase admin stock_trading migrate_economic_indicators_mongodb.js
 *
 * 사용법 (프로덕션 마이그레이션):
 *   1. 프로덕션 MongoDB에서 데이터 추출:
 *      mongosh <PROD_CONNECTION_STRING> --eval "
 *        db.fred_indicators.find().forEach(printjson);
 *        db.yfinance_indicators.find().forEach(printjson);
 *      " > prod_indicators.json
 *
 *   2. 로컬에서 이 스크립트 실행
 *
 * 작성일: 2026-02-01
 */

print("=".repeat(70));
print("경제 지표 메타데이터 마이그레이션 시작");
print("=".repeat(70));
print("");

// ============================================
// FRED 경제 지표
// ============================================
print("📊 FRED 경제 지표 삽입 중...");

const fredIndicators = [
    {
        code: 'GDP',
        name: 'GDP',
        description: '국내총생산 (Gross Domestic Product)',
        category: 'Economic Growth',
        unit: 'Billions of Dollars',
        frequency: 'quarterly',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'UNRATE',
        name: 'Unemployment Rate',
        description: '실업률',
        category: 'Labor Market',
        unit: 'Percent',
        frequency: 'monthly',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'CPIAUCSL',
        name: 'CPI',
        description: '소비자물가지수 (Consumer Price Index)',
        category: 'Inflation',
        unit: 'Index 1982-1984=100',
        frequency: 'monthly',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'FEDFUNDS',
        name: 'Federal Funds Rate',
        description: '연방기금금리',
        category: 'Monetary Policy',
        unit: 'Percent',
        frequency: 'daily',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'DGS10',
        name: '10-Year Treasury',
        description: '10년물 국채 수익률',
        category: 'Interest Rates',
        unit: 'Percent',
        frequency: 'daily',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'DGS2',
        name: '2-Year Treasury',
        description: '2년물 국채 수익률',
        category: 'Interest Rates',
        unit: 'Percent',
        frequency: 'daily',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'T10Y2Y',
        name: 'Treasury Yield Spread',
        description: '10년-2년 국채 수익률 스프레드 (경기 침체 지표)',
        category: 'Interest Rates',
        unit: 'Percent',
        frequency: 'daily',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        code: 'DEXUSEU',
        name: 'USD/EUR Exchange Rate',
        description: '달러/유로 환율',
        category: 'Currency',
        unit: 'US Dollars per Euro',
        frequency: 'daily',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    }
];

// FRED 지표 삽입 (중복 방지)
fredIndicators.forEach(indicator => {
    const result = db.fred_indicators.updateOne(
        { code: indicator.code },
        { $set: indicator },
        { upsert: true }
    );

    if (result.upsertedCount > 0) {
        print(`  ✅ 신규: ${indicator.code} - ${indicator.name}`);
    } else if (result.modifiedCount > 0) {
        print(`  🔄 업데이트: ${indicator.code} - ${indicator.name}`);
    } else {
        print(`  ⏭️  기존: ${indicator.code} - ${indicator.name}`);
    }
});

print("");
print(`FRED 지표 총 ${db.fred_indicators.countDocuments({})}건`);
print("");

// ============================================
// Yahoo Finance 지표
// ============================================
print("📈 Yahoo Finance 지표 삽입 중...");

const yfinanceIndicators = [
    {
        ticker: 'SPY',
        name: 'S&P 500 ETF',
        description: 'S&P 500 지수 추종 ETF',
        indicator_type: 'ETF',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'QQQ',
        name: 'QQQ ETF',
        description: 'NASDAQ 100 지수 추종 ETF',
        indicator_type: 'ETF',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'SOXX',
        name: 'SOXX ETF',
        description: 'iShares 반도체 섹터 ETF',
        indicator_type: 'ETF',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'DIA',
        name: 'Dow Jones ETF',
        description: 'Dow Jones Industrial Average 추종 ETF',
        indicator_type: 'ETF',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'IWM',
        name: 'Russell 2000 ETF',
        description: 'Russell 2000 소형주 지수 ETF',
        indicator_type: 'ETF',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'VIX',
        name: 'Volatility Index',
        description: 'CBOE 변동성 지수 (공포 지수)',
        indicator_type: 'INDEX',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'GLD',
        name: 'Gold ETF',
        description: '금 가격 추종 ETF',
        indicator_type: 'COMMODITY',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    },
    {
        ticker: 'USO',
        name: 'Oil ETF',
        description: '원유 가격 추종 ETF',
        indicator_type: 'COMMODITY',
        is_active: true,
        created_at: new Date(),
        updated_at: new Date()
    }
];

// Yahoo Finance 지표 삽입 (중복 방지)
yfinanceIndicators.forEach(indicator => {
    const result = db.yfinance_indicators.updateOne(
        { ticker: indicator.ticker },
        { $set: indicator },
        { upsert: true }
    );

    if (result.upsertedCount > 0) {
        print(`  ✅ 신규: ${indicator.ticker} - ${indicator.name}`);
    } else if (result.modifiedCount > 0) {
        print(`  🔄 업데이트: ${indicator.ticker} - ${indicator.name}`);
    } else {
        print(`  ⏭️  기존: ${indicator.ticker} - ${indicator.name}`);
    }
});

print("");
print(`Yahoo Finance 지표 총 ${db.yfinance_indicators.countDocuments({})}건`);
print("");

// ============================================
// 인덱스 생성
// ============================================
print("🔍 인덱스 생성 중...");

db.fred_indicators.createIndex({ code: 1 }, { unique: true });
db.fred_indicators.createIndex({ is_active: 1 });
db.fred_indicators.createIndex({ category: 1 });

db.yfinance_indicators.createIndex({ ticker: 1 }, { unique: true });
db.yfinance_indicators.createIndex({ is_active: 1 });
db.yfinance_indicators.createIndex({ indicator_type: 1 });

print("  ✅ 인덱스 생성 완료");
print("");

// ============================================
// 최종 통계
// ============================================
print("=".repeat(70));
print("마이그레이션 완료");
print("=".repeat(70));
print("FRED 지표:", db.fred_indicators.countDocuments({}), "건");
print("Yahoo Finance 지표:", db.yfinance_indicators.countDocuments({}), "건");
print("=".repeat(70));
