#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_PATH="$SCRIPT_DIR/../.env"

if [ -f "$ENV_PATH" ]; then
    set -a
    source "$ENV_PATH"
    set +a
    echo "STATUS: Plik .env wczytany poprawnie."
    echo "------------------------------------------------"
else
    echo "BŁĄD: Nie znaleziono pliku .env!"
    echo "Upewnij się, że plik znajduje się w: $ENV_PATH"
    exit 1
fi

CONTAINER_NAME="influxdb"
INFLUX_TOKEN=$INFLUXDB_ADMIN_TOKEN
INFLUX_ORG=$INFLUXDB_ORG
BUCKET=$INFLUXDB_BUCKET

show_kafka_logs() {
    echo -e "\n[INFLUX] Pobieranie danych PLC (ostatnie 15 min)..."
    docker exec -t $CONTAINER_NAME influx query \
        "from(bucket: \"$BUCKET\") 
        |> range(start: -15m) 
        |> filter(fn: (r) => r._measurement == \"plc\")
        |> limit(n: 10)" \
        --token "$INFLUX_TOKEN" --org "$INFLUX_ORG"
}

show_useful_stuff() {
    echo -e "\n[DOCKER] Aktywne kontenery:"
    docker ps --format "table {{.Names}}\t{{.Status}}"
    
    echo -e "\n[INFLUX] Dostępne tabele (measurements) w '$BUCKET':"
    docker exec -t $CONTAINER_NAME influx query \
        "import \"influxdata/influxdb/schema\" 
         schema.measurements(bucket: \"$BUCKET\")" \
        --token "$INFLUX_TOKEN" --org "$INFLUX_ORG"
}

PS3='Wybierz opcję: '
options=("Logi z Kafki" "Status i Measurementy" "Wyjście")

select opt in "${options[@]}"; do
    case $opt in
        "Logi z Kafki")
            show_kafka_logs
            ;;
        "Status i Measurementy")
            show_useful_stuff
            ;;
        "Wyjście")
            echo "Koniec skryptu."
            break
            ;;
        *) 
            echo "Nieprawidłowy wybór $REPLY"
            ;;
    esac
    echo -e "\n------------------------------------------------"
done