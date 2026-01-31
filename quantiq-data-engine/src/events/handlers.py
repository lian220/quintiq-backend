"""
Event Handlers for Quantiq Data Engine

도메인별 이벤트를 처리하는 핸들러 클래스들입니다.
"""

import logging
from typing import Dict, Any
import time as time_module
from datetime import datetime
from pytz import timezone

from src.events.schema import (
    BaseEvent,
    EventTopics,
    EconomicDataSyncRequestedPayload,
    EconomicDataUpdatedPayload,
    EconomicDataSyncFailedPayload,
    StockDataSyncRequestedPayload,
    StockDataRefreshedPayload,
    AnalysisRequestPayload,
    AnalysisCompletedPayload,
    create_event
)
from src.events.publisher import EventPublisher
from src.services.data_collector import collect_economic_data
from src.services.slack_notifier import SlackNotifier

logger = logging.getLogger(__name__)
KST = timezone('Asia/Seoul')


# ============================================================================
# Base Event Handler
# ============================================================================

class BaseEventHandler:
    """모든 Event Handler의 기본 클래스"""

    def __init__(self):
        self.event_publisher = EventPublisher()

    def handle(self, event: BaseEvent):
        """
        이벤트를 처리합니다.

        Args:
            event: 처리할 이벤트
        """
        raise NotImplementedError("Subclasses must implement handle()")


# ============================================================================
# Economic Event Handler
# ============================================================================

class EconomicEventHandler(BaseEventHandler):
    """경제 데이터 관련 이벤트를 처리합니다"""

    def handle_data_sync_requested(self, event: BaseEvent):
        """
        경제 데이터 동기화 요청 이벤트 처리

        Event: quantiq.economic.data.sync.requested
        """
        payload = event.payload
        request_id = payload.get("requestId", "unknown")
        data_types = payload.get("dataTypes", [])
        source = payload.get("source", "kafka")
        priority = payload.get("priority", "normal")
        thread_ts = payload.get("threadTs")  # Kotlin에서 전달받은 스레드 타임스탬프

        logger.info("=" * 80)
        logger.info(f"📥 경제 데이터 동기화 요청 수신")
        logger.info(f"Request ID: {request_id}")
        logger.info(f"Thread TS: {thread_ts}")
        logger.info(f"Data Types: {data_types}")
        logger.info(f"Source: {source}")
        logger.info(f"Priority: {priority}")
        logger.info("=" * 80)

        # 🔔 수집 시작 알림 (스레드 답글)
        SlackNotifier.notify_economic_data_collection_start(request_id, source, thread_ts)

        start_time = time_module.time()

        try:
            # 경제 데이터 수집 실행
            collect_economic_data()
            elapsed_time = time_module.time() - start_time

            logger.info("✅ 경제 데이터 수집 완료")

            # 🔔 수집 완료 알림 (스레드 답글)
            SlackNotifier.notify_economic_data_collection_success(
                request_id,
                {"duration": f"{elapsed_time:.2f}초"},
                thread_ts
            )

            # 완료 이벤트 발행
            success_payload = EconomicDataUpdatedPayload(
                requestId=request_id,
                dataTypes=data_types or ["gdp", "unemployment", "inflation"],
                recordsUpdated=0,  # TODO: 실제 업데이트된 레코드 수 반환
                duration=elapsed_time,
                status="success"
            )

            self.event_publisher.publish(
                EventTopics.ECONOMIC_DATA_UPDATED,
                create_event(EventTopics.ECONOMIC_DATA_UPDATED, success_payload)
            )

        except Exception as e:
            logger.error(f"❌ 경제 데이터 수집 실패: {e}")

            # 🔔 오류 알림 (스레드 답글)
            SlackNotifier.notify_economic_data_collection_error(request_id, str(e), thread_ts)

            # 실패 이벤트 발행
            error_payload = EconomicDataSyncFailedPayload(
                requestId=request_id,
                errorCode="COLLECTION_ERROR",
                errorMessage=str(e),
                retryable=True,
                retryAfter=60
            )

            self.event_publisher.publish(
                EventTopics.ECONOMIC_DATA_SYNC_FAILED,
                create_event(EventTopics.ECONOMIC_DATA_SYNC_FAILED, error_payload)
            )


# ============================================================================
# Stock Event Handler
# ============================================================================

