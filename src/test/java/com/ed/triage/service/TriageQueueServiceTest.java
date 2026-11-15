package com.ed.triage.service;

import com.ed.triage.dto.response.AssignmentResponse;
import com.ed.triage.dto.response.QueueItemResponse;
import com.ed.triage.entity.Doctor;
import com.ed.triage.entity.Patient;
import com.ed.triage.entity.TriageRecord;
import com.ed.triage.entity.Vitals;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.enums.Specialty;
import com.ed.triage.enums.TriageStatus;
import com.ed.triage.exception.NoDoctorAvailableException;
import com.ed.triage.exception.QueueEmptyException;
import com.ed.triage.repository.AuditLogRepository;
import com.ed.triage.repository.DoctorRepository;
import com.ed.triage.repository.TriageRecordRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageQueueServiceTest {

    @Mock
    private TriageRecordRepository triageRecordRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private TriageQueueService queueService;

    private Patient patient1;
    private Patient patient2;
    private Patient patient3;
    private Doctor doctor1;

    @BeforeEach
    void setUp() {
        queueService = new TriageQueueService(
                triageRecordRepository,
                doctorRepository,
                auditLogRepository,
                new SimpleMeterRegistry()
        );

        patient1 = new Patient("MRN-001", "Alice", "Smith", LocalDate.of(1990, 1, 1), "Female", "555-1111", "None", "None");
        patient1.setId(1L);

        patient2 = new Patient("MRN-002", "Bob", "Jones", LocalDate.of(1985, 5, 12), "Male", "555-2222", "None", "None");
        patient2.setId(2L);

        patient3 = new Patient("MRN-003", "Charlie", "Brown", LocalDate.of(2000, 3, 20), "Male", "555-3333", "None", "None");
        patient3.setId(3L);

        doctor1 = new Doctor("DOC-01", "Dr. Gregory House", Specialty.EMERGENCY_MEDICINE, null);
        doctor1.setId(10L);
        doctor1.setAvailable(true);
    }

    @Test
    @DisplayName("Should order patients by Priority Score DESC and Arrival Time ASC in PriorityQueue")
    void testPriorityQueueOrdering() {
        LocalDateTime now = LocalDateTime.now();

        // Record 1: ESI 3, Score: 60, Arrived: 10:00
        TriageRecord r1 = new TriageRecord(patient1, new Vitals(80, 120, 80, 16, 98.0, 37.0, 4, 15),
                "Abdominal Pain", EsiLevel.ESI_3, 60, 2, "Urgent", now.minusMinutes(30));
        r1.setId(101L);

        // Record 2: ESI 1 (Critical Resuscitation), Score: 105, Arrived: 10:15
        TriageRecord r2 = new TriageRecord(patient2, new Vitals(160, 70, 40, 30, 80.0, 36.0, 0, 7),
                "Cardiac Arrest", EsiLevel.ESI_1, 105, 3, "Resuscitation", now.minusMinutes(15));
        r2.setId(102L);

        // Record 3: ESI 3, Score: 60, Arrived: 10:05 (Later arrival than r1 with same score)
        TriageRecord r3 = new TriageRecord(patient3, new Vitals(85, 125, 82, 18, 97.0, 37.2, 5, 15),
                "Fever workup", EsiLevel.ESI_3, 60, 2, "Urgent", now.minusMinutes(25));
        r3.setId(103L);

        when(triageRecordRepository.findById(101L)).thenReturn(Optional.of(r1));
        when(triageRecordRepository.findById(102L)).thenReturn(Optional.of(r2));
        when(triageRecordRepository.findById(103L)).thenReturn(Optional.of(r3));
        when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(i -> i.getArgument(0));

        queueService.enqueue(101L, "NURSE_1");
        queueService.enqueue(102L, "NURSE_1");
        queueService.enqueue(103L, "NURSE_1");

        assertThat(queueService.getQueueSize()).isEqualTo(3);

        List<QueueItemResponse> queue = queueService.getWaitingQueueSnapshot();
        assertThat(queue).hasSize(3);

        // Position 1 should be the ESI 1 patient (r2) despite arriving later
        assertThat(queue.get(0).triageRecordId()).isEqualTo(102L);
        assertThat(queue.get(0).esiLevel()).isEqualTo(EsiLevel.ESI_1);

        // Position 2 should be r1 (arrived at -30m vs -25m for r3 with same score 60)
        assertThat(queue.get(1).triageRecordId()).isEqualTo(101L);

        // Position 3 should be r3
        assertThat(queue.get(2).triageRecordId()).isEqualTo(103L);
    }

    @Test
    @DisplayName("Should assign highest priority patient to next available physician from FIFO DoctorQueue")
    void testDispatchNextPatientToDoctor() {
        TriageRecord r = new TriageRecord(patient1, new Vitals(90, 130, 85, 18, 96.0, 37.0, 7, 15),
                "Severe headache", EsiLevel.ESI_2, 85, 2, "Emergent", LocalDateTime.now());
        r.setId(201L);

        when(triageRecordRepository.findById(201L)).thenReturn(Optional.of(r));
        when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor1));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(i -> i.getArgument(0));

        queueService.registerDoctorAvailable(doctor1);
        queueService.enqueue(201L, "NURSE_1");

        AssignmentResponse assignment = queueService.assignNextPatientToDoctor(null, "DISPATCHER");

        assertThat(assignment).isNotNull();
        assertThat(assignment.triageRecordId()).isEqualTo(201L);
        assertThat(assignment.doctorId()).isEqualTo(10L);
        assertThat(assignment.doctorName()).isEqualTo("Dr. Gregory House");

        // Queue should now be empty and doctor unavailable
        assertThat(queueService.getQueueSize()).isEqualTo(0);
        assertThat(doctor1.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should throw QueueEmptyException when dispatching with no waiting patients")
    void testDispatchEmptyQueue() {
        queueService.registerDoctorAvailable(doctor1);

        assertThatThrownBy(() -> queueService.assignNextPatientToDoctor(null, "DISPATCHER"))
                .isInstanceOf(QueueEmptyException.class);
    }

    @Test
    @DisplayName("Should throw NoDoctorAvailableException when dispatching without available doctors")
    void testDispatchNoDoctorAvailable() {
        TriageRecord r = new TriageRecord(patient1, new Vitals(80, 120, 80, 16, 98.0, 37.0, 0, 15),
                "Mild rash", EsiLevel.ESI_5, 20, 0, "Non-Urgent", LocalDateTime.now());
        r.setId(301L);

        when(triageRecordRepository.findById(301L)).thenReturn(Optional.of(r));
        when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(i -> i.getArgument(0));

        queueService.enqueue(301L, "NURSE_1");

        assertThatThrownBy(() -> queueService.assignNextPatientToDoctor(null, "DISPATCHER"))
                .isInstanceOf(NoDoctorAvailableException.class);

        // Patient should still remain in the queue
        assertThat(queueService.getQueueSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should successfully execute Undo for doctor assignment and restore state")
    void testUndoDoctorAssignment() {
        TriageRecord r = new TriageRecord(patient1, new Vitals(85, 120, 80, 16, 98.0, 37.0, 6, 15),
                "Abdominal Pain", EsiLevel.ESI_3, 65, 2, "Urgent", LocalDateTime.now());
        r.setId(401L);

        when(triageRecordRepository.findById(401L)).thenReturn(Optional.of(r));
        when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor1));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(i -> i.getArgument(0));

        queueService.registerDoctorAvailable(doctor1);
        queueService.enqueue(401L, "NURSE_1");
        queueService.assignNextPatientToDoctor(null, "DISPATCHER");

        assertThat(queueService.getQueueSize()).isEqualTo(0);
        assertThat(doctor1.isAvailable()).isFalse();

        // Perform Undo
        var undoneMemento = queueService.undoLastAction();

        assertThat(undoneMemento).isNotNull();
        assertThat(queueService.getQueueSize()).isEqualTo(1);
        assertThat(r.getStatus()).isEqualTo(TriageStatus.WAITING_IN_QUEUE);
        assertThat(r.getAssignedDoctor()).isNull();
        assertThat(doctor1.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should provide O(1) in-memory fast patient lookup")
    void testPatientLookupCache() {
        TriageRecord r = new TriageRecord(patient1, new Vitals(80, 120, 80, 16, 98.0, 37.0, 0, 15),
                "Routine checkup", EsiLevel.ESI_5, 20, 0, "Non-Urgent", LocalDateTime.now());
        r.setId(501L);

        when(triageRecordRepository.findById(501L)).thenReturn(Optional.of(r));
        when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(i -> i.getArgument(0));

        queueService.enqueue(501L, "NURSE_1");

        var lookedUp = queueService.lookupByPatientId(1L);
        assertThat(lookedUp).isPresent();
        assertThat(lookedUp.get().getId()).isEqualTo(501L);
    }
}
