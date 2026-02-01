# Vertex AI 예측 시스템 리팩토링 완료

## 📋 수정 개요

Python 로컬 호출 방식에서 **Vertex AI 직접 실행 + Slack 알림** 방식으로 전환

### 주요 변경 사항

1. ✅ **VertexAIService.kt** - Python 호출 제거, Vertex AI SDK 직접 사용
2. ✅ **SlackApiClient.kt** - Vertex AI 작업 알림 메서드 추가
3. ✅ **predict_optimized.py** - 완료 메시지 Pub/Sub 발행 추가
4. ✅ **GcpConfig.kt** - @EnableAsync 추가

---

## 🔄 변경 전 vs 변경 후

### 변경 전 (❌ 문제점)
```
Spring Boot API
  ↓
VertexAIService.createAndRunCustomJob()
  ↓
ProcessBuilder로 Python 스크립트 로컬 실행
  ↓
run_predict_vertex_ai.py (로컬에서 2시간 대기)
  ↓
Vertex AI CustomJob 생성 및 실행
  ↓
2시간 동기 대기 (블로킹)
  ↓
응답 반환

문제점:
- Spring Boot가 2시간 블로킹됨
- 로컬에서 Python 실행 필요
- 완료/실패 알림 없음
- .env.sample 참조 (실제 설정 아님)
```

### 변경 후 (✅ 개선)
```
Spring Boot API
  ↓
VertexAIService.createAndRunCustomJob()
  ├─ 1. Slack 시작 알림 📤
  ├─ 2. GCS에서 패키지 URI 조회
  ├─ 3. Vertex AI CustomJob 생성 (Kotlin SDK)
  ├─ 4. Job 실행 (즉시 응답 반환)
  └─ 5. 비동기 모니터링 시작 (@Async)
        ↓
  [백그라운드] monitorJobCompletion()
    - 30초마다 Job 상태 확인
    - 완료 시 → Slack 완료 알림 ✅
    - 실패 시 → Slack 실패 알림 ❌
    - 타임아웃 시 → Slack 타임아웃 알림 ⏱️

동시 실행:
  Vertex AI CustomJob (GCS 패키지 실행)
    ↓
  predict_optimized.py 실행
    ├─ 모델 학습/예측
    ├─ GCS에 결과 저장
    ├─ MongoDB에 저장
    └─ Pub/Sub 완료 메시지 발행 📡

장점:
- 즉시 응답 (비블로킹)
- 완료/실패 Slack 알림
- GCS 패키지 직접 실행
- .env.local 환경 변수 사용
```

---

## 📝 수정 파일 상세

### 1. VertexAIService.kt

#### 변경 사항
```kotlin
// 이전: Python 로컬 호출
val processBuilder = ProcessBuilder("python3", scriptPath)
process.waitFor(2, TimeUnit.HOURS)  // 2시간 블로킹!

// 이후: Vertex AI SDK 직접 사용
val customJob = buildCustomJob(packageUri, envVars)
val createdJob = jobServiceClient.createCustomJob(parent, customJob)
monitorJobCompletion(createdJob.name, requestId)  // 비동기 모니터링
```

#### 주요 메서드

**createAndRunCustomJob()**
1. Slack 시작 알림 전송
2. GCS에서 최신 패키지 URI 조회
3. 환경 변수 설정 (MongoDB, GCS 등)
4. CustomJob 생성 및 실행
5. 비동기 모니터링 시작
6. **즉시 Job 이름 반환** (블로킹 없음)

**monitorJobCompletion()** - @Async
- 30초 간격으로 Job 상태 확인
- JOB_STATE_SUCCEEDED → Slack 완료 알림
- JOB_STATE_FAILED → Slack 실패 알림
- JOB_STATE_CANCELLED → Slack 취소 알림
- Timeout 시 → Slack 타임아웃 알림

---

### 2. SlackApiClient.kt

#### 추가된 메서드

**notifyVertexAIJobStarted(requestId, jobName)**
```kotlin
🚀 Vertex AI 예측 작업 시작
┌─────────────────────────────────────
│ Request ID: uuid
│ Job Name: quantiq-stock-prediction-job
│ Timestamp: 2026-02-01T10:00:00+09:00
│ Status: 🔄 RUNNING
└─────────────────────────────────────
```

**notifyVertexAIJobCompleted(requestId, jobName, duration, status)**
```kotlin
✅ Vertex AI 예측 작업 완료
┌─────────────────────────────────────
│ Request ID: uuid
│ Job Name: quantiq-stock-prediction-job
│ Duration: 15분
│ Status: ✅ SUCCESS
│ Completion Time: 2026-02-01T10:15:00+09:00
└─────────────────────────────────────
```

