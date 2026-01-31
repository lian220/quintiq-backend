#!/bin/bash

# Quantiq Stop Script
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo -e "${BLUE}=========================================="
echo " 🛑 Quantiq Stop"
echo "==========================================${NC}"
echo ""

cd "$PROJECT_ROOT"

# Check for --clean flag
if [ "$1" == "--clean" ]; then
    echo -e "${RED}⚠️  Stopping and removing all containers, networks, and volumes...${NC}"
    docker compose down -v
    echo -e "${GREEN}✅ All containers, networks, and volumes removed!${NC}"
else
    # Stop all services (including kafka-ui)
    echo -e "${YELLOW}📦 Stopping all services...${NC}"
    docker compose stop

    echo ""
    echo -e "${GREEN}=========================================="
    echo "✅ All services stopped!"
    echo "==========================================${NC}"
    echo ""
    echo -e "${YELLOW}💡 Tip: Use './stop.sh --clean' to remove containers and volumes${NC}"
fi

echo ""
