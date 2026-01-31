# Local 환경 경제 데이터 수집 스케줄러 테스트 가이드

## 🎯 개요

이 가이드는 Spring Quartz 기반 경제 데이터 수집 스케줄러를 local 환경에서 테스트하는 방법을 설명합니다.

**구성:**
- **quantiq-core** (Spring Boot): Quartz 스케줄러 + REST API
- **quantiq-data-engine** (Python/FastAPI): 경제 데이터 수집
- **Kafka**: 비동기 이벤트 통신
- **MongoDB**: 데이터 저장
- **PostgreSQL**: 거래 데이터

---

## 📋 사전 준비

### 1. 환경 변수 설정

**quantiq-core:**
```bash
cd quantiq-core
cp .env.local .env
# 또는 export 명령어로 직접 설정
```

**quantiq-data-engine:**
```bash
cd quantiq-data-engine
cp .env.local .env
```

### 2. Docker Compose 실행

```bash
# quantiq 프로젝트 루트에서
docker-compose up -d

# 상태 확인
docker-compose ps
```

**예상 출력:**
```
CONTAINER ID   IMAGE                    STATUS
xxxxx          confluentinc/cp-kafka    Up 2 minutes
xxxxx          mongo                    Up 2 minutes
xxxxx          postgres                 Up 2 minutes
xxxxx          zookeeper                Up 2 minutes
```

### 3. Kafka Topics 생성

```bash
# 스크립트 실행
bash scripts/create-kafka-topics.sh

# 또는 수동으로
docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic economic.data.update.request \
  --partitions 1 \
  --replication-factor 1

docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

---

## 🚀 애플리케이션 실행

### 1. quantiq-data-engine (Python) 실행

```bash
cd quantiq-data-engine

# Python 환경 준비
python -m venv venv
source venv/bin/activate  # macOS/Linux
# 또는
venv\Scripts\activate  # Windows

# 의존성 설치
pip install -r requirements.txt

# 또는 Poetry 사용
poetry install
poetry run python src/main.py
```

**로그 확인:**
```
INFO - Quantiq Data Engine Started
INFO - Starting Data Engine API server on port 8000
INFO - Subscribed to topics: ['quantiq.analysis.request', 'economic.data.update.request']
```

**API 확인:**
```bash
curl http://localhost:10020/health
# {"status":"alive","timestamp":"2024-..."}
```

### 2. quantiq-core (Spring Boot) 실행

**옵션 A: IDE (IntelliJ/VSCode)에서 실행**
- `QuantiqCoreApplication.kt` 실행
- 또는 main() 함수 클릭 > Run

**옵션 B: 커맨드라인에서 실행**
```bash
cd quantiq-core

# Gradle 빌드 및 실행
./gradlew bootRun

# 또는
./gradlew build
java -jar build/libs/quantiq-core-0.0.1-SNAPSHOT.jar
```

**로그 확인:**
```
INFO - QuantiqCoreApplication Started
INFO - ============================================================
INFO - 주식 자동매매 스케줄러가 시작되었습니다.
INFO - 등록된 스케줄:
INFO -   - 경제 데이터: 매일 06:05
INFO -   - 23:00 작업: 매일 23:00
INFO - ============================================================
```

---

## ✅ 테스트 시나리오

### 시나리오 1: REST API를 통한 수동 트리거

**1.1 경제 데이터 수집 트리거**
```bash
curl -X POST http://localhost:10010/api/economic/trigger-update
```

**예상 응답:**
```json
{
  "success": true,
  "message": "경제 데이터 업데이트 요청이 Kafka에 발행되었습니다.",
  "timestamp": "2024-01-31T10:30:45.123Z"
}
```

**1.2 경제 데이터 상태 확인**
```bash
curl http://localhost:10010/api/economic/status
```

**예상 응답:**
```json
{
  "status": "running",
  "service": "economic-data-scheduler",
  "timestamp": "2024-01-31T10:30:45.123Z",
  "schedules": [
    {
      "name": "economicDataUpdate1",
      "time": "06:05 (KST)",
      "description": "경제 데이터 업데이트"
    },
    {
      "name": "economicDataUpdate2",
      "time": "23:00 (KST)",
      "description": "경제 데이터 재수집 및 Vertex AI 예측 병렬 실행"
    }
  ]
}
```

### 시나리오 2: Kafka 메시지 모니터링

**2.1 요청 메시지 확인**
```bash
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic economic.data.update.request \
  --from-beginning
