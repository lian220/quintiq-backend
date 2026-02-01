# Event-Driven & Job 아키텍처 TODO

## ✅ Phase 1: Event-Driven Architecture 구현 완료 (2026-01-31)

### 완료 항목

#### 1. Event Schema 표준화
- [x] 통일된 이벤트 구조 정의 (`eventId`, `eventType`, `timestamp`, `source`, `payload`)
- [x] 도메인별 토픽 명명 규칙: `quantiq.<domain>.<event-type>`
- [x] Kotlin Event Schema 구현 (`events/EventSchema.kt`)
- [x] Python Event Schema 구현 (`events/schema.py`)
- [x] 문서화: `docs/architecture/EVENT_SCHEMA.md`

#### 2. Kotlin (quantiq-core) Event 시스템
- [x] 도메인별 Event Publisher 구현
  - [x] `EconomicEventPublisher`
  - [x] `StockEventPublisher`
  - [x] `TradingEventPublisher`
  - [x] `AnalysisEventPublisher`
- [x] Kafka Producer/Consumer 설정 최적화
  - [x] `acks=all`, `idempotence=true` (정확한 1회 전송)
  - [x] 압축: `snappy`
  - [x] 재시도: 3회
- [x] Event Listener 구현 (`KafkaMessageListener.kt`)
  - [x] 분석 완료 이벤트
  - [x] 경제 데이터 업데이트 완료
  - [x] 경제 데이터 동기화 실패
  - [x] 매매 신호 감지

#### 3. Python (quantiq-data-engine) Event 시스템
- [x] Event Handler 구현
  - [x] `EconomicEventHandler`
  - [x] `StockEventHandler`
  - [x] `AnalysisEventHandler`
- [x] Event Router 구현 (토픽별 핸들러 라우팅)
- [x] Event Publisher 개선 (Singleton 패턴)
- [x] main.py Consumer 멀티 토픽 구독

#### 4. REST → Event 전환
- [x] `EconomicDataSchedulerService` → Event 발행
- [x] Backward Compatibility (Legacy 토픽 지원)

#### 5. 분석 이벤트 시스템 구현 (2026-01-31 추가)
- [x] 분석 요청 이벤트 (Kotlin → Python)
  - [x] `analysis.technical.request` - 기술적 분석 요청
  - [x] `analysis.sentiment.request` - 뉴스 감정 분석 요청
  - [x] `analysis.combined.request` - 통합 분석 요청
- [x] 분석 완료 이벤트 (Python → Kotlin)
  - [x] `analysis.technical.completed` - 기술적 분석 완료
  - [x] `analysis.sentiment.completed` - 감정 분석 완료
  - [x] `analysis.completed` - 통합 분석 완료
- [x] Slack 스레드 패턴 구현
  - [x] Kotlin: Slack 스레드 생성 및 threadTs 획득
  - [x] Kotlin: Kafka 메시지에 threadTs 포함
  - [x] Python: threadTs로 스레드 답글 형태로 진행상황 업데이트
- [x] Python 분석 서비스
  - [x] `RecommendationService` - 통합 분석 레이어
  - [x] `TechnicalAnalysisService` - SMA, RSI, MACD 계산
  - [x] `SentimentAnalysisService` - Alpha Vantage NEWS_SENTIMENT API
  - [x] 통합 점수 계산: 기술적(70%) + 감정(30%) 가중평균
- [x] 스케줄러 Job 구현
  - [x] `CombinedAnalysisJobAdapter` (23:45) - 통합 분석
  - [x] Domain/Application/Adapter 레이어 구현

#### 6. 문서화
- [x] Event Schema 문서
- [x] Event-Driven 사용 가이드
- [x] 테스트 방법 및 트러블슈팅
- [x] 분석 이벤트 스키마 (2026-01-31 추가)
  - [x] 기술적/감정/통합 분석 이벤트 정의
  - [x] Slack 스레드 패턴 문서화
  - [x] 통합 점수 계산식 문서화

### 구현된 이벤트 Flow

```
Quartz Scheduler → quantiq.economic.data.sync.requested → Data Engine
Data Engine → quantiq.economic.data.updated → Quantiq Core
Data Engine → quantiq.trading.signal.detected → Quantiq Core → Auto Trading
```

---

