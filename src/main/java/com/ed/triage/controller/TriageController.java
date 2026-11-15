package com.ed.triage.controller;

import com.ed.triage.dto.request.AssessmentRequest;
import com.ed.triage.dto.request.EnqueueRequest;
import com.ed.triage.dto.request.StatusUpdateRequest;
import com.ed.triage.dto.response.ApiResponse;
import com.ed.triage.dto.response.AssessmentResponse;
import com.ed.triage.dto.response.AssignmentResponse;
import com.ed.triage.dto.response.AuditLogResponse;
import com.ed.triage.dto.response.QueueItemResponse;
import com.ed.triage.entity.AuditLog;
import com.ed.triage.entity.Patient;
import com.ed.triage.entity.TriageRecord;
import com.ed.triage.enums.AuditActionType;
import com.ed.triage.enums.TriageStatus;
import com.ed.triage.exception.ResourceNotFoundException;
import com.ed.triage.repository.AuditLogRepository;
import com.ed.triage.repository.PatientRepository;
import com.ed.triage.repository.TriageRecordRepository;
import com.ed.triage.service.AuditService;
import com.ed.triage.service.TriageQueueService;
import com.ed.triage.service.TriageScoringService;
import com.ed.triage.service.memento.AuditActionMemento;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Emergency Department Patient Triage, Priority Queue Operations, and Clinical Auditing.
 */
@RestController
@RequestMapping("/triage")
public class TriageController {

    private static final Logger log = LoggerFactory.getLogger(TriageController.class);

