# Environment Variables 중앙 관리 가이드

> 흩어진 .env 파일을 루트 디렉토리에서 중앙 관리하는 방법

## 📋 현재 문제점

**현재 상태:**
```
quantiq/
├── .env                          # ❓ 용도 불명확
├── .env.local                    # ✅ Docker Compose에서 사용
├── .env.prod                     # ✅ 프로덕션 설정
├── .env.sample                   # ✅ 템플릿
├── quantiq-core/.env.local       # ❌ 중복
└── quantiq-data-engine/.env.local # ❌ 중복
```

**문제점:**
- 서브프로젝트마다 .env.local이 존재하여 관리 복잡
- Docker와 로컬 개발 환경 간 불일치 가능성
- 동일한 환경 변수가 여러 곳에 중복 저장

---

## ✅ 해결 방안: 루트 .env 중앙 관리

### 1. 파일 구조 (권장)

```
quantiq/
├── .env.local              # 개발 환경 (Docker & 로컬)
├── .env.prod               # 프로덕션 환경
├── .env.sample             # 템플릿 (Git 커밋)
├── quantiq-core/           # .env 파일 제거
└── quantiq-data-engine/    # .env 파일 제거
```

### 2. 용도별 .env 파일 정의

| 파일 | 용도 | Git 관리 |
|------|------|----------|
| `.env.local` | 로컬 개발 & Docker Compose | ❌ (gitignore) |
| `.env.prod` | 프로덕션 배포 | ❌ (gitignore) |
| `.env.sample` | 템플릿 (민감 정보 제거) | ✅ (커밋) |

---

## 🔧 구현 방법

### Step 1: 서브프로젝트 .env 파일 제거

```bash
# 백업 (혹시 모를 차이점 확인용)
cp quantiq-core/.env.local .env.local.core.backup
cp quantiq-data-engine/.env.local .env.local.engine.backup

# 제거
rm quantiq-core/.env.local
rm quantiq-data-engine/.env.local
```

### Step 2: quantiq-core에서 루트 .env 로드 설정

**QuantiqCoreApplication.kt에 dotenv 로딩 추가:**

```kotlin
package com.quantiq.core

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.slf4j.LoggerFactory
import java.io.File

@SpringBootApplication
class QuantiqCoreApplication

private val logger = LoggerFactory.getLogger(QuantiqCoreApplication::class.java)

fun main(args: Array<String>) {
    logger.info("Starting Quantiq Core Application...")

    // Load .env from project root (parent directory)
    val projectRoot = File(System.getProperty("user.dir")).parent ?: System.getProperty("user.dir")
    val dotenv = Dotenv.configure()
        .directory(projectRoot)
        .filename(".env.local")  // 개발 환경
        .ignoreIfMissing()
        .load()

    // Export to System properties for Spring Boot
    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
        logger.debug("Loaded env: ${entry.key}")
    }

    runApplication<QuantiqCoreApplication>(*args)
}
```

**프로덕션 환경:**
```kotlin
// 환경 변수로 프로파일 제어
val profile = System.getenv("SPRING_PROFILES_ACTIVE") ?: "local"
val envFileName = when (profile) {
    "prod" -> ".env.prod"
    else -> ".env.local"
}

val dotenv = Dotenv.configure()
    .directory(projectRoot)
    .filename(envFileName)
    .ignoreIfMissing()
    .load()
```

### Step 3: quantiq-data-engine 설정

**Python은 python-dotenv 사용:**

```python
# quantiq-data-engine/src/main.py 또는 __init__.py

from dotenv import load_dotenv
import os
from pathlib import Path

# Load .env.local from project root (parent directory)
project_root = Path(__file__).parent.parent.parent
env_path = project_root / ".env.local"
load_dotenv(dotenv_path=env_path)

# 사용 예시
FRED_API_KEY = os.getenv("FRED_API_KEY")
SLACK_WEBHOOK_URL = os.getenv("SLACK_WEBHOOK_URL_SCHEDULER")
```

**requirements.txt 또는 pyproject.toml에 추가:**
```toml
[project.dependencies]
python-dotenv = "^1.0.0"
```

### Step 4: Docker Compose는 그대로 유지

