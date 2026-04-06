package com.plcdatacollector.backend.plc.mapper;

import com.plcdatacollector.backend.plc.domain.model.GroupSummary;
import com.plcdatacollector.backend.plc.domain.model.PlcSnapshot;
import com.plcdatacollector.backend.plc.dto.kafka.GroupSummaryDto;
import com.plcdatacollector.backend.plc.dto.kafka.PlcPayloadDto;
import com.plcdatacollector.backend.plc.dto.kafka.RegisterReadingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", imports = java.time.Instant.class)
public interface PlcPayloadMapper {

    @Mapping(target = "timestamp",       expression = "java(Instant.parse(dto.timestamp()))")
    @Mapping(target = "hasAlarm",        expression = "java(hasAnyAlarm(dto))")
    @Mapping(target = "hasSuspicious",   expression = "java(hasAnySuspicious(dto))")
    @Mapping(target = "overallSeverity", expression = "java(resolveOverallSeverity(dto))")
    @Mapping(target = "groupSummaries",  expression = "java(mapGroupSummaries(dto.groupSummary()))")
    @Mapping(target = "avgSpeedRpm",     expression = "java(calculateAvgSpeed(dto))")
    PlcSnapshot toSnapshot(PlcPayloadDto dto);

    @Mapping(target = "groupName",     source = "key")
    @Mapping(target = "countLowLow",   expression = "java(getCount(dto, \"LOW_LOW\"))")
    @Mapping(target = "countLow",      expression = "java(getCount(dto, \"LOW\"))")
    @Mapping(target = "countNormal",   expression = "java(getCount(dto, \"NORMAL\"))")
    @Mapping(target = "countHigh",     expression = "java(getCount(dto, \"HIGH\"))")
    @Mapping(target = "countHighHigh", expression = "java(getCount(dto, \"HIGH_HIGH\"))")
    GroupSummary toGroupSummary(String key, GroupSummaryDto dto);

    default boolean hasAnyAlarm(PlcPayloadDto dto) {
        if (dto.registers() == null) return false;
        return dto.registers().values().stream()
                .anyMatch(RegisterReadingDto::isAlarm);
    }

    default boolean hasAnySuspicious(PlcPayloadDto dto) {
        if (dto.registers() == null) return false;
        return dto.registers().values().stream()
                .anyMatch(RegisterReadingDto::isSuspicious);
    }

    default String resolveOverallSeverity(PlcPayloadDto dto) {
        if (dto.registers() == null) return "INFO";
        boolean hasAlarm = dto.registers().values().stream()
                .anyMatch(r -> "ALARM".equals(r.severity()));
        if (hasAlarm) return "ALARM";
        boolean hasWarning = dto.registers().values().stream()
                .anyMatch(r -> "WARNING".equals(r.severity()));
        return hasWarning ? "WARNING" : "INFO";
    }

    default double calculateAvgSpeed(PlcPayloadDto dto) {
        if (dto.registers() == null) return 0.0;
        return dto.registers().values().stream()
                .filter(r -> "Speed".equals(r.group()))
                .mapToInt(RegisterReadingDto::value)
                .average()
                .orElse(0.0);
    }

    default List<GroupSummary> mapGroupSummaries(Map<String, GroupSummaryDto> groups) {
        if (groups == null) return List.of();
        return groups.entrySet().stream()
                .map(e -> toGroupSummary(e.getKey(), e.getValue()))
                .toList();
    }

    default int getCount(GroupSummaryDto dto, String key) {
        if (dto.counts() == null) return 0;
        return dto.counts().getOrDefault(key, 0);
    }
}