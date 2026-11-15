package com.ed.triage.controller;

import com.ed.triage.dto.request.PatientCreateRequest;
import com.ed.triage.dto.response.ApiResponse;
import com.ed.triage.dto.response.PatientResponse;
import com.ed.triage.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> registerPatient(@Valid @RequestBody PatientCreateRequest request) {
        PatientResponse response = patientService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Patient registered successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        List<PatientResponse> list = patientService.getAllPatients();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable Long id) {
        PatientResponse response = patientService.getPatientById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/mrn/{mrn}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientByMrn(@PathVariable String mrn) {
        PatientResponse response = patientService.getPatientByMrn(mrn);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