**docker-compose.yml:**
```yaml
services:
  quantiq-core:
    env_file:
      - .env.local  # ✅ 이미 루트 .env.local 사용 중
    # ...

  quantiq-data-engine:
    env_file:
      - .env.local  # ✅ 이미 루트 .env.local 사용 중
    # ...
```

---

## 🎯 통합 후 워크플로우

### 로컬 개발 (Docker 없이)

**quantiq-core:**
```bash
cd quantiq/quantiq-core
./gradlew bootRun  # ✅ 자동으로 ../env.local 로드
```

**quantiq-data-engine:**
```bash
cd quantiq/quantiq-data-engine
python src/main.py  # ✅ 자동으로 ../.env.local 로드
```

### Docker Compose 개발

```bash
cd quantiq
docker-compose up  # ✅ .env.local 자동 로드
```

### 프로덕션 배포

```bash
# 환경 변수로 프로파일 설정
export SPRING_PROFILES_ACTIVE=prod

# Docker Compose
docker-compose --env-file .env.prod up -d
```

---

## 📝 .env 파일 템플릿

### .env.sample (Git 커밋용)

```bash
# ============================================
# Slack Configuration
# ============================================
SLACK_WEBHOOK_URL_TRADING=https://hooks.slack.com/services/YOUR_WEBHOOK_URL
SLACK_WEBHOOK_URL_ANALYSIS=https://hooks.slack.com/services/YOUR_WEBHOOK_URL
SLACK_WEBHOOK_URL_SCHEDULER=https://hooks.slack.com/services/YOUR_WEBHOOK_URL
SLACK_BOT_TOKEN=xoxb-YOUR-BOT-TOKEN
SLACK_ENABLED=true
SLACK_CHANNEL=#trading-alerts

# ============================================
# External APIs
# ============================================
FRED_API_KEY=your_fred_api_key_here
ALPHA_VANTAGE_API_KEY=your_alpha_vantage_key_here

# ============================================
# Security
# ============================================
# AES-256 암호화 키 (최소 32자 이상 필수)
APP_ENCRYPTION_KEY=CHANGE_THIS_TO_32_CHAR_SECRET_KEY_HERE

# ============================================
# Database (Docker Compose 자동 설정)
# ============================================
# DB_HOST=localhost
# DB_PORT=5432
# DB_NAME=quantiq
# DB_USER=quantiq_user
# DB_PASSWORD=quantiq_password

# ============================================
# Google Cloud (Optional)
# ============================================
# GCP_PROJECT_ID=your-project-id
# GCP_REGION=us-central1
# GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### .env.local / .env.prod 사용법

```bash
# .env.sample을 복사하여 생성
cp .env.sample .env.local

# 실제 값으로 변경
vim .env.local  # 또는 선호하는 에디터 사용
```

---

## ✅ 장점

1. **단일 진실 공급원**: 모든 환경 변수가 루트 디렉토리에만 존재
2. **Docker & 로컬 개발 통합**: 동일한 .env 파일 사용
3. **환경별 분리**: .env.local (개발) vs .env.prod (프로덕션)
4. **서브프로젝트 독립성**: 각 프로젝트가 루트 .env 참조

---

## 🚨 주의사항

### .gitignore 확인

```gitignore
# Environment files (민감 정보 포함)
.env
.env.local
.env.prod

# Template은 커밋 허용
!.env.sample
```

### 기존 .env 파일 백업

```bash
# 통합 전 백업
tar -czf env-backup-$(date +%Y%m%d).tar.gz \
    .env* \
    quantiq-core/.env* \
    quantiq-data-engine/.env*
```

### 팀원 가이드

**새로운 개발자 온보딩:**
```bash
# 1. 프로젝트 클론
git clone <repo-url>
cd quantiq

# 2. .env 파일 생성
cp .env.sample .env.local

# 3. 실제 값 입력 (팀 리더에게 요청)
vim .env.local

# 4. Docker Compose로 실행
docker-compose up -d
```

---

## 📚 관련 문서

- [KIS 계정 관리 가이드](/docs/USER_KIS_ACCOUNT_GUIDE.md)
- [인증 및 보안 가이드](/docs/AUTHENTICATION_GUIDE.md)
- [Docker Compose 설정](/docker-compose.yml)

---

**마지막 업데이트:** 2026-02-01
**작성자:** Quantiq Development Team
