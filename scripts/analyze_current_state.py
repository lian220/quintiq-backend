#!/usr/bin/env python3
"""
Analyze current portfolio state from stock-trading
Calculate current holdings and account balances
"""

import pymongo
import os
from dotenv import load_dotenv
from collections import defaultdict
from datetime import datetime
import json

load_dotenv('/Users/imdoyeong/Desktop/workSpace/stock-trading/.env')

mongo_url = os.getenv('MONGO_URL')
mongo_user = os.getenv('MONGO_USER')
mongo_pass = os.getenv('MONGO_PASSWORD')

client = pymongo.MongoClient(mongo_url, username=mongo_user, password=mongo_pass)
db = client['stock_trading']

# Get user info
users = list(db['users'].find())
print("=" * 70)
print("ANALYZING PORTFOLIO STATE")
print("=" * 70)

for user in users:
    user_id = user.get('user_id')
    email = user.get('email')
    name = user.get('name')
    account = user.get('account_balance', {})

    print(f"\n👤 User: {user_id} ({name})")
    print(f"   Email: {email}")

    # Current account balance from user document
    print(f"\n💰 Account Balance (from user doc):")
    print(f"   Available USD: ${account.get('available_usd', 0):.2f}")
    print(f"   Total Assets USD: ${account.get('total_assets_usd', 0):.2f}")
    print(f"   Total Valuation USD: ${account.get('total_valuation_usd', 0):.2f}")
    print(f"   Total Deposit USD: ${account.get('total_deposit_usd', 0):.2f}")

    # Trading history
    trading_logs = list(db['trading_logs'].find({'user_id': user_id}))
    print(f"\n📊 Trading History: {len(trading_logs)} trades")

    # Calculate holdings from trading logs
    holdings = defaultdict(lambda: {'qty': 0, 'total_cost': 0.0, 'trades': []})

    for log in sorted(trading_logs, key=lambda x: x.get('trade_datetime', datetime.min)):
        ticker = log.get('ticker')
        order_type = log.get('order_type', 'buy').upper()
        qty = int(log.get('quantity', 0))
        price = float(log.get('price', 0))

        if order_type == 'BUY':
            holdings[ticker]['qty'] += qty
            holdings[ticker]['total_cost'] += qty * price
            holdings[ticker]['trades'].append({
                'type': 'BUY',
                'qty': qty,
                'price': price,
                'date': log.get('trade_datetime')
            })
        elif order_type == 'SELL':
            holdings[ticker]['qty'] -= qty
            holdings[ticker]['total_cost'] -= qty * price
            holdings[ticker]['trades'].append({
                'type': 'SELL',
                'qty': qty,
                'price': price,
                'date': log.get('trade_datetime')
            })

    # Display current holdings
    print(f"\n📈 Current Holdings:")
    total_holdings_value = 0
    for ticker, data in sorted(holdings.items()):
        if data['qty'] > 0:
            avg_price = data['total_cost'] / data['qty'] if data['qty'] != 0 else 0
            print(f"   {ticker:8s}: {data['qty']:4d} shares @ ${avg_price:8.2f} avg = ${data['total_cost']:10.2f}")
            total_holdings_value += data['total_cost']

    print(f"\n   Total Holdings Cost: ${total_holdings_value:.2f}")

    # Trading config
    trading_config = user.get('trading_config', {})
    print(f"\n⚙️  Trading Config:")
    print(f"   Enabled: {trading_config.get('auto_trading_enabled', False)}")
    print(f"   Min Composite Score: {trading_config.get('min_composite_score', 2.0)}")
    print(f"   Max Stocks to Buy: {trading_config.get('max_stocks_to_buy', 5)}")
    print(f"   Stop Loss: {trading_config.get('stop_loss_percent', -7.0)}%")
    print(f"   Take Profit: {trading_config.get('take_profit_percent', 5.0)}%")

    # Summary for SQL INSERT
    print(f"\n" + "=" * 70)
    print("SQL INSERT VALUES (for quantiq):")
    print("=" * 70)

    cash_balance = account.get('available_usd', 1000.0)
    total_asset = account.get('total_assets_usd', 1000.0)

    print(f"""
-- Users
INSERT INTO users (user_id, name, email, password_hash, status)
VALUES ('{user_id}', '{name}', '{email}', 'default_hash', 'ACTIVE');

-- Trading Config
INSERT INTO trading_configs
  (user_id, enabled, auto_trading_enabled, min_composite_score, max_stocks_to_buy,
   stop_loss_percent, take_profit_percent)
VALUES (
  (SELECT id FROM users WHERE user_id = '{user_id}'),
  {str(trading_config.get('auto_trading_enabled', False)).lower()},
  {str(trading_config.get('auto_trading_enabled', False)).lower()},
  {trading_config.get('min_composite_score', 2.0)},
  {trading_config.get('max_stocks_to_buy', 5)},
  {trading_config.get('stop_loss_percent', -7.0)},
  {trading_config.get('take_profit_percent', 5.0)}
);

-- Account Balances
INSERT INTO account_balances (user_id, cash, total_value, locked_cash, version)
VALUES (
  (SELECT id FROM users WHERE user_id = '{user_id}'),
  {cash_balance},
  {total_asset},
  0,
  1
);

-- Stock Holdings
""")

    for ticker, data in sorted(holdings.items()):
        if data['qty'] > 0:
            avg_price = data['total_cost'] / data['qty'] if data['qty'] != 0 else 0
            current_value = data['total_cost']  # Simplified: use cost as current value
            print(f"""INSERT INTO stock_holdings
  (user_id, ticker, quantity, average_price, total_cost, current_value)
VALUES (
  (SELECT id FROM users WHERE user_id = '{user_id}'),
  '{ticker}',
  {data['qty']},
  {avg_price:.2f},
  {data['total_cost']:.2f},
  {current_value:.2f}
);""")

print("\n" + "=" * 70)
print("✅ Analysis complete!")
print("=" * 70)
