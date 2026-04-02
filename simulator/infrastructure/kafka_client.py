from confluent_kafka import Producer
import logging
import time

logger = logging.getLogger(__name__)

def create_producer(bootstrap_servers: str, is_running_check: callable) -> Producer:
    conf = {
        'bootstrap.servers': bootstrap_servers,
        'client.id': 'plc-simulator',
        'acks': 'all',
        'compression.type': 'snappy',
        'socket.timeout.ms': 5000,
        'metadata.max.age.ms': 60000,
    }
    
    while is_running_check():
        try:
            return Producer(conf)
        except Exception as e:
            logger.error(f"Nie udało się połączyć z Kafką: {e}. Ponawiam...")
            time.sleep(5)
    return None