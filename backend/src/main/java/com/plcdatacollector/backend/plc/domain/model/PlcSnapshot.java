package com.plcdatacollector.backend.plc.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlcSnapshot {

    private String line;
    private String plcName;
    private String deviceType;
    private String profile;
    private Instant timestamp;
    private boolean hasAlarm;
    private boolean hasSuspicious;
    private String overallSeverity;
    private List<GroupSummary> groupSummaries;
    private double avgSpeedRpm;
}