package com.plcdatacollector.backend.plc.web.rest;

import com.plcdatacollector.backend.infrastructure.persistence.entity.GroupSummaryEntity;
import com.plcdatacollector.backend.infrastructure.persistence.entity.PlcSnapshotEntity;
import com.plcdatacollector.backend.infrastructure.persistence.repository.GroupSummaryRepository;
import com.plcdatacollector.backend.infrastructure.persistence.repository.PlcSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/lines")
@RequiredArgsConstructor
public class PlcController {

    private final PlcSnapshotRepository snapshotRepository;
    private final GroupSummaryRepository groupSummaryRepository;

    // GET /api/lines/{line}/snapshots
    // domyślnie ostatnia godzina, opcjonalnie ?from=&to= (ISO 8601)
    @GetMapping("/{line}/snapshots")
    public ResponseEntity<List<PlcSnapshotEntity>> getSnapshots(
            @PathVariable String line,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        Instant end   = to   != null ? to   : Instant.now();
        Instant start = from != null ? from : end.minus(1, ChronoUnit.HOURS);

        return ResponseEntity.ok(
                snapshotRepository.findByLineAndTimestampBetweenOrderByTimestampDesc(line, start, end));
    }

    // GET /api/lines/{line}/snapshots/latest
    @GetMapping("/{line}/snapshots/latest")
    public ResponseEntity<PlcSnapshotEntity> getLatest(@PathVariable String line) {
        return snapshotRepository.findTopByLineOrderByTimestampDesc(line)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/lines/{line}/alarms
    @GetMapping("/{line}/alarms")
    public ResponseEntity<List<PlcSnapshotEntity>> getAlarms(@PathVariable String line) {
        return ResponseEntity.ok(
                snapshotRepository.findByLineAndHasAlarmTrueOrderByTimestampDesc(line));
    }

    // GET /api/lines/{line}/groups
    // zwraca group summaries dla ostatniego snapshotu danej linii
    @GetMapping("/{line}/groups")
    public ResponseEntity<List<GroupSummaryEntity>> getGroups(@PathVariable String line) {
        return snapshotRepository.findTopByLineOrderByTimestampDesc(line)
                .map(snapshot -> ResponseEntity.ok(
                        groupSummaryRepository.findBySnapshot_Id(snapshot.getId())))
                .orElse(ResponseEntity.notFound().build());
    }
}