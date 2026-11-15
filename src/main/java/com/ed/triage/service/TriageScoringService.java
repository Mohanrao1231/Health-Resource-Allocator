package com.ed.triage.service;

import com.ed.triage.entity.Vitals;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.exception.InvalidVitalsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Set;

/**
 * Clinical decision support service implementing the Emergency Severity Index (ESI) version 4/5 triage algorithm.
 * Evaluates patient physiological vitals, chief complaints, danger zone vitals, and projected ED resource consumption.
 */
@Service
public class TriageScoringService {

    private static final Logger log = LoggerFactory.getLogger(TriageScoringService.class);

    // High risk keywords that immediately elevate suspicion for ESI Level 2
    private static final Set<String> CRITICAL_COMPLAINT_KEYWORDS = Set.of(
            "chest pain", "myocardial", "stroke", "cva", "altered mental", "anaphylaxis",
            "overdose", "poisoning", "active seizure", "severe asthma", "stridor",
            "massive bleeding", "hemorrhage", "gunshot", "stab wound", "major trauma",
            "syncope", "loss of consciousness", "acute dyspnea", "suicidal ideation", "severe burns"
    );

    public record TriageAssessmentResult(
            EsiLevel esiLevel,
            int priorityScore,
            int estimatedResources,
            String clinicalRationale
    ) {}

    /**
     * Calculates the ESI tier (1-5) and continuous priority score for queue ranking.
     *
     * @param vitals captured patient vital signs
     * @param chiefComplaint reason for emergency visit
     * @param patientAge patient's age in years
     * @param estimatedResourcesCount optional clinician-estimated resource count
     * @return TriageAssessmentResult containing ESI level, composite score, and rationale
     */
    public TriageAssessmentResult calculateScore(Vitals vitals, String chiefComplaint, int patientAge, Integer estimatedResourcesCount) {
        validateVitals(vitals);

        log.debug("Evaluating triage for complaint: '{}', age: {}, vitals: {}", chiefComplaint, patientAge, vitals);

        // Step 1: Decision Point A — Does this patient require immediate life-saving intervention? (ESI 1)
        if (requiresResuscitation(vitals, chiefComplaint)) {
            int score = calculateCompositePriorityScore(EsiLevel.ESI_1, vitals, patientAge);
            return new TriageAssessmentResult(
                    EsiLevel.ESI_1,
                    score,
                    Math.max(estimatedResourcesCount != null ? estimatedResourcesCount : 3, 3),
                    "ESI Level 1: Immediate life-saving resuscitation required. Unstable airway/breathing/circulation or profound unresponsiveness."
            );
        }

        // Step 2: Decision Point B — Is this a high-risk situation, confused/lethargic/disoriented, or in severe pain/distress? (ESI 2)
        String complaintLower = chiefComplaint != null ? chiefComplaint.toLowerCase(Locale.ROOT) : "";
        boolean isHighRiskComplaint = CRITICAL_COMPLAINT_KEYWORDS.stream().anyMatch(complaintLower::contains);
        boolean isSeverePain = vitals.getPainScore() != null && vitals.getPainScore() >= 8;
        boolean isAlteredMental = vitals.getGcsScore() != null && vitals.getGcsScore() < 14;
        boolean isDangerZoneVitals = hasDangerZoneVitals(vitals, patientAge);

        if (isHighRiskComplaint || isSeverePain || isAlteredMental || isDangerZoneVitals) {
            int score = calculateCompositePriorityScore(EsiLevel.ESI_2, vitals, patientAge);
            StringBuilder rationale = new StringBuilder("ESI Level 2: Emergent high-risk condition. Triggered by: ");
            if (isHighRiskComplaint) rationale.append("[High-risk clinical presentation] ");
            if (isDangerZoneVitals) rationale.append("[Danger-zone vital signs] ");
            if (isAlteredMental) rationale.append("[Altered mental state GCS=").append(vitals.getGcsScore()).append("] ");
            if (isSeverePain) rationale.append("[Severe acute pain=").append(vitals.getPainScore()).append("/10] ");

            return new TriageAssessmentResult(
                    EsiLevel.ESI_2,
                    score,
                    Math.max(estimatedResourcesCount != null ? estimatedResourcesCount : 2, 2),
                    rationale.toString().trim()
            );
        }

        // Step 3: Decision Point C — How many diagnostic/therapeutic resources are expected? (ESI 3, 4, or 5)
        int resources = determineEstimatedResources(complaintLower, estimatedResourcesCount);

        EsiLevel calculatedEsi;
        String rationale;

        if (resources >= 2) {
            // Further vital sign check: If ESI 3 vitals exceed pediatric/adult warning threshold, consider ESI 2 upgrade
            if (hasWarningZoneVitals(vitals, patientAge)) {
                int score = calculateCompositePriorityScore(EsiLevel.ESI_2, vitals, patientAge);
                return new TriageAssessmentResult(
                        EsiLevel.ESI_2,
                        score,
                        resources,
                        "ESI Level 2 (Upgraded from 3): Borderline vital signs with multiple resource requirements."
                );
            }
            calculatedEsi = EsiLevel.ESI_3;
            rationale = "ESI Level 3: Urgent. Stable vitals with 2 or more expected emergency resources.";
        } else if (resources == 1) {
            calculatedEsi = EsiLevel.ESI_4;
            rationale = "ESI Level 4: Less Urgent. Stable vitals with 1 projected diagnostic or simple therapeutic resource.";
        } else {
            calculatedEsi = EsiLevel.ESI_5;
            rationale = "ESI Level 5: Non-Urgent. Stable vitals, no emergency diagnostic resources required (e.g. exam only, prescription refill).";
        }

        int score = calculateCompositePriorityScore(calculatedEsi, vitals, patientAge);
        return new TriageAssessmentResult(calculatedEsi, score, resources, rationale);
    }

