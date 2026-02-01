# User별 KIS 계정 관리 및 수익률 조회 가이드

> User 기준으로 KIS 계정 정보를 관리하고 실시간 수익률을 조회하는 방법

## 📋 개요

기존 `.env.prod`에 하드코딩되어 있던 KIS 계정 정보를 **사용자별 DB 관리**로 변경하여:
- ✅ 여러 사용자가 각자의 KIS 계정으로 거래 가능
- ✅ 민감한 정보(AppSecret)는 **AES-256 암호화**되어 DB에 저장
- ✅ User 기준으로 **실시간 수익률** 조회 가능
- ✅ REST API를 통한 KIS 계정 등록/조회/관리

---

## 🔐 보안 설정

### 1. 암호화 키 설정

`.env.prod` 파일에 암호화 키를 추가합니다:

```bash
# 최소 32자 이상의 강력한 키 사용
APP_ENCRYPTION_KEY=QuantiqSecureKey2026ForKisApiEncryption!@#
```

⚠️ **주의사항:**
- 프로덕션 환경에서는 반드시 복잡한 키 사용
- 키 변경 시 기존 암호화된 데이터는 복호화 불가
- 환경 변수 또는 AWS Secrets Manager 사용 권장

---

## 📊 데이터베이스 마이그레이션

### 1. Flyway 마이그레이션 실행

```bash
cd quantiq-core
./gradlew flywayMigrate
```

**생성되는 테이블:** `user_kis_accounts`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT | Primary Key |
| `user_id` | BIGINT | User FK (Unique) |
| `app_key` | VARCHAR(100) | KIS API 앱 키 |
| `app_secret_encrypted` | VARCHAR(500) | **암호화된** KIS API 시크릿 |
| `account_number` | VARCHAR(20) | KIS 계좌번호 (앞 8자리) |
| `account_product_code` | VARCHAR(2) | 계좌 상품 코드 (01: 해외주식) |
| `account_type` | VARCHAR(10) | REAL (실전) / MOCK (모의) |
| `enabled` | BOOLEAN | 활성화 여부 |
| `last_used_at` | TIMESTAMP | 마지막 사용 시간 |
| `created_at` | TIMESTAMP | 생성 시간 |
| `updated_at` | TIMESTAMP | 업데이트 시간 |

---

## 🚀 API 사용법

### 1. KIS 계정 등록

**Endpoint:** `POST /api/v1/users/{userId}/kis-account`

```bash
curl -X POST http://localhost:8080/api/v1/users/user123/kis-account \
  -H "Content-Type: application/json" \
  -d '{
    "appKey": "PSSfG4nxnIKqbSWqW0gyzvnQvoJpEiMEDVYj",
    "appSecret": "h2CvdVEBABFvFQvVMnH0FzIQehQJKzPjY2i0b9rcgMrXntrixNCJLIFm69jZfRKgYa13n/rE0vod4af2E7Fs9EcC3jT+59Za9jc3xS165mzSTCB/EQ1wShxF7OAdqXSYi4ReTTxeCnjhqSNcwFe6+361+J4QroXd4RqZnEPnhKPl8DMfw+Y=",
    "accountNumber": "63999039",
    "accountProductCode": "01",
    "accountType": "MOCK",
    "enabled": true
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "KIS account registered successfully",
  "accountNumber": "63999039",
  "accountType": "MOCK"
}
```

⚠️ **보안:**
- `appSecret`은 평문으로 전송되지만, **DB에는 암호화되어 저장**됩니다.
- HTTPS 사용 필수!

---

### 2. KIS 계정 조회

**Endpoint:** `GET /api/v1/users/{userId}/kis-account`

```bash
curl -X GET http://localhost:8080/api/v1/users/user123/kis-account
```

**Response:**
```json
{
  "appKey": "PSSfG4nxnIKqbSWqW0gyzvnQvoJpEiMEDVYj",
  "accountNumber": "63999039",
  "accountProductCode": "01",
  "accountType": "MOCK",
  "enabled": true,
  "lastUsedAt": "2026-02-01T10:30:00",
  "createdAt": "2026-02-01T09:00:00"
}
```

⚠️ **주의:** 응답에 `appSecret`은 포함되지 않습니다 (보안상 이유)

