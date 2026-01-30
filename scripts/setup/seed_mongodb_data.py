#!/usr/bin/env python3
"""
MongoDB 초기 데이터 생성 스크립트 (Adapted for Quantiq)

- stocks 컬렉션에 종목 데이터 삽입
- users 컬렉션에 사용자 생성
- user_stocks 컬렉션에 사용자-종목 매핑 생성
"""
import sys
import os
from pathlib import Path
from datetime import datetime

# 프로젝트 루트 경로 추가
project_root = Path(__file__).parent.parent.parent / "quantiq-data-engine"
sys.path.insert(0, str(project_root))

from src.db import MongoDB
from src.config import settings
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def seed_stocks(db):
    """stocks 컬렉션에 종목 데이터 삽입"""
    logger.info("📦 stocks 컬렉션에 종목 데이터 삽입 중...")
    
    stocks_data = [
        {"stock_name": "애플", "ticker": "AAPL", "is_etf": False, "leverage_ticker": "AAPU", "is_active": True},
        {"stock_name": "마이크로소프트", "ticker": "MSFT", "is_etf": False, "leverage_ticker": "MSFU", "is_active": True},
        {"stock_name": "아마존", "ticker": "AMZN", "is_etf": False, "leverage_ticker": "AMZU", "is_active": True},
        {"stock_name": "구글 A", "ticker": "GOOGL", "is_etf": False, "leverage_ticker": "GGLL", "is_active": True},
        {"stock_name": "엔비디아", "ticker": "NVDA", "is_etf": False, "leverage_ticker": "NVDL", "is_active": True},
        {"stock_name": "테슬라", "ticker": "TSLA", "is_etf": False, "leverage_ticker": "TSLL", "is_active": True},
        {"stock_name": "S&P 500 ETF", "ticker": "SPY", "is_etf": True, "leverage_ticker": "UPRO", "is_active": True},
        {"stock_name": "QQQ ETF", "ticker": "QQQ", "is_etf": True, "leverage_ticker": "TQQQ", "is_active": True},
    ]
    
    for stock in stocks_data:
        stock["created_at"] = datetime.utcnow()
        stock["updated_at"] = datetime.utcnow()
        db.stocks.update_one(
            {"ticker": stock["ticker"]},
            {"$set": stock},
            upsert=True
        )
        logger.info(f"✓ {stock['stock_name']} ({stock['ticker']})")

def seed_user(db, user_id="lian"):
    """users 컬렉션에 사용자 생성"""
    logger.info(f"👤 사용자 '{user_id}' 생성 중...")
    user_doc = {
        "user_id": user_id,
        "email": "lian.dy220@gmail.com",
        "display_name": "Lian",
        "preferences": {
            "default_currency": "USD",
            "notification_enabled": True
        },
        "created_at": datetime.utcnow(),
        "updated_at": datetime.utcnow()
    }
    db.users.update_one({"user_id": user_id}, {"$set": user_doc}, upsert=True)
    return user_id

def main():
    db = MongoDB.get_db()
    if db is None:
        logger.error("MongoDB connection failed.")
        return
    
    seed_stocks(db)
    seed_user(db)
    logger.info("\n✅ 초기 데이터 생성 완료!")

if __name__ == "__main__":
    main()
