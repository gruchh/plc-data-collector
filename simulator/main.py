import logging
import os
import random
import signal
import time
from dataclasses import asdict

from core.config_loader import load_config, build_runtime_config
from core.generator import initialize_state, next_value, build_payload
from infrastructure.kafka_client import create_producer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("SimulatorMain")

CONFIG_PATH = os.getenv("CONFIG_PATH", "config/plc_profiles.yaml")
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
LINE_ID = os.getenv("LINE_ID", "A1")
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC")

is_running = True


def handle_shutdown(signum, frame):
    global is_running
    logger.info("Otrzymano sygnał zatrzymania — kończę pracę symulatora...")
    is_running = False


signal.signal(signal.SIGTERM, handle_shutdown)
signal.signal(signal.SIGINT, handle_shutdown)

def load_runtime() -> dict:
    config = load_config(CONFIG_PATH)
    return build_runtime_config(config, LINE_ID, KAFKA_TOPIC)


def log_startup(runtime: dict) -> None:
    logger.info("Symulator uruchomiony")
    logger.info(f"  Linia:    {LINE_ID}")
    logger.info(f"  PLC:      {runtime['plc_name']}")
    logger.info(f"  Profil:   {runtime['profile_name']}")
    logger.info(f"  Topic:    {runtime['topic']}")
    logger.info(f"  Interwał: {runtime['send_interval_ms']} ms")


def run_loop(producer, runtime: dict, register_values: dict) -> None:
    rng = random.Random(runtime["seed"])
    interval_s = runtime["send_interval_ms"] / 1000.0
    topic = runtime["topic"]
    key = LINE_ID if runtime["key_by_line"] else None

    while is_running:
        for reg, spec in runtime["register_specs"].items():
            register_values[reg] = next_value(register_values[reg], spec, rng)

        payload = asdict(build_payload(LINE_ID, runtime, register_values))

        try:
            producer.send(topic, key=key, value=payload)
            logger.info(f"[{LINE_ID}] Wysłano | profil={runtime['profile_name']}")
        except Exception as ex:
            logger.error(f"[{LINE_ID}] Błąd wysyłki: {ex}")

        time.sleep(interval_s)


def main() -> None:
    logger.info("Inicjalizacja konfiguracji...")
    try:
        runtime = load_runtime()
    except Exception as e:
        logger.error(f"Nie udało się załadować konfiguracji: {e}")
        return

    rng = random.Random(runtime["seed"])
    register_values = initialize_state(runtime["register_specs"], rng)

    producer = create_producer(KAFKA_BOOTSTRAP_SERVERS, lambda: is_running)
    if not producer:
        logger.error("Nie udało się utworzyć producenta Kafka — przerywam.")
        return

    log_startup(runtime)

    try:
        run_loop(producer, runtime, register_values)
    finally:
        logger.info("Flush i zamknięcie producenta Kafka...")
        producer.flush()
        producer.close()
        logger.info("Symulator zakończył pracę.")


if __name__ == "__main__":
    main()