# 자동매수 흐름 종합 가이드 (Legacy)

## 트레이딩 전략 핵심 요약
구 프로젝트에서 정의된 자동 트레이딩의 판단 및 실행 흐름입니다. `quantiq-core`의 `AutoTradingService` 구현의 논리적 배경입니다.

## 매수 판단 프로세스
1. **기술적 분석**: SMA, Golden Cross, RSI, MACD 지표를 기반으로 기술적 점수 계산.
2. **감정 분석**: 뉴스 기사 감정 점수 반영.
3. **AI 예측**: Vertex AI/Colab 모델의 향후 가격 예측 반영.
4. **종합 점수**: 위 요소들을 가중치 합산하여 최종 `recommendation_score` 산출.

## 실행 조건
- `recommendation_score` > `min_composite_score` (사용자 설정값)
- 현재 계좌 잔고 및 종목당 최대 매수 금액 제한 확인.
- 시장가 또는 지정가 주문 실행.