class StockEventHandler(BaseEventHandler):
    """주식 데이터 관련 이벤트를 처리합니다"""

    def handle_data_sync_requested(self, event: BaseEvent):
        """
        주식 데이터 동기화 요청 이벤트 처리

        Event: quantiq.stock.data.sync.requested
        """
        payload = event.payload
        request_id = payload.get("requestId", "unknown")
        symbols = payload.get("symbols", [])
        sync_type = payload.get("syncType", "full")

        logger.info(f"📥 주식 데이터 동기화 요청: {request_id}")
        logger.info(f"Symbols: {symbols}, Sync Type: {sync_type}")

        # TODO: 주식 데이터 수집 로직 구현
        # 현재는 placeholder

        try:
            start_time = time_module.time()

            # 데이터 수집 로직 (placeholder)
            logger.info(f"🔄 주식 데이터 수집 중: {symbols}")
            time_module.sleep(1)  # Simulating work

            elapsed_time = time_module.time() - start_time

            # 완료 이벤트 발행
            success_payload = StockDataRefreshedPayload(
                requestId=request_id,
                symbols=symbols,
                recordsUpdated=len(symbols),
                duration=elapsed_time,
                status="success"
            )

            self.event_publisher.publish(
                EventTopics.STOCK_DATA_REFRESHED,
                create_event(EventTopics.STOCK_DATA_REFRESHED, success_payload)
            )

            logger.info(f"✅ 주식 데이터 동기화 완료: {request_id}")

        except Exception as e:
            logger.error(f"❌ 주식 데이터 동기화 실패: {e}")


# ============================================================================
# Analysis Event Handler
# ============================================================================

class AnalysisEventHandler(BaseEventHandler):
    """분석 관련 이벤트를 처리합니다"""

    def handle_analysis_request(self, event: BaseEvent):
        """
        분석 요청 이벤트 처리

        Event: quantiq.analysis.request
        """
        payload = event.payload
        request_id = payload.get("requestId", "unknown")
        analysis_type = payload.get("analysisType", "technical")
        symbols = payload.get("symbols", [])

        logger.info(f"📥 분석 요청 수신: {request_id}")
        logger.info(f"Analysis Type: {analysis_type}, Symbols: {symbols}")

        # TODO: 분석 로직 구현
        # 현재는 placeholder

        try:
            start_time = time_module.time()

            # 분석 로직 (placeholder)
            logger.info(f"🔄 분석 실행 중: {analysis_type}")
            time_module.sleep(2)  # Simulating work

            elapsed_time = time_module.time() - start_time

            # 완료 이벤트 발행
            success_payload = AnalysisCompletedPayload(
                requestId=request_id,
                analysisType=analysis_type,
                symbols=symbols,
                recordsProcessed=len(symbols),
                duration=elapsed_time,
                status="success"
            )

            self.event_publisher.publish(
                EventTopics.ANALYSIS_COMPLETED,
                create_event(EventTopics.ANALYSIS_COMPLETED, success_payload)
            )

            logger.info(f"✅ 분석 완료: {request_id}")

        except Exception as e:
            logger.error(f"❌ 분석 실패: {e}")


# ============================================================================
# Event Router
# ============================================================================

class EventRouter:
    """
    이벤트 타입에 따라 적절한 핸들러로 라우팅합니다
    """

    def __init__(self):
        self.economic_handler = EconomicEventHandler()
        self.stock_handler = StockEventHandler()
        self.analysis_handler = AnalysisEventHandler()

        # 이벤트 타입별 핸들러 매핑
        self.handlers = {
            EventTopics.ECONOMIC_DATA_SYNC_REQUESTED: self.economic_handler.handle_data_sync_requested,
            EventTopics.STOCK_DATA_SYNC_REQUESTED: self.stock_handler.handle_data_sync_requested,
            EventTopics.ANALYSIS_REQUEST: self.analysis_handler.handle_analysis_request,

            # Legacy support
            EventTopics.LEGACY_ECONOMIC_DATA_UPDATE_REQUEST: self.economic_handler.handle_data_sync_requested,
        }

    def route(self, event: BaseEvent):
        """
        이벤트를 적절한 핸들러로 라우팅합니다

        Args:
            event: 라우팅할 이벤트
        """
        event_type = event.eventType
        handler = self.handlers.get(event_type)

        if handler:
            logger.info(f"🎯 라우팅: {event_type} → {handler.__name__}")
            handler(event)
        else:
            logger.warning(f"⚠️ 핸들러 없음: {event_type}")
