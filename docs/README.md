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
└── legacy/                  📦 레거시 문서 (참고용)
```

## 📋 주요 섹션

### 📋 [TODO 및 개발 계획](./todo/) ⭐ 먼저 여기부터!
**모든 TODO 항목과 향후 개선 계획**
- 🔴 긴급: Phase 2 데이터 엔진 통합
- 🟡 중요: 데이터베이스 마이그레이션 완료
- 🟢 추후: Phase 3 자동 매매 시스템

**주요 파일**
- `Phase1_스펙.md` - 기본 인프라 구축 (✅ 완료)
- `Phase2_스펙.md` - 데이터 엔진 통합 (🔄 진행 중)
- `Phase3_스펙.md` - 자동 매매 시스템 (🔜 예정)
- `데이터베이스_마이그레이션_TODO.md` - DB 마이그레이션 계획
- `스케줄러_마이그레이션_TODO.md` - 스케줄러 마이그레이션

---

### 🏗️ [아키텍처](./architecture/)
**시스템 설계 및 구조**
- `ARCHITECTURE.md` - 전체 시스템 아키텍처

### 🎯 [기능 명세](./features/)
**기능 로드맵 및 상세 스펙**
- `FEATURE_ROADMAP.md` - 전체 로드맵
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

1. **다음에 뭘 해야 하는지 모르겠어요?** ⭐
   - [TODO 폴더](./todo/) 확인하기
   - 우선순위별 작업 계획 확인

2. **처음 시작하시나요?**
   - [PROJECT_OVERVIEW](./guidelines/PROJECT_OVERVIEW.md) 읽기
   - [QUICK_START_RDB](./setup/QUICK_START_RDB.md) 따라하기

3. **마이그레이션 진행 중?**
   - [RDB_MIGRATION_PLAN](./migration/RDB_MIGRATION_PLAN.md) 확인
   - [DATABASE_IMPLEMENTATION](./migration/DATABASE_IMPLEMENTATION.md) 참고

4. **개발 중?**
   - [CODE_STYLE](./guidelines/CODE_STYLE.md) 확인
   - [COMMANDS](./setup/COMMANDS.md) 참고
   - [ARCHITECTURE](./architecture/ARCHITECTURE.md) 이해

## 🔍 빠른 찾기

| 상황 | 참고 문서 |
|------|---------|
| **다음에 뭘 해야 하는지 모르겠어요** ⭐ | **[TODO](./todo/)** |
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

**마지막 업데이트**: 2026-01-31
