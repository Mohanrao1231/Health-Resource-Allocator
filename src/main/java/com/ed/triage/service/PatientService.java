package com.ed.triage.service;

import com.ed.triage.dto.request.PatientCreateRequest;
import com.ed.triage.dto.response.PatientResponse;
import com.ed.triage.entity.Patient;
import com.ed.triage.exception.ResourceNotFoundException;
import com.ed.triage.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponse registerPatient(PatientCreateRequest request) {
        if (patientRepository.existsByMrn(request.mrn())) {
            throw new IllegalArgumentException("Patient with MRN " + request.mrn() + " already exists in hospital registry");
        }

        Patient patient = new Patient(
                request.mrn(),
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.gender(),
                request.contactNumber(),
                request.allergies(),
                request.medicalHistory()
        );

        Patient saved = patientRepository.save(patient);
        log.info("Registered patient {} (MRN: {})", saved.getFullName(), saved.getMrn());
        return PatientResponse.fromEntity(saved);
    }

    public PatientResponse getPatientById(Long id) {
        return patientRepository.findById(id)
                .map(PatientResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
    }

    public PatientResponse getPatientByMrn(String mrn) {
        return patientRepository.findByMrn(mrn)
                .map(PatientResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with MRN: " + mrn));
    }

    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(PatientResponse::fromEntity)
                .toList();
    }
}
