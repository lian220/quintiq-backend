# Data Migration Guide

마이그레이션 스크립트를 사용하여 stock-trading MongoDB에서 quantiq(PostgreSQL + MongoDB)로 데이터를 이동합니다.

## 📋 전제 조건

### 필수 사항
- ✅ quantiq Docker containers 실행 중 (PostgreSQL, MongoDB)
- ✅ stock-trading .env 파일 정보 확인
- ✅ quantiq .env 파일 설정 완료
- ✅ Flyway 마이그레이션 완료 (PostgreSQL 스키마 생성)

### 확인 사항
```bash
# Docker 상태 확인
docker ps | grep quantiq

# PostgreSQL 연결 테스트
psql -h localhost -p 5433 -U quantiq_user -d quantiq -c "SELECT * FROM users LIMIT 1;"

# MongoDB 연결 테스트
mongo -u quantiq_user -p quantiq_password --authenticationDatabase admin
```

## 🚀 실행 방법

### 1. 간단한 실행 (권장)
```bash
cd /Users/imdoyeong/Desktop/workSpace/quantiq
./scripts/run_migration.sh
```

### 2. 단계별 실행
```bash
# 가상환경 생성
cd scripts
python3 -m venv venv
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt

# 마이그레이션 실행
cd ..
python3 scripts/migrate_data.py
```

## 📊 마이그레이션 범위

### PostgreSQL로 이동 (트랜잭션 데이터)
| 테이블 | 원본 | 설명 |
|--------|------|------|
| `users` | stocks.users | 사용자 계정 정보 |
| `trading_configs` | stocks.trading_configs | 사용자별 거래 설정 |
| `stock_holdings` | stocks.user_stocks | 현재 보유 주식 |
| `trades` | stocks.trading_logs | 거래 기록 |
| `account_balances` | 계산 생성 | 계좌 잔액 (자동 계산) |

### MongoDB로 유지 (분석 데이터)
| 컬렉션 | 원본 | 설명 |
|--------|------|------|
| `stocks` | stocks.stocks | 종목 마스터 데이터 |
| `stock_recommendations` | stock_recommendations | 추천 신호 (시계열) |
| `stock_predictions` | stock_predictions | 예측 데이터 |
| `sentiment_analysis` | sentiment_analysis | 감정 분석 |
| `daily_stock_data` | daily_stock_data | 일일 OHLCV 데이터 |

## 📝 마이그레이션 단계

```
Phase 1: 기본 설정 (이미 완료)
├─ PostgreSQL 스키마 생성 (Flyway)
├─ MongoDB 연결 확인
└─ 환경 변수 설정

Phase 2: 사용자 데이터
├─ users → PostgreSQL
├─ trading_configs → PostgreSQL
└─ 무결성 검증

Phase 3: 거래 데이터
├─ stock_holdings → PostgreSQL (user_stocks 기반)
├─ trades → PostgreSQL (trading_logs)
└─ account_balances → PostgreSQL (자동 계산)

Phase 4: 분석 데이터
├─ stocks → MongoDB
├─ stock_recommendations → MongoDB
├─ stock_predictions → MongoDB
├─ sentiment_analysis → MongoDB
└─ daily_stock_data → MongoDB

Phase 5: 검증 및 확인 (수동)
├─ 데이터 무결성 확인
├─ 중복 제거 검증
└─ 성능 테스트
```

## 🔍 마이그레이션 후 검증

### PostgreSQL 데이터 확인
```sql
-- 사용자 수
SELECT COUNT(*) as user_count FROM users;

-- 보유 종목 수
SELECT COUNT(*) as holdings_count FROM stock_holdings;

-- 거래 기록 수
SELECT COUNT(*) as trades_count FROM trades;

-- 계좌 잔액
SELECT user_id, cash_balance, total_asset FROM account_balances;

-- 데이터 상세 조회
SELECT * FROM users LIMIT 5;
SELECT * FROM stock_holdings LIMIT 5;
```

