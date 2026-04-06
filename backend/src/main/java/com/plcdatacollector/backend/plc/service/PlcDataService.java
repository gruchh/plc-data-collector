package com.plcdatacollector.backend.plc.service;


import com.plcdatacollector.backend.infrastructure.persistence.entity.PlcSnapshotEntity;
import com.plcdatacollector.backend.infrastructure.persistence.repository.PlcSnapshotRepository;
import com.plcdatacollector.backend.plc.domain.model.PlcSnapshot;
import com.plcdatacollector.backend.plc.dto.kafka.PlcPayloadDto;
import com.plcdatacollector.backend.plc.mapper.PlcPayloadMapper;
import com.plcdatacollector.backend.plc.mapper.PlcPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlcDataService {

    private final ObjectMapper objectMapper;
    private final PlcPayloadMapper payloadMapper;
    private final PlcPersistenceMapper persistenceMapper;
    private final PlcSnapshotRepository snapshotRepository;

    @Transactional
    public void processPayload(String rawJson) {
        try {
            PlcPayloadDto dto = objectMapper.readValue(rawJson, PlcPayloadDto.class);
            PlcSnapshot snapshot = payloadMapper.toSnapshot(dto);
            PlcSnapshotEntity entity = buildEntity(snapshot);
            snapshotRepository.save(entity);

            log.debug("Zapisano snapshot linia={} timestamp={} alarm={}",
                    snapshot.getLine(), snapshot.getTimestamp(), snapshot.isHasAlarm());

        } catch (Exception e) {
            log.error("Nieoczekiwany błąd przetwarzania payloadu: {}", e.getMessage(), e);
        }
    }

    private PlcSnapshotEntity buildEntity(PlcSnapshot snapshot) {
        PlcSnapshotEntity entity = persistenceMapper.toEntity(snapshot);

        snapshot.getGroupSummaries().stream()
                .map(persistenceMapper::toGroupSummaryEntity)
                .forEach(entity::addGroupSummary);

        return entity;
    }
}