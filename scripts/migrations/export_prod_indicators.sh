#!/bin/bash

###############################################################################
# 프로덕션 MongoDB 경제 지표 데이터 추출 스크립트
#
# 사용법:
#   1. 환경변수 설정:
#      export PROD_MONGODB_URI="mongodb+srv://user:password@cluster.mongodb.net/stock_trading"
#
#   2. 스크립트 실행:
#      ./export_prod_indicators.sh
#
# 출력:
#   - prod_fred_indicators.json
#   - prod_yfinance_indicators.json
#
# 작성일: 2026-02-01
###############################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 프로덕션 MongoDB URI 확인
if [ -z "$PROD_MONGODB_URI" ]; then
    echo -e "${RED}❌ PROD_MONGODB_URI 환경변수가 설정되지 않았습니다.${NC}"
    echo ""
    echo "사용법:"
    echo "  export PROD_MONGODB_URI='mongodb+srv://user:password@cluster.mongodb.net/stock_trading'"
    echo "  ./export_prod_indicators.sh"
    exit 1
fi

echo "======================================================================"
echo "프로덕션 MongoDB 경제 지표 데이터 추출"
echo "======================================================================"
echo ""

# 출력 디렉토리 생성
OUTPUT_DIR="./prod_exports"
mkdir -p "$OUTPUT_DIR"

echo -e "${YELLOW}📥 FRED 지표 추출 중...${NC}"
mongosh "$PROD_MONGODB_URI" --quiet --eval "
    db.fred_indicators.find().forEach(doc => {
        printjson(doc);
    });
" > "$OUTPUT_DIR/prod_fred_indicators.json" 2>&1

if [ $? -eq 0 ]; then
    FRED_COUNT=$(grep -c "_id" "$OUTPUT_DIR/prod_fred_indicators.json" || echo 0)
    echo -e "${GREEN}✅ FRED 지표 ${FRED_COUNT}건 추출 완료${NC}"
else
    echo -e "${RED}❌ FRED 지표 추출 실패${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}📥 Yahoo Finance 지표 추출 중...${NC}"
mongosh "$PROD_MONGODB_URI" --quiet --eval "
    db.yfinance_indicators.find().forEach(doc => {
        printjson(doc);
    });
" > "$OUTPUT_DIR/prod_yfinance_indicators.json" 2>&1

if [ $? -eq 0 ]; then
    YAHOO_COUNT=$(grep -c "_id" "$OUTPUT_DIR/prod_yfinance_indicators.json" || echo 0)
    echo -e "${GREEN}✅ Yahoo Finance 지표 ${YAHOO_COUNT}건 추출 완료${NC}"
else
    echo -e "${RED}❌ Yahoo Finance 지표 추출 실패${NC}"
    exit 1
fi

echo ""
echo "======================================================================"
echo "추출 완료"
echo "======================================================================"
echo "출력 디렉토리: $OUTPUT_DIR"
echo "  - prod_fred_indicators.json (${FRED_COUNT}건)"
echo "  - prod_yfinance_indicators.json (${YAHOO_COUNT}건)"
echo ""
echo "다음 단계:"
echo "  1. 추출된 JSON 파일 검토"
echo "  2. 로컬/스테이징 환경에서 마이그레이션 스크립트 실행"
echo "  3. 데이터 검증 후 프로덕션 적용"
echo "======================================================================"
