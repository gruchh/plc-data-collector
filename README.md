# PLC Data Collector

A full-stack Industrial IoT (IIoT) application designed to simulate Programmable Logic Controller (PLC) behavior, process machine telemetry in real-time, and visualize the data on a modern web dashboard.

---

## 🏗️ Architecture & Project Structure

This repository is structured as a monorepo containing all components of the system:

* **`/simulator` (Python)**: Configurable PLC simulators generating mock telemetry data, spikes, and anomalies. Pushes data to Kafka topics.
* **`/backend` (Spring Boot)**: Core Java service acting as a Kafka consumer — processes incoming telemetry and exposes real-time APIs for the frontend.
* **`/frontend` (Angular)**: User interface for monitoring multiple production lines — real-time charts, active alarms, and group summaries.
* **Kafka Broker**: High-throughput message broker (KRaft mode — no Zookeeper) connecting simulators and the backend.
* **Telegraf**: Metrics collection agent — consumes data from Kafka and forwards it to InfluxDB.
* **InfluxDB**: Time-series database for storing telemetry metrics and infrastructure observability data.
* **Grafana**: Visualization layer for InfluxDB metrics — dashboards, alerting, and historical trends.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Simulator | Python 3 |
| Message Broker | Apache Kafka 4.0.2 (KRaft mode) |
| Backend | Java 21, Spring Boot |
| Frontend | Angular, TypeScript |
| Database | PostgreSQL 17 |
| Metrics Collector | Telegraf 1.36 |
| Time-Series DB | InfluxDB 2.7 |
| Dashboards | Grafana 12.4 |
| Infrastructure | Docker, Docker Compose |

---

## ⚙️ How it works (The Simulator)

The Python simulator acts as an edge device. Features include:

- Generating realistic telemetry data with configurable jitter and step limits.
- Simulating anomalies and threshold breaches (e.g., `LOW_LOW`, `HIGH_HIGH`).
- Auto-reconnecting to the Kafka broker on startup.
- Five independent simulator instances (lines **A1–A5**) running in parallel.

Each simulator instance publishes **`PlcPayload`** messages to the `plc.data.raw` Kafka topic.

---

## 📡 Kafka Message Schema — `PlcPayload`

Every message published to the `plc.data.raw` topic follows this JSON structure:

```json
{
  "timestamp": "2024-07-15T10:30:00.123456Z",
  "line": "A1",
  "plcName": "PLC_LINE_A1",
  "deviceType": "Siemens_S7-1500",
  "profile": "normal_profile",
  "registers": {
    "D1": {
      "value": 72,
      "state": "NORMAL",
      "severity": "INFO",
      "isAlarm": false,
      "isSuspicious": false,
      "group": "Temperature"
    },
    "D2": { "...": "..." }
  },
  "groupSummary": {
    "Temperature": {
      "status": "NORMAL",
      "severity": "INFO",
      "counts": {
        "LOW_LOW": 0,
        "LOW": 0,
        "NORMAL": 5,
        "HIGH": 1,
        "HIGH_HIGH": 0
      },
      "alarms": [],
      "suspicious": ["D7"]
    }
  }
}
```

### Top-level fields

