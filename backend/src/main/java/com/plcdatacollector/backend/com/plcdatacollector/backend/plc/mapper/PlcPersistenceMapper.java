package com.plcdatacollector.backend.plc.mapper;

import com.plcdatacollector.backend.infrastructure.persistence.entity.GroupSummaryEntity;
import com.plcdatacollector.backend.infrastructure.persistence.entity.PlcSnapshotEntity;
import com.plcdatacollector.backend.plc.domain.model.GroupSummary;
import com.plcdatacollector.backend.plc.domain.model.PlcSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlcPersistenceMapper {

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "groupSummaries", ignore = true)
    PlcSnapshotEntity toEntity(PlcSnapshot snapshot);

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "snapshot",   ignore = true)
    @Mapping(target = "alarms",     expression = "java(listToCsv(summary.getAlarms()))")
    @Mapping(target = "suspicious", expression = "java(listToCsv(summary.getSuspicious()))")
    GroupSummaryEntity toGroupSummaryEntity(GroupSummary summary);

    default String listToCsv(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }
}