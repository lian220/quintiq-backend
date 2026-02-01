"""
Quantiq Data Engine - Message Processing Worker

Architecture:
- Primary: Kafka message processing (all data operations)
- Secondary: Read-only REST API (health checks, status queries)
"""
import logging
import json
import time
import threading
import subprocess
import sys
from pathlib import Path
from fastapi import FastAPI
import uvicorn
from confluent_kafka import Consumer, KafkaError
from datetime import datetime
from pytz import timezone

from src.core.config import settings
from src.core.database import MongoDB
from src.features.economic_data.router import router as economic_router
from src.features.ml_package.router import router as ml_package_router
from src.features.economic_data.service import EconomicDataService
from src.services.recommendation_service import RecommendationService
from src.services.slack_notifier import SlackNotifier
from src.core.kafka import KafkaEventPublisher

KST = timezone('Asia/Seoul')

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# FastAPI app (Read-Only Status API)
app = FastAPI(
    title="Quantiq Data Engine",
    description="Message Processing Worker with Read-Only Status API"
)

# Include routers (status endpoints only)
app.include_router(economic_router)
app.include_router(ml_package_router)


@app.get("/")
def read_root():
    return {
        "service": "Quantiq Data Engine",
        "architecture": "Message Processing Worker",
        "status": "running",
        "subscribed_kafka_topics": [
            "economic.data.update.request",
            "analysis.technical.request",
            "analysis.sentiment.request",
            "analysis.combined.request",
            "ml.package.upload.request"
        ],
        "api_purpose": "Read-only health checks and status queries",
        "timestamp": datetime.now(KST).isoformat()
    }


@app.get("/health")
def health_check():
    return {"status": "alive", "timestamp": datetime.now(KST).isoformat()}


def run_api():
    logger.info("Starting Data Engine API server on port 8000")
    uvicorn.run(app, host="0.0.0.0", port=8000)


