# Event-Driven Architecture Schema

## 이벤트 토픽 명명 규칙

### 표준 형식
```
quantiq.<domain>.<event-type>
```

### 도메인별 토픽

#### Stock (주식 데이터)
```
quantiq.stock.price.updated       # 실시간 주가 업데이트
quantiq.stock.data.refreshed      # 주식 데이터 갱신 완료
quantiq.stock.data.sync.requested # 주식 데이터 동기화 요청
```

#### Trading (거래)
```
quantiq.trading.order.created     # 주문 생성
quantiq.trading.order.executed    # 주문 체결
quantiq.trading.order.cancelled   # 주문 취소
quantiq.trading.signal.detected   # 매매 신호 감지
quantiq.trading.balance.updated   # 계좌 잔고 업데이트
```

#### Analysis (분석)
```
quantiq.analysis.recommendation.generated  # 추천 생성 완료
quantiq.analysis.prediction.completed      # 예측 완료
quantiq.analysis.request                   # 분석 요청
quantiq.analysis.completed                 # 분석 완료
```

#### Economic (경제 데이터)
```
quantiq.economic.data.updated             # 경제 데이터 업데이트 완료
quantiq.economic.data.sync.requested      # 경제 데이터 동기화 요청
quantiq.economic.data.sync.failed         # 경제 데이터 동기화 실패
```

## Event Schema 표준

### 공통 Event 구조

```json
{
  "eventId": "uuid-v4",
  "eventType": "quantiq.stock.price.updated",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-core",
  "payload": {
    // 도메인별 데이터
  }
}
```

### 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| eventId | String (UUID) | ✅ | 이벤트 고유 식별자 |
| eventType | String | ✅ | 이벤트 타입 (토픽명과 동일) |
| version | String | ✅ | 이벤트 스키마 버전 |
| timestamp | String (ISO 8601) | ✅ | 이벤트 발생 시각 (KST) |
| source | String | ✅ | 이벤트 발행 서비스 |
| payload | Object | ✅ | 도메인별 이벤트 데이터 |

## 도메인별 Payload Schema

### Stock Events

#### quantiq.stock.price.updated
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "quantiq.stock.price.updated",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-data-engine",
  "payload": {
    "symbol": "AAPL",
    "price": 175.50,
    "change": 2.30,
    "changePercent": 1.33,
    "volume": 1234567,
    "marketCap": 2800000000000
  }
}
```

#### quantiq.stock.data.sync.requested
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "eventType": "quantiq.stock.data.sync.requested",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-core",
  "payload": {
    "requestId": "req-1234567890",
    "symbols": ["AAPL", "GOOGL", "MSFT"],
    "syncType": "full|incremental",
    "priority": "high|normal|low"
  }
}
```

### Trading Events

#### quantiq.trading.order.created
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440002",
  "eventType": "quantiq.trading.order.created",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-core",
  "payload": {
    "orderId": "ORD-20260131-001",
    "userId": "user-123",
    "symbol": "AAPL",
    "orderType": "market|limit",
    "side": "buy|sell",
    "quantity": 100,
    "price": 175.50,
    "status": "pending"
  }
}
```

#### quantiq.trading.order.executed
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440003",
  "eventType": "quantiq.trading.order.executed",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:05+09:00",
  "source": "quantiq-core",
  "payload": {
    "orderId": "ORD-20260131-001",
    "executedPrice": 175.48,
    "executedQuantity": 100,
    "commission": 0.50,
    "totalAmount": 17548.50
  }
}
```

#### quantiq.trading.signal.detected
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440004",
  "eventType": "quantiq.trading.signal.detected",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-data-engine",
  "payload": {
    "symbol": "AAPL",
    "signalType": "buy|sell",
    "confidence": 0.85,
    "indicators": {
      "rsi": 35.5,
      "macd": "bullish",
      "movingAverage": "golden_cross"
    },
    "recommendedAction": "buy",
    "recommendedQuantity": 100
  }
}
```

### Analysis Events

#### quantiq.analysis.recommendation.generated
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440005",
  "eventType": "quantiq.analysis.recommendation.generated",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-data-engine",
  "payload": {
    "symbol": "AAPL",
    "recommendation": "buy|hold|sell",
    "targetPrice": 185.00,
    "stopLoss": 170.00,
    "confidence": 0.78,
    "timeframe": "short|medium|long",
    "reasoning": "Strong fundamentals and technical indicators"
  }
}
```

