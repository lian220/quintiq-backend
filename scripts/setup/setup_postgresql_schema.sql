-- ============================================
-- PostgreSQL 스키마 생성 스크립트
-- ============================================
-- 목적: Quantiq 프로젝트의 모든 PostgreSQL 테이블 및 인덱스 생성
-- 작성일: 2026-02-01
-- 버전: 통합 스키마 (V1~V9)
-- ============================================

-- ============================================
-- 1. 사용자 및 거래 설정 테이블 (V1)
-- ============================================

-- 1-1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS '사용자 정보';
COMMENT ON COLUMN users.user_id IS '사용자 고유 식별자 (비즈니스 키)';
COMMENT ON COLUMN users.status IS '사용자 상태: ACTIVE, INACTIVE, SUSPENDED';

-- 1-2. Trading Configs Table
CREATE TABLE IF NOT EXISTS trading_configs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    enabled BOOLEAN DEFAULT FALSE,
    auto_trading_enabled BOOLEAN DEFAULT FALSE,
    min_composite_score DECIMAL(5, 2) DEFAULT 2.0,
    max_stocks_to_buy INTEGER DEFAULT 5,
    max_amount_per_stock DECIMAL(12, 2) DEFAULT 10000.0,
    stop_loss_percent DECIMAL(5, 2) DEFAULT -7.0,
    take_profit_percent DECIMAL(5, 2) DEFAULT 5.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_scores CHECK (min_composite_score >= 0 AND min_composite_score <= 10),
    CONSTRAINT valid_stocks CHECK (max_stocks_to_buy > 0),
    CONSTRAINT valid_amount CHECK (max_amount_per_stock > 0)
);

COMMENT ON TABLE trading_configs IS '사용자별 거래 설정';
COMMENT ON COLUMN trading_configs.min_composite_score IS '신호 신뢰도 최소값 (0-10)';

-- 1-3. Account Balances Table
CREATE TABLE IF NOT EXISTS account_balances (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    cash DECIMAL(15, 2) NOT NULL DEFAULT 0,
    total_value DECIMAL(15, 2) NOT NULL DEFAULT 0,
    locked_cash DECIMAL(15, 2) DEFAULT 0,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_cash CHECK (cash >= 0),
    CONSTRAINT valid_total CHECK (total_value >= 0),
    CONSTRAINT valid_locked CHECK (locked_cash >= 0)
);

COMMENT ON TABLE account_balances IS '사용자 계좌 잔액 (매우 자주 업데이트됨)';
COMMENT ON COLUMN account_balances.version IS 'Optimistic locking 버전번호';
COMMENT ON COLUMN account_balances.locked_cash IS '미체결 주문으로 인해 잠긴 현금';

-- 1-4. Stock Holdings Table
CREATE TABLE IF NOT EXISTS stock_holdings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    average_price DECIMAL(10, 2) NOT NULL,
    total_cost DECIMAL(15, 2) NOT NULL,
    current_value DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, ticker),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_quantity CHECK (quantity > 0),
    CONSTRAINT valid_price CHECK (average_price > 0)
);

COMMENT ON TABLE stock_holdings IS '사용자의 주식 보유 내역';

-- 1-5. Trades Table
CREATE TABLE IF NOT EXISTS trades (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    commission DECIMAL(10, 2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'EXECUTED', 'FAILED', 'CANCELLED')),
    kis_order_id VARCHAR(100),
    executed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_quantity CHECK (quantity > 0),
    CONSTRAINT valid_price CHECK (price > 0),
    CONSTRAINT valid_amount CHECK (total_amount > 0)
);

COMMENT ON TABLE trades IS '거래 기록 (감사, 정산용)';
COMMENT ON COLUMN trades.kis_order_id IS '외부 거래소 주문 ID (KIS)';

