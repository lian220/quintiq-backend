#!/usr/bin/env python3
"""
Data Migration Script: stock-trading MongoDB → quantiq (PostgreSQL + MongoDB)
Migrates data according to the hybrid architecture plan:
- PostgreSQL: Transaction data (users, trades, holdings, etc.)
- MongoDB: Analytical data (recommendations, predictions, time-series)
"""

import os
import sys
import asyncio
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional
from dotenv import load_dotenv
import pymongo
import psycopg
from decimal import Decimal

# Setup logging
log_filename = f'migration_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log'
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(log_filename),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class DataMigrator:
    def __init__(self):
        """Initialize migration configuration from environment"""
        load_dotenv()

        # Source (stock-trading)
        self.source_mongo_url = os.getenv('MONGO_URL', 'mongodb+srv://cluster-test.2dkjwjs.mongodb.net')
        self.source_mongo_user = os.getenv('MONGO_USER', 'test')
        self.source_mongo_pass = os.getenv('MONGO_PASSWORD', '')

        # Target (quantiq local)
        self.target_postgres_host = os.getenv('DB_HOST', 'localhost')
        self.target_postgres_port = int(os.getenv('DB_PORT', '5433'))
        self.target_postgres_db = os.getenv('DB_NAME', 'quantiq')
        self.target_postgres_user = os.getenv('DB_USER', 'quantiq_user')
        self.target_postgres_pass = os.getenv('DB_PASSWORD', 'quantiq_password')

        self.target_mongo_url = os.getenv('SPRING_DATA_MONGODB_URI', 'mongodb://quantiq_user:quantiq_password@localhost:27017/stock_trading?authSource=admin')

        # Connections
        self.source_mongo = None
        self.target_postgres = None
        self.target_mongo = None

        # User ID mapping: string user_id (from MongoDB) → BIGINT id (PostgreSQL)
        self.user_id_map = {}

        self.migration_stats = {
            'users': 0,
            'stocks': 0,
            'stock_holdings': 0,
            'trading_configs': 0,
            'trades': 0,
            'stock_recommendations': 0,
            'stock_predictions': 0,
            'errors': []
        }

    async def connect(self):
        """Connect to source and target databases"""
        try:
            # Connect to source MongoDB
            logger.info("Connecting to source MongoDB...")
            self.source_mongo = pymongo.MongoClient(
                self.source_mongo_url,
                username=self.source_mongo_user,
                password=self.source_mongo_pass,
                serverSelectionTimeoutMS=5000
            )
            self.source_mongo.admin.command('ping')
            logger.info("✓ Source MongoDB connected")

            # Connect to target PostgreSQL
            logger.info("Connecting to target PostgreSQL...")
            self.target_postgres = psycopg.connect(
                host=self.target_postgres_host,
                port=self.target_postgres_port,
                dbname=self.target_postgres_db,
                user=self.target_postgres_user,
                password=self.target_postgres_pass
            )
            logger.info("✓ Target PostgreSQL connected")

            # Connect to target MongoDB
            logger.info("Connecting to target MongoDB...")
            self.target_mongo = pymongo.MongoClient(self.target_mongo_url)
            self.target_mongo.admin.command('ping')
            logger.info("✓ Target MongoDB connected")

        except Exception as e:
            logger.error(f"Connection error: {e}")
            raise

    def disconnect(self):
        """Close all database connections"""
        if self.source_mongo:
            self.source_mongo.close()
        if self.target_postgres:
            self.target_postgres.close()
        if self.target_mongo:
            self.target_mongo.close()

    async def migrate_users(self):
        """Migrate users from MongoDB to PostgreSQL"""
        try:
            logger.info("Starting user migration...")
            source_db = self.source_mongo['stock_trading']
            source_users = source_db['users']

            users = list(source_users.find())
            logger.info(f"Found {len(users)} users in source")

            if len(users) == 0:
                logger.warning("No users found in source")
                return

            with self.target_postgres.cursor() as cur:
                for user in users:
                    try:
                        source_user_id = str(user.get('_id'))
                        name = user.get('name', '')
                        email = user.get('email', '')

                        # Generate hash for password if needed
                        password_hash = user.get('password_hash', 'default_hash')
                        status = user.get('status', 'ACTIVE')
                        created_at = datetime.now()

                        cur.execute("""
                            INSERT INTO users (user_id, name, email, password_hash, status, created_at)
                            VALUES (%s, %s, %s, %s, %s, %s)
                            ON CONFLICT (user_id) DO UPDATE SET name = EXCLUDED.name
                            RETURNING id
                        """, (source_user_id, name, email, password_hash, status, created_at))

                        # Store the mapping of string user_id to BIGINT id
                        result = cur.fetchone()
                        if result:
                            postgres_id = result[0]
                            # Use the user_id field (if exists) or _id as key
                            user_key = user.get('user_id') or source_user_id
                            self.user_id_map[user_key] = postgres_id
                            logger.debug(f"Mapped {user_key} → {postgres_id}")

                        self.migration_stats['users'] += 1
                    except Exception as e:
                        logger.error(f"Error migrating user {user.get('_id')}: {e}")
                        self.migration_stats['errors'].append(f"User {user.get('_id')}: {str(e)}")

                self.target_postgres.commit()

            logger.info(f"✓ Migrated {self.migration_stats['users']} users")
            logger.info(f"User ID mapping: {self.user_id_map}")

        except Exception as e:
            logger.error(f"User migration error: {e}")
            self.migration_stats['errors'].append(f"User migration: {str(e)}")

    async def migrate_stocks(self):
        """Migrate stocks to target MongoDB"""
        try:
            logger.info("Starting stocks migration...")
            source_db = self.source_mongo['stock_trading']
            source_stocks = source_db['stocks']

            stocks = list(source_stocks.find())
            logger.info(f"Found {len(stocks)} stocks in source")

            if len(stocks) == 0:
                logger.warning("No stocks found in source")
                return

            target_db = self.target_mongo['stock_trading']
            target_stocks = target_db['stocks']

            for stock in stocks:
                try:
                    # Remove MongoDB internal ID to avoid conflicts
                    stock.pop('_id', None)

                    target_stocks.update_one(
                        {'ticker': stock.get('ticker')},
                        {'$set': stock},
                        upsert=True
                    )
                    self.migration_stats['stocks'] += 1
                except Exception as e:
                    logger.error(f"Error migrating stock {stock.get('ticker')}: {e}")
                    self.migration_stats['errors'].append(f"Stock {stock.get('ticker')}: {str(e)}")

            logger.info(f"✓ Migrated {self.migration_stats['stocks']} stocks to MongoDB")

        except Exception as e:
            logger.error(f"Stocks migration error: {e}")
            self.migration_stats['errors'].append(f"Stocks migration: {str(e)}")

    async def migrate_stock_holdings(self):
        """Calculate and migrate stock_holdings from user_stocks"""
        try:
            logger.info("Starting stock_holdings migration...")
            source_db = self.source_mongo['stock_trading']
            source_user_stocks = source_db['user_stocks']

            holdings = list(source_user_stocks.find())
            logger.info(f"Found {len(holdings)} holdings in source")

            if len(holdings) == 0:
                logger.warning("No holdings found in source")
                return

            with self.target_postgres.cursor() as cur:
                for holding in holdings:
                    try:
                        source_user_id = str(holding.get('user_id', ''))
                        # Get the PostgreSQL user ID
                        postgres_user_id = self.user_id_map.get(source_user_id)

                        if not postgres_user_id:
                            logger.warning(f"No user mapping found for {source_user_id}")
                            continue

                        ticker = holding.get('ticker', '')
                        quantity = int(holding.get('quantity', 0))
                        average_price = float(holding.get('average_price', 0))
                        total_cost = float(holding.get('total_cost', holding.get('cost', 0)))
                        current_value = float(holding.get('current_value', 0))

                        cur.execute("""
                            INSERT INTO stock_holdings
                            (user_id, ticker, quantity, average_price, total_cost, current_value, updated_at)
                            VALUES (%s, %s, %s, %s, %s, %s, %s)
                            ON CONFLICT (user_id, ticker) DO UPDATE SET
                                quantity = EXCLUDED.quantity,
                                average_price = EXCLUDED.average_price,
                                total_cost = EXCLUDED.total_cost,
                                current_value = EXCLUDED.current_value,
                                updated_at = EXCLUDED.updated_at
                        """, (postgres_user_id, ticker, quantity, average_price, total_cost, current_value, datetime.now()))

                        self.migration_stats['stock_holdings'] += 1
                    except Exception as e:
                        logger.error(f"Error migrating holding {holding.get('_id')}: {e}")
                        self.migration_stats['errors'].append(f"Holding {holding.get('_id')}: {str(e)}")

                self.target_postgres.commit()

            logger.info(f"✓ Migrated {self.migration_stats['stock_holdings']} holdings")

        except Exception as e:
            logger.error(f"Holdings migration error: {e}")
            self.migration_stats['errors'].append(f"Holdings migration: {str(e)}")

    async def migrate_trading_configs(self):
        """Migrate trading configurations"""
        try:
            logger.info("Starting trading_configs migration...")
            source_db = self.source_mongo['stock_trading']
            source_configs = source_db['trading_configs']

            configs = list(source_configs.find())
            logger.info(f"Found {len(configs)} trading configs in source")

            if len(configs) == 0:
                logger.warning("No trading configs found in source")
                return

            with self.target_postgres.cursor() as cur:
                for config in configs:
                    try:
                        source_user_id = str(config.get('user_id', ''))
                        # Get the PostgreSQL user ID
                        postgres_user_id = self.user_id_map.get(source_user_id)

                        if not postgres_user_id:
                            logger.warning(f"No user mapping found for {source_user_id}")
                            continue

                        enabled = config.get('enabled', False)
                        min_composite_score = float(config.get('min_composite_score',
                                                               config.get('min_confidence', 2.0)))
                        max_stocks = int(config.get('max_stocks_to_buy',
                                                   config.get('max_buy_stocks', 5)))

                        cur.execute("""
                            INSERT INTO trading_configs
                            (user_id, enabled, min_composite_score, max_stocks_to_buy, created_at)
                            VALUES (%s, %s, %s, %s, %s)
                            ON CONFLICT (user_id) DO UPDATE SET
                                enabled = EXCLUDED.enabled,
                                min_composite_score = EXCLUDED.min_composite_score,
                                max_stocks_to_buy = EXCLUDED.max_stocks_to_buy
                        """, (postgres_user_id, enabled, min_composite_score, max_stocks, datetime.now()))

                        self.migration_stats['trading_configs'] += 1
                    except Exception as e:
                        logger.error(f"Error migrating config for user {config.get('user_id')}: {e}")
                        self.migration_stats['errors'].append(f"Config {config.get('user_id')}: {str(e)}")

                self.target_postgres.commit()

            logger.info(f"✓ Migrated {self.migration_stats['trading_configs']} trading configs")

        except Exception as e:
            logger.error(f"Trading configs migration error: {e}")
            self.migration_stats['errors'].append(f"Trading configs: {str(e)}")

    async def migrate_trades(self):
        """Migrate trading logs to trades table"""
        try:
            logger.info("Starting trades migration...")
            source_db = self.source_mongo['stock_trading']
            source_logs = source_db['trading_logs']

            logs = list(source_logs.find())
            logger.info(f"Found {len(logs)} trades in source")

            if len(logs) == 0:
                logger.warning("No trades found in source")
                return

            with self.target_postgres.cursor() as cur:
                for log in logs:
                    try:
                        source_user_id = str(log.get('user_id', ''))
                        # Get the PostgreSQL user ID
                        postgres_user_id = self.user_id_map.get(source_user_id)

                        if not postgres_user_id:
                            logger.warning(f"No user mapping found for {source_user_id}")
                            continue

                        ticker = log.get('ticker', '')
                        side = log.get('order_type', 'BUY').upper()
                        if side not in ['BUY', 'SELL']:
                            side = 'BUY'

                        quantity = int(log.get('quantity', 0))
                        price = float(log.get('price', 0))
                        total_amount = float(log.get('total_amount', 0))
                        commission = float(log.get('commission', 0))
                        status = log.get('status', 'EXECUTED')
                        kis_order_id = log.get('kis_order_id', '')
                        executed_at = log.get('executed_at')
                        if not executed_at:
                            executed_at = datetime.now()

                        cur.execute("""
                            INSERT INTO trades
                            (user_id, ticker, side, quantity, price, total_amount,
                             commission, status, kis_order_id, executed_at)
                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        """, (postgres_user_id, ticker, side, quantity, price, total_amount,
                              commission, status, kis_order_id, executed_at))

                        self.migration_stats['trades'] += 1
                    except Exception as e:
                        logger.error(f"Error migrating trade {log.get('_id')}: {e}")
                        self.migration_stats['errors'].append(f"Trade {log.get('_id')}: {str(e)}")

                self.target_postgres.commit()

            logger.info(f"✓ Migrated {self.migration_stats['trades']} trades")

        except Exception as e:
            logger.error(f"Trades migration error: {e}")
            self.migration_stats['errors'].append(f"Trades: {str(e)}")

    async def migrate_analytical_data(self):
        """Migrate analytical data (recommendations, predictions) to MongoDB"""
        try:
            logger.info("Starting analytical data migration...")
            source_db = self.source_mongo['stock_trading']
            target_db = self.target_mongo['stock_trading']

            # Collections to migrate to target MongoDB
            analytical_collections = [
                'stock_recommendations',
                'stock_predictions',
                'sentiment_analysis',
                'daily_stock_data'
            ]

            for collection_name in analytical_collections:
                try:
                    logger.info(f"Migrating {collection_name}...")
                    source_collection = source_db[collection_name]
                    target_collection = target_db[collection_name]

                    documents = list(source_collection.find())
                    logger.info(f"Found {len(documents)} documents in {collection_name}")

                    if len(documents) > 0:
                        # Remove MongoDB internal IDs
                        for doc in documents:
                            doc.pop('_id', None)

                        target_collection.insert_many(documents)
                        self.migration_stats[collection_name] = len(documents)

                except Exception as e:
                    logger.error(f"Error migrating {collection_name}: {e}")
                    self.migration_stats['errors'].append(f"{collection_name}: {str(e)}")

            logger.info("✓ Analytical data migration complete")

        except Exception as e:
            logger.error(f"Analytical data migration error: {e}")
            self.migration_stats['errors'].append(f"Analytical data: {str(e)}")

    async def calculate_account_balances(self):
        """Calculate account_balances from current holdings and trades"""
        try:
            logger.info("Calculating account balances...")

            with self.target_postgres.cursor() as cur:
                # Get all users by ID (BIGINT)
                cur.execute("SELECT id FROM users")
                users = cur.fetchall()

                for (user_id,) in users:
                    try:
                        # Get total from trades
                        cur.execute("""
                            SELECT
                                COALESCE(SUM(CASE WHEN side = 'BUY' THEN -total_amount ELSE total_amount END), 0) as net_cash_flow
                            FROM trades
                            WHERE user_id = %s AND status = 'EXECUTED'
                        """, (user_id,))
                        net_cash_flow = float(cur.fetchone()[0])

                        # Get current value from holdings
                        cur.execute("""
                            SELECT COALESCE(SUM(current_value), 0)
                            FROM stock_holdings
                            WHERE user_id = %s
                        """, (user_id,))
                        holdings_value = float(cur.fetchone()[0])

                        # Initial account value (assumption: 10,000,000 KRW)
                        initial_balance = 10000000.0
                        cash_amount = initial_balance + net_cash_flow
                        total_value = cash_amount + holdings_value

                        cur.execute("""
                            INSERT INTO account_balances
                            (user_id, cash, total_value, locked_cash, version, updated_at)
                            VALUES (%s, %s, %s, %s, %s, %s)
                            ON CONFLICT (user_id) DO UPDATE SET
                                cash = EXCLUDED.cash,
                                total_value = EXCLUDED.total_value,
                                version = account_balances.version + 1,
                                updated_at = EXCLUDED.updated_at
                        """, (user_id, cash_amount, total_value, 0, 1, datetime.now()))

                    except Exception as e:
                        logger.error(f"Error calculating balance for user {user_id}: {e}")
                        self.migration_stats['errors'].append(f"Balance calc {user_id}: {str(e)}")

                self.target_postgres.commit()

            logger.info("✓ Account balances calculated")

        except Exception as e:
            logger.error(f"Account balance calculation error: {e}")
            self.migration_stats['errors'].append(f"Balance calculation: {str(e)}")

    def print_stats(self):
        """Print migration statistics"""
        logger.info("\n" + "="*60)
        logger.info("MIGRATION SUMMARY")
        logger.info("="*60)
        logger.info(f"Users migrated: {self.migration_stats['users']}")
        logger.info(f"Stocks migrated: {self.migration_stats['stocks']}")
        logger.info(f"Holdings migrated: {self.migration_stats['stock_holdings']}")
        logger.info(f"Trading configs: {self.migration_stats['trading_configs']}")
        logger.info(f"Trades migrated: {self.migration_stats['trades']}")
        logger.info(f"Stock recommendations: {self.migration_stats.get('stock_recommendations', 0)}")
        logger.info(f"Stock predictions: {self.migration_stats.get('stock_predictions', 0)}")

        if self.migration_stats['errors']:
            logger.warning(f"\nErrors encountered: {len(self.migration_stats['errors'])}")
            for error in self.migration_stats['errors']:
                logger.warning(f"  - {error}")

        logger.info("="*60 + "\n")

    async def run(self):
        """Execute full migration"""
        try:
            await self.connect()

            logger.info("="*60)
            logger.info("Starting Data Migration: stock-trading → quantiq")
            logger.info("="*60 + "\n")

            # Phase 2: Migrate user data
            await self.migrate_users()
            await self.migrate_trading_configs()

            # Phase 3: Migrate transaction data
            await self.migrate_stock_holdings()
            await self.migrate_trades()
            await self.calculate_account_balances()

            # Phase 4: Migrate analytical data
            await self.migrate_stocks()
            await self.migrate_analytical_data()

            self.print_stats()
            logger.info("✓ Migration completed successfully!")

        except Exception as e:
            logger.error(f"Migration failed: {e}")
            self.print_stats()
            raise
        finally:
            self.disconnect()


async def main():
    """Main entry point"""
    try:
        migrator = DataMigrator()
        await migrator.run()
        return 0
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        return 1


if __name__ == '__main__':
    sys.exit(asyncio.run(main()))