---

### 3. User 수익률 조회 (전체)

**Endpoint:** `GET /api/v1/users/{userId}/balance/profit`

```bash
curl -X GET http://localhost:8080/api/v1/users/user123/balance/profit
```

**Response:**
```json
{
  "userId": "user123",
  "accountNumber": "63999039",
  "holdings": [
    {
      "ticker": "AAPL",
      "name": "APPLE INC",
      "quantity": 50,
      "averagePrice": 148.25,
      "currentPrice": 150.25,
      "evaluationAmount": 7512.50,
      "profitAmount": 100.00,
      "profitRate": 1.35,
      "currency": "USD",
      "exchange": "NASD"
    },
    {
      "ticker": "TSLA",
      "name": "TESLA INC",
      "quantity": 20,
      "averagePrice": 200.00,
      "currentPrice": 210.00,
      "evaluationAmount": 4200.00,
      "profitAmount": 200.00,
      "profitRate": 5.00,
      "currency": "USD",
      "exchange": "NASD"
    }
  ],
  "summary": {
    "totalPurchaseAmount": 11412.50,
    "totalEvaluationAmount": 11712.50,
    "realizedProfit": 0.00,
    "unrealizedProfit": 300.00,
    "totalProfit": 300.00,
    "totalProfitRate": 2.63,
    "currency": "USD"
  },
  "cashBalance": 5000.00,
  "totalAssets": 16712.50,
  "timestamp": "2026-02-01T15:30:45"
}
```

---

### 4. User 수익률 요약 조회

**Endpoint:** `GET /api/v1/users/{userId}/balance/profit-summary`

```bash
curl -X GET http://localhost:8080/api/v1/users/user123/balance/profit-summary
```

**Response:**
```json
{
  "userId": "user123",
  "totalProfitRate": 2.63,
  "totalProfit": 300.00,
  "realizedProfit": 0.00,
  "unrealizedProfit": 300.00,
  "totalAssets": 16712.50,
  "currency": "USD",
  "timestamp": "2026-02-01T15:30:45"
}
```

---

### 5. KIS 계정 활성화/비활성화

**Endpoint:** `PATCH /api/v1/users/{userId}/kis-account/toggle?enabled={true|false}`

```bash
# 비활성화
curl -X PATCH "http://localhost:8080/api/v1/users/user123/kis-account/toggle?enabled=false"

# 활성화
curl -X PATCH "http://localhost:8080/api/v1/users/user123/kis-account/toggle?enabled=true"
```

**Response:**
```json
{
  "success": true,
  "message": "KIS account disabled",
  "enabled": false
}
```

---

## 🔄 마이그레이션 프로세스

### 기존 사용자 데이터 마이그레이션

1. **기존 .env.prod의 KIS 정보 확인**
   ```bash
   KIS_APPKEY=PSSfG4nxnIKqbSWqW0gyzvnQvoJpEiMEDVYj
   KIS_APPSECRET=h2CvdVEBABFvFQvVMnH0FzIQehQJKzPjY2i0b9rcgMrXntrixNCJLIFm69jZfRKgYa13n/rE0vod4af2E7Fs9EcC3jT+59Za9jc3xS165mzSTCB/EQ1wShxF7OAdqXSYi4ReTTxeCnjhqSNcwFe6+361+J4QroXd4RqZnEPnhKPl8DMfw+Y=
   KIS_CANO=63999039
   KIS_ACNT_PRDT_CD=01
   ```

2. **API를 통해 DB에 등록**
   ```bash
   curl -X POST http://localhost:8080/api/v1/users/YOUR_USER_ID/kis-account \
     -H "Content-Type: application/json" \
     -d '{
       "appKey": "PSSfG4nxnIKqbSWqW0gyzvnQvoJpEiMEDVYj",
       "appSecret": "h2CvdVEBABFvFQvVMnH0FzIQehQJKzPjY2i0b9rcgMrXntrixNCJLIFm69jZfRKgYa13n/rE0vod4af2E7Fs9EcC3jT+59Za9jc3xS165mzSTCB/EQ1wShxF7OAdqXSYi4ReTTxeCnjhqSNcwFe6+361+J4QroXd4RqZnEPnhKPl8DMfw+Y=",
       "accountNumber": "63999039",
       "accountProductCode": "01",
       "accountType": "MOCK",
       "enabled": true
     }'
   ```

