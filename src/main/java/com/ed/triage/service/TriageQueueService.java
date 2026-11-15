package com.ed.triage.service;

import com.ed.triage.dto.response.AssignmentResponse;
import com.ed.triage.dto.response.QueueItemResponse;
import com.ed.triage.entity.AuditLog;
import com.ed.triage.entity.Doctor;
import com.ed.triage.entity.TriageRecord;
import com.ed.triage.enums.AuditActionType;
import com.ed.triage.enums.TriageStatus;
import com.ed.triage.exception.NoDoctorAvailableException;
import com.ed.triage.exception.QueueEmptyException;
import com.ed.triage.exception.ResourceNotFoundException;
import com.ed.triage.repository.AuditLogRepository;
import com.ed.triage.repository.DoctorRepository;
import com.ed.triage.repository.TriageRecordRepository;
import com.ed.triage.service.memento.AuditActionMemento;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * High-performance Emergency Triage Queue Management Engine.
 *
 * Core Data Structures mapped:
 * 1. PriorityQueue: Orders waiting patients by Severity Priority Score DESC, then Arrival Time ASC (FIFO tie-breaker).
 * 2. HashMap (ConcurrentHashMap): O(1) in-memory instant patient lookup and active triage tracking.
 * 3. Queue (ConcurrentLinkedQueue): FIFO queue managing real-time physician availability for automated dispatch.
 * 4. Stack (ArrayDeque): LIFO undo stack preserving state snapshots for error recovery and regulatory clinical audit trails.
 */
@Service
public class TriageQueueService {

    private static final Logger log = LoggerFactory.getLogger(TriageQueueService.class);

    // Comparator for emergency queue: Highest priority score first. In case of identical scores, earlier arrival wins.
    public static final Comparator<TriageRecord> TRIAGE_COMPARATOR = Comparator
            .comparingInt(TriageRecord::getPriorityScore).reversed()
            .thenComparing(TriageRecord::getArrivalTime);

    // 1. PriorityQueue for waiting patients
    private final PriorityQueue<TriageRecord> priorityQueue = new PriorityQueue<>(TRIAGE_COMPARATOR);
    private final Object queueLock = new Object();

    // 2. HashMap for O(1) patient lookup by Patient ID -> Active TriageRecord
    private final Map<Long, TriageRecord> patientLookupCache = new ConcurrentHashMap<>();
    // Lookup by TriageRecord ID -> TriageRecord
    private final Map<Long, TriageRecord> recordLookupCache = new ConcurrentHashMap<>();

    // 3. Queue for available doctors (FIFO)
    private final Queue<Doctor> availableDoctorQueue = new ConcurrentLinkedQueue<>();

    // 4. Stack for audit trail undo operations (LIFO)
    private final Deque<AuditActionMemento> undoStack = new ArrayDeque<>();
    private final Object stackLock = new Object();

    private final TriageRecordRepository triageRecordRepository;
    private final DoctorRepository doctorRepository;
    private final AuditLogRepository auditLogRepository;
    private final MeterRegistry meterRegistry;

    private Counter enqueueCounter;
    private Counter assignmentCounter;
    private Counter undoCounter;

    public TriageQueueService(TriageRecordRepository triageRecordRepository,
                              DoctorRepository doctorRepository,
                              AuditLogRepository auditLogRepository,
                              MeterRegistry meterRegistry) {
        this.triageRecordRepository = triageRecordRepository;
        this.doctorRepository = doctorRepository;
        this.auditLogRepository = auditLogRepository;
        this.meterRegistry = meterRegistry;
        initMetrics();
    }

    private void initMetrics() {
        this.enqueueCounter = meterRegistry.counter("triage.queue.enqueued.total");
        this.assignmentCounter = meterRegistry.counter("triage.queue.assigned.total");
        this.undoCounter = meterRegistry.counter("triage.queue.undo.total");

        meterRegistry.gauge("triage.queue.waiting.size", this, service -> service.getQueueSize());
        meterRegistry.gauge("triage.doctors.available.count", this, service -> service.getAvailableDoctorCount());
    }

