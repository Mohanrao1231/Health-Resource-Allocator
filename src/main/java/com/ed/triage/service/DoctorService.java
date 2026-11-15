package com.ed.triage.service;

import com.ed.triage.dto.request.DoctorCreateRequest;
import com.ed.triage.dto.response.DoctorResponse;
import com.ed.triage.entity.Department;
import com.ed.triage.entity.Doctor;
import com.ed.triage.exception.ResourceNotFoundException;
import com.ed.triage.repository.DepartmentRepository;
import com.ed.triage.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final TriageQueueService triageQueueService;

    public DoctorService(DoctorRepository doctorRepository,
                         DepartmentRepository departmentRepository,
                         TriageQueueService triageQueueService) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.triageQueueService = triageQueueService;
    }

    @Transactional
    public DoctorResponse createDoctor(DoctorCreateRequest request) {
        if (doctorRepository.findByEmployeeId(request.employeeId()).isPresent()) {
            throw new IllegalArgumentException("Doctor with employee ID " + request.employeeId() + " already exists");
        }

        Department department = null;
        if (request.departmentCode() != null && !request.departmentCode().isBlank()) {
            department = departmentRepository.findByCode(request.departmentCode())
                    .orElse(null);
        }

        Doctor doctor = new Doctor(request.employeeId(), request.name(), request.specialty(), department);
        Doctor saved = doctorRepository.save(doctor);

        triageQueueService.registerDoctorAvailable(saved);
        log.info("Registered emergency physician: Dr. {} ({})", saved.getName(), saved.getSpecialty());
        return DoctorResponse.fromEntity(saved);
    }

    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(DoctorResponse::fromEntity)
                .toList();
    }

    public DoctorResponse getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .map(DoctorResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Physician not found with ID: " + id));
    }

    public List<DoctorResponse> getAvailableDoctors() {
        return triageQueueService.getAvailableDoctors().stream()
                .map(DoctorResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DoctorResponse updateAvailability(Long id, boolean available) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Physician not found with ID: " + id));

        doctor.setAvailable(available);
        if (!available) {
            triageQueueService.unregisterDoctor(id);
        } else {
            triageQueueService.registerDoctorAvailable(doctor);
        }

        Doctor updated = doctorRepository.save(doctor);
        log.info("Physician {} availability toggled to: {}", doctor.getName(), available);
        return DoctorResponse.fromEntity(updated);
    }
}
