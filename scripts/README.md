# Data Migration Scripts

stock-trading MongoDB → quantiq (PostgreSQL + MongoDB) 데이터 마이그레이션 도구 모음

## 📦 포함된 파일

| 파일 | 역할 | 설명 |
|------|------|------|
| `run_migration.sh` | 메인 실행 스크립트 | 마이그레이션 자동 실행 (권장) |
| `migrate_data.py` | 마이그레이션 로직 | 실제 데이터 이동 처리 |
| `validate_migration.py` | 검증 스크립트 | 마이그레이션 후 데이터 무결성 확인 |
| `requirements.txt` | 의존성 정의 | Python 패키지 목록 |
| `MIGRATION_GUIDE.md` | 상세 가이드 | 전체 마이그레이션 프로세스 설명 |
| `README.md` | 이 파일 | 빠른 시작 가이드 |

## 🚀 빠른 시작

### 1️⃣ 사전 확인
```bash
# Docker 상태 확인
docker ps | grep quantiq

# PostgreSQL 연결 테스트
psql -h localhost -p 5433 -U quantiq_user -d quantiq -c "SELECT 1"

# MongoDB 연결 테스트
mongosh -u quantiq_user -p quantiq_password --authenticationDatabase admin
```

### 2️⃣ 마이그레이션 실행
```bash
cd /Users/imdoyeong/Desktop/workSpace/quantiq
./scripts/run_migration.sh
```

### 3️⃣ 데이터 검증
```bash
python3 scripts/validate_migration.py
```

## 📊 마이그레이션 흐름

```
┌─────────────────────────────────────────────────────────────┐
│         stock-trading MongoDB (원본)                         │
│  - users, stocks, user_stocks, trading_logs, etc           │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ migrate_data.py
                       ▼
         ┌─────────────────────────────────┐
         │    Data Transformation Layer     │
         │  - 스키마 매핑                   │
         │  - 데이터 검증                   │
         │  - 타입 변환                     │
         └────────┬───────────────┬────────┘
                  │               │
         ┌────────▼─┐     ┌──────▼────────┐
         │PostgreSQL│     │   MongoDB      │
         │(RDB)     │     │   (분석)       │
         ├──────────┤     ├────────────────┤
         │- users   │     │- stocks        │
         │- trades  │     │- recommendations
         │- holdings│     │- predictions   │
         │- configs │     │- sentiment     │
         │- balances│     │- daily_data    │
         └──────────┘     └────────────────┘
                │               │
                └───────┬───────┘
                        │
                        │ validate_migration.py
                        ▼
         ┌──────────────────────────────┐
         │   Validation Report           │
         │ - 무결성 확인                  │
         │ - 일관성 검증                  │
         │ - 성능 평가                    │
         └──────────────────────────────┘
```

## 📋 마이그레이션 매핑

### PostgreSQL (RDB) - 트랜잭션 데이터
```
stock-trading          →  quantiq
────────────────────────────────────────
stocks.users           →  users
stocks.trading_configs →  trading_configs
stocks.user_stocks     →  stock_holdings (계산)
stocks.trading_logs    →  trades
(계산)                 →  account_balances (계산)
```

### MongoDB - 분석 데이터
```
stock-trading              →  quantiq
──────────────────────────────────────────
stocks.stocks              →  stocks
stocks.stock_recommendations  →  stock_recommendations
stocks.stock_predictions   →  stock_predictions
stocks.sentiment_analysis  →  sentiment_analysis
stocks.daily_stock_data    →  daily_stock_data
```

## 🔧 명령어 상세

### 옵션 1: 자동 실행 (권장)
```bash
# 가장 간단하고 권장되는 방법
./scripts/run_migration.sh

# 출력 예시:
# ==========================================
# Data Migration: stock-trading → quantiq
# ==========================================
# ...
# ✓ Migration completed successfully!
```

### 옵션 2: 수동 단계별 실행
```bash
# 1. 가상환경 생성
cd scripts
python3 -m venv venv
source venv/bin/activate

# 2. 의존성 설치
pip install -r requirements.txt

# 3. 마이그레이션 실행
cd ..
python3 scripts/migrate_data.py

# 4. 검증 실행
python3 scripts/validate_migration.py
```

## 📊 로그 파일

마이그레이션 실행 후 로그가 생성됩니다:
```bash
# 최신 로그 확인
tail -f migration_*.log

# 에러만 필터링
grep ERROR migration_*.log

# 통계 확인
grep "MIGRATION SUMMARY" -A 20 migration_*.log
```

