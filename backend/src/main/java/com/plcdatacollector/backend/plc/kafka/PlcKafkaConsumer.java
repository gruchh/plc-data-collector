package com.plcdatacollector.backend.plc.kafka;

import com.plcdatacollector.backend.plc.service.PlcDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlcKafkaConsumer {

    private final PlcDataService plcDataService;
    private final ObjectMapper objectMapper;

    private final Map<String, Instant> lastSavedAt = new ConcurrentHashMap<>();

    private static final long SAVE_INTERVAL_SECONDS  = 60;
    private static final long ALARM_COOLDOWN_SECONDS = 10;

    @KafkaListener(
            topics      = "${plc.kafka.topic:plc.data.raw}",
            groupId     = "${plc.kafka.group-id:plc-collector}",
            concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.debug("Odebrano — partition={} offset={}", record.partition(), record.offset());
        try {
            String rawJson = record.value();
            String line = extractLine(rawJson);
            boolean hasAlarm = hasAlarm(rawJson);

            boolean shouldSave = (!hasAlarm && isIntervalElapsed(line, SAVE_INTERVAL_SECONDS))
                    || ( hasAlarm && isIntervalElapsed(line, ALARM_COOLDOWN_SECONDS));

            if (shouldSave) {
                plcDataService.processPayload(rawJson);
                lastSavedAt.put(line, Instant.now());

                if (hasAlarm) {
                    log.info("Zapisano snapshot z ALARMEM — linia={}", line);
                } else {
                    log.debug("Zapisano snapshot (interwał minutowy) — linia={}", line);
                }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Błąd przetwarzania offset={}: {}", record.offset(), e.getMessage(), e);
        }
    }

    private boolean hasAlarm(String rawJson) {
        return rawJson.contains("\"isAlarm\":true");
    }

    private String extractLine(String rawJson) {
        try {
            return objectMapper.readTree(rawJson).path("line").asText("UNKNOWN");
        } catch (Exception e) {
            log.warn("Nie można wyciągnąć pola 'line' z JSON: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private boolean isIntervalElapsed(String line, long seconds) {
        Instant last = lastSavedAt.get(line);
        if (last == null) return true;
        return Instant.now().getEpochSecond() - last.getEpochSecond() >= seconds;
    }
}