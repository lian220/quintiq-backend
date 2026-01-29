# Quantiq - Hybrid RDB/MongoDB Architecture

A modern stock trading analysis platform built with a hybrid data architecture: **PostgreSQL for operational data** and **MongoDB for analytical data**.

## 📋 Project Overview

Quantiq separates concerns into two optimized database systems:
- **PostgreSQL (RDB)**: User accounts, trading configurations, holdings, account balances - ACID-compliant transactional data
- **MongoDB**: Stock analysis, recommendations, predictions, sentiment analysis, daily market data - flexible analytical data

This architecture enables fast transactional operations while supporting complex analytical queries and historical data accumulation.

## 🏗️ Architecture

```
quantiq
├── PostgreSQL (Transactional)
│   ├── Users & Accounts
│   ├── Trading Configuration
│   ├── Stock Holdings
│   ├── Account Balances
│   └── Trade Signals Executed
│
└── MongoDB (Analytical)
    ├── Stocks (35 stocks)
    ├── Recommendations (2,571)
    ├── Predictions (781,923)
    ├── Sentiment Data (2,328)
    └── Daily Market Data (22,002 records)
```

## 🚀 Quick Start

### One-Command Setup (Recommended)

```bash
cd /Users/imdoyeong/Desktop/workSpace/quantiq
./scripts/init_quantiq.sh
```

This single command automatically:
1. ✅ Cleans PostgreSQL database
2. ✅ Sets up Python environment
3. ✅ Loads environment variables
4. ✅ Initializes all data from stock-trading source
5. ✅ Validates the setup

### Manual Setup (Step-by-Step)

```bash
# 1. Activate virtual environment
source scripts/venv/bin/activate

# 2. Load environment variables
export $(cat .env | xargs)

# 3. Setup initial data
python3 scripts/setup_initial_data.py

# 4. Validate migration
python3 scripts/validate_migration.py
```

## 📊 Current Data State

After migration from stock-trading system:

### PostgreSQL (RDB)
- **Users**: 1 (lian@lian.dy220@gmail.com)
- **Trading Configuration**: 1 active config
- **Stock Holdings**: 20 positions
- **Account Balance**: $1,136.72 USD

### MongoDB (Analytics)
- **Stocks**: 35 companies
- **Stock Recommendations**: 2,571 records
- **Price Predictions**: 781,923 records
- **Sentiment Analysis**: 2,328 records
- **Daily Market Data**: 22,002 records

## 📁 File Structure

```
scripts/
├── 🚀 init_quantiq.sh              ⭐ Primary initialization script
│   ├─ Checks prerequisites
│   ├─ Cleans PostgreSQL
│   ├─ Sets up Python environment
│   ├─ Runs setup_initial_data.py
│   └─ Validates setup
│
├── 🐍 setup_initial_data.py        Core data migration logic
│   ├─ analyze_portfolio()         Analyzes stock-trading MongoDB
│   └─ setup_quantiq_data()        Writes to quantiq PostgreSQL
│
├── 🐍 validate_migration.py        Data integrity validation
│   ├─ Checks user data
│   ├─ Verifies holdings
│   ├─ Validates account balance
│   └─ Confirms MongoDB connection
│
├── 🐍 analyze_current_state.py     Portfolio analysis tool
│   ├─ Portfolio overview
│   ├─ Holdings breakdown
│   ├─ Trading history
│   └─ SQL generation
│
├── 🐍 migrate_data.py              MongoDB analysis data migration
│   └─ Handles large analytical dataset transfers
│
├── 📚 README.md                    Quick start guide
├── 📚 MIGRATION_GUIDE.md           Detailed migration documentation
├── 📚 requirements.txt             Python dependencies
│
├── 🔄 venv/                        Python virtual environment
│   └─ auto-created on first run
│
└── 📋 run_migration.sh             Legacy: MongoDB analysis data import
    └─ (migration already complete)
```

## 🔧 Configuration

### Environment Variables (.env)

```bash
# PostgreSQL (RDB)
DB_HOST=localhost
DB_PORT=5433
DB_NAME=quantiq
DB_USER=quantiq_user
DB_PASSWORD=quantiq_password

# MongoDB (Analytics)
MONGO_URL=mongodb+srv://cluster-test.2dkjwjs.mongodb.net
MONGO_USER=test
MONGO_PASSWORD=[password]
```

