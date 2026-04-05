package com.plcdatacollector.backend.plc.dto.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterReadingDto(
        int value,
        String state,
        String severity,
        @JsonProperty("isAlarm")     boolean isAlarm,
        @JsonProperty("isSuspicious") boolean isSuspicious,
        String group
) {}