#!/bin/bash

echo "======================================"
echo "🚀 Vertex AI Job 테스트 시작"
echo "======================================"

# Spring Boot 실행 확인
echo ""
echo "1️⃣ Spring Boot 실행 확인..."
if ! pgrep -f "quantiq-core" > /dev/null; then
    echo "❌ Spring Boot가 실행되지 않았습니다."
    echo "   다음 명령으로 실행하세요:"
    echo "   cd quantiq-core && ./gradlew bootRun"
    exit 1
fi
echo "✅ Spring Boot 실행 중"

# API 호출
echo ""
echo "2️⃣ Vertex AI Job 실행..."
echo "   API 엔드포인트: POST http://localhost:8080/api/v1/vertex-ai/run"

RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/vertex-ai/run \
  -H "Content-Type: application/json" \
  2>&1)

echo ""
echo "📥 응답:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"

# Job ID 추출
JOB_ID=$(echo "$RESPONSE" | jq -r '.jobId' 2>/dev/null)

if [ -z "$JOB_ID" ] || [ "$JOB_ID" = "null" ]; then
    echo ""
    echo "❌ Job 실행 실패"
    exit 1
fi

echo ""
echo "✅ Job 실행 성공!"
echo "   Job ID: $JOB_ID"

# 로그 확인 안내
echo ""
echo "======================================"
echo "📋 다음 단계:"
echo "======================================"
echo "1. Vertex AI Console에서 Job 로그 확인:"
echo "   https://console.cloud.google.com/vertex-ai/training/custom-jobs"
echo ""
echo "2. Spring Boot 로그 확인:"
echo "   tail -f quantiq-core/logs/application.log"
echo ""
echo "3. Job 상태 확인 API:"
echo "   curl http://localhost:8080/api/v1/vertex-ai/status/$JOB_ID | jq"
echo ""
echo "======================================"

