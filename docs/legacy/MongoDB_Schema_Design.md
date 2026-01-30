# MongoDB 스키마 설계 문서 (Legacy)

## 개요
이 문서는 구 프로젝트(`stock-trading`)에서 정의된 MongoDB 스키마 설계입니다. `quantiq` 프로젝트의 초기 데이터 구조 및 인덱스 설계의 기반이 되었습니다.

## 핵심 설계 원칙
1. **종목 정보와 사용자 설정 분리**
   - `stocks`: 종목 기본 정보 (모든 사용자 공통)
   - `user_stocks`: 사용자별 관심 종목 및 설정 (개인화)

2. **레버리지 사용 여부는 사용자별 설정**
   - 종목 자체에는 `leverage_ticker`만 저장
   - 실제 레버리지 사용 여부는 각 사용자가 `user_stocks`에서 설정

## 주요 컬렉션
- `stocks`: 종목 기본 정보
- `user_stocks`: 사용자별 관심 종목
- `daily_stock_data`: 날짜별 통합 조회용 (주가, 거래량, 추천 등)
- `stock_recommendations`: 종목별 시계열 추천 이력
- `trading_configs`: 거래 설정 (RDB 이전 예정)
- `trading_logs`: 거래 이력
