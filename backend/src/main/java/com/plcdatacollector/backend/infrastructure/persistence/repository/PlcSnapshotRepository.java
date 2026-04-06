package com.plcdatacollector.backend.infrastructure.persistence.repository;

import com.plcdatacollector.backend.infrastructure.persistence.entity.PlcSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlcSnapshotRepository extends JpaRepository<PlcSnapshotEntity, Long> {

    long countByLineAndTimestampBetween(
            String line, Instant from, Instant to);

    long countByLineAndHasAlarmTrueAndTimestampBetween(
            String line, Instant from, Instant to);

    long countByLineAndHasSuspiciousTrueAndTimestampBetween(
            String line, Instant from, Instant to);

    @Query("""
            SELECT AVG(s.avgSpeedRpm)
            FROM PlcSnapshotEntity s
            WHERE s.line = :line
              AND s.timestamp BETWEEN :from AND :to
            """)
    Optional<Double> findAvgSpeedByLineAndTimestampBetween(
            @Param("line") String line,
            @Param("from") Instant from,
            @Param("to")   Instant to);

    List<PlcSnapshotEntity> findByLineAndTimestampBetweenOrderByTimestampDesc(
            String line, Instant from, Instant to);

    Optional<PlcSnapshotEntity> findTopByLineOrderByTimestampDesc(String line);

    List<PlcSnapshotEntity> findByLineAndHasAlarmTrueOrderByTimestampDesc(String line);
}