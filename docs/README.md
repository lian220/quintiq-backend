# 📚 Quantiq 프로젝트 문서

RDB 마이그레이션 및 시스템 아키텍처 최적화를 위한 완전한 문서 가이드입니다.

## 🗂️ 폴더 구조

```
docs/
├── README.md               📖 메인 인덱스
├── migration/              🔄 RDB 마이그레이션
├── database/               🗄️ 데이터베이스
├── architecture/           🏗️ 시스템 아키텍처
├── setup/                  ⚙️ 설정 및 실행
└── guidelines/             📋 개발 가이드라인
```

## 📋 주요 섹션

### 🔄 [마이그레이션](./migration/)
**RDB 마이그레이션 계획 및 구현**
- `RDB_MIGRATION_PLAN.md` - 전체 마이그레이션 전략
- `DATABASE_STRATEGY.md` - DB 설계 및 최적화 전략
- `DATABASE_IMPLEMENTATION.md` - 구현 상세 가이드

### 🗄️ [데이터베이스](./database/)
**데이터베이스 설계 및 활용**
- 스키마 설계
- 성능 최적화
- 쿼리 가이드

### 🏗️ [아키텍처](./architecture/)
**시스템 설계 및 구조**
- `ARCHITECTURE.md` - 전체 아키텍처 설명

### ⚙️ [설정 및 실행](./setup/)
**개발 환경 설정 및 시작**
- `SETUP_GUIDE.md` - 개발 환경 설정
- `QUICK_START_RDB.md` - 빠른 시작 가이드
- `COMMANDS.md` - 주요 커맨드

### 📋 [가이드라인](./guidelines/)
**개발 규칙 및 프로젝트 정보**
- `CODE_STYLE.md` - 코드 스타일 가이드
- `PROJECT_OVERVIEW.md` - 프로젝트 개요

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
| 시스템 구조를 이해하고 싶어요 | [아키텍처](./architecture/ARCHITECTURE.md) |
| 코드 스타일을 알고 싶어요 | [코드 스타일](./guidelines/CODE_STYLE.md) |
| 커맨드를 알고 싶어요 | [커맨드](./setup/COMMANDS.md) |

**마지막 업데이트**: 2025-01-29
