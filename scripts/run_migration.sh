#!/bin/bash

# Migration Runner Script
# Usage: ./run_migration.sh [--dry-run] [--verbose]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "Data Migration: stock-trading → quantiq"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if .env exists in project root
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo -e "${RED}✗ Error: .env file not found at $PROJECT_ROOT/.env${NC}"
    exit 1
fi

echo "📁 Project root: $PROJECT_ROOT"
echo "📁 Script directory: $SCRIPT_DIR"
echo ""

# Create virtual environment if it doesn't exist
if [ ! -d "$SCRIPT_DIR/venv" ]; then
    echo -e "${YELLOW}📦 Creating virtual environment...${NC}"
    python3 -m venv "$SCRIPT_DIR/venv"
fi

# Activate virtual environment
echo -e "${YELLOW}🔧 Activating virtual environment...${NC}"
source "$SCRIPT_DIR/venv/bin/activate"

# Install/upgrade dependencies
echo -e "${YELLOW}📥 Installing dependencies...${NC}"
pip install -q --upgrade pip
pip install -q -r "$SCRIPT_DIR/requirements.txt"

# Load environment variables
echo -e "${YELLOW}🔐 Loading environment variables...${NC}"
cd "$PROJECT_ROOT"
set -a
source .env
set +a

# Run migration
echo ""
echo -e "${YELLOW}🚀 Starting migration...${NC}"
echo ""

cd "$SCRIPT_DIR"
python3 migrate_data.py

# Check result
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Migration completed successfully!${NC}"
    echo ""
    echo -e "${GREEN}📊 Check the migration log for details:${NC}"
    ls -lh migration_*.log | tail -1
    exit 0
else
    echo ""
    echo -e "${RED}✗ Migration failed!${NC}"
    exit 1
fi
