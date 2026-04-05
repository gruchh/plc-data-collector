package com.plcdatacollector.backend.plc.domain.model;


import java.time.Instant;
import java.util.List;


public class PlcSnapshot {

    private String line;
    private String plcName;
    private String deviceType;
    private String profile;
    private Instant timestamp;

    private boolean hasAlarm;
    private boolean hasSuspicious;
    private String overallSeverity;

    private List<RegisterReading> registers;
    private List<GroupSummary> groupSummaries;
}