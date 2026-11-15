package com.ed.triage.repository;

import com.ed.triage.entity.Doctor;
import com.ed.triage.enums.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmployeeId(String employeeId);
    List<Doctor> findByAvailableTrue();
    List<Doctor> findBySpecialty(Specialty specialty);
}
