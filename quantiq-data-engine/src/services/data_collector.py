import requests
import pandas as pd
import yfinance as yf
from datetime import datetime
import time
import logging
from src.config import settings
from src.db import MongoDB

logger = logging.getLogger(__name__)

# --- Helper Functions ---

def load_indicators_from_mongodb():
    """Load active indicators from MongoDB."""
    try:
        db = MongoDB.get_db()
        
        # FRED indicators
        fred_indicators = {}
        try:
            active_fred = db.fred_indicators.find({"is_active": True})
            for indicator in active_fred:
                code = indicator.get("code")
                name = indicator.get("name")
                if code and name:
                    fred_indicators[code] = name
        except Exception as e:
            logger.warning(f"Failed to load fred_indicators: {e}")
        
        # Yahoo Finance indicators
        yfinance_indicators = {}
        try:
            active_yfinance = db.yfinance_indicators.find({"is_active": True})
            for indicator in active_yfinance:
                ticker = indicator.get("ticker")
                name = indicator.get("name")
                if ticker and name:
                    yfinance_indicators[name] = ticker
        except Exception as e:
            logger.warning(f"Failed to load yfinance_indicators: {e}")
            
        return fred_indicators, yfinance_indicators
        
    except Exception as e:
        logger.error(f"Failed to load indicators from MongoDB: {e}")
        return {}, {}

def download_yahoo_chart(symbol, start_date, end_date, interval="1d"):
    """
    Fetch historical data from Yahoo Finance Chart API.
    Refactored to be cleaner.
    """
    try:
        # Utilize yfinance library directly efficiently if possible, 
        # but sticking to requests as per original implementation for specific control if needed.
        # However, yfinance Ticker.history is usually more robust. 
        # For migration, I will switch to yfinance library for simplicity and robustness.
        
        ticker = yf.Ticker(symbol)
        df = ticker.history(start=start_date, end=end_date, interval=interval)
        
        if df.empty:
            return pd.DataFrame()
        
        # Keep only Close and handle index
        df = df[['Close']]
        df.index = pd.to_datetime(df.index.date) # Reset time to 00:00:00 or just date
        
        # Handle duplicates (keep last)
        df = df[~df.index.duplicated(keep='last')]
        
        return df
        
    except Exception as e:
        logger.error(f"Error downloading yahoo chart for {symbol}: {e}")
        return pd.DataFrame()

def get_short_interest_data(ticker_symbol: str) -> dict:
    try:
        ticker = yf.Ticker(ticker_symbol)
        info = ticker.info
        
        shares_short = info.get('sharesShort')
        shares_short_prior = info.get('sharesShortPriorMonth')
        
        short_interest = {
            'sharesShort': shares_short,
            'sharesShortPriorMonth': shares_short_prior,
            'shortRatio': info.get('shortRatio'),
            'shortPercentOfFloat': info.get('shortPercentOfFloat')
        }
        
        # Filter None
        return {k: v for k, v in short_interest.items() if v is not None}
    except Exception as e:
        logger.warning(f"Failed to get short interest for {ticker_symbol}: {e}")
        return {}

# --- Main Collection Logic ---

def collect_economic_data(start_date='2006-01-01', end_date=None):
    if end_date is None:
        end_date = datetime.today().strftime('%Y-%m-%d')
        
    logger.info(f"Starting data collection: {start_date} ~ {end_date}")
    
    fred_indicators, yfinance_indicators = load_indicators_from_mongodb()
    
    # 1. FRED Data
    fred_dfs = []
    for code, name in fred_indicators.items():
        try:
            # Using FRED API logic from original
            # Simplified for brevity in this migration, assuming API key presence
            url = f'https://api.stlouisfed.org/fred/series/observations'
            params = {
                'series_id': code,
                'api_key': settings.FRED_API_KEY,
                'file_type': 'json',
                'observation_start': start_date,
                'observation_end': end_date
            }
            resp = requests.get(url, params=params)
            if resp.status_code == 200:
                data = resp.json().get('observations', [])
                if data:
                    df = pd.DataFrame(data)[['date', 'value']]
                    df.columns = ['date', name]
                    df['date'] = pd.to_datetime(df['date'])
                    fred_dfs.append(df.set_index('date').resample('D').ffill())
            time.sleep(0.5)
        except Exception as e:
            logger.error(f"FRED error {code}: {e}")
            
    # 2. Yahoo Indicators
    yfinance_dfs = []
    for name, ticker in yfinance_indicators.items():
        df = download_yahoo_chart(ticker, start_date, end_date)
        if not df.empty:
            df.columns = [name]
            yfinance_dfs.append(df)
        time.sleep(0.5)
        
    # 3. Active Stocks
    db = MongoDB.get_db()
    active_stocks = list(db.stocks.find({"is_active": True}))
    stock_dfs = []
    short_interest_data = {}
    
    for stock in active_stocks:
        ticker = stock.get("ticker")
        name = stock.get("stock_name")
        if not ticker: continue
        
        df = download_yahoo_chart(ticker, start_date, end_date)
        if not df.empty:
            df.columns = [name]
            stock_dfs.append(df)
            
            # Short Interest
            si = get_short_interest_data(ticker)
            if si:
                # Store latest short interest for all dates in this batch (approximation)
                # In real world, we might want historical SI, but yfinance only gives current
                for date in df.index:
                    date_str = date.strftime('%Y-%m-%d')
                    if date_str not in short_interest_data:
                        short_interest_data[date_str] = {}
                    short_interest_data[date_str][ticker] = {"short_interest": si}
        time.sleep(0.5)
        
    # Merge All
    all_dfs = fred_dfs + yfinance_dfs + stock_dfs
    if not all_dfs:
        return None, {}
        
    result_df = pd.concat(all_dfs, axis=1, join='outer')
    result_df.sort_index(inplace=True)
    result_df.ffill(inplace=True)
    
    # Clean index (date only)
    result_df.index = pd.to_datetime(result_df.index.date)
    result_df = result_df[~result_df.index.duplicated(keep='last')]
    
    return result_df, short_interest_data
