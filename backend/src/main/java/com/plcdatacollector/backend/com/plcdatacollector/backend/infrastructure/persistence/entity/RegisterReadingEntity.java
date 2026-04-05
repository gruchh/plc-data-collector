package com.plcdatacollector.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "register_readings", indexes = {
        @Index(name = "idx_register_snapshot", columnList = "snapshot_id"),
        @Index(name = "idx_register_group",    columnList = "group_name"),
        @Index(name = "idx_register_alarm",    columnList = "alarm")
})
public class RegisterReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private PlcSnapshotEntity snapshot;

    @Column(nullable = false, length = 10)
    private String registerKey;

    private int value;

    @Column(length = 20)
    private String state;

    @Column(length = 20)
    private String severity;

    private boolean alarm;
    private boolean suspicious;

    @Column(name = "group_name", length = 50)
    private String groupName;
}