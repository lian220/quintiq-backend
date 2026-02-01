# KIS Developers 해외주식 API 레퍼런스

> 한국투자증권 오픈API - 해외주식 거래 API 전체 목록

## 📋 목차

- [API 서버 정보](#api-서버-정보)
- [기본시세 API](#1-기본시세-api-13개)
- [시세분석 API](#2-시세분석-api-15개)
- [주문 API](#3-주문-api-6개)
- [계좌 조회 API](#4-계좌-조회-api-10개)
- [요청 파라미터](#주요-요청-파라미터)
- [지원 거래소](#지원-거래소)
- [인증 헤더](#공통-인증-헤더)
- [API 호출 예시](#api-호출-예시)

---

## API 서버 정보

| 환경 | URL |
|------|-----|
| **Production** | `https://openapi.koreainvestment.com:9443` |
| **Simulation** | `https://openapivts.koreainvestment.com:29443` |
| **Real-time WebSocket** | `ws://ops.koreainvestment.com:21000` |

---

## API 목록 (총 44개)

### 1. 기본시세 API (13개)

시세 조회 및 기본 정보

| API 명칭 | HTTP | Endpoint | 설명 |
|---------|------|----------|------|
| 해외주식 현재가 조회 | GET | `/uapi/overseas-price/v1/quotations/price` | 실시간 현재가 조회 |
| 해외주식 현재가 상세 | GET | `/uapi/overseas-price/v1/quotations/price-detail` | 상세 시세 정보 |
| 해외주식 일별 시세 | GET | `/uapi/overseas-price/v1/quotations/dailyprice` | 일별 종가 데이터 |
| 해외주식 기간별 시세 | GET | `/uapi/overseas-price/v1/quotations/inquire-daily-chartprice` | 차트용 일별 시세 |
| 해외주식 분별 시세 | GET | `/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice` | 분별 차트 데이터 |
| 해외주식 지수 분별 차트 | GET | `/uapi/overseas-price/v1/quotations/inquire-time-indexchartprice` | 지수 분별 차트 |
| 해외주식 종목 검색 | GET | `/uapi/overseas-price/v1/quotations/inquire-search` | 종목명/코드 검색 |
| 해외주식 호가 조회 | GET | `/uapi/overseas-price/v1/quotations/inquire-asking-price` | 매수/매도 호가 |
| 해외주식 통화 정보 | GET | `/uapi/overseas-price/v1/quotations/inquire-ccnl` | 통화별 환율 정보 |
| 해외주식 업종별 시세 | GET | `/uapi/overseas-price/v1/quotations/industry-price` | 업종별 시세 조회 |
| 해외주식 업종/테마 조회 | GET | `/uapi/overseas-price/v1/quotations/industry-theme` | 업종/테마 분류 |
| 해외휴장일 조회 | GET | `/uapi/overseas-stock/v1/quotations/countries-holiday` | 국가별 휴장일 |
| 미국 장 운영 시간 | GET | `/uapi/overseas-stock/v1/quotations/countries-operation-time` | 거래소 운영시간 |

---

### 2. 시세분석 API (15개)

순위, 뉴스, 투자 정보

#### 순위 조회 (9개)

| API 명칭 | HTTP | Endpoint | 설명 |
|---------|------|----------|------|
| 시가총액 순위 | GET | `/uapi/overseas-stock/v1/ranking/market-cap` | 시가총액 상위 |
| 가격 변동 순위 | GET | `/uapi/overseas-stock/v1/ranking/price-fluct` | 가격 변동폭 |
| 상승/하락률 순위 | GET | `/uapi/overseas-stock/v1/ranking/updown-rate` | 등락률 순위 |
| 거래량 순위 | GET | `/uapi/overseas-stock/v1/ranking/trade-vol` | 거래량 상위 |
| 거래량 급증 순위 | GET | `/uapi/overseas-stock/v1/ranking/volume-surge` | 거래량 급증 |
| 거래량 파워 순위 | GET | `/uapi/overseas-stock/v1/ranking/volume-power` | 거래강도 순위 |
| P/B 비율 순위 | GET | `/uapi/overseas-stock/v1/ranking/trade-pbmn` | PBR 순위 |
| 성장률 순위 | GET | `/uapi/overseas-stock/v1/ranking/trade-growth` | 성장률 순위 |
| 회전율 순위 | GET | `/uapi/overseas-stock/v1/ranking/trade-turnover` | 회전율 순위 |

#### 뉴스 및 투자정보 (6개)

| API 명칭 | HTTP | Endpoint | 설명 |
|---------|------|----------|------|
| 뉴스 제목 조회 | GET | `/uapi/overseas-price/v1/quotations/news-title` | 종목 관련 뉴스 |
| 속보 뉴스 조회 | GET | `/uapi/overseas-price/v1/quotations/brknews-title` | 긴급 속보 뉴스 |
| 배당/권리일 조회 | GET | `/uapi/overseas-price/v1/quotations/period-rights` | 배당 일정 |
| 권리 내용 조회 | GET | `/uapi/overseas-price/v1/quotations/rights-by-ice` | 권리 상세 정보 |
| 콜 가능성 조회 | GET | `/uapi/overseas-price/v1/quotations/colable-by-company` | 콜 옵션 정보 |
| 종목 정보 조회 | GET | `/uapi/overseas-price/v1/quotations/inquire-info` | 종목 기본 정보 |

---

### 3. 주문 API (6개)

매수/매도 주문 실행 및 취소 (모두 POST)

| API 명칭 | HTTP | Endpoint | 설명 |
|---------|------|----------|------|
| 해외주식 주문 | POST | `/uapi/overseas-stock/v1/trading/order` | 신규 주문 (매수/매도) |
| 해외주식 장중 주문 | POST | `/uapi/overseas-stock/v1/trading/daytime-order` | 장중 실시간 주문 |
| 해외주식 예약 주문 | POST | `/uapi/overseas-stock/v1/trading/order-resv` | 시간외 예약 주문 |
| 해외주식 주문 취소 | POST | `/uapi/overseas-stock/v1/trading/order-rvsecncl` | 주문 취소/정정 |
| 해외주식 장중 취소 | POST | `/uapi/overseas-stock/v1/trading/daytime-order-rvsecncl` | 장중 취소 |
| 해외주식 예약 취소 | POST | `/uapi/overseas-stock/v1/trading/order-resv-ccnl` | 예약 주문 취소 |

---

### 4. 계좌 조회 API (10개)

잔고 및 거래내역 조회

#### 계좌 잔고 (5개)

| API 명칭 | HTTP | Endpoint | 설명 |
|---------|------|----------|------|
| 해외주식 잔고 | GET | `/uapi/overseas-stock/v1/trading/inquire-balance` | 전체 잔고 조회 |
| 해외주식 현재 잔고 | GET | `/uapi/overseas-stock/v1/trading/inquire-present-balance` | 실시간 잔고 |
| 해외주식 주문 가능 조회 | GET | `/uapi/overseas-stock/v1/trading/inquire-nccs` | 주문 가능 수량 |
| 해외주식 매수 가능 조회 | GET | `/uapi/overseas-stock/v1/trading/inquire-psamount` | 매수 가능 금액 |
| 해외주식 통화별 잔고 | GET | `/uapi/overseas-stock/v1/trading/inquire-ccnl` | 통화별 보유 현황 |

#### 거래 내역 (5개)

| API 명칭 | HTTP | Endpoint | 설명 |
|---------|------|----------|------|
| 해외주식 예약주문 내역 | GET | `/uapi/overseas-stock/v1/trading/order-resv-list` | 예약 주문 목록 |
| 해외주식 기간별 거래내역 | GET | `/uapi/overseas-stock/v1/trading/inquire-period-trans` | 기간별 체결 내역 |
| 해외주식 기간별 수익 | GET | `/uapi/overseas-stock/v1/trading/inquire-period-profit` | 기간별 손익 |
| 해외주식 외화담보금액 | GET | `/uapi/overseas-stock/v1/trading/foreign-margin` | 외화 담보 정보 |
| 해외주식 결제기준 잔고 | GET | `/uapi/overseas-stock/v1/trading/inquire-paymt-stdr-balance` | 결제일 기준 잔고 |

---

## 주요 요청 파라미터

| 파라미터 | 타입 | 설명 | 예시 |
|---------|------|------|------|
| `CANO` | String | 계좌번호 (앞 8자리) | "12345678" |
| `ACNT_PRDT_CD` | String | 계좌상품코드 | "01" (해외주식) |
| `PDNO` | String | 종목코드 (티커) | "AAPL", "MSFT", "TSLA" |
| `OVRS_EXCG_CD` | String | 거래소코드 | "NASD", "NYSE", "AMEX" |
| `SIDE_RSLS_CLS_CODE` | String | 매수/매도 구분 | "01"(매수), "02"(매도) |
| `ORD_DVSN_CD` | String | 주문유형 | "00"(지정가), "01"(시장가) |
| `ORD_QTY` | Integer | 주문수량 | 10, 50, 100 |
| `ORD_UNPR` | Decimal | 주문가격 (지정가) | "150.50" |
| `INQR_STRT_DT` | String | 조회 시작일 | "20240101" (YYYYMMDD) |
| `INQR_END_DT` | String | 조회 종료일 | "20240131" (YYYYMMDD) |

---

## 지원 거래소

| 코드 | 거래소명 | 국가 | 통화 |
|------|---------|------|------|
| `NASD` | NASDAQ | 미국 | USD |
| `NYSE` | New York Stock Exchange | 미국 | USD |
| `AMEX` | American Stock Exchange | 미국 | USD |
| `SHAA` | Shanghai Stock Exchange | 중국 | CNY |
| `SZHK` | Shenzhen Stock Exchange | 중국 | CNY |
| `HKEX` | Hong Kong Stock Exchange | 홍콩 | HKD |
| `TSEC` | Taiwan Stock Exchange | 대만 | TWD |
| `NSEI` | National Stock Exchange India | 인도 | INR |
| `HASE` | Hanoi Stock Exchange | 베트남 | VND |

---

## 공통 인증 헤더

모든 API 호출 시 필수 헤더:

```http
Authorization: Bearer {accessToken}
appkey: {발급받은 앱키}
appsecret: {발급받은 시크릿키}
tr_id: {거래 ID}
custtype: P
Content-Type: application/json
```

### Transaction ID (tr_id) 규칙

| API 종류 | Production | Simulation |
|---------|-----------|------------|
| 조회 (GET) | `TTTS` + 4자리 | `VTTC` + 4자리 |
| 실행 (POST) | `TTTT` + 4자리 | `VTTT` + 4자리 |

**예시:**
- 현재가 조회: `TTTS1012` (실전), `VTTC1012` (모의)
- 주문 실행: `TTTT1002` (실전), `VTTT1002` (모의)

---

## API 호출 예시

### 1. 현재가 조회 (GET)

```bash
GET /uapi/overseas-price/v1/quotations/price?PDNO=AAPL&OVRS_EXCG_CD=NASD
```

**Request Headers:**
```http
Authorization: Bearer eyJhbGc...
appkey: PSXXXXXXXXXXXXXXxxx
appsecret: xxxxxxxxxxxxxxxxxxx
tr_id: TTTS1012
custtype: P
```

**Response:**
```json
{
  "rt_cd": "0",
  "msg_cd": "정상",
  "msg1": "성공",
  "output": {
    "rsym": "AAPL",
    "zdiv": "NASD",
    "curr": "USD",
    "last": "150.25",
    "sign": "2",
    "rate": "1.25",
    "diff": "1.85",
    "ordy": "150.25",
    "t_xprc": "150.25"
  }
}
```

---

### 2. 주문 실행 (POST) - 매수

```bash
POST /uapi/overseas-stock/v1/trading/order
```

**Request Headers:**
```http
Authorization: Bearer eyJhbGc...
appkey: PSXXXXXXXXXXXXXXxxx
appsecret: xxxxxxxxxxxxxxxxxxx
tr_id: TTTT1002
custtype: P
Content-Type: application/json
```

**Request Body:**
```json
{
  "CANO": "12345678",
  "ACNT_PRDT_CD": "01",
  "PDNO": "AAPL",
  "ORD_DVSN_CD": "00",
  "ORD_QTY": "10",
  "ORD_UNPR": "150.50",
  "OVRS_EXCG_CD": "NASD",
  "SLL_TYPE": "00"
}
```

**Response:**
```json
{
  "rt_cd": "0",
  "msg_cd": "정상",
  "msg1": "주문이 정상적으로 처리되었습니다.",
  "output": {
    "ORD_NO": "202401010001",
    "ORD_TMD": "153045",
    "ORD_DVSN_CD": "00",
    "ORD_QTY": "10",
    "ORD_UNPR": "150.50"
  }
}
```

---

### 3. 잔고 조회 (GET)

```bash
GET /uapi/overseas-stock/v1/trading/inquire-balance?CANO=12345678&ACNT_PRDT_CD=01&OVRS_EXCG_CD=NASD&TR_CRCY_CD=USD
```

**Request Headers:**
```http
Authorization: Bearer eyJhbGc...
appkey: PSXXXXXXXXXXXXXXxxx
appsecret: xxxxxxxxxxxxxxxxxxx
tr_id: TTTS3012
custtype: P
```

**Response:**
```json
{
  "rt_cd": "0",
  "msg_cd": "정상",
  "msg1": "조회 성공",
  "output1": [
    {
      "pdno": "AAPL",
      "prdt_name": "APPLE INC",
      "pchs_avg_pric": "148.25",
      "ovrs_cblc_qty": "50",
      "now_pric2": "150.25",
      "ovrs_stck_evlu_amt": "7512.50",
      "evlu_pfls_amt": "100.00",
      "evlu_pfls_rt": "1.35"
    }
  ],
  "output2": {
    "frcr_pchs_amt1": "7412.50",
    "ovrs_rlzt_pfls_amt": "100.00",
    "ovrs_tot_pfls": "200.00",
    "rlzt_erng_rt": "1.35"
  }
}
```

---

### 4. 주문 취소 (POST)

```bash
POST /uapi/overseas-stock/v1/trading/order-rvsecncl
```

**Request Body:**
```json
{
  "CANO": "12345678",
  "ACNT_PRDT_CD": "01",
  "ORGN_ODNO": "202401010001",
  "RVSE_CNCL_DVSN_CD": "02",
  "ORD_QTY": "0",
  "OVRS_EXCG_CD": "NASD"
}
```

---

## API 분류 통계

| 분류 | 개수 | HTTP Method |
|------|------|-------------|
| **기본시세** | 13 | GET |
| **시세분석** | 15 | GET |
| **주문 실행** | 6 | POST |
| **계좌 조회** | 10 | GET |
| **총계** | **44** | GET: 38, POST: 6 |

---

## 주요 기능 체크리스트

- ✅ 시세 조회 (현재가, 일일, 분별 차트)
- ✅ 시장 분석 (순위, 뉴스, 투자정보)
- ✅ 거래 실행 (매수/매도, 주문 취소)
- ✅ 계좌 관리 (잔고, 주문 내역, 수익)
- ✅ 다중 통화 지원 (USD, HKD, CNY, TWD, INR, VND)
- ✅ 실시간 시세 (WebSocket)
- ✅ 예약 주문 기능
- ✅ 권리/배당 정보 조회

---

## 구현 우선순위 (Quantiq Core)

### Phase 1 - 필수 기능 (MVP)
1. ✅ OAuth 인증 (`/oauth2/tokenP`)
2. 🔄 현재가 조회 (`/quotations/price`)
3. 🔄 잔고 조회 (`/inquire-balance`)
4. 🔄 주문 실행 (`/trading/order`)
5. 🔄 주문 취소 (`/order-rvsecncl`)

### Phase 2 - 핵심 기능
6. 📋 기간별 거래내역 (`/inquire-period-trans`)
7. 📋 매수 가능 금액 조회 (`/inquire-psamount`)
8. 📋 일별 시세 (`/dailyprice`)
9. 📋 호가 조회 (`/inquire-asking-price`)

### Phase 3 - 확장 기능
10. 순위 조회 (시가총액, 거래량 등)
11. 뉴스 조회
12. 예약 주문
13. 실시간 시세 (WebSocket)

---

## 참고 문서

- [KIS Developers 공식 사이트](https://apiportal.koreainvestment.com/)
- OAuth 인증: `/apiservice/oauth2`
- 해외주식 주문/계좌: `/apiservice/apiservice-oversea-stock-order`
- 해외주식 기본시세: `/apiservice/apiservice-oversea-stock-quotations`
- 해외주식 시세분석: `/apiservice/apiservice-oversea-stock-quotations2`
- 해외주식 실시간시세: `/apiservice/apiservice-oversea-stock-streaming`

---

## 주의사항

⚠️ **모의투자 계좌 사용 권장**
- 실전 거래 전 반드시 모의투자 환경에서 테스트
- 모의투자 서버: `https://openapivts.koreainvestment.com:29443`

⚠️ **API 호출 제한**
- 초당 호출 횟수 제한 존재 (계약에 따라 상이)
- Rate Limit 초과 시 429 에러 반환

⚠️ **보안**
- API Key는 환경변수로 관리 (`.env.prod`)
- Git 커밋 시 절대 포함하지 말 것

---

**마지막 업데이트:** 2026-02-01
**문서 버전:** 1.0.0
**분석 출처:** KIS Developers HTML 문서 4개 (기본시세, 시세분석, 실시간시세, 주문 및 계좌)
