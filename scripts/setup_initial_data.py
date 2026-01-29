#!/usr/bin/env python3
"""
Setup initial data for quantiq from stock-trading analysis
Creates clean initial state for the new system
"""

import pymongo
import psycopg
import os
from dotenv import load_dotenv
from collections import defaultdict
from datetime import datetime
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Load environment
load_dotenv()

# Source (stock-trading)
source_mongo_url = os.getenv('MONGO_URL', 'mongodb+srv://cluster-test.2dkjwjs.mongodb.net')
source_mongo_user = os.getenv('MONGO_USER', 'test')
source_mongo_pass = os.getenv('MONGO_PASSWORD', '')

# Target (quantiq)
target_postgres_host = os.getenv('DB_HOST', 'localhost')
target_postgres_port = int(os.getenv('DB_PORT', '5433'))
target_postgres_db = os.getenv('DB_NAME', 'quantiq')
target_postgres_user = os.getenv('DB_USER', 'quantiq_user')
target_postgres_pass = os.getenv('DB_PASSWORD', 'quantiq_password')

def analyze_portfolio():
    """Analyze portfolio from stock-trading MongoDB"""
    logger.info("Analyzing portfolio from stock-trading...")

    try:
        client = pymongo.MongoClient(
            source_mongo_url,
            username=source_mongo_user,
            password=source_mongo_pass,
            serverSelectionTimeoutMS=5000
        )
        db = client['stock_trading']

        users = list(db['users'].find())
        if not users:
            logger.warning("No users found in stock-trading")
            return None

        user = users[0]
        user_id = user.get('user_id')
        email = user.get('email')
        name = user.get('name', user_id)
        account = user.get('account_balance', {})
        trading_config = user.get('trading_config', {})

        # Get trading logs
        trading_logs = list(db['trading_logs'].find({'user_id': user_id}))
        logger.info(f"Found {len(trading_logs)} trading logs for user {user_id}")

        # Calculate holdings
        holdings = defaultdict(lambda: {'qty': 0, 'total_cost': 0.0})

        for log in trading_logs:
            ticker = log.get('ticker')
            order_type = log.get('order_type', 'buy').upper()
            qty = int(log.get('quantity', 0))
            price = float(log.get('price', 0))

            if order_type == 'BUY':
                holdings[ticker]['qty'] += qty
                holdings[ticker]['total_cost'] += qty * price
            elif order_type == 'SELL':
                holdings[ticker]['qty'] -= qty
                holdings[ticker]['total_cost'] -= qty * price

        # Remove zero holdings
        holdings = {k: v for k, v in holdings.items() if v['qty'] > 0}

        portfolio_data = {
            'user_id': user_id,
            'email': email,
            'name': name,
            'cash_balance': float(account.get('available_usd', 1000.0)),
            'total_asset': float(account.get('total_assets_usd', 1000.0)),
            'holdings': holdings,
            'trading_config': {
                'enabled': trading_config.get('auto_trading_enabled', False),
                'min_composite_score': float(trading_config.get('min_composite_score', 2.0)),
                'max_stocks_to_buy': int(trading_config.get('max_stocks_to_buy', 5)),
                'stop_loss_percent': float(trading_config.get('stop_loss_percent', -7.0)),
                'take_profit_percent': float(trading_config.get('take_profit_percent', 5.0)),
            }
        }

        logger.info(f"✓ Portfolio analysis complete: {len(holdings)} holdings, ${portfolio_data['cash_balance']:.2f} cash")
        return portfolio_data

    except Exception as e:
        logger.error(f"Error analyzing portfolio: {e}")
        raise

