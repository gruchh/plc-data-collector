package com.plcdatacollector.backend.plc.dto.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupSummaryDto(
        String status,
        String severity,
        Map<String, Integer> counts,
        List<String> alarms,
        List<String> suspicious
) {}