    @PostConstruct
    public void warmCachesAndQueues() {
        log.info("Initializing in-memory triage data structures from persistent store...");

        // Load all available doctors into the FIFO queue
        List<Doctor> doctors = doctorRepository.findByAvailableTrue();
        availableDoctorQueue.clear();
        availableDoctorQueue.addAll(doctors);
        log.info("Initialized Doctor FIFO Queue with {} available physicians", doctors.size());

        // Load active waiting triage records into PriorityQueue and HashMap caches
        List<TriageRecord> waitingRecords = triageRecordRepository.findAllWaitingInQueueOrdered();
        synchronized (queueLock) {
            priorityQueue.clear();
            patientLookupCache.clear();
            recordLookupCache.clear();

            for (TriageRecord record : waitingRecords) {
                priorityQueue.offer(record);
                recordLookupCache.put(record.getId(), record);
                if (record.getPatient() != null) {
                    patientLookupCache.put(record.getPatient().getId(), record);
                }
            }
        }
        log.info("Initialized Triage PriorityQueue and O(1) HashMaps with {} waiting patients", waitingRecords.size());
    }

    /**
     * Adds an assessed patient record into the emergency PriorityQueue.
     */
    @Transactional
    public TriageRecord enqueue(Long triageRecordId, String operator) {
        TriageRecord record = triageRecordRepository.findById(triageRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Triage record not found: " + triageRecordId));

        record.setStatus(TriageStatus.WAITING_IN_QUEUE);
        TriageRecord savedRecord = triageRecordRepository.save(record);

        synchronized (queueLock) {
            // Remove previous instance if present to avoid duplicates
            priorityQueue.removeIf(r -> r.getId().equals(savedRecord.getId()));
            priorityQueue.offer(savedRecord);

            recordLookupCache.put(savedRecord.getId(), savedRecord);
            if (savedRecord.getPatient() != null) {
                patientLookupCache.put(savedRecord.getPatient().getId(), savedRecord);
            }
        }

        // Push snapshot to undo stack
        AuditActionMemento memento = AuditActionMemento.forEnqueue(savedRecord, operator);
        synchronized (stackLock) {
            undoStack.push(memento);
        }

        // Log audit trail
        auditLogRepository.save(new AuditLog(
                AuditActionType.ENQUEUED,
                savedRecord.getId(),
                savedRecord.getPatient() != null ? savedRecord.getPatient().getId() : null,
                null,
                "Patient enqueued into triage priority queue with priority score: " + savedRecord.getPriorityScore(),
                TriageStatus.ASSESSED,
                TriageStatus.WAITING_IN_QUEUE,
                operator
        ));

        enqueueCounter.increment();
        log.info("Enqueued Patient {} (MRN: {}) [ESI: {}, Score: {}] to PriorityQueue. Queue size: {}",
                savedRecord.getPatient() != null ? savedRecord.getPatient().getFullName() : "N/A",
                savedRecord.getPatient() != null ? savedRecord.getPatient().getMrn() : "N/A",
                savedRecord.getEsiLevel(),
                savedRecord.getPriorityScore(),
                getQueueSize());

        return savedRecord;
    }

    /**
     * Returns an ordered snapshot of all patients currently waiting in the PriorityQueue.
     */
    public List<QueueItemResponse> getWaitingQueueSnapshot() {
        List<TriageRecord> sortedList;
        synchronized (queueLock) {
            // Copy elements and sort according to Priority comparator to preserve queue state
            sortedList = new ArrayList<>(priorityQueue);
            sortedList.sort(TRIAGE_COMPARATOR);
        }

        List<QueueItemResponse> responses = new ArrayList<>(sortedList.size());
        for (int i = 0; i < sortedList.size(); i++) {
            responses.add(QueueItemResponse.fromEntity(i + 1, sortedList.get(i)));
        }
        return responses;
    }

    /**
     * Dispatches the next highest-priority patient to an available physician.
     */
    @Transactional
    public AssignmentResponse assignNextPatientToDoctor(Long specificDoctorId, String operator) {
        TriageRecord nextPatientRecord;
        synchronized (queueLock) {
            if (priorityQueue.isEmpty()) {
                throw new QueueEmptyException("No patients currently waiting in the emergency triage queue");
            }
            nextPatientRecord = priorityQueue.poll();
        }

        Doctor assignedDoctor = null;

        if (specificDoctorId != null) {
            assignedDoctor = doctorRepository.findById(specificDoctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Physician not found: " + specificDoctorId));
            if (!assignedDoctor.isAvailable()) {
                // Re-enqueue patient if doctor is not available
                synchronized (queueLock) {
                    priorityQueue.offer(nextPatientRecord);
                }
                throw new NoDoctorAvailableException("Requested physician " + assignedDoctor.getName() + " is currently busy or unavailable");
            }
            // Remove from available doctor queue
            availableDoctorQueue.removeIf(d -> d.getId().equals(specificDoctorId));
        } else {
            // Poll next available doctor from FIFO Queue
            while (!availableDoctorQueue.isEmpty() && assignedDoctor == null) {
                Doctor candidate = availableDoctorQueue.poll();
                if (candidate != null) {
                    // Re-verify freshness from database
                    Optional<Doctor> freshDoctor = doctorRepository.findById(candidate.getId());
                    if (freshDoctor.isPresent() && freshDoctor.get().isAvailable()) {
                        assignedDoctor = freshDoctor.get();
                    }
                }
            }

            if (assignedDoctor == null) {
                // Re-enqueue patient back into PriorityQueue since no doctor could be dispatched
                synchronized (queueLock) {
                    priorityQueue.offer(nextPatientRecord);
                }
                throw new NoDoctorAvailableException("All emergency physicians are currently engaged with patients. No doctor available.");
            }
        }

        // Update Doctor state
        assignedDoctor.setAvailable(false);
        assignedDoctor.setActiveTriageRecordId(nextPatientRecord.getId());
        assignedDoctor.setLastAssignedAt(LocalDateTime.now());
        doctorRepository.save(assignedDoctor);

        // Update TriageRecord state
        nextPatientRecord.setStatus(TriageStatus.IN_CONSULTATION);
        nextPatientRecord.setAssignedDoctor(assignedDoctor);
        nextPatientRecord.setConsultationStartedAt(LocalDateTime.now());
        TriageRecord savedRecord = triageRecordRepository.save(nextPatientRecord);

        // Update caches
        recordLookupCache.put(savedRecord.getId(), savedRecord);
        if (savedRecord.getPatient() != null) {
            patientLookupCache.put(savedRecord.getPatient().getId(), savedRecord);
        }

        // Push to undo stack
        AuditActionMemento memento = AuditActionMemento.forDoctorAssignment(savedRecord, assignedDoctor.getId(), operator);
        synchronized (stackLock) {
            undoStack.push(memento);
        }

        // Audit logging
        auditLogRepository.save(new AuditLog(
                AuditActionType.ASSIGNED_TO_DOCTOR,
                savedRecord.getId(),
                savedRecord.getPatient() != null ? savedRecord.getPatient().getId() : null,
                assignedDoctor.getId(),
                "Assigned to Dr. " + assignedDoctor.getName() + " (" + assignedDoctor.getSpecialty() + ")",
                TriageStatus.WAITING_IN_QUEUE,
                TriageStatus.IN_CONSULTATION,
                operator
        ));

        assignmentCounter.increment();
        log.info("Dispatched ESI {} Patient {} to Dr. {}",
                savedRecord.getEsiLevel(),
                savedRecord.getPatient() != null ? savedRecord.getPatient().getFullName() : "N/A",
                assignedDoctor.getName());

        return AssignmentResponse.from(savedRecord, assignedDoctor, "Patient successfully assigned to emergency physician");
    }

    /**
     * Updates the status of a triage record and ensures PriorityQueue/cache integrity.
     */
    @Transactional
    public TriageRecord updateStatus(Long triageRecordId, TriageStatus newStatus, String clinicalNotes, String operator) {
        TriageRecord record = triageRecordRepository.findById(triageRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Triage record not found: " + triageRecordId));

        TriageStatus previousStatus = record.getStatus();
        if (previousStatus == newStatus) {
            return record;
        }

        record.setStatus(newStatus);
        if (clinicalNotes != null && !clinicalNotes.isBlank()) {
            record.setClinicalNotes(clinicalNotes);
        }

        if (newStatus == TriageStatus.COMPLETED || newStatus == TriageStatus.CANCELLED || newStatus == TriageStatus.TRANSFERRED) {
            record.setCompletedAt(LocalDateTime.now());
            // If patient had an assigned doctor, free the doctor back into the available doctor queue
            if (record.getAssignedDoctor() != null) {
                Doctor doctor = record.getAssignedDoctor();
                doctor.setAvailable(true);
                doctor.setActiveTriageRecordId(null);
                doctorRepository.save(doctor);
                registerDoctorAvailable(doctor);
            }
        }

        TriageRecord savedRecord = triageRecordRepository.save(record);

        synchronized (queueLock) {
            if (newStatus == TriageStatus.WAITING_IN_QUEUE) {
                priorityQueue.removeIf(r -> r.getId().equals(savedRecord.getId()));
                priorityQueue.offer(savedRecord);
            } else {
                priorityQueue.removeIf(r -> r.getId().equals(savedRecord.getId()));
            }

            recordLookupCache.put(savedRecord.getId(), savedRecord);
            if (savedRecord.getPatient() != null) {
                patientLookupCache.put(savedRecord.getPatient().getId(), savedRecord);
            }
        }

        // Push snapshot to undo stack
        AuditActionMemento memento = AuditActionMemento.forStatusUpdate(savedRecord, previousStatus, newStatus, operator);
        synchronized (stackLock) {
            undoStack.push(memento);
        }

        auditLogRepository.save(new AuditLog(
                AuditActionType.STATUS_UPDATED,
                savedRecord.getId(),
                savedRecord.getPatient() != null ? savedRecord.getPatient().getId() : null,
                savedRecord.getAssignedDoctor() != null ? savedRecord.getAssignedDoctor().getId() : null,
                "Status updated from " + previousStatus + " to " + newStatus + (clinicalNotes != null ? " | Notes: " + clinicalNotes : ""),
                previousStatus,
                newStatus,
                operator
        ));

        log.info("Updated TriageRecord {} status: {} -> {}", triageRecordId, previousStatus, newStatus);
        return savedRecord;
    }

    /**
     * Executes an Undo operation by popping the last triage state snapshot from the Stack.
     */
    @Transactional
    public AuditActionMemento undoLastAction() {
        AuditActionMemento memento;
        synchronized (stackLock) {
            if (undoStack.isEmpty()) {
                throw new IllegalStateException("No actions available on the undo stack");
            }
            memento = undoStack.pop();
        }

        log.info("Executing Undo for action: {} on TriageRecord {}", memento.actionType(), memento.triageRecordId());

        TriageRecord record = triageRecordRepository.findById(memento.triageRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Cannot undo action for missing triage record: " + memento.triageRecordId()));

        TriageStatus targetPreviousStatus = memento.previousStatus();

        if (memento.actionType() == AuditActionType.ASSIGNED_TO_DOCTOR) {
            // Revert doctor assignment: Free the doctor and put record back in PriorityQueue
            if (memento.newDoctorId() != null) {
                doctorRepository.findById(memento.newDoctorId()).ifPresent(doctor -> {
                    doctor.setAvailable(true);
                    doctor.setActiveTriageRecordId(null);
                    doctorRepository.save(doctor);
                    registerDoctorAvailable(doctor);
                });
            }
            record.setAssignedDoctor(null);
            record.setConsultationStartedAt(null);
            record.setStatus(targetPreviousStatus);
            triageRecordRepository.save(record);

            synchronized (queueLock) {
                priorityQueue.removeIf(r -> r.getId().equals(record.getId()));
                priorityQueue.offer(record);
            }
        } else if (memento.actionType() == AuditActionType.ENQUEUED) {
            // Revert enqueue: Remove from PriorityQueue and restore to ASSESSED
            record.setStatus(targetPreviousStatus);
            triageRecordRepository.save(record);

            synchronized (queueLock) {
                priorityQueue.removeIf(r -> r.getId().equals(record.getId()));
            }
        } else if (memento.actionType() == AuditActionType.STATUS_UPDATED) {
            record.setStatus(targetPreviousStatus);
            triageRecordRepository.save(record);

            synchronized (queueLock) {
                if (targetPreviousStatus == TriageStatus.WAITING_IN_QUEUE) {
                    priorityQueue.removeIf(r -> r.getId().equals(record.getId()));
                    priorityQueue.offer(record);
                } else {
                    priorityQueue.removeIf(r -> r.getId().equals(record.getId()));
                }
            }
        }

        // Update lookup caches
        recordLookupCache.put(record.getId(), record);
        if (record.getPatient() != null) {
            patientLookupCache.put(record.getPatient().getId(), record);
        }

        // Record Undo in Audit Log
        auditLogRepository.save(new AuditLog(
                AuditActionType.UNDO_EXECUTED,
                record.getId(),
                memento.patientId(),
                memento.newDoctorId(),
                "UNDO executed for " + memento.actionType() + ". Reverted status to " + targetPreviousStatus,
                memento.newStatus(),
                targetPreviousStatus,
                memento.operator()
        ));

        undoCounter.increment();
        return memento;
    }

    /**
     * O(1) in-memory fast lookup for active patient triage record.
     */
    public Optional<TriageRecord> lookupByPatientId(Long patientId) {
        TriageRecord cached = patientLookupCache.get(patientId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return triageRecordRepository.findActiveTriageByPatientId(patientId);
    }

    /**
     * O(1) in-memory fast lookup for triage record by ID.
     */
    public Optional<TriageRecord> lookupByRecordId(Long recordId) {
        TriageRecord cached = recordLookupCache.get(recordId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return triageRecordRepository.findById(recordId);
    }

    /**
     * Registers a doctor as available and enqueues them in the available doctor FIFO queue.
     */
    public void registerDoctorAvailable(Doctor doctor) {
        if (doctor != null && doctor.isAvailable()) {
            availableDoctorQueue.removeIf(d -> d.getId().equals(doctor.getId()));
            availableDoctorQueue.offer(doctor);
            log.debug("Doctor {} added to available FIFO queue. Available count: {}", doctor.getName(), availableDoctorQueue.size());
        }
    }

    /**
     * Removes doctor from the available FIFO queue (e.g. going off-duty or busy).
     */
    public void unregisterDoctor(Long doctorId) {
        availableDoctorQueue.removeIf(d -> d.getId().equals(doctorId));
    }

    public int getQueueSize() {
        synchronized (queueLock) {
            return priorityQueue.size();
        }
    }

    public int getAvailableDoctorCount() {
        return availableDoctorQueue.size();
    }

    public int getUndoStackDepth() {
        synchronized (stackLock) {
            return undoStack.size();
        }
    }

    public List<Doctor> getAvailableDoctors() {
        return new ArrayList<>(availableDoctorQueue);
    }
}
