# Event-Driven Architecture 사용 가이드

## 개요

Quantiq 플랫폼은 Event-Driven Architecture (EDA)를 채택하여 서비스 간 느슨한 결합과 확장성을 제공합니다.

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                     Event-Driven Architecture                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐         Kafka Topics          ┌─────────┐ │
│  │  Quantiq Core    │◄───────────────────────────────►│  Data   │ │
│  │   (Kotlin)       │                                │ Engine  │ │
│  │                  │   quantiq.economic.data.*      │(Python) │ │
│  │  Event Publishers│   quantiq.stock.data.*         │Event    │ │
│  │  Event Listeners │   quantiq.trading.*            │Handlers │ │
│  └────────┬─────────┘   quantiq.analysis.*          └────┬────┘ │
│           │                                                │      │
│           └────────────────┬───────────────────────────────┘      │
│                            │                                      │
│                   ┌────────▼────────┐                            │
│                   │  Kafka Cluster   │                            │
│                   │  (Event Bus)     │                            │
│                   │                  │                            │
│                   │  - Persistence   │                            │
│                   │  - Scalability   │                            │
│                   │  - Reliability   │                            │
│                   └──────────────────┘                            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## 핵심 이벤트 도메인

### 1. Economic (경제 데이터)
```yaml
quantiq.economic.data.sync.requested:
  Publisher: quantiq-core (Scheduler)
  Consumer: quantiq-data-engine (EconomicEventHandler)
  Flow: 스케줄러 → 데이터 수집 요청

quantiq.economic.data.updated:
  Publisher: quantiq-data-engine
  Consumer: quantiq-core (KafkaMessageListener)
  Flow: 데이터 수집 완료 → 후속 처리

quantiq.economic.data.sync.failed:
  Publisher: quantiq-data-engine
  Consumer: quantiq-core (KafkaMessageListener)
  Flow: 오류 발생 → 재시도 로직
```

### 2. Stock (주식 데이터)
```yaml
quantiq.stock.data.sync.requested:
  Publisher: quantiq-core
  Consumer: quantiq-data-engine (StockEventHandler)

quantiq.stock.data.refreshed:
  Publisher: quantiq-data-engine
  Consumer: quantiq-core

quantiq.stock.price.updated:
  Publisher: quantiq-data-engine
  Consumer: quantiq-core
```

### 3. Trading (거래)
```yaml
quantiq.trading.order.created:
  Publisher: quantiq-core
  Consumer: quantiq-data-engine (분석 기록)

quantiq.trading.signal.detected:
  Publisher: quantiq-data-engine
  Consumer: quantiq-core (자동 매매 로직)
```

### 4. Analysis (분석)
```yaml
quantiq.analysis.request:
  Publisher: quantiq-core
  Consumer: quantiq-data-engine (AnalysisEventHandler)

quantiq.analysis.completed:
  Publisher: quantiq-data-engine
  Consumer: quantiq-core (자동 매매 트리거)
```

## 사용 예제

### Kotlin (quantiq-core)에서 이벤트 발행

#### 1. 경제 데이터 동기화 요청

```kotlin
@Service
class MyService(
    private val economicEventPublisher: EconomicEventPublisher
) {
    fun requestEconomicDataSync() {
        val payload = EconomicDataSyncRequestedPayload(
            requestId = "req-${UUID.randomUUID()}",
            dataTypes = listOf("gdp", "unemployment"),
            source = "manual",
            priority = "high"
        )

        economicEventPublisher.publishDataSyncRequested(payload)
    }
}
```

#### 2. 주문 생성 이벤트 발행

```kotlin
@Service
class TradingService(
    private val tradingEventPublisher: TradingEventPublisher
) {
    fun createOrder(userId: String, symbol: String, quantity: Int) {
        val payload = TradingOrderCreatedPayload(
            orderId = "ORD-${UUID.randomUUID()}",
            userId = userId,
            symbol = symbol,
            orderType = "market",
            side = "buy",
            quantity = quantity,
            price = null,
            status = "pending"
        )

        tradingEventPublisher.publishOrderCreated(payload)
    }
}
```

#### 3. 이벤트 수신 (Consumer)

```kotlin
@Service
class KafkaMessageListener(
    private val objectMapper: ObjectMapper
) {
    @KafkaListener(topics = [EventTopics.ECONOMIC_DATA_UPDATED])
    fun listenEconomicDataUpdated(message: String) {
        val event = objectMapper.readTree(message)
        val payload = event.get("payload")

        // 처리 로직
        logger.info("경제 데이터 업데이트 완료: ${payload}")
    }
}
```

### Python (quantiq-data-engine)에서 이벤트 처리

#### 1. 이벤트 수신 및 처리

```python
# src/events/handlers.py

class EconomicEventHandler(BaseEventHandler):
    def handle_data_sync_requested(self, event: BaseEvent):
        payload = event.payload
        request_id = payload.get("requestId")

        # 데이터 수집 로직
        collect_economic_data()

        # 완료 이벤트 발행
        success_payload = EconomicDataUpdatedPayload(
            requestId=request_id,
            dataTypes=["gdp", "unemployment"],
            recordsUpdated=150,
            duration=12.5,
            status="success"
        )

        self.event_publisher.publish(
            EventTopics.ECONOMIC_DATA_UPDATED,
            create_event(EventTopics.ECONOMIC_DATA_UPDATED, success_payload)
        )
```

#### 2. 이벤트 발행

