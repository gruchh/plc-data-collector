package com.plcdatacollector.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plc_snapshots", indexes = {
        @Index(name = "idx_snapshot_line",           columnList = "line"),
        @Index(name = "idx_snapshot_timestamp",      columnList = "timestamp"),
        @Index(name = "idx_snapshot_line_timestamp", columnList = "line, timestamp")
})
public class PlcSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String line;

    @Column(nullable = false)
    private String plcName;

    private String deviceType;
    private String profile;

    @Column(nullable = false)
    private Instant timestamp;

    private boolean hasAlarm;
    private boolean hasSuspicious;

    @Column(length = 20)
    private String overallSeverity;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupSummaryEntity> groupSummaries = new ArrayList<>();

    private double avgSpeedRpm;

    public void addGroupSummary(GroupSummaryEntity summary) {
        groupSummaries.add(summary);
        summary.setSnapshot(this);
    }
}