-- 1-6. Trade Signals Executed Table
CREATE TABLE IF NOT EXISTS trade_signals_executed (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recommendation_id VARCHAR(100) NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    signal VARCHAR(20) NOT NULL CHECK (signal IN ('BUY', 'SELL', 'HOLD')),
    confidence DECIMAL(3, 2) NOT NULL,
    execution_decision VARCHAR(20) NOT NULL CHECK (execution_decision IN ('EXECUTED', 'SKIPPED', 'FAILED')),
    skip_reason VARCHAR(255),
    executed_trade_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (executed_trade_id) REFERENCES trades(id) ON DELETE SET NULL,
    CONSTRAINT valid_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

COMMENT ON TABLE trade_signals_executed IS '거래 신호 실행 로그 (감사, 재현용)';
COMMENT ON COLUMN trade_signals_executed.recommendation_id IS 'MongoDB stock_recommendations._id';
COMMENT ON COLUMN trade_signals_executed.execution_decision IS '실행 여부 및 결과';

-- ============================================
-- 2. 인덱스 생성 (V2)
-- ============================================

-- Users 인덱스
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Trading Configs 인덱스
CREATE INDEX IF NOT EXISTS idx_trading_configs_enabled ON trading_configs(user_id, enabled);

-- Account Balances 인덱스
CREATE INDEX IF NOT EXISTS idx_account_balances_user_id ON account_balances(user_id);

-- Stock Holdings 인덱스
CREATE INDEX IF NOT EXISTS idx_stock_holdings_user_id ON stock_holdings(user_id);
CREATE INDEX IF NOT EXISTS idx_stock_holdings_ticker ON stock_holdings(ticker);

-- Trades 인덱스
CREATE INDEX IF NOT EXISTS idx_trades_user_ticker_date ON trades(user_id, ticker, executed_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_trades_status ON trades(status);
CREATE INDEX IF NOT EXISTS idx_trades_kis_order_id ON trades(kis_order_id) WHERE kis_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_trades_executed_at ON trades(executed_at DESC NULLS LAST) WHERE status = 'EXECUTED';

-- Trade Signals Executed 인덱스
CREATE INDEX IF NOT EXISTS idx_trade_signals_user_timestamp ON trade_signals_executed(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_trade_signals_recommendation_id ON trade_signals_executed(recommendation_id);

-- ============================================
-- 3. Quartz Scheduler 테이블 (V3)
-- ============================================

-- QRTZ_JOB_DETAILS
CREATE TABLE IF NOT EXISTS quartz_job_details (
    sched_name        VARCHAR(120) NOT NULL,
    job_name          VARCHAR(200) NOT NULL,
    job_group         VARCHAR(200) NOT NULL,
    description       VARCHAR(250),
    job_class_name    VARCHAR(250) NOT NULL,
    is_durable        BOOLEAN      NOT NULL,
    is_nonconcurrent  BOOLEAN      NOT NULL,
    is_update_data    BOOLEAN      NOT NULL,
    requests_recovery BOOLEAN      NOT NULL,
    job_data          BYTEA,
    PRIMARY KEY (sched_name, job_name, job_group)
);

-- QRTZ_TRIGGERS
CREATE TABLE IF NOT EXISTS quartz_triggers (
    sched_name     VARCHAR(120) NOT NULL,
    trigger_name   VARCHAR(200) NOT NULL,
    trigger_group  VARCHAR(200) NOT NULL,
    job_name       VARCHAR(200) NOT NULL,
    job_group      VARCHAR(200) NOT NULL,
    description    VARCHAR(250),
    next_fire_time BIGINT,
    prev_fire_time BIGINT,
    priority       INTEGER,
    trigger_state  VARCHAR(16)  NOT NULL,
    trigger_type   VARCHAR(8)   NOT NULL,
    start_time     BIGINT       NOT NULL,
    end_time       BIGINT,
    calendar_name  VARCHAR(200),
    misfire_instr  SMALLINT,
    job_data       BYTEA,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, job_name, job_group)
        REFERENCES quartz_job_details (sched_name, job_name, job_group)
);

-- QRTZ_CRON_TRIGGERS
CREATE TABLE IF NOT EXISTS quartz_cron_triggers (
    sched_name      VARCHAR(120) NOT NULL,
    trigger_name    VARCHAR(200) NOT NULL,
    trigger_group   VARCHAR(200) NOT NULL,
    cron_expression VARCHAR(120) NOT NULL,
    time_zone_id    VARCHAR(80),
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES quartz_triggers (sched_name, trigger_name, trigger_group)
);

-- QRTZ_SIMPLE_TRIGGERS
CREATE TABLE IF NOT EXISTS quartz_simple_triggers (
    sched_name      VARCHAR(120) NOT NULL,
    trigger_name    VARCHAR(200) NOT NULL,
    trigger_group   VARCHAR(200) NOT NULL,
    repeat_count    BIGINT       NOT NULL,
    repeat_interval BIGINT       NOT NULL,
    times_triggered BIGINT       NOT NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES quartz_triggers (sched_name, trigger_name, trigger_group)
);

-- QRTZ_SIMPROP_TRIGGERS
CREATE TABLE IF NOT EXISTS quartz_simprop_triggers (
    sched_name    VARCHAR(120) NOT NULL,
    trigger_name  VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    str_prop_1    VARCHAR(512),
    str_prop_2    VARCHAR(512),
    str_prop_3    VARCHAR(512),
    int_prop_1    INTEGER,
    int_prop_2    INTEGER,
    long_prop_1   BIGINT,
    long_prop_2   BIGINT,
    dec_prop_1    NUMERIC(13, 4),
    dec_prop_2    NUMERIC(13, 4),
    bool_prop_1   BOOLEAN,
    bool_prop_2   BOOLEAN,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES quartz_triggers (sched_name, trigger_name, trigger_group)
);

-- QRTZ_CALENDARS
CREATE TABLE IF NOT EXISTS quartz_calendars (
    sched_name    VARCHAR(120) NOT NULL,
    calendar_name VARCHAR(200) NOT NULL,
    calendar      BYTEA        NOT NULL,
    PRIMARY KEY (sched_name, calendar_name)
);

-- QRTZ_FIRED_TRIGGERS
CREATE TABLE IF NOT EXISTS quartz_fired_triggers (
    sched_name        VARCHAR(120) NOT NULL,
    entry_id          VARCHAR(95)  NOT NULL,
    trigger_name      VARCHAR(200) NOT NULL,
    trigger_group     VARCHAR(200) NOT NULL,
    instance_name     VARCHAR(200) NOT NULL,
    fired_time        BIGINT       NOT NULL,
    sched_time        BIGINT       NOT NULL,
    priority          INTEGER      NOT NULL,
    state             VARCHAR(16)  NOT NULL,
    job_name          VARCHAR(200),
    job_group         VARCHAR(200),
    is_nonconcurrent  BOOLEAN,
    requests_recovery BOOLEAN,
    PRIMARY KEY (sched_name, entry_id)
);

-- QRTZ_LOCKS
CREATE TABLE IF NOT EXISTS quartz_locks (
    sched_name VARCHAR(120) NOT NULL,
    lock_name  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (sched_name, lock_name)
);

-- QRTZ_PAUSED_TRIGGER_GRPS
CREATE TABLE IF NOT EXISTS quartz_paused_trigger_grps (
    sched_name    VARCHAR(120) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    PRIMARY KEY (sched_name, trigger_group)
);

-- QRTZ_SCHEDULER_STATE
CREATE TABLE IF NOT EXISTS quartz_scheduler_state (
    sched_name        VARCHAR(120) NOT NULL,
    instance_name     VARCHAR(200) NOT NULL,
    last_checkin_time BIGINT       NOT NULL,
    checkin_interval  BIGINT       NOT NULL,
    PRIMARY KEY (sched_name, instance_name)
);

-- Quartz 인덱스
CREATE INDEX IF NOT EXISTS idx_quartz_triggers_next_fire_time ON quartz_triggers (sched_name, next_fire_time);
CREATE INDEX IF NOT EXISTS idx_quartz_triggers_trigger_state ON quartz_triggers (sched_name, trigger_state);
CREATE INDEX IF NOT EXISTS idx_quartz_fired_triggers_instance_name ON quartz_fired_triggers (sched_name, instance_name);

-- ============================================
-- 4. KIS (한국투자증권) 관련 테이블 (V4, V5)
-- ============================================

-- 4-1. User KIS Accounts (V4)
CREATE TABLE IF NOT EXISTS user_kis_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    app_key VARCHAR(100) NOT NULL,
    app_secret_encrypted VARCHAR(500) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    account_product_code VARCHAR(2) NOT NULL DEFAULT '01',
    account_type VARCHAR(10) NOT NULL DEFAULT 'MOCK',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_kis_account_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_user_kis_accounts_user_id ON user_kis_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_user_kis_accounts_account_type ON user_kis_accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_user_kis_accounts_enabled ON user_kis_accounts(enabled);

COMMENT ON TABLE user_kis_accounts IS '사용자별 KIS(한국투자증권) 계정 정보';
COMMENT ON COLUMN user_kis_accounts.app_key IS 'KIS API 앱 키';
COMMENT ON COLUMN user_kis_accounts.app_secret_encrypted IS 'KIS API 시크릿 (AES-256 암호화)';
COMMENT ON COLUMN user_kis_accounts.account_number IS 'KIS 계좌번호 (앞 8자리)';
COMMENT ON COLUMN user_kis_accounts.account_product_code IS '계좌 상품 코드 (01: 해외주식)';
COMMENT ON COLUMN user_kis_accounts.account_type IS '계정 타입 (REAL: 실전, MOCK: 모의)';

-- 4-2. KIS Tokens (V5)
CREATE TABLE IF NOT EXISTS kis_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_type VARCHAR(10) NOT NULL CHECK (account_type IN ('MOCK', 'REAL')),
    access_token TEXT NOT NULL,
    expiration_time TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_kis_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_user_account_type
        UNIQUE(user_id, account_type)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_kis_tokens_user_account ON kis_tokens(user_id, account_type);
CREATE INDEX IF NOT EXISTS idx_kis_tokens_expiration ON kis_tokens(expiration_time);
CREATE INDEX IF NOT EXISTS idx_kis_tokens_active ON kis_tokens(is_active) WHERE is_active = TRUE;

COMMENT ON TABLE kis_tokens IS 'KIS API Access Token 저장 (사용자별, 계정 타입별)';
COMMENT ON COLUMN kis_tokens.user_id IS '사용자 ID (users 테이블 참조)';
COMMENT ON COLUMN kis_tokens.account_type IS '계정 타입: MOCK(모의투자), REAL(실전투자)';
COMMENT ON COLUMN kis_tokens.access_token IS 'KIS API Access Token (JWT)';
COMMENT ON COLUMN kis_tokens.expiration_time IS '토큰 만료 시간';
COMMENT ON COLUMN kis_tokens.is_active IS '활성화 여부';

-- ============================================
-- 5. Stocks 테이블 (V6)
-- ============================================

CREATE TABLE IF NOT EXISTS stocks (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    stock_name VARCHAR(200) NOT NULL,
    stock_name_en VARCHAR(200),
    is_etf BOOLEAN NOT NULL DEFAULT FALSE,
    leverage_ticker VARCHAR(20),
    exchange VARCHAR(50),
    sector VARCHAR(100),
    industry VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stocks_ticker UNIQUE(ticker)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_stocks_ticker ON stocks(ticker);
CREATE INDEX IF NOT EXISTS idx_stocks_is_active ON stocks(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_stocks_sector ON stocks(sector) WHERE sector IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stocks_industry ON stocks(industry) WHERE industry IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stocks_is_etf ON stocks(is_etf) WHERE is_etf = TRUE;

COMMENT ON TABLE stocks IS '주식 메타데이터 (티커, 이름, ETF 여부 등)';
COMMENT ON COLUMN stocks.ticker IS '주식 티커 심볼 (예: AAPL, TSLA)';
COMMENT ON COLUMN stocks.stock_name IS '주식 한글명 (예: 애플)';
COMMENT ON COLUMN stocks.stock_name_en IS '주식 영문명 (예: Apple Inc.)';
COMMENT ON COLUMN stocks.is_etf IS 'ETF 여부';
COMMENT ON COLUMN stocks.leverage_ticker IS '레버리지 상품 티커';
COMMENT ON COLUMN stocks.exchange IS '거래소 (예: NASDAQ, NYSE)';
COMMENT ON COLUMN stocks.sector IS '섹터 (예: Technology, Healthcare)';
COMMENT ON COLUMN stocks.industry IS '산업 (예: Consumer Electronics)';
COMMENT ON COLUMN stocks.is_active IS '활성화 여부 (거래 가능 여부)';

-- ============================================
-- 6. 경제 지표 테이블 (V9)
-- ============================================

-- 6-1. FRED 경제 지표
CREATE TABLE IF NOT EXISTS fred_indicators (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    unit VARCHAR(50),
    frequency VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fred_code_not_empty CHECK (code <> '')
);

-- 6-2. Yahoo Finance 지표
CREATE TABLE IF NOT EXISTS yfinance_indicators (
    id SERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    indicator_type VARCHAR(50) DEFAULT 'ETF',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ticker_not_empty CHECK (ticker <> '')
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_fred_is_active ON fred_indicators(is_active);
CREATE INDEX IF NOT EXISTS idx_fred_category ON fred_indicators(category);
CREATE INDEX IF NOT EXISTS idx_yfinance_is_active ON yfinance_indicators(is_active);
CREATE INDEX IF NOT EXISTS idx_yfinance_type ON yfinance_indicators(indicator_type);

COMMENT ON TABLE fred_indicators IS 'FRED(Federal Reserve Economic Data) 경제 지표 메타데이터';
COMMENT ON TABLE yfinance_indicators IS 'Yahoo Finance 시장 지표 메타데이터';
COMMENT ON COLUMN fred_indicators.code IS 'FRED API 지표 코드 (예: GDP, UNRATE)';
COMMENT ON COLUMN fred_indicators.frequency IS '데이터 발표 주기';
COMMENT ON COLUMN yfinance_indicators.ticker IS 'Yahoo Finance 티커 심볼 (예: SPY, QQQ)';
COMMENT ON COLUMN yfinance_indicators.indicator_type IS '지표 유형 (INDEX/ETF/COMMODITY/CURRENCY)';

-- ============================================
-- 스키마 생성 완료
-- ============================================