    /**
     * Checks criteria for ESI 1 (Immediate Resuscitation).
     */
    private boolean requiresResuscitation(Vitals vitals, String complaint) {
        if (complaint != null) {
            String lower = complaint.toLowerCase(Locale.ROOT);
            if (lower.contains("cardiac arrest") || lower.contains("respiratory arrest") ||
                lower.contains("unresponsive apnea") || lower.contains("pulseless")) {
                return true;
            }
        }

        // Profound hypoxia
        if (vitals.getOxygenSaturation() != null && vitals.getOxygenSaturation() < 85.0) {
            return true;
        }

        // Profound shock
        if (vitals.getSystolicBp() != null && vitals.getSystolicBp() > 0 && vitals.getSystolicBp() < 70) {
            return true;
        }

        // Severe coma / unresponsiveness
        if (vitals.getGcsScore() != null && vitals.getGcsScore() <= 8) {
            return true;
        }

        // Extreme heart rate derangements
        if (vitals.getHeartRate() != null && (vitals.getHeartRate() < 40 || vitals.getHeartRate() > 180)) {
            return true;
        }

        return false;
    }

    /**
     * Danger zone vitals indicative of severe decompensation (ESI 2).
     */
    private boolean hasDangerZoneVitals(Vitals vitals, int age) {
        // Oxygen desaturation below 92%
        if (vitals.getOxygenSaturation() != null && vitals.getOxygenSaturation() < 92.0) {
            return true;
        }

        // Severe tachypnea or hypoventilation
        if (vitals.getRespiratoryRate() != null && (vitals.getRespiratoryRate() > 30 || vitals.getRespiratoryRate() < 10)) {
            return true;
        }

        // Severe tachycardia or bradycardia
        if (vitals.getHeartRate() != null && (vitals.getHeartRate() > 130 || vitals.getHeartRate() < 50)) {
            return true;
        }

        // Hypotension or Hypertensive Crisis
        if (vitals.getSystolicBp() != null && (vitals.getSystolicBp() < 90 || vitals.getSystolicBp() > 200)) {
            return true;
        }

        // High fever or hypothermia
        if (vitals.getTemperature() != null && (vitals.getTemperature() >= 40.0 || vitals.getTemperature() < 35.0)) {
            return true;
        }

        return false;
    }

