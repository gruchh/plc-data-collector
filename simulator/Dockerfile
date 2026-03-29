FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV CONFIG_PATH=/app/config/plc_profiles.yaml
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV LINE_ID=A1
ENV KAFKA_TOPIC=plc.data.raw

CMD ["python", "main.py"]