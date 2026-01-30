"""
MongoDB 스키마 및 인덱스 생성 스크립트 (Adapted for Quantiq)

이 스크립트는 MongoDB에 필요한 collections과 인덱스를 생성합니다.
quantiq-data-engine의 환경을 사용합니다.
"""
import sys
import os
from pathlib import Path

# 프로젝트 루트 경로 추가 (quantiq-data-engine)
project_root = Path(__file__).parent.parent.parent / "quantiq-data-engine"
sys.path.insert(0, str(project_root))

from src.db import MongoDB
from src.config import settings
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def create_indexes():
    """모든 collections에 인덱스 생성"""
    try:
        db = MongoDB.get_db()
        if db is None:
            logger.error("MongoDB connection failed.")
            return

        logger.info("MongoDB 인덱스 생성 시작...")
        
        # 1. stocks collection
        logger.info("stocks collection 인덱스 생성 중...")
        db.stocks.create_index([("ticker", 1)], unique=True, name="ticker_unique")
        db.stocks.create_index([("stock_name", 1)], unique=True, name="stock_name_unique")
        db.stocks.create_index([("is_active", 1)], name="is_active_idx")
        logger.info("✓ stocks 인덱스 생성 완료")
        
        # 2. users collection
        logger.info("users collection 인덱스 생성 중...")
        db.users.create_index([("user_id", 1)], unique=True, name="user_id_unique")
        logger.info("✓ users 인덱스 생성 완료")
        
        # 3. user_stocks collection
        logger.info("user_stocks collection 인덱스 생성 중...")
        db.user_stocks.create_index(
            [("user_id", 1), ("stock_id", 1)], 
            unique=True, 
            name="user_stock_unique"
        )
        db.user_stocks.create_index([("user_id", 1), ("is_active", 1)], name="user_active_stocks_idx")
        db.user_stocks.create_index([("ticker", 1)], name="ticker_idx")
        logger.info("✓ user_stocks 인덱스 생성 완료")
        
        # 4. economic_data collection
        logger.info("economic_data collection 인덱스 생성 중...")
        db.economic_data.create_index([("date", 1)], unique=True, name="date_unique")
        logger.info("✓ economic_data 인덱스 생성 완료")
        
        # 5. daily_stock_data collection
        logger.info("daily_stock_data collection 인덱스 생성 중...")
        db.daily_stock_data.create_index([("date", 1)], unique=True, name="date_unique")
        db.daily_stock_data.create_index([("recommendations", 1)], name="recommendations_exists_idx", sparse=True)
        db.daily_stock_data.create_index([("date", 1), ("recommendations", 1)], name="date_recommendations_idx")
        logger.info("✓ daily_stock_data 인덱스 생성 완료")
        
        # 12. trading_configs collection
        logger.info("trading_configs collection 인덱스 생성 중...")
        db.trading_configs.create_index([("user_id", 1)], unique=True, name="user_id_unique")
        logger.info("✓ trading_configs 인덱스 생성 완료")
        
        # 13. trading_logs collection
        logger.info("trading_logs collection 인덱스 생성 중...")
        db.trading_logs.create_index([("user_id", 1), ("created_at", -1)], name="user_created_idx")
        db.trading_logs.create_index([("ticker", 1), ("created_at", -1)], name="ticker_created_idx")
        logger.info("✓ trading_logs 인덱스 생성 완료")
        
        logger.info("\n✅ 모든 인덱스 생성 완료!")
        
    except Exception as e:
        logger.error(f"인덱스 생성 중 오류 발생: {e}")
        raise

if __name__ == "__main__":
    create_indexes()
