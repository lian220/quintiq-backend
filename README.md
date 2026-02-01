# Quantiq - 알고리즘 트레이딩 플랫폼

PostgreSQL과 MongoDB를 활용한 하이브리드 아키텍처 기반의 주식 자동 매매 시스템입니다.

---

## 🎯 **[→ 다음 작업 보기 (다음_작업.md)](./다음_작업.md)** ⭐

**현재 상태**: Phase 3 진행 중 (85%)
**다음 할 일**: 자동 매도 로직 고도화 (1-2일)

---

## 📋 프로젝트 개요

Quantiq는 거래 데이터와 분석 데이터를 분리하여 최적화된 성능을 제공합니다:

- **PostgreSQL (RDB)**: 사용자 계정, 거래 설정, 보유 종목, 계좌 잔고 등 트랜잭션 데이터
- **MongoDB**: 주식 분석, 추천, 예측, 감정 분석, 일일 시장 데이터 등 분석 데이터

이 구조를 통해 빠른 거래 처리와 복잡한 분석 쿼리를 동시에 지원합니다.

## 🏗️ 시스템 아키텍처

### Hexagonal Architecture + Event-Driven (2026년 최신 트렌드)

**Quantiq Core (Kotlin)**: Hexagonal Architecture (Ports & Adapters)
**Data Engine (Python)**: Feature-based Clean Architecture
**통신**: Event-Driven with Kafka

```
┌─────────────────────────────────────────────────────────────────┐
│                        Quantiq Platform                          │
│         (Hexagonal Architecture + Event-Driven)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │            Quantiq Core (Kotlin - Hexagonal)              │  │
│  │                                                            │  │
│  │  Input Adapter          Application         Domain        │  │
│  │  ┌─────────┐           ┌──────────┐      ┌─────────┐     │  │
│  │  │REST API │──────────►│ UseCase  │─────►│  Port   │     │  │
│  │  │Scheduler│           │ Service  │      │Interface│     │  │
│  │  └─────────┘           └──────────┘      └─────────┘     │  │
│  │                              │                │           │  │
│  │                              ▼                ▼           │  │
│  │  Output Adapter    ┌─────────────────────────────┐       │  │
│  │  ┌──────────────┐  │ Kafka | Quartz | Slack    │       │  │
│  │  │              │  │ WebClient | JPA           │       │  │
│  │  └──────────────┘  └─────────────────────────────┘       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              ▲ Kafka ▼                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │         Data Engine (Python - Feature-based)              │  │
│  │                                                            │  │
│  │  features/                                                │  │
│  │    ├─ economic_data/  (수집 → 저장)                       │  │
│  │    └─ analysis/       (분석 → 발행)                       │  │
│  │                                                            │  │
│  │  core/  (config, database, dependencies)                 │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌────────▼─────────┐                        ┌────────▼─────────┐│
│  │   PostgreSQL     │                        │     MongoDB      ││
│  │   (Port 5433)    │                        │   (Port 27017)   ││
│  │                  │                        │                  ││
│  │  - users         │                        │  - stocks        ││
│  │  - holdings      │                        │  - predictions   ││
│  │  - balances      │                        │  - sentiment     ││
│  │  - configs       │                        │  - daily_data    ││
│  └──────────────────┘                        └──────────────────┘│
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 핵심 아키텍처 특징

#### Hexagonal Architecture (Quantiq Core)
✅ **명확한 계층 분리**: Domain(비즈니스 로직) ← Application(Use Case) ← Adapter(외부 연동)
✅ **테스트 용이성**: Port 인터페이스 기반 모킹으로 독립적 테스트
✅ **프레임워크 독립성**: 비즈니스 로직이 Spring에 의존하지 않음
✅ **확장성**: Adapter 교체로 쉬운 기술 스택 변경 (Kafka → RabbitMQ 등)

#### Event-Driven Architecture
✅ **Polyglot Microservices**: Kotlin (거래) + Python (분석) 최적 조합
✅ **비동기 통신**: Kafka 기반 이벤트 스트리밍으로 느슨한 결합
✅ **CQRS Pattern**: PostgreSQL (Command) + MongoDB (Query) 분리
✅ **Resilience**: 장애 격리 및 자동 복구

### 주요 컴포넌트

#### 1. Quantiq Core (Kotlin/Spring Boot) - **Hexagonal Architecture**
- **포트**: 10010
- **역할**: REST API 제공, 자동 매매 실행, 스케줄링
- **기술**: Spring Boot 3.2.2, Kotlin 1.9.22, Java 21, JPA, Quartz
- **아키텍처**: Hexagonal (Ports & Adapters)
- **구조**:
  - `domain/`: 비즈니스 로직 및 Port 인터페이스
  - `application/`: Use Case 구현
  - `adapter/`: Input (REST, Scheduler) / Output (Kafka, DB, Slack)
- **API 문서**: Swagger UI (SpringDoc OpenAPI 2.3.0)

#### 2. Data Engine (Python/FastAPI) - **Feature-based Clean Architecture**
- **포트**: 10020
- **역할**: 데이터 수집, 분석, 예측 모델 실행
- **기술**: FastAPI, pandas, scikit-learn, pymongo
- **구조**:
  - `features/`: 기능별 모듈 (economic_data, analysis)
  - `core/`: 공통 설정 (database, config, dependencies)

#### 3. PostgreSQL (RDB)
- **포트**: 5433
- **역할**: 트랜잭션 데이터 저장
- **데이터**: 사용자, 보유 종목, 계좌 잔고, 거래 설정

#### 4. MongoDB (분석 DB)
- **호스트**: MongoDB Atlas
- **역할**: 분석 데이터 저장
- **데이터**: 35개 종목, 78만+ 예측 데이터, 추천 및 감정 분석

## 🛠️ 기술 스펙

### Backend
```yaml
Quantiq Core:
  언어: Kotlin 1.9.22
  JDK: Java 21
  프레임워크: Spring Boot 3.2.2
  빌드: Gradle
  주요 라이브러리:
    - Spring Data JPA
    - Spring Web
    - Spring Kafka
    - Quartz Scheduler
    - SpringDoc OpenAPI 2.3.0 (Swagger)
    - PostgreSQL Driver
    - MongoDB Driver
    - Flyway (DB Migration)

