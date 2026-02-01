#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: ./monitor_vertex_ai.sh <JOB_ID>"
    echo "Example: ./monitor_vertex_ai.sh projects/123/locations/us-central1/customJobs/456"
    exit 1
fi

JOB_ID="$1"

echo "======================================"
echo "📊 Vertex AI Job 모니터링"
echo "======================================"
echo "Job ID: $JOB_ID"
echo ""

# Job 상태 체크
check_status() {
    RESPONSE=$(curl -s "http://localhost:8080/api/v1/vertex-ai/status?jobId=$JOB_ID")
    STATE=$(echo "$RESPONSE" | jq -r '.state' 2>/dev/null)
    STATE_DESC=$(echo "$RESPONSE" | jq -r '.stateDescription' 2>/dev/null)
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 상태: $STATE ($STATE_DESC)"
    
    case "$STATE" in
        "JOB_STATE_SUCCEEDED")
            echo ""
            echo "✅ Job 성공!"
            return 0
            ;;
        "JOB_STATE_FAILED")
            echo ""
            echo "❌ Job 실패!"
            echo "로그 확인:"
            echo "https://console.cloud.google.com/vertex-ai/training/custom-jobs"
            return 1
            ;;
        "JOB_STATE_CANCELLED")
            echo ""
            echo "⚠️ Job 취소됨"
            return 1
            ;;
        *)
            return 2
            ;;
    esac
}

# 30초마다 상태 체크
while true; do
    check_status
    STATUS=$?
    
    if [ $STATUS -eq 0 ] || [ $STATUS -eq 1 ]; then
        break
    fi
    
    sleep 30
done

echo ""
echo "======================================"
echo "모니터링 종료"
echo "======================================"