| Field | Type | Description |
|---|---|---|
| `timestamp` | `string` (ISO 8601 UTC) | Time the snapshot was captured |
| `line` | `string` | Production line identifier, e.g. `A1`–`A5` |
| `plcName` | `string` | Human-readable PLC device name |
| `deviceType` | `string` | PLC hardware model string |
| `profile` | `string` | Active simulation profile (see [Profiles](#profiles)) |
| `registers` | `Dict[str, RegisterReading]` | Register key → reading object (e.g. `D1`–`D100`) |
| `groupSummary` | `Dict[str, GroupSummary]` | Group name → aggregated group status |

---

### `RegisterReading`

Each register (`D1`–`D100`) produces one reading per snapshot.

| Field | Type | Description |
|---|---|---|
| `value` | `int` | Raw register value |
| `state` | `RegisterState` | Threshold state of the current value |
| `severity` | `Severity` | Derived severity level |
| `isAlarm` | `bool` | `true` if state is `LOW_LOW` or `HIGH_HIGH` |
| `isSuspicious` | `bool` | `true` if state is `LOW` or `HIGH` |
| `group` | `string` | Measurement group this register belongs to |

#### `RegisterState` enum

| Value | Meaning |
|---|---|
| `LOW_LOW` | Below the lowest threshold — **alarm** |
| `LOW` | Below lower warning threshold — suspicious |
| `NORMAL` | Within the expected operating range |
| `HIGH` | Above upper warning threshold — suspicious |
| `HIGH_HIGH` | Above the highest threshold — **alarm** |

#### `Severity` enum

| Value | Triggers on |
|---|---|
| `INFO` | `NORMAL` state |
| `WARNING` | `LOW` or `HIGH` state |
| `ALARM` | `LOW_LOW` or `HIGH_HIGH` state |

---

### `GroupSummary`

Registers are organised into named measurement groups (e.g. `Temperature`, `Pressure`, `Current`, `Vibration`, `Speed`). Each group exposes an aggregated summary.

| Field | Type | Description |
|---|---|---|
| `status` | `string` | Worst `RegisterState` seen in this group |
| `severity` | `string` | Worst `Severity` seen in this group |
| `counts` | `Dict[str, int]` | Count of registers per `RegisterState` |
| `alarms` | `List[str]` | Register keys currently in alarm (`LOW_LOW` / `HIGH_HIGH`) |
| `suspicious` | `List[str]` | Register keys currently suspicious (`LOW` / `HIGH`) |

---

### Register Groups (D1–D100)

| Group | Registers | Measurement |
|---|---|---|
| Temperature | D1–D20 | °C / process temperatures |
| Current | D21–D40 | Amperes / motor currents |
| Pressure | D41–D60 | Bar / pneumatic & hydraulic |
| Vibration | D61–D80 | mm/s / mechanical vibration |
| Speed | D81–D100 | RPM / drive speeds |

---

### Profiles

The simulator can run in different operating modes, configured per instance:

| Profile | Behaviour |
|---|---|
| `normal_profile` | Steady operation within normal bounds |
| `intensive_profile` | Higher values, increased anomaly probability |
| `slow_profile` | Reduced activity, low step variance |
| `stress_profile` | Frequent threshold breaches and spikes |
| `night_profile` | Low-load, minimal variance |

---

## 📊 Observability — Telegraf, InfluxDB & Grafana

Telemetry flows from Kafka through Telegraf into InfluxDB, where Grafana provides dashboards and alerting.

```
Kafka (plc.data.raw) ──► Telegraf ──► InfluxDB ──► Grafana
```

### Telegraf
Telegraf acts as the metrics collection agent. It consumes messages directly from the `plc.data.raw` Kafka topic and writes them to InfluxDB.

- Config file: `./monitoring/telegraf/telegraf.conf`
- Waits for both Kafka (healthy) and InfluxDB (started) before launching.

### InfluxDB
InfluxDB stores all time-series telemetry data. It is auto-initialised on first start using environment variables from `.env`.

- **URL:** [http://localhost:8086](http://localhost:8086)
- Config: `INFLUXDB_*` variables in `.env`

### Grafana
Grafana provides pre-built dashboards backed by InfluxDB as a datasource.

- **URL:** [http://localhost:3000](http://localhost:3000)
- Default credentials: configured via `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` in `.env`
- Datasource: InfluxDB (configure manually or via provisioning)
- Dashboard provisioning directory: `./monitoring/grafana/provisioning/`

---

## 🛠️ Getting Started

### Prerequisites
Make sure you have [Docker](https://www.docker.com/) and Docker Compose installed.

### Environment variables
Copy the example file and fill in your values before starting:

```bash
cp .env.example .env
```

Required variables:

| Variable | Description |
|---|---|
| `POSTGRES_DB` | PostgreSQL database name |
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `INFLUXDB_USERNAME` | InfluxDB admin username |
| `INFLUXDB_PASSWORD` | InfluxDB admin password |
| `INFLUXDB_ORG` | InfluxDB organisation name |
| `INFLUXDB_BUCKET` | InfluxDB bucket name |
| `INFLUXDB_ADMIN_TOKEN` | InfluxDB admin token (used by Telegraf & Grafana) |
| `GRAFANA_ADMIN_USER` | Grafana admin username |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password |

### Running the entire stack
```bash
# 1. Clone the repository
git clone https://github.com/gruchh/plc-data-collector.git
cd plc-data-collector

# 2. Configure environment
cp .env.example .env
# Edit .env with your values

# 3. Start all services
docker compose up -d
```

### Service URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| InfluxDB | http://localhost:8086 |
| Grafana | http://localhost:3000 |
| Kafka | localhost:9092 |
| PostgreSQL | localhost:5432 |