Data Engine:
  언어: Python 3.11+
  프레임워크: FastAPI
  주요 라이브러리:
    - pandas
    - numpy
    - pymongo
    - requests
```

### Database
```yaml
PostgreSQL:
  버전: 15
  포트: 5433
  스키마: 4개 테이블 (users, holdings, balances, configs)

MongoDB:
  호스트: Atlas
  컬렉션: 5개 (stocks, predictions, recommendations, sentiment, daily_data)
```

### Infrastructure
```yaml
Docker Compose Services:
  - zookeeper (Confluent Zookeeper 7.5.0)
  - kafka (Confluent Kafka 7.5.0)
  - kafka-ui (Kafka UI - Port 8089)
  - postgresql (PostgreSQL 15-alpine - Port 5433)
  - mongodb (MongoDB latest - Port 27017)
  - quantiq-core (Spring Boot - Port 10010)
  - quantiq-data-engine (FastAPI - Port 10020)

리소스 제한:
  - 총 CPU: 6.5 코어 (최대)
  - 총 메모리: 6.5GB (최대), 3.5GB (예약)

환경 변수:
  - .env.sample: 환경 변수 템플릿
  - .env.prod: 프로덕션 환경 (gitignore)
```

## 🚀 빠른 시작

### 1. 환경 설정
```bash
# 저장소 클론
git clone <repository-url>
cd quantiq

# 환경 변수 설정
cp .env.sample .env.prod
# .env.prod 파일을 열어서 실제 값으로 수정

# Docker 서비스 시작
./start.sh

# 서비스 종료
./stop.sh              # 컨테이너 중지
./stop.sh --clean      # 컨테이너 + 볼륨 삭제
```

### 2. 서비스 접근

#### API Endpoints
```bash
# Quantiq Core REST API
curl http://localhost:10010/api/stocks

# Data Engine API
curl http://localhost:10020/health
```

#### Web UI
| 서비스 | URL | 설명 |
|--------|-----|------|
| **Swagger UI** | http://localhost:10010/swagger-ui.html | REST API 문서 및 테스트 |
| **API Docs (JSON)** | http://localhost:10010/api-docs | OpenAPI 3.0 스펙 |
| **Kafka UI** | http://localhost:8089 | Kafka 토픽 및 메시지 모니터링 |

#### API 문서 (Swagger)
Swagger UI에서 다음 기능을 제공합니다:
- **대화형 API 문서**: 모든 엔드포인트 확인 및 테스트
- **요청/응답 스키마**: 자동 생성된 데이터 모델
- **실시간 테스트**: 브라우저에서 직접 API 호출
- **인증 지원**: API 키 및 토큰 테스트

주요 API 그룹:
- `Stock`: 주식 종목 관리
- `User`: 사용자 계정 관리
- `Balance`: 계좌 잔고 조회
- `Trading`: 자동 매매 설정
- `Analysis`: 분석 데이터 조회
- `Scheduler`: 스케줄러 제어

### 3. 데이터베이스 접근
```bash
# PostgreSQL
docker exec -it quantiq-postgres psql -U quantiq_user -d quantiq