```

**예상 메시지:**
```json
{
  "timestamp": "2024-01-31T10:30:45.123Z",
  "source": "quartz_scheduler",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**2.2 완료 메시지 확인**
```bash
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic economic.data.updated \
  --from-beginning
```

### 시나리오 3: MongoDB 데이터 확인

```bash
# MongoDB 접속
mongosh

# 데이터베이스 선택
use stock_trading

# 최신 경제 데이터 확인
db.daily_stock_data.find().sort({date: -1}).limit(1).pretty()

# 예상 문서 구조
{
  _id: ObjectId(...),
  date: ISODate("2024-01-31"),
  fred_indicators: {
    "T10YIE": 2.45,
    "T10Y2Y": 0.52,
    "FEDFUNDS": 5.33,
    ...
  },
  yfinance_indicators: {
    "S&P 500": 5000.12,
    "VIX": 14.5,
    ...
  },
  stocks: {...},
  created_at: ISODate(...),
  updated_at: ISODate(...)
}

# 컬렉션 통계
db.daily_stock_data.stats()

# 나가기
exit
```

### 시나리오 4: 스케줄러 상태 확인

**4.1 Spring API를 통한 상태 확인**
```bash
curl http://localhost:10010/api/scheduler/status
```

**예상 응답:**
```json
{
  "isRunning": true,
  "scheduledJobCount": 7,
  "activeTriggerCount": 7
}
```

**4.2 모든 스케줄 조회**
```bash
curl http://localhost:10010/api/scheduler/schedules
```

**4.3 특정 스케줄 일시 중지**
```bash
curl -X POST http://localhost:10010/api/scheduler/schedules/economicDataUpdateTrigger/pause
```

---

## 🔍 로그 확인

### quantiq-core 로그

```bash
# Spring 로그 필터링
# 경제 데이터 관련 로그만 확인
grep "경제 데이터" <log_file>
grep "ECONOMIC" <log_file>
```

### quantiq-data-engine 로그

```bash
# Python 로그 확인 (실시간)
poetry run python src/main.py 2>&1 | grep -E "(경제|Economic|SUCCESS|ERROR)"
```

### Kafka 로그

```bash
# Kafka 브로커 로그
docker logs kafka | tail -50

# Zookeeper 로그
docker logs zookeeper | tail -50
```

---

## ⏰ 스케줄러 테스트 (자동 실행)

### 자동 실행 시뮬레이션

Python을 사용하여 특정 시간에 자동 실행을 시뮬레이션할 수 있습니다:

```bash
# scripts/test_scheduler.py 실행
python scripts/test_scheduler.py
```

**또는 수동으로 시뮬레이션:**

```python
from datetime import datetime
import requests

# 현재 시간이 06:05 또는 23:00이 되도록 테스트
def trigger_at_specific_time():
    url = "http://localhost:10010/api/economic/trigger-update"
    response = requests.post(url)
    print(f"Status: {response.status_code}")
    print(f"Response: {response.json()}")

trigger_at_specific_time()
```

---

## 🛠️ 트러블슈팅

### 문제 1: Kafka 연결 실패

```
ERROR: Connection refused to localhost:9092
```

**해결방법:**
```bash
# Docker 상태 확인
docker-compose ps

# Kafka 재시작
docker-compose restart kafka

# Kafka 로그 확인
docker logs kafka
```

### 문제 2: MongoDB 연결 실패

```
ERROR: Failed to connect to MongoDB
```

**해결방법:**
```bash
# MongoDB 상태 확인
docker exec mongodb mongosh --eval "db.adminCommand('ping')"

# MongoDB 재시작
docker-compose restart mongodb
```

### 문제 3: 경제 데이터 수집 실패 (FRED API)

```
ERROR: FRED API request failed
```

**해결방법:**
```bash
# API Key 확인
echo $FRED_API_KEY

# API 테스트
curl "https://api.stlouisfed.org/fred/series/observations?series_id=T10YIE&api_key=YOUR_API_KEY"
```

### 문제 4: Spring Boot 빌드 실패

```
ERROR: Gradle build failed
```

**해결방법:**
```bash
# Gradle 캐시 삭제
./gradlew clean

# 다시 빌드
./gradlew build
```

---

## 📊 성능 모니터링

### 1. CPU/Memory 사용량 확인

```bash
# Docker 리소스 사용량
docker stats

# 또는 실시간 모니터링
watch -n 1 'docker stats'
```

### 2. Kafka 메시지 처리량

```bash
# Consumer lag 확인
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group quantiq-data-engine-group \
  --describe
```

### 3. 경제 데이터 수집 성능

```bash
# MongoDB 연산 시간 측정
mongosh
db.daily_stock_data.aggregate([
  { $match: { date: ISODate("2024-01-31") } },
  { $project: { date: 1, _id: 1 } }
]).explain("executionStats")
```

---

## ✨ 다음 단계

1. ✅ 경제 데이터 수집 스케줄러 완성
2. 📊 기술적 분석 통합
3. 🤖 Vertex AI 예측 통합
4. 💰 자동 매매 실행
5. 📈 대시보드 구현

---

## 📞 문의

문제가 발생하면:

1. 로그를 먼저 확인하세요
2. Kafka 토픽이 생성되었는지 확인하세요
3. MongoDB/PostgreSQL이 정상 실행 중인지 확인하세요
4. 포트 충돌이 없는지 확인하세요

---

## 📝 로그 위치

- **quantiq-core**: 콘솔 + `quantiq-core/logs/` (설정 시)
- **quantiq-data-engine**: 콘솔 + Python stderr
- **Kafka**: `docker logs kafka`
- **MongoDB**: `docker logs mongodb`
- **PostgreSQL**: `docker logs postgres`