## 🚀 Phase 2: Job-based 아키텍처 전환 (리소스 최적화)

### 목표

**현재**: Python Data Engine이 항상 실행되며 Kafka Consumer로 메시지 대기 (리소스 상시 사용)

**개선**: 필요할 때만 Python Job을 실행하여 리소스 효율화

### 구현 방안

#### 옵션 1: Celery + Kafka (추천 ⭐)

**장점**:
- Python 생태계 표준 분산 작업 큐
- Kafka 메시지 → Celery Task 자동 트리거
- 작업 재시도, 우선순위, 스케줄링 기본 지원
- 모니터링 도구 (Flower) 제공

**구조**:
```
quantiq-core → Kafka Event → Celery Worker → Python Job 실행 → 결과 이벤트 발행
```

**구현 단계**:
1. [ ] Celery + Redis 설치 및 설정
2. [ ] Kafka Consumer → Celery Task Trigger 브릿지 구현
3. [ ] 도메인별 Celery Task 정의
   - [ ] `tasks/economic_data_task.py`
   - [ ] `tasks/stock_analysis_task.py`
   - [ ] `tasks/prediction_task.py`
4. [ ] Celery Worker Docker 컨테이너 추가
5. [ ] Flower 모니터링 UI 설정

**예상 코드**:
```python
# tasks/economic_data_task.py
from celery import Celery

app = Celery('quantiq', broker='redis://redis:6379/0')

@app.task(bind=True, max_retries=3)
def collect_economic_data_task(self, request_id, data_types):
    try:
        collect_economic_data()
        # 완료 이벤트 발행
        EventPublisher.publish(EventTopics.ECONOMIC_DATA_UPDATED, ...)
    except Exception as e:
        self.retry(exc=e, countdown=60)
```

**docker-compose 추가**:
```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  celery-worker:
    build:
      context: ./quantiq-data-engine
    command: celery -A tasks worker --loglevel=info
    depends_on:
      - redis
      - kafka

  flower:
    build:
      context: ./quantiq-data-engine
    command: celery -A tasks flower
    ports:
      - "5555:5555"
```

---

#### 옵션 2: Kubernetes CronJob + Event Trigger

**장점**:
- K8s 네이티브 (이미 K8s 사용 중이면 추천)
- 자동 스케일링, 리소스 제한
- Job 실행 히스토리 관리

**구조**:
```
Kafka Event → K8s Job Controller → Python Pod 생성 → 작업 실행 → Pod 종료
```

**구현 단계**:
1. [ ] Kubernetes 클러스터 설정
2. [ ] Job Controller 구현 (Kafka Consumer → K8s Job 생성)
3. [ ] Python Job Pod 이미지 최적화
4. [ ] CronJob 스케줄 정의 (정기 작업)
5. [ ] Event-triggered Job 정의 (on-demand 작업)

**예상 코드**:
```yaml
# k8s/jobs/economic-data-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: economic-data-collection
spec:
  template:
    spec:
      containers:
      - name: data-collector
        image: quantiq-data-engine:latest
        command: ["python", "jobs/collect_economic_data.py"]
        env:
        - name: REQUEST_ID
          value: "req-12345"
      restartPolicy: OnFailure
```

---

#### 옵션 3: Docker + Kafka Consumer + Process Pool

**장점**:
- 현재 인프라 그대로 활용
- 복잡도 낮음
- 빠른 구현

**구조**:
```
Kafka Consumer (항상 실행) → Process Pool → Worker Process 생성/종료
```

**구현 단계**:
1. [ ] Python multiprocessing.Pool 구현
2. [ ] Kafka 메시지 수신 시 Worker Process 실행
3. [ ] 작업 완료 후 Process 종료
4. [ ] Process Pool 크기 동적 조정

**예상 코드**:
```python
from multiprocessing import Pool

def worker(event_data):
    # 작업 실행
    collect_economic_data()
    # 완료 이벤트 발행
    EventPublisher.publish(...)

# main.py
pool = Pool(processes=4)

while True:
    msg = consumer.poll(1.0)
    if msg:
        pool.apply_async(worker, (msg,))
```

---

#### 옵션 4: AWS Lambda / Cloud Functions (Serverless)

**장점**:
- 완전 자동 스케일링
- 사용한 만큼만 과금
- 관리 오버헤드 제로

