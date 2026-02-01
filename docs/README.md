# 📚 Quantiq 프로젝트 문서

알고리즘 트레이딩 플랫폼 Quantiq의 전체 문서입니다.

## 🗂️ 폴더 구조

```
docs/
├── todo/                    📋 TODO 및 개발 계획 ⭐ 시작점
├── architecture/            🏗️ 시스템 아키텍처
├── setup/                   ⚙️ 개발 환경 & 배포
├── features/                ⭐ 주요 기능 & 분석
├── migration/               🔄 마이그레이션 가이드
├── guidelines/              📖 개발 가이드라인
├── analysis/                📊 데이터 분석 결과
├── kis/                     💼 KIS API 연동
└── legacy/                  📦 레거시 문서 (참고용)
```

## 📋 주요 섹션

### 📋 [TODO 및 개발 계획](./todo/) ⭐ 먼저 여기부터!
**모든 TODO 항목과 향후 개선 계획**

**주요 파일**
- ✅ `Phase1_스펙.md` - 기본 인프라 구축 (완료)
- ✅ `Phase2_스펙.md` - 데이터 엔진 통합 (완료)
- 🔜 `Phase3_스펙.md` - 자동 매매 시스템 (진행 중)
- ✅ `Event_Driven_및_Job_아키텍처_TODO.md` - Event-Driven 구현 (완료)
- 🔄 `스케줄러_마이그레이션_TODO.md` - 스케줄러 마이그레이션 (8/9 완료)
- 📊 `분석_검증_체크리스트.md` - 분석 시스템 검증
- 📈 `기능_로드맵.md` - 전체 기능 로드맵

---

### 🏗️ [아키텍처](./architecture/)
**시스템 설계 및 구조**

**시스템 아키텍처**
- `ARCHITECTURE.md` - 전체 시스템 아키텍처 개요
- `시스템_아키텍처.md` - 상세 시스템 구조
- `데이터베이스_설계.md` - PostgreSQL + MongoDB 하이브리드 설계

**이벤트 기반 아키텍처**
- `이벤트_기반_아키텍처.md` - Event-Driven Architecture 설계
- `이벤트_스키마.md` - Kafka 이벤트 스키마 정의
- `스케줄러_아키텍처.md` - Quartz Scheduler 구조

**KIS API 연동**
- `KIS_토큰_관리.md` - KIS 토큰 관리 시스템
- `마이그레이션_히스토리.md` - MongoDB → PostgreSQL 마이그레이션 이력

---

### ⚙️ [설정 및 배포](./setup/)
**개발 환경 설정 및 운영**

**환경 설정**
- `환경설정_가이드.md` - 개발 환경 설정
- `RDB_빠른시작.md` - PostgreSQL 기반 빠른 시작
- `로컬_테스트_가이드.md` - 로컬 테스트 방법

**운영 가이드**
- `배포_운영_가이드.md` - 프로덕션 배포
- `스케줄러_운영_가이드.md` - Quartz Scheduler 운영 (8개 스케줄)
- `명령어_가이드.md` - 주요 CLI 명령어

**Slack 연동**
- `Slack_설정_가이드.md` - Slack 설정 및 연동
- `Slack_스레드_알림.md` - 스레드 기반 알림 시스템
- `Slack_알림_요약.md` - 알림 체계 정리

---

### 💼 [KIS API](./kis/)
**한국투자증권 API 연동**
- `KIS_OVERSEAS_STOCK_API.md` - 해외주식 API 명세

---

### 🔐 [인증 및 보안](.)
**사용자 인증 및 KIS 계정 관리**
- `AUTHENTICATION_GUIDE.md` - 인증 시스템 가이드
- `USER_KIS_ACCOUNT_GUIDE.md` - 사용자별 KIS 계정 관리
- `ENV_MANAGEMENT_GUIDE.md` - 환경변수 관리

---

### 🎯 [기능 명세](./features/)
**기능 로드맵 및 상세 스펙**
- `README.md` - 기능 개요
- `ANALYSIS_ARCHITECTURE.md` - 분석 시스템 설계
- `DOCUMENT_REVIEW.md` - 문서 리뷰
- `MODEL_COMPARISON.md` - 모델 비교
- `PLATFORM_ANALYSIS.md` - 플랫폼 분석

---

### 📊 [분석 결과](./analysis/)
**데이터 분석 리포트**
- `FRED_데이터_분석_2026-02-01.md` - FRED 경제 데이터 분석

---

### 📋 [가이드라인](./guidelines/)
**개발 규칙 및 프로젝트 정보**
- `PROJECT_OVERVIEW.md` - 프로젝트 개요
- `CODE_STYLE.md` - 코드 스타일 가이드

---

### 🔄 [마이그레이션](./migration/)
**RDB 마이그레이션 계획 및 구현**

**전략 및 계획**
- `RDB_MIGRATION_PLAN.md` - 전체 마이그레이션 전략
- `DATABASE_STRATEGY.md` - DB 설계 전략
- `IMPLEMENTATION_PLAN.md` - 구현 계획