    /**
     * Warning zone vitals that upgrade an otherwise ESI 3 patient.
     */
    private boolean hasWarningZoneVitals(Vitals vitals, int age) {
        if (vitals.getOxygenSaturation() != null && vitals.getOxygenSaturation() < 94.0) {
            return true;
        }
        if (vitals.getHeartRate() != null && vitals.getHeartRate() > 115) {
            return true;
        }
        if (vitals.getRespiratoryRate() != null && vitals.getRespiratoryRate() > 24) {
            return true;
        }
        return false;
    }

    /**
     * Determines projected resource count based on complaint keywords or clinician estimation.
     */
    private int determineEstimatedResources(String complaint, Integer manualEstimation) {
        if (manualEstimation != null && manualEstimation >= 0) {
            return manualEstimation;
        }

        if (complaint == null || complaint.isBlank()) {
            return 0;
        }

        // 2+ resources indicators: complex pain, abdominal pain, fever workup, complex lacerations, suspected fracture
        if (complaint.contains("abdominal pain") || complaint.contains("vomiting") ||
            complaint.contains("fever") || complaint.contains("fall") ||
            complaint.contains("headache") || complaint.contains("dizziness") ||
            complaint.contains("fracture") || complaint.contains("kidney stone")) {
            return 2;
        }

        // 1 resource indicators: simple sprain, simple xray, minor laceration, rash needing prescription
        if (complaint.contains("sprain") || complaint.contains("ankle pain") ||
            complaint.contains("wrist pain") || complaint.contains("laceration") ||
            complaint.contains("cut") || complaint.contains("sore throat") ||
            complaint.contains("ear pain") || complaint.contains("urinary")) {
            return 1;
        }

        // 0 resources indicators: suture removal, medication refill, dressing change
        if (complaint.contains("refill") || complaint.contains("suture removal") ||
            complaint.contains("dressing") || complaint.contains("rash") ||
            complaint.contains("checkup")) {
            return 0;
        }

        // Default heuristic for general ED intake
        return 1;
    }

    /**
     * Computes a continuous priority score (0-100+) for fine-grained ranking inside the PriorityQueue.
     * ESI 1: ~100+
     * ESI 2: ~80-99
     * ESI 3: ~60-79
     * ESI 4: ~40-59
     * ESI 5: ~20-39
     */
    private int calculateCompositePriorityScore(EsiLevel esiLevel, Vitals vitals, int age) {
        int base = esiLevel.getBasePriorityWeight();
        int modifier = 0;

        // Severe pain increases priority inside the same tier
        if (vitals.getPainScore() != null) {
            modifier += Math.min(vitals.getPainScore(), 10);
        }

        // Low SpO2 penalty
        if (vitals.getOxygenSaturation() != null && vitals.getOxygenSaturation() < 95.0) {
            modifier += (int) ((95.0 - vitals.getOxygenSaturation()) * 2);
        }

        // Age vulnerability factor (infants < 2 years or elderly > 75 years)
        if (age < 2 || age > 75) {
            modifier += 5;
        }

        return base + modifier;
    }

    private void validateVitals(Vitals vitals) {
        if (vitals == null) {
            throw new InvalidVitalsException("Vital signs cannot be null for clinical triage assessment");
        }
        if (vitals.getHeartRate() == null || vitals.getHeartRate() < 0) {
            throw new InvalidVitalsException("Valid heart rate (>= 0 bpm) is mandatory for triage scoring");
        }
        if (vitals.getSystolicBp() == null || vitals.getSystolicBp() < 0) {
            throw new InvalidVitalsException("Valid systolic blood pressure (>= 0 mmHg) is mandatory for triage scoring");
        }
        if (vitals.getRespiratoryRate() == null || vitals.getRespiratoryRate() < 0) {
            throw new InvalidVitalsException("Valid respiratory rate (>= 0 breaths/min) is mandatory for triage scoring");
        }
        if (vitals.getOxygenSaturation() == null || vitals.getOxygenSaturation() < 0 || vitals.getOxygenSaturation() > 100) {
            throw new InvalidVitalsException("Valid oxygen saturation (0-100%) is mandatory for triage scoring");
        }
    }
}