def main():
    logger.info("Quantiq Data Engine Started (Feature-based Architecture)")

    # Start API server in a separate thread
    api_thread = threading.Thread(target=run_api, daemon=True)
    api_thread.start()

    # 1. Start MongoDB connection
    db = MongoDB.get_db()
    if db is None:
        logger.error("Failed to connect to MongoDB")
        return

    # 2. Setup Kafka Consumer
    conf = {
        'bootstrap.servers': settings.KAFKA_BOOTSTRAP_SERVERS,
        'group.id': 'quantiq-data-engine-fresh',  # Fresh consumer group for clean start
        'auto.offset.reset': 'earliest'
    }

    # Wait for Kafka to be ready
    time.sleep(10)

    consumer = Consumer(conf)

    # 토픽 구독 (경제 데이터 + 분석 요청 + ML 패키지)
    topics = [
        settings.KAFKA_TOPIC_ECONOMIC_DATA_UPDATE_REQUEST,
        "analysis.technical.request",
        "analysis.sentiment.request",
        "analysis.combined.request",
        "ml.package.upload.request"
    ]
    consumer.subscribe(topics)
    logger.info(f"Subscribed to topics: {topics}")

    # Services 초기화
    economic_service = EconomicDataService()
    recommendation_service = RecommendationService()

    try:
        while True:
            msg = consumer.poll(1.0)

            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    logger.error(f"Consumer error: {msg.error()}")
                    continue

            try:
                topic_name = msg.topic()
                message = json.loads(msg.value().decode('utf-8'))
                logger.info(f"Received request from topic '{topic_name}': {message}")

                # 경제 데이터 업데이트 요청 처리
                if topic_name == settings.KAFKA_TOPIC_ECONOMIC_DATA_UPDATE_REQUEST:
                    # payload 필드에서 실제 데이터 추출
                    payload = message.get("payload", message)
                    request_id = payload.get("requestId", "unknown")
                    source = payload.get("source", "kafka")
                    thread_ts = payload.get("threadTs")  # Kotlin에서 전달받은 스레드 타임스탬프
                    target_date = payload.get("targetDate")  # 수집할 기준 날짜 (YYYY-MM-DD)

                    logger.info("=" * 80)
                    logger.info("경제 데이터 업데이트 Kafka 메시지 수신")
                    logger.info(f"Request ID: {request_id}")
                    logger.info(f"Target Date: {target_date or '당일'}")
                    logger.info(f"Thread TS: {thread_ts}")
                    logger.info("=" * 80)

                    # 🔔 수집 시작 알림 (스레드 답글)
                    SlackNotifier.notify_economic_data_collection_start(request_id, source, thread_ts)

                    start_time = time.time()
                    try:
                        # Service 호출 (날짜 파라미터 전달)
                        result = economic_service.collect_economic_data(target_date=target_date)
                        elapsed_time = time.time() - start_time

                        logger.info("✅ 경제 데이터 수집 완료")

                        # 수집 결과 데이터 구성
                        collection_summary = {
                            "target_date": result.get("target_date"),
                            "duration": f"{elapsed_time:.2f}초",
                            "fred_collected": result.get("fred_collected", 0),
                            "yahoo_collected": result.get("yahoo_collected", 0),
                            "total_indicators": result.get("fred_collected", 0) + result.get("yahoo_collected", 0)
                        }

                        # 🔔 수집 완료 알림 (스레드 답글)
                        SlackNotifier.notify_economic_data_collection_success(
                            request_id,
                            collection_summary,
                            thread_ts
                        )

                        # 완료 이벤트 발행
                        KafkaEventPublisher.publish("ECONOMIC_DATA_UPDATED", {
                            "status": "success",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "duration": elapsed_time
                        })
                    except Exception as e:
                        logger.error(f"❌ 경제 데이터 수집 실패: {e}")

                        # 🔔 오류 알림 (스레드 답글)
                        SlackNotifier.notify_economic_data_collection_error(request_id, str(e), thread_ts)

                        # 오류 이벤트 발행
                        KafkaEventPublisher.publish("ECONOMIC_DATA_UPDATE_FAILED", {
                            "status": "failed",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "error": str(e)
                        })
                        raise

                # 기술적 분석 요청 처리
                elif topic_name == "analysis.technical.request":
                    payload = message.get("payload", message)
                    request_id = payload.get("requestId", "unknown")
                    thread_ts = payload.get("threadTs")  # Kotlin에서 전달받은 스레드 타임스탬프

                    logger.info("=" * 80)
                    logger.info("기술적 분석 요청 Kafka 메시지 수신")
                    logger.info(f"Request ID: {request_id}")
                    logger.info(f"Thread TS: {thread_ts}")
                    logger.info("=" * 80)

                    start_time = time.time()
                    try:
                        # Service 호출
                        result = recommendation_service.run_technical_analysis(request_id, thread_ts)
                        elapsed_time = time.time() - start_time

                        logger.info("✅ 기술적 분석 완료")

                        # 완료 이벤트 발행
                        KafkaEventPublisher.publish("ANALYSIS_TECHNICAL_COMPLETED", {
                            "status": "success",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "duration": elapsed_time,
                            "result": result
                        })
                    except Exception as e:
                        logger.error(f"❌ 기술적 분석 실패: {e}")
                        KafkaEventPublisher.publish("ANALYSIS_TECHNICAL_FAILED", {
                            "status": "failed",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "error": str(e)
                        })

                # 감정 분석 요청 처리
                elif topic_name == "analysis.sentiment.request":
                    payload = message.get("payload", message)
                    request_id = payload.get("requestId", "unknown")
                    thread_ts = payload.get("threadTs")

                    logger.info("=" * 80)
                    logger.info("뉴스 감정 분석 요청 Kafka 메시지 수신")
                    logger.info(f"Request ID: {request_id}")
                    logger.info(f"Thread TS: {thread_ts}")
                    logger.info("=" * 80)

                    start_time = time.time()
                    try:
                        result = recommendation_service.run_sentiment_analysis(request_id, thread_ts)
                        elapsed_time = time.time() - start_time

                        logger.info("✅ 뉴스 감정 분석 완료")

                        KafkaEventPublisher.publish("ANALYSIS_SENTIMENT_COMPLETED", {
                            "status": "success",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "duration": elapsed_time,
                            "result": result
                        })
                    except Exception as e:
                        logger.error(f"❌ 뉴스 감정 분석 실패: {e}")
                        KafkaEventPublisher.publish("ANALYSIS_SENTIMENT_FAILED", {
                            "status": "failed",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "error": str(e)
                        })

                # 통합 분석 요청 처리
                elif topic_name == "analysis.combined.request":
                    payload = message.get("payload", message)
                    request_id = payload.get("requestId", "unknown")
                    thread_ts = payload.get("threadTs")

                    logger.info("=" * 80)
                    logger.info("통합 분석 요청 Kafka 메시지 수신")
                    logger.info(f"Request ID: {request_id}")
                    logger.info(f"Thread TS: {thread_ts}")
                    logger.info("=" * 80)

                    start_time = time.time()
                    try:
                        result = recommendation_service.run_combined_analysis(request_id, thread_ts)
                        elapsed_time = time.time() - start_time

                        logger.info("✅ 통합 분석 완료")

                        KafkaEventPublisher.publish("ANALYSIS_COMPLETED", {
                            "status": "success",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "duration": elapsed_time,
                            "result": result
                        })
                    except Exception as e:
                        logger.error(f"❌ 통합 분석 실패: {e}")
                        KafkaEventPublisher.publish("ANALYSIS_FAILED", {
                            "status": "failed",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "error": str(e)
                        })

                # ML 패키지 업로드 요청 처리
                elif topic_name == "ml.package.upload.request":
                    payload = message.get("payload", message)
                    request_id = payload.get("requestId", "unknown")
                    thread_ts = payload.get("threadTs")
                    script_path = payload.get("scriptPath", "predict_optimized.py")

                    logger.info("=" * 80)
                    logger.info("ML 패키지 업로드 요청 Kafka 메시지 수신")
                    logger.info(f"Request ID: {request_id}")
                    logger.info(f"Script Path: {script_path}")
                    logger.info(f"Thread TS: {thread_ts}")
                    logger.info("=" * 80)

                    # 🔔 업로드 시작 알림
                    SlackNotifier.notify_ml_package_upload_start(request_id, thread_ts)

                    start_time = time.time()
                    try:
                        # upload_to_gcs.py 스크립트 경로
                        script_dir = Path(__file__).parent / "scripts" / "utils"
                        upload_script = script_dir / "upload_to_gcs.py"
                        predict_script = script_dir / script_path

                        if not upload_script.exists():
                            raise FileNotFoundError(f"업로드 스크립트를 찾을 수 없습니다: {upload_script}")
                        if not predict_script.exists():
                            raise FileNotFoundError(f"예측 스크립트를 찾을 수 없습니다: {predict_script}")

                        logger.info(f"업로드 스크립트: {upload_script}")
                        logger.info(f"예측 스크립트: {predict_script}")

                        # 스크립트 실행
                        logger.info("GCS 업로드 스크립트 실행 중...")
                        result = subprocess.run(
                            [sys.executable, str(upload_script), "--file", str(predict_script)],
                            capture_output=True,
                            text=True,
                            timeout=300  # 5분 타임아웃
                        )

                        # 로그 출력
                        if result.stdout:
                            logger.info("=== 스크립트 출력 ===")
                            for line in result.stdout.split('\n'):
                                if line.strip():
                                    logger.info(line)

                        if result.stderr:
                            logger.warning("=== 스크립트 경고/오류 ===")
                            for line in result.stderr.split('\n'):
                                if line.strip():
                                    logger.warning(line)

                        # 실행 결과 확인
                        if result.returncode != 0:
                            error_msg = f"스크립트 실행 실패 (exit code: {result.returncode})"
                            if result.stderr:
                                error_msg += f"\n{result.stderr}"
                            raise RuntimeError(error_msg)

                        # 성공 응답에서 GCS URI와 버전 추출
                        gcs_uri = None
                        version = None
                        combined_output = result.stdout + "\n" + result.stderr

                        for line in combined_output.split('\n'):
                            if "GCS URI:" in line:
                                gcs_uri = line.split("GCS URI:")[-1].strip()
                            if "패키지 버전:" in line or "새 버전:" in line:
                                try:
                                    version_str = line.split(":")[-1].strip().replace("v", "")
                                    version = int(version_str)
                                except (ValueError, IndexError):
                                    pass

                        elapsed_time = time.time() - start_time
                        logger.info("✅ ML 패키지 업로드 완료")

                        # 🔔 업로드 완료 알림
                        upload_summary = {
                            "gcs_uri": gcs_uri,
                            "version": f"v{version}" if version else "unknown",
                            "duration": f"{elapsed_time:.2f}초"
                        }
                        SlackNotifier.notify_ml_package_upload_success(request_id, upload_summary, thread_ts)

                        # 완료 이벤트 발행
                        KafkaEventPublisher.publish("ML_PACKAGE_UPLOADED", {
                            "status": "success",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "duration": elapsed_time,
                            "gcsUri": gcs_uri,
                            "version": version
                        })

                    except subprocess.TimeoutExpired:
                        error_msg = "스크립트 실행 타임아웃 (5분 초과)"
                        logger.error(f"❌ {error_msg}")
                        SlackNotifier.notify_ml_package_upload_error(request_id, error_msg, thread_ts)
                        KafkaEventPublisher.publish("ML_PACKAGE_UPLOAD_FAILED", {
                            "status": "failed",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "error": error_msg
                        })
                    except Exception as e:
                        logger.error(f"❌ ML 패키지 업로드 실패: {e}")
                        SlackNotifier.notify_ml_package_upload_error(request_id, str(e), thread_ts)
                        KafkaEventPublisher.publish("ML_PACKAGE_UPLOAD_FAILED", {
                            "status": "failed",
                            "timestamp": datetime.now(KST).isoformat(),
                            "requestId": request_id,
                            "error": str(e)
                        })

            except Exception as e:
                logger.error(f"Error processing message: {e}")

    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()


if __name__ == "__main__":
    main()