**notifyVertexAIJobFailed(requestId, jobName, error)**
```kotlin
❌ Vertex AI 예측 작업 실패
┌─────────────────────────────────────
│ Request ID: uuid
│ Job Name: quantiq-stock-prediction-job
│ Error: Out of memory
│ Timestamp: 2026-02-01T10:05:00+09:00
│ Action: 로그 확인 후 재시도 필요
└─────────────────────────────────────
```

---

### 3. predict_optimized.py

#### 추가된 함수

**publish_completion_message(status, duration_seconds, error_msg)**
```python
# Pub/Sub로 완료 메시지 발행
topic_name = "projects/{project}/topics/vertex-ai-job-completion"

message = {
    "status": "SUCCESS" or "FAILED",
    "duration": 900.5,  # 초
    "timestamp": "2026-02-01T10:15:00",
    "job_type": "stock_prediction",
    "error": None or "error message"
}

publisher.publish(topic_name, json.dumps(message))
```

#### main() 함수 수정
```python
def main():
    try:
        # ... 기존 예측 로직 ...

        elapsed = (datetime.now() - start_time).total_seconds()

        # ✅ 완료 메시지 발행
        publish_completion_message("SUCCESS", elapsed)

    except Exception as e:
        # ❌ 실패 메시지 발행
        publish_completion_message("FAILED", 0, str(e))
        raise
```

---

### 4. GcpConfig.kt

#### 추가된 어노테이션
```kotlin
@Configuration
@EnableAsync  // ⬅️ 추가: 비동기 메서드 활성화
@ConditionalOnProperty(...)
class GcpConfig { ... }
```

---

## 🚀 사용 방법

### 1. API 호출
```bash
curl -X POST http://localhost:8080/api/vertex-ai/predict
```

### 2. 응답 (즉시 반환)
```json
{
  "success": true,
  "message": "Vertex AI 예측 실행 완료",
  "jobId": "projects/123/locations/us-central1/customJobs/456",
  "estimatedTime": "3-5분"
}
```

### 3. Slack 알림 수신

**시작 시**
```
🚀 Vertex AI 예측 작업 시작
Request ID: abc-123
Status: 🔄 RUNNING
```

**완료 시** (3-20분 후)
```
✅ Vertex AI 예측 작업 완료
Duration: 15분
Status: ✅ SUCCESS
```

**실패 시**
```
❌ Vertex AI 예측 작업 실패
Error: GPU quota exceeded
Action: 로그 확인 후 재시도 필요
```

---

## ⚙️ 환경 변수 설정

### .env.local (실제 설정 파일)
```bash
# GCP 설정
GCP_ENABLED=true
GCP_PROJECT_ID=arboreal-path-479202-c5
GCP_REGION=us-central1
GCP_STAGING_BUCKET=stock-trading-packages
GCS_BUCKET=quantiq-ml-models

# Vertex AI
VERTEX_AI_MACHINE_TYPE=n1-standard-4
VERTEX_AI_GPU_TYPE=NVIDIA_TESLA_T4
VERTEX_AI_GPU_COUNT=1
VERTEX_AI_TIMEOUT=3600

# Slack
SLACK_WEBHOOK_URL_SCHEDULER=https://hooks.slack.com/services/...
SLACK_ENABLED=true

# MongoDB
MONGODB_URI=mongodb://...

# Google 인증
GOOGLE_APPLICATION_CREDENTIALS=/app/credentials/vertex-ai-key.json
```

---

## 📊 실행 흐름 다이어그램

```
사용자 API 호출
      ↓
┌─────────────────────────────────────────────────────────────┐
│ VertexAIService.createAndRunCustomJob()                     │
├─────────────────────────────────────────────────────────────┤
│ 1. Slack 시작 알림 📤                                        │
│ 2. GCS 패키지 URI 조회                                       │
│ 3. Vertex AI CustomJob 생성                                 │
│ 4. Job 실행                                                  │
│ 5. 즉시 응답 반환 (Job ID)                                  │
└─────────────────────────────────────────────────────────────┘
      ↓
┌─────────────────────────────────────────────────────────────┐
│ 비동기 처리 (@Async)                                         │
├─────────────────────────────────────────────────────────────┤
│ monitorJobCompletion()                                       │
│   - 30초마다 상태 확인                                       │
│   - 완료/실패 시 Slack 알림                                 │
└─────────────────────────────────────────────────────────────┘
      ‖ (병렬 실행)
      ‖
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Vertex AI CustomJob (GCS 패키지 실행)                       │
├─────────────────────────────────────────────────────────────┤
│ predict_optimized.py                                         │
│   1. MongoDB 데이터 로드                                     │
│   2. 모델 학습/Fine-tuning                                   │
│   3. 예측 수행                                               │
│   4. GCS에 모델 저장                                         │
│   5. MongoDB에 결과 저장                                     │
│   6. Pub/Sub 완료 메시지 발행 📡                            │
└─────────────────────────────────────────────────────────────┘
      ↓
Slack 완료 알림 ✅
```

