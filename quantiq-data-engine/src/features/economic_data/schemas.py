"""Economic Data Pydantic Schemas"""
from pydantic import BaseModel
from typing import Optional


class EconomicDataRequest(BaseModel):
    """경제 데이터 수집 요청"""
    target_date: Optional[str] = None  # YYYY-MM-DD 형식, 미입력 시 당일


class EconomicDataResponse(BaseModel):
    """경제 데이터 수집 응답"""
    success: bool
    message: str
    timestamp: str
    target_date: Optional[str] = None
    fred_collected: Optional[int] = None
    yahoo_collected: Optional[int] = None


class StatusResponse(BaseModel):
    """상태 조회 응답"""
    service: str
    status: str
    timestamp: str
    supported_triggers: list[str]
