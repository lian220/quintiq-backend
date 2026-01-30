#!/bin/bash

# Quantiq Stop Script
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}=========================================="
echo " 🛑 Quantiq Stop"
echo "==========================================${NC}"
echo ""

cd "$PROJECT_ROOT"

# Stop all services
echo -e "${YELLOW}📦 Stopping all services...${NC}"
docker-compose stop

echo ""
echo -e "${GREEN}=========================================="
echo "✅ Quantiq Stopped!"
echo "==========================================${NC}"
echo ""
