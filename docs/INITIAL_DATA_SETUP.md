# 초기 데이터 설정 가이드

> ⚠️ **중요**: 하이브리드 데이터베이스 아키텍처 사용
> - **PostgreSQL**: 정형 데이터 (stocks, users, trades)
> - **MongoDB**: 비정형 데이터 (ML 예측, 분석 결과, daily_stock_data)

---

## 🎯 하이브리드 데이터베이스 아키텍처

### ✅ PostgreSQL - 정형 데이터 저장소
**역할**: 구조화된 트랜잭션 데이터
- **stocks**: 주식 메타데이터 (35개 - ticker, name, sector 등)
- **users**: 사용자 정보
- **trading_configs**: 거래 설정
- **trades**: 거래 내역
- **account_balances**: 계좌 잔고
- **stock_holdings**: 보유 주식
- **상태**: ✅ Flyway 자동 마이그레이션

### ✅ MongoDB - 비정형 데이터 저장소
**역할**: ML/AI 결과, 복잡한 분석 데이터
- **daily_stock_data**: 일별 주식 데이터 (복잡한 nested structure)
- **prediction_results**: Vertex AI 예측 결과
- **stock_recommendations**: AI 추천 결과 (기술적 지표 포함)
- **sentiment_analysis**: 뉴스 감정 분석
- **stock_analysis_results**: 종합 분석 결과
- **~~stocks~~**: ⚠️ 삭제 예정 (PostgreSQL로 전환 완료)
- **상태**: ✅ Production 동기화

---

## 📋 필수 PostgreSQL 테이블

### 1. stocks (필수) ✅
**용도**: 주식 기본 정보
**데이터 수**: 35개
**상태**: ✅ Flyway 마이그레이션 완료

```bash
# 동기화 명령어
python scripts/sync_from_prod_mongodb.py --stocks-only --live --force
```

### 2. daily_stock_data (필수) ✅
**용도**: 주식 일별 시세 데이터
**데이터 수**: 7,336개
**상태**: ✅ Production에서 동기화 완료

```bash
# stocks + daily_stock_data 동시 동기화
python scripts/sync_from_prod_mongodb.py --essential --live --force
```

### 3. stock_recommendations (선택)
**용도**: AI 주식 추천 결과
**데이터 수**: 953개
**상태**: ⚠️ 필요시 동기화

### 4. stock_predictions (선택)
**용도**: 주식 예측 데이터
**데이터 수**: 260,748개
**상태**: ⚠️ 필요시 동기화 (용량 큼)

### 5. sentiment_analysis (선택)
**용도**: 뉴스/소셜미디어 감성 분석
**데이터 수**: 831개
**상태**: ⚠️ 필요시 동기화

### 6. prediction_results (자동생성)
**용도**: 새로운 예측 결과 저장
**데이터 수**: 0개 (신규 시스템에서 생성)
**상태**: ⏳ 자동 생성됨

### 7. economic_data (자동생성)
**용도**: 경제 지표 데이터 (FRED API)
**데이터 수**: 0개 (신규 시스템에서 수집)
**상태**: ⏳ 자동 수집됨

---

## 🔄 동기화 명령어 정리

### 기본: stocks만 (가장 빠름)
```bash
python scripts/sync_from_prod_mongodb.py --stocks-only --live --force
```

### 추천: stocks + daily_stock_data (필수 데이터)
```bash
python scripts/sync_from_prod_mongodb.py --essential --live --force
```

### 전체: 모든 컬렉션 (시간 오래 걸림)
```bash
python scripts/sync_from_prod_mongodb.py --all --live --force
```

---

## 📊 PostgreSQL 데이터 (이미 있음)

### 사용자 데이터
- **users**: 사용자 정보
- **trading_configs**: 자동매매 설정
- **stock_holdings**: 보유 주식
- **trades**: 거래 내역
- **account_balances**: 계좌 잔고

### stocks 테이블
- **데이터 수**: 35개 (MongoDB와 동기화됨)
- **상태**: ✅ PostgreSQL 마이그레이션 완료

---

## 🚀 초기 설정 체크리스트

### 1단계: MongoDB 초기화 후 복구 (완료)
- [x] MongoDB 컨테이너 재시작
- [x] stocks 데이터 동기화 (35개)
- [x] daily_stock_data 동기화 (7,336개)

### 2단계: 데이터 검증
```bash
# MongoDB 데이터 확인
docker compose exec mongodb mongosh stock_trading \
  --authenticationDatabase admin \
  -u quantiq_user -p quantiq_password \
  --eval "
    print('stocks:', db.stocks.countDocuments({}));
    print('daily_stock_data:', db.daily_stock_data.countDocuments({}));
  "

# PostgreSQL 데이터 확인
docker compose exec postgresql psql -U quantiq_user -d quantiq \
  -c "SELECT COUNT(*) as stocks FROM stocks;"
```

### 3단계: 애플리케이션 재시작
```bash
# quantiq-core 재시작
docker compose restart quantiq-core

# 로그 확인
docker compose logs -f quantiq-core
```

### 4단계: 추가 데이터 동기화 (필요시)
```bash
# stock_recommendations 추가
python scripts/sync_from_prod_mongodb.py --all --live --force
```

---

## 🔧 트러블슈팅

### MongoDB 데이터가 비어있을 때
```bash
# 1. Production에서 재동기화
python scripts/sync_from_prod_mongodb.py --essential --live --force

# 2. 데이터 확인
docker compose exec mongodb mongosh stock_trading \
  --authenticationDatabase admin \
  -u quantiq_user -p quantiq_password \
  --eval "db.stocks.countDocuments({})"
```

### MongoDB 손상 시
```bash
# 1. 컨테이너 중지 및 삭제
docker compose stop mongodb
docker compose rm -f mongodb

# 2. 데이터 디렉토리 초기화 (주의!)
rm -rf data/mongodb && mkdir -p data/mongodb

# 3. MongoDB 재시작
docker compose up -d mongodb

# 4. 데이터 복구
python scripts/sync_from_prod_mongodb.py --essential --live --force
```

### PostgreSQL stocks 데이터가 없을 때
```bash
# Migration 스크립트 실행
cd scripts
python setup_initial_data.py
```

---

## 📝 Production MongoDB 정보

- **URI**: `mongodb+srv://cluster-test.2dkjwjs.mongodb.net`
- **Database**: `stock_trading`
- **User**: `test`
- **Password**: `6n2AB4V2halcSvfv` (변경 권장)

---

## ⚠️ 주의사항

1. **데이터 동기화는 Production → Local만 가능**
   - Local → Production 동기화 금지 (데이터 손실 위험)

2. **대용량 컬렉션 주의**
   - `stock_predictions` (260K): 동기화 시간 오래 걸림
   - 필요시에만 `--all` 옵션 사용

3. **자동 생성 컬렉션**
   - `prediction_results`, `economic_data`는 시스템 실행 시 자동 생성
   - 별도 동기화 불필요

4. **정기 백업**
   - Production MongoDB는 정기적으로 백업
   - Local MongoDB는 개발용이므로 손실 가능
