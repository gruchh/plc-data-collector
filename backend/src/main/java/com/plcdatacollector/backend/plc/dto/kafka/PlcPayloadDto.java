package com.plcdatacollector.backend.plc.dto.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlcPayloadDto(
        String timestamp,
        String line,
        String plcName,
        String deviceType,
        String profile,
        Map<String, RegisterReadingDto> registers,
        Map<String, GroupSummaryDto> groupSummary
) {}