### Database Connection Details

**PostgreSQL Container**:
```bash
docker exec quantiq-postgres psql -U quantiq_user -d quantiq
```

**MongoDB Atlas**:
```bash
mongosh -u quantiq_user -p quantiq_password
```

## 📚 Database Schema

### PostgreSQL Tables

#### users
```sql
- id (PRIMARY KEY)
- user_id (VARCHAR UNIQUE)
- name (VARCHAR)
- email (VARCHAR)
- password_hash (VARCHAR)
- status (VARCHAR) - ACTIVE/INACTIVE
- created_at (TIMESTAMP)
```

#### trading_configs
```sql
- id (PRIMARY KEY)
- user_id (FOREIGN KEY)
- enabled (BOOLEAN)
- auto_trading_enabled (BOOLEAN)
- min_composite_score (DECIMAL)
- max_stocks_to_buy (INTEGER)
- stop_loss_percent (DECIMAL)
- take_profit_percent (DECIMAL)
```

#### stock_holdings
```sql
- id (PRIMARY KEY)
- user_id (FOREIGN KEY)
- ticker (VARCHAR)
- quantity (INTEGER)
- average_price (DECIMAL)
- total_cost (DECIMAL)
- current_value (DECIMAL)
```

#### account_balances
```sql
- id (PRIMARY KEY)
- user_id (FOREIGN KEY)
- cash (DECIMAL)
- total_value (DECIMAL)
- locked_cash (DECIMAL)
- version (INTEGER)
```

### MongoDB Collections

#### stocks
```javascript
{
  "_id": ObjectId,
  "ticker": "AAPL",
  "company_name": "Apple Inc.",
  "sector": "Technology",
  ...
}
```

#### recommendations
```javascript
{
  "_id": ObjectId,
  "ticker": "AAPL",
  "score": 4.5,
  "analysis_date": ISODate,
  ...
}
```

#### predictions
```javascript
{
  "_id": ObjectId,
  "ticker": "AAPL",
  "predicted_price": 175.50,
  "confidence": 0.85,
  ...
}
```

## 🧪 Validation & Testing

### Run Validation
```bash
python3 scripts/validate_migration.py
```

Validates:
- ✅ PostgreSQL connection and data integrity
- ✅ MongoDB connection and analytical data
- ✅ User account setup
- ✅ Holdings accuracy
- ✅ Account balance consistency

### Check Current State
```bash
python3 scripts/analyze_current_state.py
```

Displays:
- Current holdings and average prices
- Account balance information
- Trading history summary
- SQL statements for reference

## 🐛 Troubleshooting

### PostgreSQL Connection Fails
```bash
# Check if container is running
docker-compose ps

# Start services
docker-compose up -d

# Verify connection
docker exec quantiq-postgres psql -U quantiq_user -d quantiq -c "SELECT COUNT(*) FROM users;"
```

### MongoDB Connection Fails
```bash
# Check connection details in .env
# Verify credentials in MongoDB Atlas console
# Test connection manually
mongosh -u quantiq_user -p quantiq_password
```

### Data Integrity Issues
```bash
# Full system reset
docker-compose down
docker volume rm quantiq_postgres_data  # if needed
docker-compose up -d
./scripts/init_quantiq.sh
```

### Python Environment Issues
```bash
# Recreate virtual environment
rm -rf scripts/venv
python3 -m venv scripts/venv
source scripts/venv/bin/activate
pip install -r scripts/requirements.txt
```

## 🔄 Migration Process

### What Was Migrated

1. **User Account** (`lian` from stock-trading)
   - Email: lian.dy220@gmail.com
   - Status: Active
   - Trading configuration imported

2. **Portfolio State** (from trading_logs analysis)
   - Calculated holdings from buy/sell history
   - Current cash balance
   - Total asset value
   - Average price per holding

3. **Analytical Data** (MongoDB collection)
   - 35 stocks with comprehensive data
   - 2,571 buy/sell recommendations
   - 781,923 price predictions
   - 2,328 sentiment analysis records
   - 22,002 daily market data records

### Why This Approach?