3. **.env.prod에서 KIS 개인 정보 제거**
   - 기존: `KIS_APPKEY`, `KIS_APPSECRET`, `KIS_CANO`, `KIS_ACNT_PRDT_CD` 삭제
   - 유지: `KIS_BASE_URL`, `KIS_REAL_URL` (공통 설정)

---

## 📈 수익률 계산 로직

KIS API의 잔고 조회 응답에 이미 수익률이 계산되어 제공됩니다:

### 종목별 수익률
```
평가 금액 = 현재가 × 보유 수량
평가 손익 = 평가 금액 - 매수 금액
수익률 (%) = (평가 손익 / 매수 금액) × 100
```

### 전체 계좌 수익률
```
총 손익 = 실현 손익 + 미실현 손익
전체 수익률 (%) = (총 손익 / 총 매수 금액) × 100
```

---

## 🧪 테스트

### 1. 로컬 테스트

```bash
# 1. 앱 실행
cd quantiq-core
./gradlew bootRun

# 2. KIS 계정 등록
curl -X POST http://localhost:8080/api/v1/users/test_user/kis-account \
  -H "Content-Type: application/json" \
  -d '{"appKey":"test_key","appSecret":"test_secret","accountNumber":"12345678","accountType":"MOCK"}'

# 3. 수익률 조회
curl -X GET http://localhost:8080/api/v1/users/test_user/balance/profit-summary
```

### 2. Swagger UI 테스트

브라우저에서 접속:
```
http://localhost:8080/swagger-ui.html
```

**API 그룹:**
- `User KIS Account Controller` - KIS 계정 관리
- `User Balance Controller` - 수익률 조회

---

## ⚠️ 주의사항

### 보안
1. **HTTPS 필수**: Production 환경에서는 반드시 HTTPS 사용
2. **암호화 키 관리**: `APP_ENCRYPTION_KEY`는 환경 변수 또는 Secrets Manager 사용
3. **API 인증**: JWT 또는 OAuth2 인증 추가 권장

### 성능
1. **캐싱**: 수익률 조회는 1분 캐싱 권장 (너무 빈번한 KIS API 호출 방지)
2. **Rate Limit**: KIS API 호출 제한 고려

### 운영
1. **모의투자 먼저**: 실전 투자 전 반드시 모의투자(`MOCK`) 계정으로 테스트
2. **계정 백업**: KIS 계정 정보는 정기적으로 백업
3. **암호화 키 백업**: 암호화 키 분실 시 복호화 불가

---

## 🔧 트러블슈팅

### Q1. "KIS account not found or not active" 에러

**원인:** User의 KIS 계정이 등록되지 않았거나 비활성화됨

**해결:**
```bash
# KIS 계정 등록
curl -X POST http://localhost:8080/api/v1/users/{userId}/kis-account -d {...}

# 또는 활성화
curl -X PATCH "http://localhost:8080/api/v1/users/{userId}/kis-account/toggle?enabled=true"
```

---

### Q2. "Encryption key must be at least 32 characters" 에러

**원인:** `.env.prod`의 `APP_ENCRYPTION_KEY`가 32자 미만

**해결:**
```bash
# .env.prod
APP_ENCRYPTION_KEY=QuantiqSecureKey2026ForKisApiEncryption!@#
```

---

### Q3. 수익률이 0으로 표시됨

**원인:** KIS API 응답 파싱 실패 또는 보유 종목 없음

**해결:**
1. KIS 계정에 실제 보유 종목이 있는지 확인
2. 로그 확인: `com.quantiq.core.application.balance.BalanceService`
3. KIS API 응답 구조 변경 여부 확인

---

## 📚 관련 문서

- [KIS API 레퍼런스](/docs/kis/KIS_OVERSEAS_STOCK_API.md)
- [데이터베이스 스키마](/docs/database/schema.md)
- [보안 가이드](/docs/security/encryption.md)

---

**마지막 업데이트:** 2026-02-01
**작성자:** Quantiq Development Team
