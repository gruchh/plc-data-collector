package com.plcdatacollector.backend.plc.web.rest;

import com.plcdatacollector.backend.infrastructure.persistence.repository.PlcSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oee")
@RequiredArgsConstructor
public class OeeController {

    private final PlcSnapshotRepository snapshotRepository;

    private static final List<String> LINES = List.of("A1", "A2", "A3", "A4", "A5");

    @GetMapping("/{line}")
    public ResponseEntity<Map<String, Object>> getOee(
            @PathVariable String line,
            @RequestParam(defaultValue = "1") int hours) {

        Instant to   = Instant.now();
        Instant from = to.minus(hours, ChronoUnit.HOURS);

        return ResponseEntity.ok(calculateOee(line, from, to));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllOee(
            @RequestParam(defaultValue = "1") int hours) {

        Instant to = Instant.now();
        Instant from = to.minus(hours, ChronoUnit.HOURS);

        List<Map<String, Object>> result = LINES.stream()
                .map(line -> calculateOee(line, from, to))
                .toList();

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> calculateOee(String line, Instant from, Instant to) {
        long total = snapshotRepository.countByLineAndTimestampBetween(line, from, to);

        if (total == 0) {
            return Map.of("line", line, "oee", 0.0, "message", "brak danych");
        }

        long alarmCount = snapshotRepository.countByLineAndHasAlarmTrueAndTimestampBetween(line, from, to);
        long suspiciousCount = snapshotRepository.countByLineAndHasSuspiciousTrueAndTimestampBetween(line, from, to);
        double avgSpeed = snapshotRepository.findAvgSpeedByLineAndTimestampBetween(line, from, to).orElse(0.0);

        double availability = (double) (total - alarmCount) / total;
        double performance = Math.min(avgSpeed / 1500.0, 1.0);
        double quality = (double) (total - suspiciousCount) / total;
        double oee = availability * performance * quality;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("line", line);
        result.put("from", from);
        result.put("to", to);
        result.put("availability", round(availability));
        result.put("performance", round(performance));
        result.put("quality", round(quality));
        result.put("oee", round(oee));

        return result;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}