"""
ML 패키지 관리 라우터 (Read-Only Status API)
"""
import logging
from fastapi import APIRouter
from datetime import datetime
from pytz import timezone

KST = timezone('Asia/Seoul')

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/ml", tags=["ML Package"])


@router.get("/package-status")
async def get_package_status():
    """
    현재 GCS에 업로드된 패키지 상태 조회 (Read-Only)
    """
    # TODO: GCS에서 최신 버전 정보 조회
    return {
        "service": "ml-package-manager",
        "status": "running",
        "message": "패키지 상태 조회 (구현 예정)",
        "supported_triggers": [
            "Kafka Topic: ml.package.upload.request (구현 예정)"
        ],
        "timestamp": datetime.now(KST).isoformat()
    }