#### quantiq.analysis.request
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440006",
  "eventType": "quantiq.analysis.request",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-core",
  "payload": {
    "requestId": "req-analysis-1234",
    "analysisType": "technical|fundamental|sentiment",
    "symbols": ["AAPL", "GOOGL"],
    "parameters": {
      "timeframe": "1d",
      "indicators": ["RSI", "MACD", "MA"]
    }
  }
}
```

### Economic Events

#### quantiq.economic.data.updated
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440007",
  "eventType": "quantiq.economic.data.updated",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-data-engine",
  "payload": {
    "requestId": "req-economic-1234",
    "dataTypes": ["gdp", "unemployment", "inflation"],
    "recordsUpdated": 150,
    "duration": 12.5,
    "status": "success"
  }
}
```

#### quantiq.economic.data.sync.requested
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440008",
  "eventType": "quantiq.economic.data.sync.requested",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-core",
  "payload": {
    "requestId": "req-economic-1234",
    "dataTypes": ["gdp", "unemployment", "inflation"],
    "source": "scheduled|manual",
    "priority": "normal"
  }
}
```

## Error Event Schema

에러 이벤트는 `.failed` 접미사를 사용합니다.

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440009",
  "eventType": "quantiq.economic.data.sync.failed",
  "version": "1.0",
  "timestamp": "2026-01-31T10:30:00+09:00",
  "source": "quantiq-data-engine",
  "payload": {
    "requestId": "req-economic-1234",
    "errorCode": "API_RATE_LIMIT",
    "errorMessage": "FRED API rate limit exceeded",
    "retryable": true,
    "retryAfter": 60
  }
}
```

## Event Flow Patterns

### 1. Request-Response Pattern
```
quantiq-core → quantiq.economic.data.sync.requested → quantiq-data-engine
quantiq-data-engine → quantiq.economic.data.updated → quantiq-core
```

### 2. Fire-and-Forget Pattern
```
quantiq-core → quantiq.trading.order.created → (no response)
```

### 3. Event Chain Pattern
```
quantiq.stock.price.updated
  → quantiq.analysis.request
  → quantiq.analysis.recommendation.generated
  → quantiq.trading.signal.detected
  → quantiq.trading.order.created
```

## 구현 가이드

### Kotlin (quantiq-core)

```kotlin
// Event Schema
data class BaseEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val version: String = "1.0",
    val timestamp: String = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toString(),
    val source: String = "quantiq-core",
    val payload: Any
)

// Event Publisher
@Service
class EventPublisher(private val kafkaTemplate: KafkaTemplate<String, String>) {
    fun publish(topic: String, event: BaseEvent) {
        kafkaTemplate.send(topic, objectMapper.writeValueAsString(event))
    }
}
```

### Python (quantiq-data-engine)

```python
# Event Schema
@dataclass
class BaseEvent:
    eventId: str = field(default_factory=lambda: str(uuid.uuid4()))
    eventType: str
    version: str = "1.0"
    timestamp: str = field(default_factory=lambda: datetime.now(KST).isoformat())
    source: str = "quantiq-data-engine"
    payload: dict

# Event Publisher
class EventPublisher:
    @staticmethod
    def publish(topic: str, event: BaseEvent):
        producer.produce(topic, json.dumps(asdict(event)))
```

## 마이그레이션 계획

### Phase 1: 기존 토픽 유지 + 새 토픽 추가
- 기존: `economic.data.update.request`
- 신규: `quantiq.economic.data.sync.requested`
- 양쪽 모두 발행하여 호환성 유지

### Phase 2: 새 토픽으로 전환
- 모든 코드가 새 토픽을 사용하도록 수정
- 기존 토픽은 deprecated 표시

### Phase 3: 기존 토픽 제거
- 기존 토픽 구독 및 발행 코드 제거
- Kafka에서 토픽 삭제

---

**작성일**: 2026-01-31
**버전**: 1.0
**관리**: Quantiq Development Team
