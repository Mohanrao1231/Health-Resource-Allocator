package com.ed.triage.controller;

import com.ed.triage.dto.request.DoctorAvailabilityRequest;
import com.ed.triage.dto.request.DoctorCreateRequest;
import com.ed.triage.dto.response.ApiResponse;
import com.ed.triage.dto.response.DoctorResponse;
import com.ed.triage.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        DoctorResponse created = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Doctor registered successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.ok(doctors));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAvailableDoctors() {
        List<DoctorResponse> doctors = doctorService.getAvailableDoctors();
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + doctors.size() + " available physician(s)", doctors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        DoctorResponse doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.ok(doctor));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DoctorAvailabilityRequest request) {
        DoctorResponse updated = doctorService.updateAvailability(id, request.available());
        return ResponseEntity.ok(ApiResponse.ok("Physician availability updated", updated));
    }
}
