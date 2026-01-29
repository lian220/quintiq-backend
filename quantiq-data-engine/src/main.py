import logging
import json
import time
import threading
from fastapi import FastAPI
import uvicorn
from confluent_kafka import Consumer, KafkaError
from src.config import settings
from src.db import MongoDB
from src.services.data_collector import collect_economic_data
from src.services.technical_analysis import TechnicalAnalysisService
from src.services.sentiment_analysis import SentimentAnalysisService
from src.events.publisher import publish_event

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# FastAPI app for status/health checks
app = FastAPI(title="Quantiq Data Engine")

@app.get("/")
def read_root():
    return {"status": "Quantiq Data Engine is running", "kafka_topic": "quantiq.analysis.request"}

@app.get("/health")
def health_check():
    return {"status": "alive"}

def run_api():
    logger.info("Starting Data Engine API server on port 8000")
    uvicorn.run(app, host="0.0.0.0", port=8000)

def main():
    logger.info("Quantiq Data Engine Started")
    
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
        'group.id': 'quantiq-data-engine-group',
        'auto.offset.reset': 'earliest'
    }
    
    # Wait for Kafka to be ready
    time.sleep(10) 
    
    consumer = Consumer(conf)
    topic = "quantiq.analysis.request"
    consumer.subscribe([topic])
    logger.info(f"Subscribed to {topic}")

    technical_service = TechnicalAnalysisService()
    sentiment_service = SentimentAnalysisService()

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
                payload = json.loads(msg.value().decode('utf-8'))
                logger.info(f"Received request: {payload}")
                
                request_type = payload.get("type", "ALL")
                
                if request_type in ["ALL", "TECHNICAL"]:
                    logger.info("Starting Technical Analysis...")
                    collect_economic_data()
                    technical_service.analyze_stocks()
                    publish_event("TECHNICAL_COMPLETED", {"status": "success"})
                    
                if request_type in ["ALL", "SENTIMENT"]:
                    logger.info("Starting Sentiment Analysis...")
                    sentiment_service.fetch_and_store_sentiment()
                    publish_event("SENTIMENT_COMPLETED", {"status": "success"})
                    
                # Signal global completion
                publish_event("ANALYSIS_COMPLETED", {"type": request_type, "status": "success"})
                
            except Exception as e:
                logger.error(f"Error processing message: {e}")
                
    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()

if __name__ == "__main__":
    main()
