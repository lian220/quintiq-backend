# Quantiq - 알고리즘 트레이딩 플랫폼

PostgreSQL과 MongoDB를 활용한 하이브리드 아키텍처 기반의 주식 자동 매매 시스템입니다.

## 📋 프로젝트 개요

Quantiq는 거래 데이터와 분석 데이터를 분리하여 최적화된 성능을 제공합니다:

- **PostgreSQL (RDB)**: 사용자 계정, 거래 설정, 보유 종목, 계좌 잔고 등 트랜잭션 데이터
- **MongoDB**: 주식 분석, 추천, 예측, 감정 분석, 일일 시장 데이터 등 분석 데이터

이 구조를 통해 빠른 거래 처리와 복잡한 분석 쿼리를 동시에 지원합니다.

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                     Quantiq Platform                     │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────┐         ┌──────────────────┐      │
│  │  Quantiq Core    │         │  Data Engine     │      │
│  │   (Kotlin)       │◄───────►│   (Python)       │      │
│  │                  │         │                  │      │
│  │  - REST API      │         │  - 데이터 수집    │      │
│  │  - 자동 매매     │         │  - 분석 엔진      │      │
│  │  - 스케줄러      │         │  - 예측 모델      │      │
│  └────────┬─────────┘         └────────┬─────────┘      │
│           │                            │                 │
│           │                            │                 │
│  ┌────────▼─────────┐         ┌────────▼─────────┐      │
│  │   PostgreSQL     │         │     MongoDB      │      │
│  │   (Port 5433)    │         │   (MongoDB Atlas)│      │
│  │                  │         │                  │      │
│  │  - users         │         │  - stocks        │      │
│  │  - holdings      │         │  - predictions   │      │
│  │  - balances      │         │  - sentiment     │      │
│  │  - configs       │         │  - daily_data    │      │
│  └──────────────────┘         └──────────────────┘      │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### 주요 컴포넌트

#### 1. Quantiq Core (Kotlin/Spring Boot)
- **포트**: 10010
- **역할**: REST API 제공, 자동 매매 실행, 스케줄링
- **기술**: Spring Boot 3.4.1, Kotlin 2.1.0, JPA, Quartz

#### 2. Data Engine (Python)
- **포트**: 8000
- **역할**: 데이터 수집, 분석, 예측 모델 실행
- **기술**: FastAPI, pandas, scikit-learn

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
  언어: Kotlin 2.1.0
  프레임워크: Spring Boot 3.4.1
  빌드: Gradle 8.11.1
  주요 라이브러리:
    - Spring Data JPA
    - Spring Web
    - Quartz Scheduler
    - PostgreSQL Driver
    - MongoDB Driver

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
Docker:
  - quantiq-postgres (PostgreSQL 15)
  - quantiq-core (Spring Boot)
  - quantiq-data-engine (FastAPI)

환경 변수:
  - .env: 로컬 개발 환경
  - .env.prod: 프로덕션 환경
```

## 🚀 빠른 시작

### 1. 환경 설정
```bash
# 저장소 클론
git clone <repository-url>
cd quantiq

# Docker 서비스 시작
docker-compose up -d

# 초기 데이터 설정
./scripts/init_quantiq.sh
```

### 2. 서비스 접근
```bash
# Quantiq Core API
curl http://localhost:10010/api/users/lian

# Data Engine API
curl http://localhost:8000/health
```

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
- **[로컬 테스트 가이드](./docs/setup/LOCAL_TEST_GUIDE.md)** - 테스트 방법
- **[Slack 설정](./docs/setup/SLACK_SETUP_GUIDE.md)** - 알림 설정

### 개발 진행 상황
- **Phase 1**: ✅ 기본 인프라 구축 완료
- **Phase 2**: 🔄 데이터 엔진 통합 진행 중
- **Phase 3**: 🔜 자동 매매 시스템 예정

---

**마지막 업데이트**: 2026-01-31
**상태**: ✅ Phase 1 완료, Phase 2 진행 중
**관리자**: Quantiq Development Team
