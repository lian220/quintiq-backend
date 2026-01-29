from confluent_kafka import Producer
import json
import logging
from src.config import settings

logger = logging.getLogger(__name__)

class EventPublisher:
    _producer = None
    
    @classmethod
    def get_producer(cls):
        if cls._producer is None:
            try:
                conf = {
                    'bootstrap.servers': settings.KAFKA_BOOTSTRAP_SERVERS,
                    'client.id': 'quantiq-data-engine',
                    # Add robustness settings
                    'acks': 'all',
                    'retries': 3
                }
                cls._producer = Producer(conf)
                logger.info(f"Kafka producer created for {settings.KAFKA_BOOTSTRAP_SERVERS}")
            except Exception as e:
                logger.error(f"Failed to create Kafka producer: {e}")
                raise
        return cls._producer

    @classmethod
    def delivery_report(cls, err, msg):
        """Called once for each message produced to indicate delivery result."""
        if err is not None:
            logger.error(f'Message delivery failed: {err}')
        else:
            logger.info(f'Message delivered to {msg.topic()} [{msg.partition()}]')

    @classmethod
    def publish(cls, topic: str, message: dict):
        try:
            p = cls.get_producer()
            # Asynchronous produce
            p.produce(
                topic, 
                json.dumps(message).encode('utf-8'),
                callback=cls.delivery_report
            )
            # Find time to flush() - in high throughput this should be managed differently,
            # but for our daily batch, flushing immediately is fine to ensure delivery.
            p.flush()
            
        except Exception as e:
            logger.error(f"Failed to publish event to {topic}: {e}")
            import traceback
            logger.error(traceback.format_exc())
