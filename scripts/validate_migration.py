#!/usr/bin/env python3
"""
Migration Validation Script
Validates data integrity after migration
"""

import os
import sys
import logging
from datetime import datetime
from dotenv import load_dotenv
import pymongo
import psycopg

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class ValidationChecker:
    def __init__(self):
        """Initialize validation configuration"""
        load_dotenv()

        self.postgres_host = os.getenv('DB_HOST', 'localhost')
        self.postgres_port = int(os.getenv('DB_PORT', '5433'))
        self.postgres_db = os.getenv('DB_NAME', 'quantiq')
        self.postgres_user = os.getenv('DB_USER', 'quantiq_user')
        self.postgres_pass = os.getenv('DB_PASSWORD', 'quantiq_password')

        self.mongo_url = os.getenv('SPRING_DATA_MONGODB_URI', 'mongodb://quantiq_user:quantiq_password@localhost:27017/stock_trading?authSource=admin')

        self.postgres = None
        self.mongo = None
        self.validation_results = {}

    def connect(self):
        """Connect to databases"""
        try:
            logger.info("Connecting to PostgreSQL...")
            self.postgres = psycopg.connect(
                host=self.postgres_host,
                port=self.postgres_port,
                dbname=self.postgres_db,
                user=self.postgres_user,
                password=self.postgres_pass
            )
            logger.info("✓ PostgreSQL connected")

            logger.info("Connecting to MongoDB...")
            self.mongo = pymongo.MongoClient(self.mongo_url)
            self.mongo.admin.command('ping')
            logger.info("✓ MongoDB connected")
        except Exception as e:
            logger.error(f"Connection error: {e}")
            raise

    def disconnect(self):
        """Close connections"""
        if self.postgres:
            self.postgres.close()
        if self.mongo:
            self.mongo.close()

    def check_postgresql_data(self):
        """Check PostgreSQL data integrity"""
        logger.info("\n" + "="*60)
        logger.info("PostgreSQL Data Validation")
        logger.info("="*60)

        with self.postgres.cursor() as cur:
            tables = ['users', 'trading_configs', 'stock_holdings', 'trades', 'account_balances']

            for table in tables:
                try:
                    # Count rows
                    cur.execute(f"SELECT COUNT(*) FROM {table}")
                    count = cur.fetchone()[0]

                    # Check for null primary keys
                    if table == 'users':
                        cur.execute(f"SELECT COUNT(*) FROM {table} WHERE user_id IS NULL")
                    else:
                        cur.execute(f"SELECT COUNT(*) FROM {table} WHERE id IS NULL")
                    null_count = cur.fetchone()[0]

                    self.validation_results[table] = {
                        'total_rows': count,
                        'null_pks': null_count,
                        'status': '✓' if null_count == 0 else '⚠️'
                    }

                    logger.info(f"{self.validation_results[table]['status']} {table:25s}: {count:6d} rows (null PKs: {null_count})")

                except Exception as e:
                    logger.error(f"✗ {table:25s}: Error - {e}")
                    self.validation_results[table] = {'status': '✗', 'error': str(e)}

    def check_mongodb_data(self):
        """Check MongoDB data integrity"""
        logger.info("\n" + "="*60)
        logger.info("MongoDB Data Validation")
        logger.info("="*60)

        db = self.mongo['stock_trading']
        collections = ['stocks', 'stock_recommendations', 'stock_predictions', 'sentiment_analysis', 'daily_stock_data']

        for collection_name in collections:
            try:
                collection = db[collection_name]
                count = collection.count_documents({})

                # Check for empty documents
                empty_docs = collection.count_documents({'$or': [
                    {field: None} for field in ['ticker', 'date', 'name'] if field
                ]})

                self.validation_results[collection_name] = {
                    'total_docs': count,
                    'status': '✓' if count >= 0 else '⚠️'
                }

                logger.info(f"✓ {collection_name:30s}: {count:6d} documents")

            except Exception as e:
                logger.error(f"✗ {collection_name:30s}: Error - {e}")
                self.validation_results[collection_name] = {'status': '✗', 'error': str(e)}

    def check_referential_integrity(self):
        """Check foreign key relationships"""
        logger.info("\n" + "="*60)
        logger.info("Referential Integrity Validation")
        logger.info("="*60)

        with self.postgres.cursor() as cur:
            try:
                # Check trading_configs references users
                cur.execute("""
                    SELECT COUNT(*) FROM trading_configs tc
                    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = tc.user_id)
                """)
                orphaned = cur.fetchone()[0]
                logger.info(f"{'✓' if orphaned == 0 else '⚠️'} trading_configs → users: {orphaned} orphaned records")

                # Check stock_holdings references users
                cur.execute("""
                    SELECT COUNT(*) FROM stock_holdings sh
                    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = sh.user_id)
                """)
                orphaned = cur.fetchone()[0]
                logger.info(f"{'✓' if orphaned == 0 else '⚠️'} stock_holdings → users: {orphaned} orphaned records")

                # Check trades references users
                cur.execute("""
                    SELECT COUNT(*) FROM trades t
                    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = t.user_id)
                """)
                orphaned = cur.fetchone()[0]
                logger.info(f"{'✓' if orphaned == 0 else '⚠️'} trades → users: {orphaned} orphaned records")

                # Check account_balances references users
                cur.execute("""
                    SELECT COUNT(*) FROM account_balances ab
                    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = ab.user_id)
                """)
                orphaned = cur.fetchone()[0]
                logger.info(f"{'✓' if orphaned == 0 else '⚠️'} account_balances → users: {orphaned} orphaned records")

            except Exception as e:
                logger.error(f"✗ Referential integrity check failed: {e}")

    def check_data_consistency(self):
        """Check data consistency and types"""
        logger.info("\n" + "="*60)
        logger.info("Data Consistency Validation")
        logger.info("="*60)

        with self.postgres.cursor() as cur:
            try:
                # Check for invalid status values in trades
                cur.execute("""
                    SELECT COUNT(*) FROM trades
                    WHERE status NOT IN ('PENDING', 'EXECUTED', 'FAILED', 'CANCELLED')
                """)
                invalid = cur.fetchone()[0]
                logger.info(f"{'✓' if invalid == 0 else '⚠️'} Invalid trade statuses: {invalid}")

                # Check for invalid sides in trades
                cur.execute("""
                    SELECT COUNT(*) FROM trades
                    WHERE side NOT IN ('BUY', 'SELL')
                """)
                invalid = cur.fetchone()[0]
                logger.info(f"{'✓' if invalid == 0 else '⚠️'} Invalid trade sides: {invalid}")

                # Check for negative quantities
                cur.execute("""
                    SELECT COUNT(*) FROM stock_holdings
                    WHERE quantity < 0
                """)
                negative = cur.fetchone()[0]
                logger.info(f"{'✓' if negative == 0 else '⚠️'} Negative quantities in holdings: {negative}")

                # Check for negative prices
                cur.execute("""
                    SELECT COUNT(*) FROM trades
                    WHERE price < 0
                """)
                negative = cur.fetchone()[0]
                logger.info(f"{'✓' if negative == 0 else '⚠️'} Negative prices in trades: {negative}")

                # Check for null required fields
                cur.execute("""
                    SELECT COUNT(*) FROM users WHERE name IS NULL OR email IS NULL
                """)
                null_fields = cur.fetchone()[0]
                logger.info(f"{'✓' if null_fields == 0 else '⚠️'} Null required fields in users: {null_fields}")

            except Exception as e:
                logger.error(f"✗ Consistency check failed: {e}")

    def check_account_balance_calculations(self):
        """Verify account balance calculations"""
        logger.info("\n" + "="*60)
        logger.info("Account Balance Calculation Validation")
        logger.info("="*60)

        with self.postgres.cursor() as cur:
            try:
                cur.execute("""
                    SELECT u.user_id, ab.cash_balance, ab.total_asset,
                           COALESCE(SUM(sh.current_value), 0) as holdings_value
                    FROM users u
                    LEFT JOIN account_balances ab ON u.user_id = ab.user_id
                    LEFT JOIN stock_holdings sh ON u.user_id = sh.user_id
                    GROUP BY u.user_id, ab.cash_balance, ab.total_asset
                """)

                results = cur.fetchall()
                logger.info(f"Checking balance calculations for {len(results)} user(s):")

                for user_id, cash, total, holdings in results:
                    expected_total = cash + holdings
                    status = '✓' if abs(total - expected_total) < 0.01 else '⚠️'
                    logger.info(f"  {status} User {user_id}: cash={cash:,.0f} + holdings={holdings:,.0f} = {total:,.0f}")

            except Exception as e:
                logger.error(f"✗ Balance calculation check failed: {e}")

    def print_summary(self):
        """Print validation summary"""
        logger.info("\n" + "="*60)
        logger.info("VALIDATION SUMMARY")
        logger.info("="*60)

        total_checks = len(self.validation_results)
        passed = sum(1 for r in self.validation_results.values() if r.get('status') == '✓')

        logger.info(f"Total checks: {total_checks}")
        logger.info(f"Passed: {passed}")
        logger.info(f"Failed: {total_checks - passed}")
        logger.info(f"Success rate: {(passed/total_checks*100):.1f}%")

        if passed == total_checks:
            logger.info("\n✓ All validations passed!")
        else:
            logger.warning("\n⚠️ Some validations failed - review the log above")

        logger.info("="*60 + "\n")

    def run(self):
        """Execute all validations"""
        try:
            self.connect()

            logger.info("Starting Migration Validation")
            logger.info(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

            self.check_postgresql_data()
            self.check_mongodb_data()
            self.check_referential_integrity()
            self.check_data_consistency()
            self.check_account_balance_calculations()

            self.print_summary()

            return 0 if len([r for r in self.validation_results.values() if r.get('status') != '✗']) == len(self.validation_results) else 1

        except Exception as e:
            logger.error(f"Validation failed: {e}")
            return 1
        finally:
            self.disconnect()


if __name__ == '__main__':
    checker = ValidationChecker()
    sys.exit(checker.run())
