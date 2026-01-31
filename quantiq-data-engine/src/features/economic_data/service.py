"""Economic Data Service - 비즈니스 로직"""
import logging
import requests
import yfinance as yf
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, Any
from collections import defaultdict

from .repository import EconomicDataRepository
from src.core.config import settings

logger = logging.getLogger(__name__)


class EconomicDataService:
    """경제 데이터 수집 서비스"""

    def __init__(self):
        self.repository = EconomicDataRepository()

    def collect_economic_data(self) -> Dict[str, Any]:
        """
        경제 데이터를 수집하여 daily_stock_data에 저장합니다.
        날짜별로 fred_indicators와 yfinance_indicators를 통합하여 저장합니다.
        """
        try:
            logger.info("경제 데이터 수집 시작")

            # 날짜 범위 설정
            # GDP는 분기별, CPI/실업률은 월별이므로 90일 조회
            end_date = datetime.now()
            start_date = end_date - timedelta(days=90)
            start_date_str = start_date.strftime("%Y-%m-%d")
            end_date_str = end_date.strftime("%Y-%m-%d")

            # FRED 및 Yahoo Finance 지표 조회
            fred_indicators = self._load_fred_indicators()
            yfinance_indicators = self._load_yfinance_indicators()

            # 날짜별 데이터를 그룹화할 딕셔너리
            daily_data = defaultdict(lambda: {
                "fred_indicators": {},
                "yfinance_indicators": {}
            })

            # FRED 데이터 수집 (날짜별로 그룹화)
            fred_count = self._collect_fred_data_grouped(
                fred_indicators, start_date_str, end_date_str, daily_data
            )

            # Yahoo Finance 데이터 수집 (날짜별로 그룹화)
            yahoo_count = self._collect_yahoo_data_grouped(
                yfinance_indicators, start_date_str, end_date_str, daily_data
            )

            # daily_stock_data에 날짜별로 저장
            saved_dates = 0
            for date_str, data in daily_data.items():
                if self.repository.upsert_daily_data(date_str, data):
                    saved_dates += 1
                    logger.info(f"✅ daily_stock_data 저장: {date_str} (FRED: {len(data['fred_indicators'])}, Yahoo: {len(data['yfinance_indicators'])})")

            logger.info(f"경제 데이터 수집 완료: FRED={fred_count}개 지표, Yahoo={yahoo_count}개 지표, {saved_dates}일치 저장")

            return {
                "success": True,
                "fred_collected": fred_count,
                "yahoo_collected": yahoo_count,
                "dates_saved": saved_dates
            }

        except Exception as e:
            logger.error(f"경제 데이터 수집 실패: {e}")
            return {
                "success": False,
                "error": str(e)
            }

    def _load_fred_indicators(self) -> Dict[str, str]:
        """FRED 지표를 조회합니다."""
        indicators = {}
        try:
            docs = self.repository.find_active_indicators("fred_indicators")
            for doc in docs:
                if "code" in doc and "name" in doc:
                    indicators[doc["code"]] = doc["name"]
        except Exception as e:
            logger.error(f"FRED 지표 조회 실패: {e}")
        return indicators

    def _load_yfinance_indicators(self) -> Dict[str, str]:
        """Yahoo Finance 지표를 조회합니다."""
        indicators = {}
        try:
            docs = self.repository.find_active_indicators("yfinance_indicators")
            for doc in docs:
                if "ticker" in doc and "name" in doc:
                    indicators[doc["name"]] = doc["ticker"]
        except Exception as e:
            logger.error(f"Yahoo Finance 지표 조회 실패: {e}")
        return indicators

    def _collect_fred_data_grouped(
        self,
        indicators: Dict[str, str],
        start_date: str,
        end_date: str,
        daily_data: Dict[str, Dict]
    ) -> int:
        """
        FRED 데이터를 수집하여 daily_data에 날짜별로 그룹화합니다.

        Args:
            indicators: {code: name} 형식의 FRED 지표 딕셔너리
            start_date: 시작 날짜
            end_date: 종료 날짜
            daily_data: 날짜별 데이터를 저장할 딕셔너리 (참조로 전달)

        Returns:
            성공적으로 수집한 지표 개수
        """
        success_count = 0

        for code, name in indicators.items():
            try:
                df = self._fetch_fred_data(code, start_date, end_date)

                if df is not None and not df.empty:
                    # 각 날짜별로 데이터를 그룹화
                    for date, row in df.iterrows():
                        date_str = date.strftime("%Y-%m-%d")
                        value = float(row.iloc[0]) if not pd.isna(row.iloc[0]) else None

                        if value is not None:
                            daily_data[date_str]["fred_indicators"][name] = value

                    success_count += 1
                    logger.info(f"✅ FRED 데이터 수집 완료: {code} ({name})")

            except Exception as e:
                logger.error(f"❌ FRED 데이터 수집 실패: {code} - {e}")

        return success_count

    def _collect_yahoo_data_grouped(
        self,
        indicators: Dict[str, str],
        start_date: str,
        end_date: str,
        daily_data: Dict[str, Dict]
    ) -> int:
        """
        Yahoo Finance 데이터를 수집하여 daily_data에 날짜별로 그룹화합니다.

        Args:
            indicators: {name: ticker} 형식의 Yahoo Finance 지표 딕셔너리
            start_date: 시작 날짜
            end_date: 종료 날짜
            daily_data: 날짜별 데이터를 저장할 딕셔너리 (참조로 전달)

        Returns:
            성공적으로 수집한 지표 개수
        """
        success_count = 0

        for name, ticker in indicators.items():
            try:
                df = self._fetch_yahoo_data(ticker, start_date, end_date)

                if df is not None and not df.empty:
                    # 각 날짜별로 데이터를 그룹화
                    for date, row in df.iterrows():
                        date_str = date.strftime("%Y-%m-%d")
                        close_price = float(row["Close"]) if "Close" in row and not pd.isna(row["Close"]) else None

                        if close_price is not None:
                            daily_data[date_str]["yfinance_indicators"][name] = close_price

                    success_count += 1
                    logger.info(f"✅ Yahoo Finance 데이터 수집 완료: {ticker} ({name})")

            except Exception as e:
                logger.error(f"❌ Yahoo Finance 데이터 수집 실패: {ticker} - {e}")

        return success_count

    def _fetch_fred_data(self, series_id: str, start_date: str, end_date: str) -> pd.DataFrame:
        """FRED API에서 데이터를 가져옵니다."""
        try:
            url = "https://api.stlouisfed.org/fred/series/observations"
            params = {
                "series_id": series_id,
                "api_key": settings.FRED_API_KEY,
                "file_type": "json",
                "observation_start": start_date,
                "observation_end": end_date
            }

            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()

            data = response.json()
            observations = data.get("observations", [])

            if not observations:
                return None

            df = pd.DataFrame(observations)
            df["date"] = pd.to_datetime(df["date"])
            df = df.set_index("date")
            df["value"] = pd.to_numeric(df["value"], errors="coerce")

            return df[["value"]]

        except Exception as e:
            logger.error(f"FRED 데이터 가져오기 실패: {series_id} - {e}")
            return None

    def _fetch_yahoo_data(self, ticker: str, start_date: str, end_date: str) -> pd.DataFrame:
        """Yahoo Finance에서 데이터를 가져옵니다."""
        try:
            stock = yf.Ticker(ticker)
            df = stock.history(start=start_date, end=end_date, interval="1d")

            if df is None or df.empty:
                return None

            return df

        except Exception as e:
            logger.error(f"Yahoo Finance 데이터 가져오기 실패: {ticker} - {e}")
            return None
