import logging
import os
import random
import signal
import time
import json
from dataclasses import asdict
from core.config_loader import load_config, build_runtime_config
from core.generator import initialize_state, next_value, build_payload
from infrastructure.kafka_client import create_producer

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s - %(message)s")
logger = logging.getLogger("SimulatorMain")

is_running = True

def handle_shutdown(signum, frame):
    global is_running
    is_running = False

signal.signal(signal.SIGTERM, handle_shutdown)
signal.signal(signal.SIGINT, handle_shutdown)

def delivery_report(err, msg):
    if err is not None:
        logger.error(f"Błąd wysyłki: {err}")

def run_loop(producer, runtime: dict, register_values: dict) -> None:
    rng = random.Random(runtime["seed"])
    topic = runtime["topic"]
    key = os.getenv("LINE_ID") if runtime["key_by_line"] else None

    while is_running:
        for reg, spec in runtime["register_specs"].items():
            register_values[reg] = next_value(register_values[reg], spec, rng)

        payload = asdict(build_payload(os.getenv("LINE_ID"), runtime, register_values))

        try:
            producer.produce(
                topic, 
                value=json.dumps(payload).encode('utf-8'),
                key=key.encode('utf-8') if key else None,
                callback=delivery_report
            )
            producer.poll(0)
            logger.info(f"[{os.getenv('LINE_ID')}] Wysłano | profil={runtime['profile_name']}")
        except Exception as ex:
            logger.error(f"[{os.getenv('LINE_ID')}] Błąd: {ex}")

        time.sleep(1)

def main() -> None:
    runtime = build_runtime_config(load_config(os.getenv("CONFIG_PATH", "config/plc_profiles.yaml")), os.getenv("LINE_ID"), os.getenv("KAFKA_TOPIC"))
    register_values = initialize_state(runtime["register_specs"], random.Random(runtime["seed"]))

    producer = create_producer(os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"), lambda: is_running)
    
    logger.info("Czekam na gotowość brokera Kafka...")
    
    while is_running:
        try:
            producer.list_topics(timeout=5)
            logger.info("Połączenie z Kafką nawiązane!")
            break
        except Exception:
            logger.warning("Broker jeszcze niegotowy, czekam 2 sekundy...")
            time.sleep(2)

    try:
        run_loop(producer, runtime, register_values)
    finally:
        logger.info("Zamykanie producenta Kafka...")
        producer.flush()
        logger.info("Symulator zakończył pracę.")

if __name__ == "__main__":
    main()