```python
from src.events.schema import create_event, EventTopics
from src.events.publisher import EventPublisher
from src.events.schema import TradingSignalDetectedPayload

# 매매 신호 이벤트 발행
payload = TradingSignalDetectedPayload(
    symbol="AAPL",
    signalType="buy",
    confidence=0.85,
    indicators={"rsi": 35.5, "macd": "bullish"},
    recommendedAction="buy",
    recommendedQuantity=100
)

event = create_event(EventTopics.TRADING_SIGNAL_DETECTED, payload)
EventPublisher.publish(EventTopics.TRADING_SIGNAL_DETECTED, event)
```

## 테스트 방법

### 1. 서비스 시작

```bash
# Docker Compose로 전체 서비스 시작
./start.sh

# 로그 확인
docker logs -f quantiq-core
docker logs -f quantiq-data-engine
```

### 2. Kafka 토픽 확인

```bash
# Kafka UI 접속
open http://localhost:8089

# 또는 CLI로 토픽 확인
docker exec -it quantiq-kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 3. 이벤트 발행 테스트

#### 경제 데이터 업데이트 수동 트리거

```bash
# Quantiq Core API를 통한 트리거
curl -X POST http://localhost:10010/api/economic/trigger-update

# 또는 Data Engine API를 통한 트리거 (Legacy)
curl -X POST http://localhost:10020/api/economic/collect
```

#### Kafka UI에서 확인
1. http://localhost:8089 접속
2. Topics 메뉴 선택
3. `quantiq.economic.data.sync.requested` 토픽 선택
4. Messages 탭에서 이벤트 확인

### 4. 로그 모니터링

#### Quantiq Core 로그
```bash
docker logs -f quantiq-core | grep "📤\|📥\|✅\|❌"
```

예상 출력:
```
📤 Publishing event to topic [quantiq.economic.data.sync.requested]
✅ Event published successfully
📥 경제 데이터 업데이트 완료 이벤트 수신
✅ 경제 데이터 업데이트 완료
```

#### Quantiq Data Engine 로그
```bash
docker logs -f quantiq-data-engine | grep "📥\|📤\|✅\|❌"
```

예상 출력:
```
📥 Kafka 메시지 수신
🎯 라우팅: quantiq.economic.data.sync.requested → handle_data_sync_requested
✅ 경제 데이터 수집 완료
📤 Publishing event to topic [quantiq.economic.data.updated]
```

## Event Flow 예제

### 경제 데이터 수집 Flow

```
1. Quartz Scheduler (06:05 KST)
   └─> EconomicDataSchedulerService.triggerEconomicDataUpdate()
       └─> EconomicEventPublisher.publishDataSyncRequested()
           └─> Kafka: quantiq.economic.data.sync.requested

2. Quantiq Data Engine
   └─> Consumer: main.py
       └─> EventRouter.route()
           └─> EconomicEventHandler.handle_data_sync_requested()
               └─> collect_economic_data()
                   └─> EventPublisher.publish()
                       └─> Kafka: quantiq.economic.data.updated

3. Quantiq Core
   └─> KafkaMessageListener.listenEconomicDataUpdated()
       └─> 로깅 및 후속 처리
```

### 매매 신호 → 자동 매매 Flow

```
1. Data Engine (분석 완료)
   └─> TradingEventHandler
       └─> EventPublisher.publish()
           └─> Kafka: quantiq.trading.signal.detected

2. Quantiq Core
   └─> KafkaMessageListener.listenTradingSignalDetected()
       └─> AutoTradingService.processSignal()
           └─> TradingEventPublisher.publishOrderCreated()
               └─> Kafka: quantiq.trading.order.created

3. Data Engine (기록)
   └─> TradingEventHandler.handle_order_created()
       └─> MongoDB에 거래 기록 저장
```

## 트러블슈팅

### Kafka 연결 실패

```bash
# Kafka 상태 확인
docker ps | grep kafka

# Kafka 로그 확인
docker logs quantiq-kafka

# Kafka 재시작
docker restart quantiq-kafka
```

### 이벤트가 전달되지 않음

1. **토픽 존재 확인**
   ```bash
   docker exec -it quantiq-kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

2. **Consumer Group 확인**
   ```bash
   docker exec -it quantiq-kafka kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --list
   ```

3. **메시지 소비 확인**
   ```bash
   docker exec -it quantiq-kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic quantiq.economic.data.sync.requested \
     --from-beginning
   ```

### 이벤트 형식 오류

- Event Schema 문서 참조: `docs/architecture/EVENT_SCHEMA.md`
- BaseEvent 구조 확인
- JSON 직렬화 오류 로그 확인

## 성능 모니터링

### Kafka 메트릭

Kafka UI (http://localhost:8089)에서 확인:
- Producer/Consumer 지연 시간
- 메시지 처리량 (Throughput)
- Consumer Lag

### 애플리케이션 메트릭

로그에서 확인:
- 이벤트 발행/수신 성공률
- 처리 시간 (duration)
- 오류율

## 다음 단계

1. **Circuit Breaker 추가**
   - Resilience4j 적용
   - 장애 격리 강화

2. **API Gateway 도입**
   - Kong 또는 Spring Cloud Gateway
   - Rate Limiting, Auth 적용

3. **Event Sourcing 확장**
   - 이벤트 히스토리 저장
   - 상태 재구성 기능

4. **Saga Pattern 구현**
   - 분산 트랜잭션 관리
   - 보상 트랜잭션 (Compensation)

---

**작성일**: 2026-01-31
**버전**: 1.0
**관리**: Quantiq Development Team