# MongoDB (Atlas)
mongosh -u quantiq_user -p quantiq_password
```

## 📚 상세 문서

프로젝트의 모든 문서는 `docs/` 폴더에 체계적으로 정리되어 있습니다.

### 시작하기
- **[TODO 및 개발 계획](./docs/todo/)** ⭐ - 우선순위별 작업 계획 확인
- **[빠른 시작 가이드](./docs/setup/QUICK_START_RDB.md)** - 빠른 환경 설정
- **[설정 가이드](./docs/setup/SETUP_GUIDE.md)** - 상세 환경 설정

### 아키텍처 및 설계
- **[시스템 아키텍처](./docs/architecture/ARCHITECTURE.md)** - 전체 시스템 구조
- **[Event-Driven Architecture 가이드](./docs/architecture/EVENT_DRIVEN_GUIDE.md)** ⭐ NEW - 이벤트 기반 아키텍처
- **[Event Schema 명세](./docs/architecture/EVENT_SCHEMA.md)** ⭐ NEW - 이벤트 스키마 및 토픽 정의
- **[데이터베이스 전략](./docs/migration/DATABASE_STRATEGY.md)** - DB 설계 전략
- **[마이그레이션 계획](./docs/migration/RDB_MIGRATION_PLAN.md)** - RDB 마이그레이션

### 개발 가이드
- **[프로젝트 개요](./docs/guidelines/PROJECT_OVERVIEW.md)** - 프로젝트 소개
- **[코드 스타일](./docs/guidelines/CODE_STYLE.md)** - 코드 작성 규칙
- **[주요 커맨드](./docs/setup/COMMANDS.md)** - CLI 명령어

### 기능 명세
- **[기능 로드맵](./docs/todo/기능_로드맵.md)** - 전체 기능 계획
- **[Phase 1-3 스펙](./docs/todo/)** - 단계별 구현 스펙
- **[분석 아키텍처](./docs/features/ANALYSIS_ARCHITECTURE.md)** - 분석 시스템

## 🔍 현재 상태

### PostgreSQL (운영 DB)
- 사용자: 1명 (lian@lian.dy220@gmail.com)
- 보유 종목: 20개
- 계좌 잔고: $1,136.72 USD
- 거래 설정: 1개 (활성화)

### MongoDB (분석 DB)
- 종목 데이터: 35개
- 추천 데이터: 2,571건
- 가격 예측: 781,923건
- 감정 분석: 2,328건
- 일일 시장 데이터: 22,002건

## 📞 문의 및 지원

### 문제 해결
- **[로컬 테스트 가이드](./docs/setup/로컬_테스트_가이드.md)** - 테스트 방법
- **[Slack 설정](./docs/setup/Slack_설정_가이드.md)** - 알림 설정

### 개발 진행 상황
- **Phase 1**: ✅ 기본 인프라 구축 완료 (2026-01-29)
- **Phase 2**: ✅ Event-Driven Architecture 구축 완료 (2026-01-31)
- **Phase 3**: 🔄 자동 매매 시스템 구현 중 (80% 완료)
  - ✅ 경제 데이터 수집 (FRED, Yahoo Finance)
  - ✅ 기술적 분석 (SMA, RSI, MACD)
  - ✅ 감정 분석 (Alpha Vantage)
  - ✅ Vertex AI 예측 모델 통합
  - ✅ Slack 알림 (날짜 정보 포함)
  - 🔄 실시간 매도 로직 개선 중

### 최근 업데이트 (2026-02-01)
- ✅ **Slack 알림 개선**: 날짜 정보 추가, MongoDB URI 버그 수정
- ✅ **날짜 범위 기능**: 경제 데이터 및 분석 API에 날짜 범위 지원
- ✅ **Vertex AI**: GCP/Vertex AI 무조건 활성화 설정
- ✅ **REST API 구조 개편**: 컨트롤러 리팩토링 완료
- ✅ **Stock 마이그레이션**: MongoDB → PostgreSQL 전환 완료

### 아키텍처 특징 (2026-01-31 구현)
- **Quantiq Core (Kotlin)**: Hexagonal Architecture (Ports & Adapters)
- **Data Engine (Python)**: Feature-based Clean Architecture
- **통신**: Kafka 기반 Event-Driven Architecture
- **데이터베이스**: PostgreSQL (정형) + MongoDB (비정형) 하이브리드
- **테스트 용이성**: Port 인터페이스 기반 독립적 테스트
- **프레임워크 독립성**: 비즈니스 로직이 Spring에 의존하지 않음

---

**마지막 업데이트**: 2026-02-01
**현재 단계**: Phase 3 진행 중 (80% 완료)
**관리자**: Quantiq Development Team
