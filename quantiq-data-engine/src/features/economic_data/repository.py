"""Economic Data Repository - MongoDB 데이터 접근"""
import logging
from typing import Dict, Any, List
from src.core.database import MongoDB

logger = logging.getLogger(__name__)


class EconomicDataRepository:
    """경제 데이터 저장소"""

    def __init__(self):
        self.db = MongoDB.get_db()

    def save_data(self, collection: str, data: Dict[str, Any]) -> bool:
        """데이터를 저장합니다."""
        try:
            if self.db is None:
                logger.error("MongoDB 연결 없음")
                return False

            collection_obj = self.db[collection]

            # upsert 방식으로 저장 (중복 방지)
            filter_query = {"date": data.get("date")}

            if "code" in data:
                filter_query["code"] = data["code"]
            elif "ticker" in data:
                filter_query["ticker"] = data["ticker"]

            collection_obj.update_one(
                filter_query,
                {"$set": data},
                upsert=True
            )

            return True

        except Exception as e:
            logger.error(f"데이터 저장 실패: {e}")
            return False

    def find_active_indicators(self, collection: str) -> List[Dict[str, Any]]:
        """활성화된 지표를 조회합니다."""
        try:
            if self.db is None:
                logger.error("MongoDB 연결 없음")
                return []

            collection_obj = self.db[collection]
            return list(collection_obj.find({"is_active": True}))

        except Exception as e:
            logger.error(f"지표 조회 실패: {e}")
            return []
