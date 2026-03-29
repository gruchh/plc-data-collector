import json
import logging
import time
from kafka import KafkaProducer

logger = logging.getLogger(__name__)

def create_producer(bootstrap_servers: str, is_running_check: callable) -> KafkaProducer:
    """
    is_running_check upewnia się, że nie utkniemy w pętli łączenia, 
    gdyby kontener dostał sygnał zamknięcia w trakcie startu.
    """
    while is_running_check():
        try:
            producer = KafkaProducer(
                bootstrap_servers=bootstrap_servers,
                value_serializer=lambda v: json.dumps(v, separators=(",", ":")).encode("utf-8"),
                key_serializer=lambda v: v.encode("utf-8") if v else None,
                linger_ms=20,
                acks="all",
                retries=10,
            )
            logger.info(f"Pomyślnie połączono z Kafka: {bootstrap_servers}")
            return producer
        except Exception as ex:
            logger.warning(f"Kafka niedostępna ({bootstrap_servers}): {ex}. Ponawiam za 5s...")
            time.sleep(5)
    return None