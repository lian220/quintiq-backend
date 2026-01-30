# MongoDB 마이그레이션 구현 가이드 (Legacy)

## 개요
이 문서는 구 프로젝트에서 RDB에서 MongoDB로 전환할 때 사용된 가이드입니다. `quantiq` 프로젝트의 데이터 변환 로직(Wide -> Long Format)을 이해하는 데 유용합니다.

## 핵심 내용
- **데이터 저장 방식 변경**: RDB의 Wide Format(날짜별 한 행에 모든 종목)에서 MongoDB의 Long Format(종목별 별도 문서)으로 변경.
- **서비스 레이어 리팩토링**: 비동기 쿼리 기반으로 FastAPI와 호환되도록 변경.
- **개인화 기능**: 사용자별 관심 종목(`user_stocks`) 추가 로직.

## 참고용 쿼리 예시
```python
# 사용자별 관심 종목 주가 조회
user_stocks = await db.user_stocks.find({"user_id": user_id, "is_active": True}).to_list(None)
tickers = [s["ticker"] for s in user_stocks]
prices = await db.stock_prices.find({"ticker": {"$in": tickers}, "date": {"$gte": start_date}}).sort("date", 1).to_list(None)
```
