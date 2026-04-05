package com.plcdatacollector.backend.plc.domain.model;

public class RegisterReading {

    private String registerKey;
    private int value;
    private String state;
    private String severity;
    private boolean alarm;
    private boolean suspicious;
    private String groupName;
}