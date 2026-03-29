BROKER="kafka-broker"
TOPIC="${KAFKA_TOPIC:-plc.data.raw}"
CONSUMER="/opt/kafka/bin/kafka-console-consumer.sh"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

if ! docker ps --format '{{.Names}}' | grep -q "^${BROKER}$"; then
    echo -e "${RED}❌ Kontener '${BROKER}' nie działa. Uruchom najpierw: docker compose up${NC}"
    exit 1
fi

echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║        Kafka Payload Inspector           ║${NC}"
echo -e "${CYAN}║  topic: ${TOPIC}${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${GREEN}1)${NC} Ostatnie 5 wiadomości (pretty JSON)"
echo -e "  ${GREEN}2)${NC} Nowe wiadomości na żywo (pretty JSON)"
echo -e "  ${GREEN}3)${NC} Z kluczem (linia | payload)"
echo -e "  ${GREEN}4)${NC} Tylko alarmy (filtr: isAlarm)"
echo -e "  ${GREEN}5)${NC} Surowy JSON (bez formatowania)"
echo ""
read -rp "Wybierz opcję [1-5]: " choice

HAS_JQ=$(docker exec "$BROKER" sh -c 'command -v jq' 2>/dev/null)
if [ -z "$HAS_JQ" ]; then
    echo -e "${YELLOW}⚠️  jq niedostępne w kontenerze — instaluję...${NC}"
    docker exec "$BROKER" sh -c 'apt-get update -qq && apt-get install -y -qq jq' 2>/dev/null \
        || docker exec "$BROKER" sh -c 'apk add --quiet jq' 2>/dev/null \
        || { echo -e "${RED}❌ Nie udało się zainstalować jq — używam surowego JSON${NC}"; HAS_JQ=""; }
fi

JQ_CMD='jq .'
if [ -z "$HAS_JQ" ]; then
    JQ_CMD='cat'
fi

echo ""
echo -e "${YELLOW}▶ Ctrl+C aby zatrzymać${NC}"
echo "────────────────────────────────────────────"

case "$choice" in
    1)
        echo -e "${CYAN}Ostatnie 5 wiadomości:${NC}"
        docker exec -it "$BROKER" sh -c "
            $CONSUMER \
              --bootstrap-server localhost:9092 \
              --topic $TOPIC \
              --from-beginning \
              --max-messages 5 \
              2>/dev/null
        " | $JQ_CMD
        ;;
    2)
        echo -e "${CYAN}Nowe wiadomości na żywo:${NC}"
        docker exec -it "$BROKER" sh -c "
            $CONSUMER \
              --bootstrap-server localhost:9092 \
              --topic $TOPIC \
              2>/dev/null
        " | $JQ_CMD
        ;;
    3)
        echo -e "${CYAN}Wiadomości z kluczem (linia | payload):${NC}"
        docker exec -it "$BROKER" sh -c "
            $CONSUMER \
              --bootstrap-server localhost:9092 \
              --topic $TOPIC \
              --property print.key=true \
              --property key.separator=' | ' \
              --from-beginning \
              --max-messages 10 \
              2>/dev/null
        "
        ;;
    4)
        echo -e "${CYAN}Tylko wiadomości z alarmami:${NC}"
        docker exec -it "$BROKER" sh -c "
            $CONSUMER \
              --bootstrap-server localhost:9092 \
              --topic $TOPIC \
              --from-beginning \
              2>/dev/null
        " | $JQ_CMD | grep -A 5 '"isAlarm": true'
        ;;
    5)
        echo -e "${CYAN}Surowy JSON — ostatnie 5 wiadomości:${NC}"
        docker exec -it "$BROKER" sh -c "
            $CONSUMER \
              --bootstrap-server localhost:9092 \
              --topic $TOPIC \
              --from-beginning \
              --max-messages 5 \
              2>/dev/null
        "
        ;;
    *)
        echo -e "${RED}Nieprawidłowa opcja${NC}"
        exit 1
        ;;
esac