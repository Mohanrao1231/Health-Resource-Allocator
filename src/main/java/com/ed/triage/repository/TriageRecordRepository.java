package com.ed.triage.repository;

import com.ed.triage.entity.TriageRecord;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.enums.TriageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    List<TriageRecord> findByPatientIdOrderByArrivalTimeDesc(Long patientId);
    List<TriageRecord> findByStatus(TriageStatus status);
    List<TriageRecord> findByEsiLevel(EsiLevel esiLevel);

    @Query("SELECT t FROM TriageRecord t WHERE t.patient.id = :patientId AND t.status IN ('ASSESSED', 'WAITING_IN_QUEUE', 'IN_CONSULTATION')")
    Optional<TriageRecord> findActiveTriageByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT t FROM TriageRecord t WHERE t.status = 'WAITING_IN_QUEUE' ORDER BY t.priorityScore DESC, t.arrivalTime ASC")
    List<TriageRecord> findAllWaitingInQueueOrdered();
}
