# 스케줄러 아키텍처

**목적**: QuantIQ 스케줄러 시스템의 구조와 작동 방식 설명
**대상**: 개발자, 시스템 관리자
**작성일**: 2026-01-31

---

## 📋 목차

1. [개요](#개요)
2. [아키텍처 구조](#아키텍처-구조)
3. [Quartz 스케줄러](#quartz-스케줄러)
4. [Job 실행 흐름](#job-실행-흐름)
5. [등록된 Job](#등록된-job)
6. [데이터 저장소](#데이터-저장소)
7. [이벤트 기반 통합](#이벤트-기반-통합)

---

## 개요

QuantIQ 스케줄러는 **Quartz Scheduler**를 기반으로 하며, **Hexagonal Architecture** 패턴을 적용하여 구현되었습니다.

### 주요 특징

- **정기 작업 자동화**: 경제 데이터 업데이트, 병렬 분석 등을 자동으로 실행
- **유연한 스케줄 관리**: Cron 표현식을 사용한 정밀한 시간 제어
- **영속성 지원**: PostgreSQL에 스케줄 상태 저장 (애플리케이션 재시작 시에도 유지)
- **클러스터링 지원**: 분산 환경에서 안전한 Job 실행
- **REST API**: HTTP API를 통한 스케줄러 제어 (pause/resume/start/stop)

### 기술 스택

| 구성 요소 | 기술 |
|----------|------|
| 스케줄러 | Quartz Scheduler 2.3+ |
| 언어 | Kotlin |
| 프레임워크 | Spring Boot 3.x |
| 아키텍처 | Hexagonal Architecture |
| 저장소 | PostgreSQL |
| 타임존 | Asia/Seoul (KST) |

---

## 아키텍처 구조

### Hexagonal Architecture 적용

```
┌─────────────────────────────────────────────────────────────┐
│                      Input Adapters                         │
├─────────────────────────────────────────────────────────────┤
│  SchedulerRestController    EconomicDataUpdateJobAdapter    │
│  (REST API)                 (Quartz Job)                    │
└────────────────┬─────────────────────┬──────────────────────┘
                 │                     │
                 ↓                     ↓
┌─────────────────────────────────────────────────────────────┐
│                   Domain Layer (Port)                       │
├─────────────────────────────────────────────────────────────┤
│  Input Port:  SchedulerUseCase                              │
│  Output Port: SchedulerRepository                           │
└────────────────┬─────────────────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────────────────────┐
│                  Application Layer                          │
├─────────────────────────────────────────────────────────────┤
│  SchedulerManagementService (UseCase 구현)                  │
└────────────────┬─────────────────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────────────────────┐
│                   Output Adapters                           │
├─────────────────────────────────────────────────────────────┤
│  QuartzSchedulerAdapter (Repository 구현)                   │
│  ↓                                                           │
│  Quartz Scheduler → PostgreSQL                              │
└─────────────────────────────────────────────────────────────┘
```

### 주요 컴포넌트

#### Input Adapters
- **SchedulerRestController**: HTTP 요청을 받아 UseCase로 전달
  - 경로: `/api/scheduler/*`
  - 기능: 상태 조회, 스케줄 관리, 시작/중지
- **EconomicDataUpdateJobAdapter**: Quartz 트리거를 받아 경제 데이터 업데이트 실행
- **ParallelAnalysisJob**: Quartz 트리거를 받아 병렬 분석 실행

#### Domain Layer (Port)
- **SchedulerUseCase** (Input Port): 스케줄러 관리 비즈니스 로직 인터페이스
- **SchedulerRepository** (Output Port): 스케줄러 저장소 인터페이스

#### Application Layer
- **SchedulerManagementService**: UseCase 구현체, 비즈니스 로직 처리

#### Output Adapters
- **QuartzSchedulerAdapter**: SchedulerRepository 구현, Quartz와 연동

---

## Quartz 스케줄러

### 핵심 개념

| 개념 | 설명 | 예시 |
|------|------|------|
| **Job** | 실행할 작업 | `EconomicDataUpdateJobAdapter` |
| **JobDetail** | Job의 메타데이터 | Job 이름, 설명, 클래스 |
| **Trigger** | 실행 시점 정의 | Cron 표현식 기반 트리거 |
| **Scheduler** | Job과 Trigger 관리 | Quartz Scheduler 인스턴스 |

### Cron 표현식 구조

```
초(0-59) 분(0-59) 시(0-23) 일(1-31) 월(1-12) 요일(0-7) [년도]
```

**예시:**
```kotlin
"0 5 6 * * ?"      // 매일 06:05:00 실행
"0 5 23 * * ?"     // 매일 23:05:00 실행
"0 0 12 * * MON"   // 매주 월요일 12:00 실행
```

### QuartzConfig.kt 구조

```kotlin
@Configuration
class QuartzConfig {

    // 1. JobDetail 생성 (Job 메타데이터)
    @Bean
    fun economicDataUpdateJobDetail(): JobDetail {
        return JobBuilder.newJob(EconomicDataUpdateJobAdapter::class.java)
            .withIdentity("economicDataUpdateJob")
            .storeDurably()  // 트리거 없어도 유지
            .build()
    }

    // 2. Trigger 생성 (실행 시점)
    @Bean
    fun economicDataUpdateTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(economicDataUpdateJobDetail())
            .withIdentity("economicDataUpdateTrigger")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 5 6 * * ?")
                    .inTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
            )
            .build()
    }
}
```

---

## Job 실행 흐름

### 1. 스케줄 기반 실행 (Cron)

```
시간 도달 (예: 06:05)
    ↓
Quartz Scheduler 트리거 발동
    ↓
Job Adapter 실행 (예: EconomicDataUpdateJobAdapter)
    ↓
UseCase 호출 (예: economicDataUseCase.triggerEconomicDataUpdate())
    ↓
이벤트 발행 (Kafka: quantiq.economic.data.request)
    ↓
Data Engine 처리
    ↓
완료 이벤트 수신 (Kafka: quantiq.economic.data.completed)
    ↓
로그 기록 및 완료
```

### 2. REST API를 통한 수동 실행

```
HTTP Request (POST /api/scheduler/start)
    ↓
SchedulerRestController
    ↓
SchedulerManagementService (UseCase)
    ↓
QuartzSchedulerAdapter (Repository)
    ↓
Quartz Scheduler 시작
    ↓
HTTP Response (success: true)
```

### 3. Job 실행 라이프사이클

```kotlin
// Job 인터페이스 구현
class EconomicDataUpdateJobAdapter : Job {

    override fun execute(context: JobExecutionContext?) {
        try {
            logger.info("경제 데이터 업데이트 시작")

            // 1. UseCase 호출
            economicDataUseCase.triggerEconomicDataUpdate()
                .thenAccept { result ->
                    logger.info("✅ 업데이트 완료: $result")
                }
                .exceptionally { e ->
                    logger.error("❌ 업데이트 실패", e)
                    null
                }
                .get()  // 비동기 완료 대기

            logger.info("경제 데이터 업데이트 종료")
        } catch (e: Exception) {
            logger.error("Job 실행 중 오류", e)
            throw JobExecutionException(e)  // Quartz에 실패 알림
        }
    }
}
```

---

## 등록된 Job

### 1. 경제 데이터 업데이트 (EconomicDataUpdateJobAdapter)

| 속성 | 값 |
|------|-----|
| **Job 이름** | `economicDataUpdateJob` |
| **Trigger 이름** | `economicDataUpdateTrigger` |
| **실행 시간** | 매일 06:05 (KST) |
| **Cron 표현식** | `0 5 6 * * ?` |
| **역할** | 경제 데이터 업데이트 트리거 |
| **이벤트 발행** | `quantiq.economic.data.request` |
| **구현 상태** | ✅ 구현 완료 |

**실행 흐름:**
```
06:05 → EconomicDataUpdateJobAdapter
      → EconomicDataUseCase.triggerEconomicDataUpdate()
      → Kafka 이벤트 발행
      → Data Engine 처리
```

### 2. 병렬 분석 (ParallelAnalysisJob)

| 속성 | 값 |
|------|-----|
| **Job 이름** | `parallelAnalysisJob` |
| **Trigger 이름** | `parallelAnalysisTrigger` |
| **실행 시간** | 매일 23:05 (KST) |
| **Cron 표현식** | `0 5 23 * * ?` |
| **역할** | 기술적 지표 + 감정 분석 병렬 실행 |
| **구현 상태** | ⚠️ 스켈레톤만 존재 (TODO) |

**현재 구현:**
```kotlin
override fun execute(context: JobExecutionContext?) {
    logger.info("병렬 분석 시작 (23:05)")

    // TODO: 병렬 분석 로직 구현
    // - 기술적 지표 분석
    // - 감정 분석

    logger.info("병렬 분석 완료")
}
```

**예정된 구현:**
- 기술적 지표 분석 서비스 호출
- 감정 분석 서비스 호출
- Kafka 이벤트 발행
- 결과 저장

---

## 데이터 저장소

### PostgreSQL 테이블 구조

Quartz는 스케줄 정보를 PostgreSQL에 저장하여 영속성을 보장합니다.

#### 주요 테이블

| 테이블 | 역할 | 주요 컬럼 |
|--------|------|----------|
| `quartz_job_details` | Job 메타데이터 | job_name, job_class_name, is_durable |
| `quartz_triggers` | 트리거 정보 | trigger_name, next_fire_time, trigger_state |
| `quartz_cron_triggers` | Cron 트리거 | cron_expression, time_zone_id |
| `quartz_fired_triggers` | 실행 중인 Job | fired_time, state, instance_name |
| `quartz_scheduler_state` | 스케줄러 상태 | instance_name, last_checkin_time |
| `quartz_locks` | 분산 락 | lock_name |

#### 테이블 관계도

```
quartz_job_details (부모)
    ↓ FK
quartz_triggers
    ↓ FK
quartz_cron_triggers (Cron 타입일 때)
quartz_simple_triggers (Simple 타입일 때)
quartz_simprop_triggers (Property 타입일 때)
```

#### 마이그레이션 파일

- **위치**: `quantiq-core/src/main/resources/db/migration/V3__Create_Quartz_Tables.sql`
- **적용 시점**: Flyway가 애플리케이션 시작 시 자동 실행
- **버전**: V3 (경제 데이터 테이블 이후)

### 스케줄 조회 쿼리 예시

```sql
-- 모든 스케줄 조회
SELECT
    t.trigger_name,
    t.job_name,
    t.next_fire_time,
    t.prev_fire_time,
    t.trigger_state,
    ct.cron_expression,
    ct.time_zone_id
FROM quartz_triggers t
LEFT JOIN quartz_cron_triggers ct
    ON t.trigger_name = ct.trigger_name;

-- 실행 예정 시간 확인
SELECT
    trigger_name,
    to_timestamp(next_fire_time / 1000) AT TIME ZONE 'Asia/Seoul' as next_run,
    trigger_state
FROM quartz_triggers
WHERE trigger_state = 'WAITING'
ORDER BY next_fire_time;
```

---

## 이벤트 기반 통합

### Kafka 이벤트 연동

스케줄러는 **이벤트 기반 아키텍처**와 통합되어 있습니다.

#### 발행 이벤트 (Scheduler → Kafka)

| 이벤트 토픽 | 발행자 | 타이밍 | 페이로드 |
|------------|--------|--------|----------|
| `quantiq.economic.data.request` | EconomicDataUpdateJobAdapter | 매일 06:05 | `{"requestedAt": "2026-01-31T06:05:00"}` |
| `quantiq.analysis.request` | ParallelAnalysisJob (예정) | 매일 23:05 | `{"type": "PARALLEL", "requestedAt": "..."}` |

#### 구독 이벤트 (Kafka → Scheduler)

| 이벤트 토픽 | 구독자 | 처리 내용 |
|------------|--------|----------|
| `quantiq.economic.data.completed` | KafkaMessageListener | 경제 데이터 업데이트 완료 처리 |
| `quantiq.analysis.completed` | KafkaMessageListener | 분석 완료 처리 (AutoTrading 트리거) |

### 이벤트 흐름 예시

```
[06:05] Quartz Scheduler
    ↓
EconomicDataUpdateJobAdapter.execute()
    ↓
EconomicDataUseCase.triggerEconomicDataUpdate()
    ↓
EventPublisher.publishEconomicDataRequest()
    ↓
Kafka Topic: quantiq.economic.data.request
    ↓
Data Engine (Python) 구독
    ↓
경제 데이터 수집 및 처리
    ↓
Kafka Topic: quantiq.economic.data.completed
    ↓
KafkaMessageListener (quantiq-core) 구독
    ↓
EconomicDataCompletedEventHandler.handle()
    ↓
완료 로그 및 알림 (Slack)
```

### 이벤트 스키마

자세한 이벤트 스키마는 [EVENT_SCHEMA.md](./EVENT_SCHEMA.md)를 참조하세요.

---

## 주요 클래스 참조

| 클래스 | 경로 | 역할 |
|--------|------|------|
| `QuartzConfig` | `scheduler/QuartzConfig.kt` | Job 및 Trigger 등록 |
| `EconomicDataUpdateJobAdapter` | `adapter/input/scheduler/` | 경제 데이터 업데이트 Job |
| `ParallelAnalysisJob` | `scheduler/ParallelAnalysisJob.kt` | 병렬 분석 Job |
| `SchedulerRestController` | `adapter/input/rest/` | REST API 엔드포인트 |
| `SchedulerManagementService` | `application/scheduler/` | UseCase 구현체 |
| `QuartzSchedulerAdapter` | `adapter/output/persistence/` | Quartz 연동 Adapter |
| `SchedulerUseCase` | `domain/scheduler/port/input/` | Input Port |
| `SchedulerRepository` | `domain/scheduler/port/output/` | Output Port |

---

## 설정 파일

### application.yml

```yaml
spring:
  quartz:
    job-store-type: jdbc          # PostgreSQL 사용
    jdbc:
      initialize-schema: never     # Flyway가 스키마 관리
    properties:
      org.quartz.scheduler.instanceName: QuantIQScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.class: org.quartz.impl.jdbcjobstore.JobStoreTX
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      org.quartz.jobStore.tablePrefix: quartz_
      org.quartz.jobStore.isClustered: true  # 클러스터링 지원
      org.quartz.threadPool.threadCount: 10
```

### Gradle 의존성

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.postgresql:postgresql")
}
```

---

## 확장 가능성

### 새 Job 추가 방법

1. **Job 클래스 작성**
```kotlin
@Component
class NewAnalysisJob : Job {
    override fun execute(context: JobExecutionContext?) {
        // Job 로직
    }
}
```

2. **QuartzConfig에 등록**
```kotlin
@Bean
fun newAnalysisJobDetail(): JobDetail {
    return JobBuilder.newJob(NewAnalysisJob::class.java)
        .withIdentity("newAnalysisJob")
        .storeDurably()
        .build()
}

@Bean
fun newAnalysisTrigger(): Trigger {
    return TriggerBuilder.newTrigger()
        .forJob(newAnalysisJobDetail())
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 12 * * ?"))
        .build()
}
```

### 클러스터링 고려사항

- Quartz는 PostgreSQL 락을 사용하여 분산 환경에서 중복 실행 방지
- `quartz_locks` 테이블을 통한 분산 락 관리
- 여러 인스턴스가 동시에 실행되어도 안전

---

## 관련 문서

- [스케줄러 운영 가이드](../setup/SCHEDULER_GUIDE.md)
- [이벤트 기반 아키텍처 가이드](./EVENT_DRIVEN_GUIDE.md)
- [이벤트 스키마](./EVENT_SCHEMA.md)
- [스케줄러 마이그레이션 TODO](../todo/스케줄러_마이그레이션_TODO.md)

---

**마지막 업데이트**: 2026-01-31
**버전**: 1.0
**작성자**: QuantIQ Development Team
