"""Economic Data Router - FastAPI 엔드포인트"""
import logging
import time as time_module
from datetime import datetime
from fastapi import APIRouter, Depends
from pytz import timezone

from .service import EconomicDataService
from .schemas import EconomicDataRequest, EconomicDataResponse, StatusResponse
from src.services.slack_notifier import SlackNotifier

logger = logging.getLogger(__name__)
KST = timezone('Asia/Seoul')

router = APIRouter(prefix="/api/economic", tags=["economic"])


def get_service():
    """Dependency Injection for Service"""
    return EconomicDataService()


@router.post("/collect", response_model=EconomicDataResponse)
def collect_economic_data_endpoint(
    request: EconomicDataRequest = EconomicDataRequest(),
    service: EconomicDataService = Depends(get_service)
):
    """
    경제 데이터 수집 REST API 엔드포인트

    Args:
        target_date: 수집할 기준 날짜 (YYYY-MM-DD). 미입력 시 당일 기준으로 조회
    """
    request_id = f"rest-{int(time_module.time())}"
    target_date = request.target_date

    try:
        logger.info("=" * 80)
        logger.info(f"경제 데이터 수집 REST API 요청 받음 (기준일: {target_date or '당일'})")
        logger.info("=" * 80)

        # 🔔 수집 시작 알림
        SlackNotifier.notify_economic_data_collection_start(request_id, "rest_api")

        start_time = time_module.time()
        result = service.collect_economic_data(target_date=target_date)
        elapsed_time = time_module.time() - start_time

        logger.info("✅ 경제 데이터 수집 완료")
        logger.info("=" * 80)

        # 🔔 수집 완료 알림
        SlackNotifier.notify_economic_data_collection_success(
            request_id,
            {"duration": f"{elapsed_time:.2f}초", "target_date": result.get("target_date")}
        )

        return EconomicDataResponse(
            success=True,
            message="경제 데이터 수집 완료",
            timestamp=datetime.now(KST).isoformat(),
            target_date=result.get("target_date"),
            fred_collected=result.get("fred_collected"),
            yahoo_collected=result.get("yahoo_collected")
        )

    except Exception as e:
        logger.error(f"❌ 경제 데이터 수집 실패: {e}")

        # 🔔 오류 알림
        SlackNotifier.notify_economic_data_collection_error(request_id, str(e))

        return EconomicDataResponse(
            success=False,
            message=f"경제 데이터 수집 실패: {str(e)}",
            timestamp=datetime.now(KST).isoformat()
        )


@router.get("/status", response_model=StatusResponse)
def get_economic_data_status():
    """경제 데이터 수집 상태 조회"""
    return StatusResponse(
        service="economic-data-collector",
        status="running",
        timestamp=datetime.now(KST).isoformat(),
        supported_triggers=[
            "Kafka Topic: economic.data.update.request",
            "REST API: POST /api/economic/collect"
        ]
    )