def setup_quantiq_data(portfolio_data):
    """Setup initial data in quantiq PostgreSQL"""
    logger.info("Setting up quantiq PostgreSQL...")

    try:
        conn = psycopg.connect(
            host=target_postgres_host,
            port=target_postgres_port,
            dbname=target_postgres_db,
            user=target_postgres_user,
            password=target_postgres_pass
        )

        with conn.cursor() as cur:
            # 1. Insert user
            logger.info(f"Inserting user: {portfolio_data['user_id']}")
            cur.execute("""
                INSERT INTO users (user_id, name, email, password_hash, status)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (user_id) DO UPDATE SET name = EXCLUDED.name
                RETURNING id
            """, (
                portfolio_data['user_id'],
                portfolio_data['name'],
                portfolio_data['email'],
                'initial_hash',
                'ACTIVE'
            ))

            user_postgres_id = cur.fetchone()[0]
            logger.info(f"✓ User inserted with ID: {user_postgres_id}")

            # 2. Insert trading config
            logger.info("Inserting trading config...")
            config = portfolio_data['trading_config']
            cur.execute("""
                INSERT INTO trading_configs
                (user_id, enabled, auto_trading_enabled, min_composite_score,
                 max_stocks_to_buy, stop_loss_percent, take_profit_percent)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (user_id) DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    min_composite_score = EXCLUDED.min_composite_score
            """, (
                user_postgres_id,
                config['enabled'],
                config['enabled'],
                config['min_composite_score'],
                config['max_stocks_to_buy'],
                config['stop_loss_percent'],
                config['take_profit_percent']
            ))
            logger.info("✓ Trading config inserted")

            # 3. Insert holdings
            logger.info(f"Inserting {len(portfolio_data['holdings'])} stock holdings...")
            for ticker, data in portfolio_data['holdings'].items():
                avg_price = data['total_cost'] / data['qty'] if data['qty'] > 0 else 0
                cur.execute("""
                    INSERT INTO stock_holdings
                    (user_id, ticker, quantity, average_price, total_cost, current_value)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    ON CONFLICT (user_id, ticker) DO UPDATE SET
                        quantity = EXCLUDED.quantity,
                        average_price = EXCLUDED.average_price
                """, (
                    user_postgres_id,
                    ticker,
                    data['qty'],
                    avg_price,
                    data['total_cost'],
                    data['total_cost']  # Use cost as current value
                ))

            logger.info(f"✓ {len(portfolio_data['holdings'])} holdings inserted")

            # 4. Insert account balance
            logger.info("Inserting account balance...")
            cur.execute("""
                INSERT INTO account_balances
                (user_id, cash, total_value, locked_cash, version)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (user_id) DO UPDATE SET
                    cash = EXCLUDED.cash,
                    total_value = EXCLUDED.total_value,
                    version = account_balances.version + 1
            """, (
                user_postgres_id,
                portfolio_data['cash_balance'],
                portfolio_data['total_asset'],
                0,
                1
            ))
            logger.info("✓ Account balance inserted")

            conn.commit()
            logger.info("✓ All data committed successfully!")

            # Display summary
            print("\n" + "=" * 70)
            print("✅ QUANTIQ INITIAL DATA SETUP COMPLETE")
            print("=" * 70)
            print(f"\n👤 User: {portfolio_data['user_id']}")
            print(f"   Email: {portfolio_data['email']}")
            print(f"\n💰 Account:")
            print(f"   Cash Balance: ${portfolio_data['cash_balance']:.2f}")
            print(f"   Total Assets: ${portfolio_data['total_asset']:.2f}")
            print(f"\n📊 Holdings: {len(portfolio_data['holdings'])} positions")
            for ticker, data in sorted(portfolio_data['holdings'].items()):
                avg_price = data['total_cost'] / data['qty']
                print(f"   {ticker:8s}: {data['qty']:3d} shares @ ${avg_price:8.2f} avg")
            print("\n" + "=" * 70)

    except Exception as e:
        logger.error(f"Error setting up quantiq data: {e}")
        raise
    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    try:
        logger.info("Starting initial data setup...")
        portfolio_data = analyze_portfolio()
        if portfolio_data:
            setup_quantiq_data(portfolio_data)
            logger.info("✓ Setup complete!")
        else:
            logger.error("Failed to analyze portfolio")
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        exit(1)
