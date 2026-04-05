package com.plcdatacollector.backend.plc.domain.model;

import java.util.List;

public class GroupSummary {

    private String groupName;
    private String status;
    private String severity;

    private int countLowLow;
    private int countLow;
    private int countNormal;
    private int countHigh;
    private int countHighHigh;

    private List<String> alarms;
    private List<String> suspicious;
}