    private final TriageScoringService scoringService;
    private final TriageQueueService queueService;
    private final PatientRepository patientRepository;
    private final TriageRecordRepository triageRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    public TriageController(TriageScoringService scoringService,
                            TriageQueueService queueService,
                            PatientRepository patientRepository,
                            TriageRecordRepository triageRecordRepository,
                            AuditLogRepository auditLogRepository,
                            AuditService auditService) {
        this.scoringService = scoringService;
        this.queueService = queueService;
        this.patientRepository = patientRepository;
        this.triageRecordRepository = triageRecordRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    /**
     * POST /triage/assess
     * Assesses patient clinical vitals and chief complaint, computes ESI level and priority score,
     * persists TriageRecord, and optionally enqueues automatically into the PriorityQueue.
     */
    @PostMapping("/assess")
    @Transactional
    public ResponseEntity<ApiResponse<AssessmentResponse>> assessPatient(@Valid @RequestBody AssessmentRequest request) {
        log.info("Received triage assessment request for complaint: '{}'", request.chiefComplaint());

        // Resolve or create patient
        Patient patient = resolvePatient(request);

        // Run clinical ESI algorithm
        TriageScoringService.TriageAssessmentResult result = scoringService.calculateScore(
                request.vitals(),
                request.chiefComplaint(),
                patient.getAge(),
                request.estimatedResources()
        );

        LocalDateTime arrivalTime = request.arrivalTime() != null ? request.arrivalTime() : LocalDateTime.now();

        // Build and persist TriageRecord
        TriageRecord record = new TriageRecord(
                patient,
                request.vitals(),
                request.chiefComplaint(),
                result.esiLevel(),
                result.priorityScore(),
                result.estimatedResources(),
                request.clinicalNotes() != null ? request.clinicalNotes() : result.clinicalRationale(),
                arrivalTime
        );

        TriageRecord savedRecord = triageRecordRepository.save(record);

        // Audit log creation
        auditLogRepository.save(new AuditLog(
                AuditActionType.ASSESSMENT_CREATED,
                savedRecord.getId(),
                patient.getId(),
                null,
                "Assessed as " + result.esiLevel() + " (Score: " + result.priorityScore() + "). " + result.clinicalRationale(),
                null,
                TriageStatus.ASSESSED,
                "TRIAGE_NURSE"
        ));

        // Auto-enqueue if requested
        if (request.autoEnqueue()) {
            savedRecord = queueService.enqueue(savedRecord.getId(), "AUTO_ENQUEUE");
        }

        AssessmentResponse responseDto = AssessmentResponse.fromEntity(savedRecord);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Triage assessment completed successfully", responseDto));
    }

    /**
     * POST /triage/queue
     * Adds an assessed patient to the Emergency Waiting Priority Queue.
     */
    @PostMapping("/queue")
    public ResponseEntity<ApiResponse<AssessmentResponse>> enqueuePatient(@Valid @RequestBody EnqueueRequest request) {
        log.info("Enqueueing triage record: {}", request.triageRecordId());
        String operator = request.operator() != null ? request.operator() : "TRIAGE_OFFICER";
        TriageRecord record = queueService.enqueue(request.triageRecordId(), operator);
        return ResponseEntity.ok(ApiResponse.ok("Patient placed into priority waiting queue", AssessmentResponse.fromEntity(record)));
    }

    /**
     * GET /triage/queue
     * Returns an ordered list of all patients currently waiting in the Priority Queue.
     */
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<QueueItemResponse>>> getWaitingQueue() {
        List<QueueItemResponse> queueList = queueService.getWaitingQueueSnapshot();
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + queueList.size() + " waiting patient(s)", queueList));
    }

    /**
     * POST /triage/next
     * Dispatches the next highest-priority patient to an available physician.
     */
    @PostMapping("/next")
    public ResponseEntity<ApiResponse<AssignmentResponse>> assignNextPatient(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false, defaultValue = "DISPATCH_CONTROLLER") String operator) {
        log.info("Dispatching next priority patient (preferred doctorId: {})", doctorId);
        AssignmentResponse assignment = queueService.assignNextPatientToDoctor(doctorId, operator);
        return ResponseEntity.ok(ApiResponse.ok("Physician assigned successfully", assignment));
    }

    /**
     * PUT /triage/{id}/status
     * Updates the status of a triage encounter (e.g. IN_CONSULTATION, COMPLETED, CANCELLED).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AssessmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        String operator = request.operator() != null ? request.operator() : "ATTENDING_STAFF";
        TriageRecord updated = queueService.updateStatus(id, request.status(), request.clinicalNotes(), operator);
        return ResponseEntity.ok(ApiResponse.ok("Triage status updated to " + request.status(), AssessmentResponse.fromEntity(updated)));
    }

    /**
     * POST /triage/undo
     * Reverts the most recent triage action using the LIFO undo stack.
     */
    @PostMapping("/undo")
    public ResponseEntity<ApiResponse<String>> undoLastAction() {
        AuditActionMemento undoneMemento = queueService.undoLastAction();
        String msg = String.format("Successfully rolled back %s on Triage Record ID %d (Reverted to %s)",
                undoneMemento.actionType(), undoneMemento.triageRecordId(), undoneMemento.previousStatus());
        return ResponseEntity.ok(ApiResponse.ok(msg, msg));
    }

    /**
     * GET /triage/lookup/patient/{patientId}
     * O(1) in-memory fast patient triage lookup.
     */
    @GetMapping("/lookup/patient/{patientId}")
    public ResponseEntity<ApiResponse<AssessmentResponse>> lookupByPatientId(@PathVariable Long patientId) {
        TriageRecord record = queueService.lookupByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("No active triage record found for patient ID: " + patientId));
        return ResponseEntity.ok(ApiResponse.ok("Active triage record found", AssessmentResponse.fromEntity(record)));
    }

    /**
     * GET /triage/{id}
     * Retrieves full triage record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssessmentResponse>> getTriageRecordById(@PathVariable Long id) {
        TriageRecord record = triageRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Triage record not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok(AssessmentResponse.fromEntity(record)));
    }

    /**
     * GET /triage/audit-logs
     * Returns recent audit logs for regulatory and clinical compliance.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs() {
        List<AuditLogResponse> logs = auditService.getRecentAuditLogs();
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + logs.size() + " audit trail entry/entries", logs));
    }

    private Patient resolvePatient(AssessmentRequest request) {
        if (request.patientId() != null) {
            return patientRepository.findById(request.patientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.patientId()));
        }

        if (request.patientMrn() != null && !request.patientMrn().isBlank()) {
            return patientRepository.findByMrn(request.patientMrn())
                    .orElseGet(() -> createPatientFromRequest(request, request.patientMrn()));
        }

        // Auto-generate MRN for walk-in patient
        String autoMrn = "MRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return createPatientFromRequest(request, autoMrn);
    }

    private Patient createPatientFromRequest(AssessmentRequest request, String mrn) {
        Patient p = new Patient(
                mrn,
                request.firstName() != null ? request.firstName() : "Unknown",
                request.lastName() != null ? request.lastName() : "Patient",
                request.dateOfBirth() != null ? request.dateOfBirth() : java.time.LocalDate.now().minusYears(35),
                request.gender() != null ? request.gender() : "Unspecified",
                null,
                null,
                null
        );
        return patientRepository.save(p);
    }
}