### MongoDB 데이터 확인
```javascript
// 종목 수
db.stocks.count()

// 추천 신호 수
db.stock_recommendations.count()

// 샘플 데이터
db.stocks.findOne()
db.stock_recommendations.findOne()
```

## ⚠️ 주의 사항

### 마이그레이션 전
- 🔒 **백업 완료**: 원본 MongoDB 백업 확인
- 🧹 **테스트 환경**: 프로덕션이 아닌 로컬에서만 실행
- 🔌 **네트워크**: stock-trading MongoDB 접근 가능 확인
- 📊 **데이터량**: 대용량 데이터의 경우 시간이 걸릴 수 있음

### 마이그레이션 중
- ⏸️ **중단 금지**: 스크립트 실행 중 중단하지 않기
- 📡 **네트워크 안정**: 인터넷 연결 끊김 주의
- 🔌 **DB 접근**: 마이그레이션 중 DB 접근 제한

### 마이그레이션 후
- ✅ **데이터 검증**: 모든 테이블 데이터 확인
- 🔄 **중복 제거**: 재실행 시 중복 데이터 확인
- 📊 **성능 테스트**: 쿼리 성능 검증
- 🔐 **보안 확인**: 암호화 필드 검증

## 🛠️ 문제 해결

### 연결 오류
```
Error: connection to stock-trading MongoDB failed
→ stock-trading MongoDB 연결 정보 확인 (.env 파일)
→ 네트워크 접근성 확인
```

### PostgreSQL 오류
```
Error: connection to PostgreSQL failed
→ docker ps로 PostgreSQL 컨테이너 실행 확인
→ 포트 5433 접근 가능 확인
→ 사용자 인증 정보 확인
```

### 데이터 검증 오류
```
Error: duplicate key value violates unique constraint
→ 재실행 전에 대상 데이터베이스 초기화
→ 또는 ON CONFLICT 절이 자동으로 처리
```

### 메모리 부족
```
Error: MemoryError
→ 스크립트를 배치로 나누어 실행
→ 또는 메모리 사용량 모니터링
```

## 📋 로그 확인

마이그레이션 실행 후 로그 파일이 생성됩니다:
```bash
# 최신 로그 확인
cat migration_*.log | tail -50

# 에러만 확인
grep ERROR migration_*.log

# 통계 확인
grep "MIGRATION SUMMARY" -A 20 migration_*.log
```

## 🔄 재실행

마이그레이션은 **멱등성**을 지원합니다 (중복 데이터 자동 처리):
```bash
# 안전하게 재실행 가능
./scripts/run_migration.sh
```

단, 대량의 중복 데이터를 피하려면:
```bash
# 대상 DB의 데이터를 먼저 정리한 후 재실행
psql -h localhost -p 5433 -U quantiq_user -d quantiq \
  -c "TRUNCATE users, trading_configs, stock_holdings, trades, account_balances CASCADE;"
```

## 📞 지원

문제 발생 시:
1. 로그 파일 확인 (`migration_*.log`)
2. 데이터베이스 연결 상태 확인
3. 에러 메시지 분석
4. 필요시 데이터베이스 초기화 후 재실행

## 🎯 다음 단계

마이그레이션 완료 후:
1. ✅ 모든 데이터 검증 완료
2. ✅ 성능 테스트 통과
3. ✅ 이상 현상 없음 확인
4. ➡️ **Dual-Write 모드 활성화**: application.yml의 `DB_DUAL_WRITE: true` 설정
5. ➡️ **읽기 소스 전환**: `DB_READ_SOURCE: rdb`로 변경
6. ➡️ **점진적 검증**: 실제 트래픽에서 동작 확인
7. ➡️ **최종 전환**: MongoDB 의존성 제거

---

**생성 일시**: 2026-01-29
**마이그레이션 버전**: 1.0
**대상 시스템**: quantiq (PostgreSQL + MongoDB)
**원본 시스템**: stock-trading (MongoDB)
