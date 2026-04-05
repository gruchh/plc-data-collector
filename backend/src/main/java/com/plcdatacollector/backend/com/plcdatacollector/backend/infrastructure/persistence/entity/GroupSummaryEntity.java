package com.plcdatacollector.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_summaries", indexes = {
        @Index(name = "idx_group_snapshot", columnList = "snapshot_id"),
        @Index(name = "idx_group_name",     columnList = "group_name")
})
public class GroupSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PlcSnapshotEntity snapshot;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(length = 20)
    private String status;

    @Column(length = 20)
    private String severity;

    private int countLowLow;
    private int countLow;
    private int countNormal;
    private int countHigh;
    private int countHighHigh;

    @Column(columnDefinition = "TEXT")
    private String alarms;

    @Column(columnDefinition = "TEXT")
    private String suspicious;
}