**실행 가이드**
- `DATABASE_IMPLEMENTATION.md` - 구현 상세 가이드
- `EXECUTION_GUIDE.md` - 실행 절차
- `API_MIGRATION.md` - API 마이그레이션 현황
- `SCHEDULER_MIGRATION_PRIORITY.md` - 스케줄러 우선순위

**참고 문서**
- `QUICK_REFERENCE.md` - 빠른 참조
- `DATA_STRUCTURE_MIGRATION.md` - 데이터 구조 변환
- `DAILY_STOCK_DATA_IMPLEMENTATION.md` - 일별 주식 데이터

---

### 📦 [레거시 문서](./legacy/)
**참고용 문서 (구 시스템)**
- `MongoDB_Schema_Design.md` - MongoDB 스키마 (구버전)
- `MongoDB_Migration_Implementation.md` - MongoDB 마이그레이션 (구버전)
- `Trading_Flow_Guide.md` - 거래 플로우 (구버전)

---

## 🚀 빠른 시작

### 1️⃣ **다음에 뭘 해야 하는지 모르겠어요?** ⭐
   - [TODO 폴더](./todo/) 확인하기
   - 우선순위별 작업 계획 확인

### 2️⃣ **처음 시작하시나요?**
   1. [PROJECT_OVERVIEW](./guidelines/PROJECT_OVERVIEW.md) 읽기
   2. [환경설정_가이드](./setup/환경설정_가이드.md) 따라하기
   3. [RDB_빠른시작](./setup/RDB_빠른시작.md) 실행

### 3️⃣ **개발 중?**
   - [CODE_STYLE](./guidelines/CODE_STYLE.md) 확인
   - [명령어_가이드](./setup/명령어_가이드.md) 참고
   - [ARCHITECTURE](./architecture/ARCHITECTURE.md) 이해

### 4️⃣ **운영 배포?**
   - [배포_운영_가이드](./setup/배포_운영_가이드.md) 확인
   - [스케줄러_운영_가이드](./setup/스케줄러_운영_가이드.md) 참고
   - [Slack_설정_가이드](./setup/Slack_설정_가이드.md) 설정

---

## 🔍 빠른 찾기

| 상황 | 참고 문서 |
|------|---------|
| **다음 작업 확인** ⭐ | [TODO](./todo/) |
| **프로젝트 이해** | [프로젝트 개요](./guidelines/PROJECT_OVERVIEW.md) |
| **환경 설정** | [환경설정_가이드](./setup/환경설정_가이드.md) |
| **빠른 시작** | [RDB_빠른시작](./setup/RDB_빠른시작.md) |
| **마이그레이션** | [마이그레이션 계획](./migration/RDB_MIGRATION_PLAN.md) |
| **DB 설계** | [데이터베이스_설계](./architecture/데이터베이스_설계.md) |
| **스케줄러 운영** | [스케줄러_운영_가이드](./setup/스케줄러_운영_가이드.md) |
| **Slack 연동** | [Slack_설정_가이드](./setup/Slack_설정_가이드.md) |
| **KIS 계정 관리** | [USER_KIS_ACCOUNT_GUIDE](./USER_KIS_ACCOUNT_GUIDE.md) |
| **인증 시스템** | [AUTHENTICATION_GUIDE](./AUTHENTICATION_GUIDE.md) |
| **환경변수 관리** | [ENV_MANAGEMENT_GUIDE](./ENV_MANAGEMENT_GUIDE.md) |
| **배포 방법** | [배포_가이드](./setup/배포_운영_가이드.md) |
| **시스템 구조** | [아키텍처](./architecture/ARCHITECTURE.md) |
| **이벤트 구조** | [이벤트_스키마](./architecture/이벤트_스키마.md) |
| **코드 스타일** | [코드 스타일](./guidelines/CODE_STYLE.md) |
| **명령어 모음** | [명령어_가이드](./setup/명령어_가이드.md) |

---

## 📊 현재 구현 상태

### ✅ 완료된 기능
- ✅ Event-Driven Architecture (Kafka 기반)
- ✅ PostgreSQL + MongoDB 하이브리드 DB
- ✅ KIS API 연동 (해외주식)
- ✅ 사용자별 KIS 계정 관리
- ✅ Quartz Scheduler (8개 스케줄)
- ✅ Slack 스레드 알림
- ✅ 경제 데이터 수집 (FRED, Yahoo Finance)
- ✅ 기술적 분석 (SMA, RSI, MACD)
- ✅ 뉴스 감정 분석 (Alpha Vantage)
- ✅ 통합 분석 및 자동 추천

### 🔄 진행 중
- 🔄 자동 매매 시스템 (8/9 완료)
- 🔄 Vertex AI 예측 모델 통합

### 🔜 예정
- 🔜 실시간 시세 연동
- 🔜 포트폴리오 최적화
- 🔜 백테스팅 시스템

---

**마지막 업데이트**: 2026-02-01
**문서 버전**: 2.0