**단점**:
- 클라우드 종속
- Cold Start 지연
- 실행 시간 제한 (15분)

**구현 단계**:
1. [ ] Lambda 함수 작성
2. [ ] Kafka → Lambda Trigger 설정 (EventBridge)
3. [ ] 환경 변수 및 시크릿 관리
4. [ ] VPC 설정 (DB 접근)

---

### 성능 비교

| 방식 | 리소스 효율 | 복잡도 | 확장성 | 모니터링 | 추천도 |
|------|------------|--------|--------|----------|--------|
| **Celery + Redis** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ 추천 |
| K8s CronJob | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 🟡 K8s 환경 시 |
| Process Pool | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ | 🟡 빠른 구현 |
| AWS Lambda | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 🔴 클라우드 환경 시 |

---

## 📋 Phase 2 구현 계획 (Celery 방식)

### Step 1: Celery 기본 설정
- [ ] Redis 추가 (docker-compose.yml)
- [ ] Celery 설치 (`requirements.txt`)
- [ ] Celery app 초기화 (`celeryconfig.py`)

### Step 2: Task 정의
- [ ] `tasks/__init__.py`
- [ ] `tasks/economic_data_task.py`
- [ ] `tasks/stock_analysis_task.py`
- [ ] `tasks/prediction_task.py`

### Step 3: Kafka → Celery 브릿지
- [ ] `bridges/kafka_celery_bridge.py`
  - Kafka Consumer → Celery Task 트리거
  - 이벤트 타입별 Task 매핑

### Step 4: Worker 설정
- [ ] Celery Worker Dockerfile
- [ ] docker-compose에 worker 추가
- [ ] 리소스 제한 설정

### Step 5: 모니터링
- [ ] Flower UI 설정
- [ ] Task 실행 로그
- [ ] 실패 Task 재시도 정책

### Step 6: 기존 코드 마이그레이션
- [ ] `main.py` → Kafka-Celery 브릿지로 변경
- [ ] Event Handler → Celery Task로 전환
- [ ] 기존 Consumer 코드 제거

### Step 7: 테스트
- [ ] Task 실행 테스트
- [ ] 재시도 로직 테스트
- [ ] 성능 테스트 (리소스 사용량)

---

## 🎯 예상 효과

### 현재 (Always-On Consumer)
```
quantiq-data-engine:
  CPU: 0.5 core (상시)
  Memory: 512MB (상시)
  실행 시간: 24시간
```

### 개선 후 (Job-based)
```
Kafka Consumer (lightweight):
  CPU: 0.1 core (상시)
  Memory: 128MB (상시)

Celery Worker (필요 시):
  CPU: 1 core (작업 시)
  Memory: 512MB (작업 시)
  실행 시간: 작업 시간만 (예: 1일 30분)

리소스 절감: ~70-80%
```

---

## 📚 참고 자료

### Celery
- 공식 문서: https://docs.celeryq.dev/
- Kafka Integration: https://docs.celeryq.dev/en/stable/userguide/configuration.html
- Best Practices: https://docs.celeryq.dev/en/stable/userguide/tasks.html

### Kubernetes Jobs
- Job 가이드: https://kubernetes.io/docs/concepts/workloads/controllers/job/
- CronJob 가이드: https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/

### 아키텍처 패턴
- Event-Driven Microservices: https://martinfowler.com/articles/201701-event-driven.html
- Job Queue Patterns: https://aws.amazon.com/blogs/compute/orchestrating-a-job-queue-with-aws-batch/

---

## ⚠️ 주의사항

1. **Stateless Job 설계**
   - Job은 상태를 유지하지 않아야 함
   - 모든 컨텍스트는 메시지 또는 DB에서 로드

2. **Idempotency (멱등성)**
   - 같은 Job이 여러 번 실행되어도 안전해야 함
   - 중복 실행 방지 로직 필요

3. **에러 핸들링**
   - Retry 정책 명확히 정의
   - Dead Letter Queue 설정
   - 실패 알림 (Slack)

4. **모니터링**
   - Job 실행 시간 추적
   - 실패율 모니터링
   - 리소스 사용량 대시보드

---

**작성일**: 2026-01-31
**현재 상태**: Phase 1 완료, Phase 2 계획 수립
**다음 액션**: Celery 기본 설정 및 POC 구현