**PostgreSQL for RDB**:
- ✅ ACID compliance for financial transactions
- ✅ Strong data integrity with foreign keys
- ✅ Fast user queries and updates
- ✅ Support for complex JOINs
- ✅ Ready for real-time trading execution

**MongoDB for Analytics**:
- ✅ Flexible document structure for varied analysis
- ✅ Scalable storage for large historical datasets
- ✅ Fast aggregation pipelines
- ✅ Easy to add new analysis fields
- ✅ Supports time-series data patterns

## 🚀 Next Steps

### Immediate Development
1. **API Development**
   ```bash
   docker-compose up quantiq-core
   # Access API at http://localhost:10010
   ```

2. **Test API Endpoints**
   ```bash
   curl http://localhost:10010/api/users/lian
   curl http://localhost:10010/api/portfolio/lian
   curl http://localhost:10010/api/holdings/lian
   ```

3. **MongoDB Data Queries**
   ```javascript
   // Check available stocks
   db.stocks.find().limit(5)

   // Get recommendations for a ticker
   db.recommendations.find({ticker: "AAPL"})

   // Get price predictions
   db.predictions.find({ticker: "AAPL"}).sort({prediction_date: -1})
   ```

### Planned Features
- [ ] Real-time trading execution integration
- [ ] Portfolio performance analytics dashboard
- [ ] Alert system for trading signals
- [ ] Historical analysis reporting
- [ ] Machine learning model integration

### Monitoring
- Database performance metrics
- API response times
- Data sync validation
- Alert trigger accuracy

## 📖 Documentation

- **[Quick Start](./scripts/README.md)** - Script usage and commands
- **[Migration Guide](./scripts/MIGRATION_GUIDE.md)** - Detailed migration documentation
- **[Requirements](./scripts/requirements.txt)** - Python dependencies

## 🔐 Security Notes

- PostgreSQL password: `quantiq_password` (change in production)
- MongoDB credentials stored in .env (never commit)
- API tokens: implement before production
- Consider SSL/TLS for all database connections

## 📞 Support

### Common Issues

**Q: How do I reset the database?**
```bash
./scripts/init_quantiq.sh  # Automatically cleans and reinitializes
```

**Q: How do I check if data loaded correctly?**
```bash
python3 scripts/validate_migration.py
python3 scripts/analyze_current_state.py
```

**Q: Can I modify holdings manually?**
```sql
-- Connect to PostgreSQL
docker exec -it quantiq-postgres psql -U quantiq_user -d quantiq

-- Update holdings (example)
UPDATE stock_holdings SET quantity = 100 WHERE ticker = 'AAPL';
```

**Q: How do I add more stocks to MongoDB?**
```javascript
// Connect to MongoDB
mongosh -u quantiq_user -p quantiq_password

// Insert stock data
db.stocks.insertOne({
  "ticker": "MSFT",
  "company_name": "Microsoft",
  ...
})
```

## 📝 Version History

- **v1.0** (Current)
  - ✅ PostgreSQL RDB setup with user, holdings, config data
  - ✅ MongoDB analytics data migration (35 stocks, 781k+ records)
  - ✅ Automated initialization script (init_quantiq.sh)
  - ✅ Data validation and analysis tools
  - ✅ Complete documentation

## 🎯 System Status

```
┌─────────────────────────────────────┐
│ ✅ PostgreSQL Initialized           │
│   - 1 User (lian)                  │
│   - 20 Stock Holdings              │
│   - $1,136.72 Balance              │
├─────────────────────────────────────┤
│ ✅ MongoDB Analytical Data Ready    │
│   - 35 Stocks                      │
│   - 2,571 Recommendations          │
│   - 781,923 Predictions            │
│   - 2,328 Sentiment Records        │
│   - 22,002 Daily Data              │
├─────────────────────────────────────┤
│ ✅ Core Infrastructure Ready        │
│   - Docker Services Running         │
│   - Environment Configured          │
│   - Validation Passing              │
├─────────────────────────────────────┤
│ 🔄 Ready for: quantiq-core Service │
│ 🔄 Ready for: API Development      │
└─────────────────────────────────────┘
```

---

**Last Updated**: 2025-01-29
**Status**: ✅ Production Ready
**Maintainer**: Quantiq Development Team