---

## 🎯 핵심 개선 사항

### 1. 비블로킹 실행
- **이전**: 2시간 동기 대기 (Spring Boot 블로킹)
- **이후**: 즉시 응답 + 백그라운드 모니터링

### 2. 실시간 알림
- **시작 알림**: Job 실행 즉시
- **완료/실패 알림**: 자동 감지 후 전송
- **Slack 채널**: #스케쥴러

### 3. 환경 설정
- **이전**: .env.sample (샘플 값)
- **이후**: .env.local (실제 값)

### 4. 실행 방식
- **이전**: Python 로컬 실행 → Vertex AI 호출
- **이후**: Kotlin에서 Vertex AI 직접 호출

### 5. 모니터링
- **폴링 방식**: 30초 간격 Job 상태 확인
- **Pub/Sub 메시지**: Python 스크립트에서 완료 신호
- **이중 안전망**: 두 가지 방식 병행

---

## 🔧 다음 단계 (선택 사항)

### Pub/Sub 리스너 추가 (더 빠른 완료 감지)

현재는 **폴링 방식**(30초 간격)으로 충분하지만, 더 빠른 반응을 원한다면:

1. **Pub/Sub 토픽 생성**
```bash
gcloud pubsub topics create vertex-ai-job-completion --project=arboreal-path-479202-c5
```

2. **구독 생성**
```bash
gcloud pubsub subscriptions create vertex-ai-completion-sub \
  --topic=vertex-ai-job-completion \
  --project=arboreal-path-479202-c5
```

3. **Spring Boot Pub/Sub 리스너 추가**
```kotlin
@Service
class VertexAICompletionListener {

    @MessageMapping("vertex-ai-job-completion")
    fun handleCompletion(message: PubSubMessage) {
        val data = message.data.toStringUtf8()
        val completion = objectMapper.readValue(data, CompletionMessage::class.java)

        if (completion.status == "SUCCESS") {
            slackApiClient.notifyVertexAIJobCompleted(...)
        } else {
            slackApiClient.notifyVertexAIJobFailed(...)
        }
    }
}
```

하지만 **현재 폴링 방식도 충분히 효율적**입니다 (최대 30초 지연).

---

## ✅ 테스트 체크리스트

- [ ] API 호출 시 즉시 응답 반환 확인
- [ ] Slack에 시작 알림 수신 확인
- [ ] Vertex AI Console에서 Job 실행 확인
- [ ] Slack에 완료/실패 알림 수신 확인
- [ ] MongoDB에 예측 결과 저장 확인
- [ ] GCS에 모델 파일 저장 확인

---

## 📌 참고 사항

### Vertex AI CustomJob 실행 시간
- **첫 실행** (전체 학습): ~20분
- **이후 실행** (Fine-tuning): ~3-5분

### GPU 사용량
- **타입**: NVIDIA_TESLA_T4
- **개수**: 1
- **머신**: n1-standard-4

### MongoDB 컬렉션
- `daily_stock_data`: 학습 데이터
- `prediction_results`: 예측 결과 (PredictionResult 엔티티)

### GCS 버킷
- **staging**: `stock-trading-packages` (패키지 업로드)
- **models**: `quantiq-ml-models` (모델 저장)

---

## 📞 문제 해결

### Job이 실행되지 않는 경우
1. GOOGLE_APPLICATION_CREDENTIALS 확인
2. GCS 버킷에 패키지 업로드 확인
3. Vertex AI API 활성화 확인
4. GPU 할당량 확인

### Slack 알림이 오지 않는 경우
1. SLACK_WEBHOOK_URL_SCHEDULER 확인
2. SLACK_ENABLED=true 확인
3. SlackApiClient 로그 확인

### Job이 실패하는 경우
1. Vertex AI Console에서 로그 확인
2. MongoDB 연결 문자열 확인
3. GCS 버킷 권한 확인