## ✅ 검증 항목

마이그레이션 후 자동 검증:
- ✓ 각 테이블별 레코드 수 확인
- ✓ NULL 기본키 확인
- ✓ 외래키 관계 검증
- ✓ 데이터 타입 일관성 확인
- ✓ 계정 잔액 계산 검증
- ✓ MongoDB 컬렉션 문서 수 확인

## ⚠️ 주의 사항

| 항목 | 설명 |
|------|------|
| 🔒 백업 | 마이그레이션 전 원본 MongoDB 백업 필수 |
| 🧹 테스트 환경 | 프로덕션이 아닌 로컬에서만 실행 |
| 🔌 네트워크 | stock-trading MongoDB 접근 필수 |
| ⏸️ 중단 금지 | 스크립트 실행 중 중단하지 않기 |
| 🔄 재실행 | 안전하게 재실행 가능 (중복 자동 처리) |

## 🔍 검증 쿼리

마이그레이션 후 수동 검증:

```bash
# PostgreSQL 접속
psql -h localhost -p 5433 -U quantiq_user -d quantiq

# 사용자 수 확인
SELECT COUNT(*) as user_count FROM users;

# 보유 종목 수
SELECT COUNT(*) as holdings_count FROM stock_holdings;

# 거래 기록 수
SELECT COUNT(*) as trades_count FROM trades;

# 계좌 잔액 확인
SELECT user_id, cash_balance, total_asset FROM account_balances;
```

```bash
# MongoDB 접속
mongosh -u quantiq_user -p quantiq_password --authenticationDatabase admin

# 종목 수
db.stocks.count()

# 추천 신호 수
db.stock_recommendations.count()

# 샘플 데이터 확인
db.stocks.findOne()
```

## 🛠️ 문제 해결

### 연결 오류
```
ERROR: connection to stock-trading MongoDB failed
→ .env 파일의 MONGO_URL, MONGO_USER, MONGO_PASSWORD 확인
→ 네트워크 접근성 확인
```

### PostgreSQL 포트 충돌
```
ERROR: connection to PostgreSQL failed (port 5433)
→ docker ps로 컨테이너 실행 확인
→ netstat -an | grep 5433으로 포트 사용 확인
```

### 메모리 부족
```
ERROR: MemoryError during migration
→ 대량 데이터의 경우 배치 크기 조정
→ migrate_data.py에서 배치 처리 구현
```

## 📞 로그 분석

```bash
# 마이그레이션 통계 확인
cat migration_*.log | grep "MIGRATION SUMMARY" -A 20

# 에러 목록 확인
cat migration_*.log | grep "errors\|ERROR\|Error"

# 각 테이블별 마이그레이션 결과
cat migration_*.log | grep "migrated"

# 실행 시간 확인
cat migration_*.log | head -1
cat migration_*.log | tail -1
```

## 🔄 재실행

마이그레이션은 멱등성을 지원하므로 안전하게 재실행 가능:

```bash
# 그냥 재실행 (중복 자동 처리)
./scripts/run_migration.sh

# 또는 데이터 초기화 후 재실행
psql -h localhost -p 5433 -U quantiq_user -d quantiq \
  -c "TRUNCATE users, trading_configs, stock_holdings, trades, account_balances CASCADE;"
./scripts/run_migration.sh
```

## 📈 마이그레이션 후 단계

1. **검증 완료** ✓ validate_migration.py 통과
2. **성능 테스트** → 쿼리 성능 측정
3. **Dual-Write 활성화** → application.yml에서 `DB_DUAL_WRITE: true`
4. **읽기 소스 전환** → `DB_READ_SOURCE: rdb`
5. **점진적 검증** → 실제 트래픽 모니터링
6. **최종 전환** → MongoDB 의존성 제거

## 📚 추가 정보

- **상세 가이드**: [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)
- **마이그레이션 계획**: `/tmp/migration_plan.md`
- **프로젝트 구조**: 프로젝트 루트의 `docs/` 폴더

## 🎯 다음 단계

마이그레이션 완료 후:
```bash
# 1. 검증 실행
python3 scripts/validate_migration.py

# 2. 성능 테스트 (선택)
# - 주요 쿼리 성능 비교
# - 인덱스 효율성 확인

# 3. 응용프로그램 재시작
docker-compose restart quantiq-core

# 4. 통합 테스트
# - API 요청 테스트
# - 데이터 읽기/쓰기 검증
```

---

**마지막 업데이트**: 2026-01-29
**버전**: 1.0
**상태**: 프로덕션 준비 완료
