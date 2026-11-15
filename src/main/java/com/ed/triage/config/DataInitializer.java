package com.ed.triage.config;

import com.ed.triage.entity.Department;
import com.ed.triage.entity.Doctor;
import com.ed.triage.entity.Patient;
import com.ed.triage.enums.Specialty;
import com.ed.triage.repository.DepartmentRepository;
import com.ed.triage.repository.DoctorRepository;
import com.ed.triage.repository.PatientRepository;
import com.ed.triage.service.TriageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds default Emergency Department units, physicians, and sample patient records.
 */
@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TriageQueueService triageQueueService;

    public DataInitializer(DepartmentRepository departmentRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository,
                           TriageQueueService triageQueueService) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.triageQueueService = triageQueueService;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() == 0) {
            log.info("Seeding Emergency Department units...");
            Department resus = departmentRepository.save(new Department("ED-RESUS", "Resuscitation & Critical Care Unit", "Ground Floor - Bay 1-4", 6));
            Department trauma = departmentRepository.save(new Department("ED-TRAUMA", "Acute Trauma Center", "Ground Floor - Trauma Suites A & B", 8));
            Department acute = departmentRepository.save(new Department("ED-ACUTE", "Acute Adult Emergency Ward", "Ground Floor - Yellow Zone", 20));
            Department pediatric = departmentRepository.save(new Department("ED-PEDIATRIC", "Pediatric Emergency Department", "Ground Floor - East Wing", 12));
            Department fasttrack = departmentRepository.save(new Department("ED-FASTTRACK", "Fast Track & Minor Injuries Unit", "Ground Floor - Green Zone", 15));

            log.info("Seeding Emergency Physicians...");
            Doctor d1 = doctorRepository.save(new Doctor("DOC-101", "Dr. Sarah Mitchell", Specialty.EMERGENCY_MEDICINE, resus));
            Doctor d2 = doctorRepository.save(new Doctor("DOC-102", "Dr. James Rodriguez", Specialty.TRAUMA_SURGERY, trauma));
            Doctor d3 = doctorRepository.save(new Doctor("DOC-103", "Dr. Emily Chen", Specialty.CARDIOLOGY, acute));
            Doctor d4 = doctorRepository.save(new Doctor("DOC-104", "Dr. Marcus Vance", Specialty.PEDIATRICS, pediatric));
            Doctor d5 = doctorRepository.save(new Doctor("DOC-105", "Dr. Priya Patel", Specialty.EMERGENCY_MEDICINE, fasttrack));

            for (Doctor d : List.of(d1, d2, d3, d4, d5)) {
                triageQueueService.registerDoctorAvailable(d);
            }
        }

        if (patientRepository.count() == 0) {
            log.info("Seeding Registered Patients...");
            patientRepository.save(new Patient("MRN-89210", "Arthur", "Pendleton", LocalDate.of(1952, 4, 12), "Male", "+1-555-0192", "Penicillin", "Coronary artery disease, Type 2 Diabetes"));
            patientRepository.save(new Patient("MRN-67341", "Elena", "Rostova", LocalDate.of(1988, 11, 23), "Female", "+1-555-0143", "None", "Asthma"));
            patientRepository.save(new Patient("MRN-45129", "David", "Kim", LocalDate.of(1995, 7, 8), "Male", "+1-555-0177", "Sulfa drugs", "None"));
            patientRepository.save(new Patient("MRN-33820", "Maya", "Lin", LocalDate.of(2020, 2, 15), "Female", "+1-555-0188", "Peanuts", "None"));
        }
    }
}
