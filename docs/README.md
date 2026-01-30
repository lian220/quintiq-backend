# 📚 Quantiq 프로젝트 문서

RDB 마이그레이션 및 시스템 아키텍처 최적화를 위한 완전한 문서 가이드입니다.

## 🗂️ 폴더 구조

```
docs/
├── README.md               📖 메인 인덱스
├── architecture/           🏗️ 시스템 아키텍처
├── features/               🎯 기능 명세 및 로드맵
├── guidelines/             📋 개발 가이드라인
├── migration/              🔄 RDB 마이그레이션
├── setup/                  ⚙️ 설정 및 배포
└── legacy/                 📦 레거시 참고 문서
```

## 📋 주요 섹션

### 🏗️ [아키텍처](./architecture/)
**시스템 설계 및 구조**
- `ARCHITECTURE.md` - 전체 시스템 아키텍처

### 🎯 [기능 명세](./features/)
**기능 로드맵 및 상세 스펙**
- `FEATURE_ROADMAP.md` - 전체 로드맵
- `PHASE_1~3_SPECS.md` - 단계별 구현 스펙
- `ANALYSIS_ARCHITECTURE.md` - 분석 시스템 설계

### 📋 [가이드라인](./guidelines/)
**개발 규칙 및 프로젝트 정보**
- `PROJECT_OVERVIEW.md` - 프로젝트 개요
- `CODE_STYLE.md` - 코드 스타일 가이드

### 🔄 [마이그레이션](./migration/)
**RDB 마이그레이션 계획 및 구현**
- `RDB_MIGRATION_PLAN.md` - 전체 마이그레이션 전략
- `DATABASE_STRATEGY.md` - DB 설계 전략
- `DATABASE_IMPLEMENTATION.md` - 구현 상세 가이드
- `API_MIGRATION.md` - API 마이그레이션 현황

### ⚙️ [설정 및 배포](./setup/)
**개발 환경 설정 및 배포**
- `SETUP_GUIDE.md` - 개발 환경 설정
- `QUICK_START_RDB.md` - 빠른 시작 가이드
- `COMMANDS.md` - 주요 커맨드
- `DEPLOYMENT.md` - 배포 가이드

## 🚀 빠른 시작

1. **처음 시작하시나요?**
   - [PROJECT_OVERVIEW](./guidelines/PROJECT_OVERVIEW.md) 읽기
   - [QUICK_START_RDB](./setup/QUICK_START_RDB.md) 따라하기

2. **마이그레이션 진행 중?**
   - [RDB_MIGRATION_PLAN](./migration/RDB_MIGRATION_PLAN.md) 확인
   - [DATABASE_IMPLEMENTATION](./migration/DATABASE_IMPLEMENTATION.md) 참고

3. **개발 중?**
   - [CODE_STYLE](./guidelines/CODE_STYLE.md) 확인
   - [COMMANDS](./setup/COMMANDS.md) 참고
   - [ARCHITECTURE](./architecture/ARCHITECTURE.md) 이해

## 🔍 빠른 찾기

| 상황 | 참고 문서 |
|------|---------|
| 프로젝트를 이해하고 싶어요 | [프로젝트 개요](./guidelines/PROJECT_OVERVIEW.md) |
| 개발 환경을 설정하고 싶어요 | [설정 가이드](./setup/SETUP_GUIDE.md) |
| 빠르게 시작하고 싶어요 | [빠른 시작](./setup/QUICK_START_RDB.md) |
| 마이그레이션 계획을 알고 싶어요 | [마이그레이션 계획](./migration/RDB_MIGRATION_PLAN.md) |
| DB 전략을 알고 싶어요 | [DB 전략](./migration/DATABASE_STRATEGY.md) |
| 기능 로드맵을 알고 싶어요 | [기능 로드맵](./features/FEATURE_ROADMAP.md) |
| 배포 방법을 알고 싶어요 | [배포 가이드](./setup/DEPLOYMENT.md) |
| 시스템 구조를 이해하고 싶어요 | [아키텍처](./architecture/ARCHITECTURE.md) |
| 코드 스타일을 알고 싶어요 | [코드 스타일](./guidelines/CODE_STYLE.md) |
| 커맨드를 알고 싶어요 | [커맨드](./setup/COMMANDS.md) |

**마지막 업데이트**: 2026-01-30
