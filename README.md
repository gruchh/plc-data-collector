# PLC Data Collector

A full-stack Industrial IoT (IIoT) application designed to simulate Programmable Logic Controller (PLC) behavior, process machine telemetry in real-time, and visualize the data on a modern web dashboard.

## 🏗️ Architecture & Project Structure

This repository is structured as a monorepo containing all components of the system:

* **`/simulator` (Python)**: Configurable PLC simulators generating mock telemetry data, spikes, and anomalies. Pushes data to Kafka topics.
* **`/backend` (Spring Boot)**: Core Java service acting as a Kafka consumer — processes incoming telemetry and exposes real-time APIs for the frontend.
* **`/frontend` (Angular)**: User interface for monitoring multiple production lines — real-time charts, active alarms, and group summaries.
* **Kafka Broker**: High-throughput message broker (KRaft mode — no Zookeeper) connecting simulators and the backend.

## 🚀 Tech Stack

* **Simulator:** Python 3
* **Message Broker:** Apache Kafka (KRaft mode)
* **Backend:** Java 17+, Spring Boot
* **Frontend:** Angular, TypeScript
* **Infrastructure:** Docker, Docker Compose

## ⚙️ How it works (The Simulator)

The Python simulator acts as an edge device. Features include:
- Generating realistic telemetry data with configurable jitter and step limits.
- Simulating anomalies and threshold breaches (e.g., `LOW_LOW`, `HIGH_HIGH`).
- Auto-reconnecting to the Kafka broker on startup.
- Five independent simulator instances (lines A1–A5) running in parallel.

## 🛠️ Getting Started

### Prerequisites
Make sure you have [Docker](https://www.docker.com/) and Docker Compose installed.

### Running the entire stack
```bash
# 1. Clone the repository
git clone https://github.com/gruchh/plc-data-collector.git
cd plc-data-collector

# 2. Start all services
docker compose up -d
```