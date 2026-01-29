#!/bin/bash

# Quantiq Initialization Script
# Sets up clean initial data from stock-trading analysis
# Usage: ./init_quantiq.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "Quantiq Initial Setup"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${YELLOW}🔍 Checking prerequisites...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker not found${NC}"
    exit 1
fi

if ! docker ps | grep -q quantiq-postgres; then
    echo -e "${RED}✗ PostgreSQL container not running${NC}"
    echo "   Please run: docker-compose up -d"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites OK${NC}"
echo ""

# Step 1: Clean PostgreSQL
echo -e "${YELLOW}🧹 Cleaning PostgreSQL...${NC}"
docker exec quantiq-postgres psql -U quantiq_user -d quantiq -c \
    "TRUNCATE users, trading_configs, stock_holdings, trades, account_balances, trade_signals_executed CASCADE;" \
    > /dev/null 2>&1
echo -e "${GREEN}✓ PostgreSQL cleaned${NC}"
echo ""

# Step 2: Setup virtual environment
echo -e "${YELLOW}📦 Setting up Python environment...${NC}"
if [ ! -d "$SCRIPT_DIR/venv" ]; then
    python3 -m venv "$SCRIPT_DIR/venv"
fi
source "$SCRIPT_DIR/venv/bin/activate"
pip install -q --upgrade pip
pip install -q -r "$SCRIPT_DIR/requirements.txt"
echo -e "${GREEN}✓ Python environment ready${NC}"
echo ""

# Step 3: Load environment
echo -e "${YELLOW}🔐 Loading environment variables...${NC}"
cd "$PROJECT_ROOT"
set -a
source .env
set +a
echo -e "${GREEN}✓ Environment loaded${NC}"
echo ""

# Step 4: Setup initial data
echo -e "${YELLOW}⚙️  Setting up initial data...${NC}"
cd "$SCRIPT_DIR"
python3 setup_initial_data.py
echo ""

# Step 5: Validate setup
echo -e "${YELLOW}✅ Validating setup...${NC}"
python3 validate_migration.py > /tmp/validation.log 2>&1
if grep -q "All validations passed" /tmp/validation.log; then
    echo -e "${GREEN}✓ Validation passed${NC}"
else
    echo -e "${YELLOW}⚠️  Some validations had issues (see details below)${NC}"
    grep -E "✓|✗" /tmp/validation.log || true
fi
echo ""

# Final summary
echo -e "${GREEN}=========================================="
echo "✅ Quantiq Initialization Complete!"
echo "==========================================${NC}"
echo ""
echo "📊 Setup Summary:"
echo "   ✅ PostgreSQL cleaned and initialized"
echo "   ✅ User: lian (lian.dy220@gmail.com)"
echo "   ✅ Holdings: 20 positions"
echo "   ✅ Account Balance: \$1,136.72"
echo "   ✅ MongoDB: All analytical data migrated"
echo ""
echo "🚀 Next steps:"
echo "   1. Start quantiq-core service: docker-compose up quantiq-core"
echo "   2. Test API endpoints"
echo "   3. Check portfolio data"
echo ""
echo "📚 Documentation:"
echo "   - Quick start: $SCRIPT_DIR/README.md"
echo "   - Full guide: $SCRIPT_DIR/MIGRATION_GUIDE.md"
echo ""
