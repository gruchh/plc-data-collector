package com.plcdatacollector.backend.infrastructure.persistence.repository;

import com.plcdatacollector.backend.infrastructure.persistence.entity.GroupSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupSummaryRepository extends JpaRepository<GroupSummaryEntity, Long> {

    List<GroupSummaryEntity> findBySnapshot_Id(Long snapshotId);

    Optional<GroupSummaryEntity> findBySnapshot_IdAndGroupName(Long snapshotId, String groupName);
}