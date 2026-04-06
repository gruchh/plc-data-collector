package com.plcdatacollector.backend.plc.service;

import com.plcdatacollector.backend.infrastructure.persistence.repository.PlcSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OeeCalculationService {

    private final PlcSnapshotRepository snapshotRepository;

    @Value("${plc.oee.window-minutes:5}")
    private int windowMinutes;

    @Value("${plc.oee.nominal-speed-rpm:1500}")
    private double nominalSpeedRpm;

    private static final List<String> LINES = List.of("A1", "A2", "A3", "A4", "A5");

    @Scheduled(fixedRateString = "${plc.oee.window-minutes:5}000", initialDelay = 60_000)
    public void calculateOeeForAllLines() {
        Instant windowEnd   = Instant.now();
        Instant windowStart = windowEnd.minus(windowMinutes, ChronoUnit.MINUTES);

        LINES.forEach(line -> calculateForLine(line, windowStart, windowEnd));
    }

    private void calculateForLine(String line, Instant from, Instant to) {
        long total = snapshotRepository.countByLineAndTimestampBetween(line, from, to);

        if (total == 0) {
            log.debug("Brak snapshotów dla linii={} w oknie {}-{}", line, from, to);
            return;
        }

        double availability = calculateAvailability(line, from, to, total);
        double performance  = calculatePerformance(line, from, to);
        double quality      = calculateQuality(line, from, to, total);
        double oee          = availability * performance * quality;

        log.info("OEE linia={} availability={:.2f} performance={:.2f} quality={:.2f} oee={:.2f}",
                line, availability, performance, quality, oee);
    }

    private double calculateAvailability(String line, Instant from, Instant to, long total) {
        long alarmSnapshots = snapshotRepository
                .countByLineAndHasAlarmTrueAndTimestampBetween(line, from, to);
        return (double) (total - alarmSnapshots) / total;
    }

    private double calculatePerformance(String line, Instant from, Instant to) {
        return snapshotRepository
                .findAvgSpeedByLineAndTimestampBetween(line, from, to)
                .map(avgSpeed -> Math.min(avgSpeed / nominalSpeedRpm, 1.0))
                .orElse(1.0);
    }

    private double calculateQuality(String line, Instant from, Instant to, long total) {
        long suspiciousSnapshots = snapshotRepository
                .countByLineAndHasSuspiciousTrueAndTimestampBetween(line, from, to);
        return (double) (total - suspiciousSnapshots) / total;
    }
}