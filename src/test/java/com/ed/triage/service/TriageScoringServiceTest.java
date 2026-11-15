package com.ed.triage.service;

import com.ed.triage.entity.Vitals;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.exception.InvalidVitalsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriageScoringServiceTest {

    private TriageScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new TriageScoringService();
    }

    @Test
    @DisplayName("Should assign ESI 1 (Resuscitation) for cardiac arrest or pulseless state")
    void testEsi1CardiacArrest() {
        Vitals vitals = new Vitals(0, 0, 0, 0, 0.0, 36.5, 0, 3);
        var result = scoringService.calculateScore(vitals, "Patient in cardiac arrest, CPR in progress", 55, 3);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_1);
        assertThat(result.priorityScore()).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should assign ESI 1 for severe hypoxia (SpO2 < 85%)")
    void testEsi1SevereHypoxia() {
        Vitals vitals = new Vitals(120, 110, 70, 32, 82.0, 37.0, 5, 14);
        var result = scoringService.calculateScore(vitals, "Acute respiratory failure", 60, 2);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_1);
    }

    @Test
    @DisplayName("Should assign ESI 1 for profound shock (Systolic BP < 70)")
    void testEsi1ProfoundShock() {
        Vitals vitals = new Vitals(140, 65, 40, 28, 92.0, 36.2, 4, 11);
        var result = scoringService.calculateScore(vitals, "Septic shock with altered sensorium", 72, 3);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_1);
    }

    @Test
    @DisplayName("Should assign ESI 1 for unresponsive coma (GCS <= 8)")
    void testEsi1UnresponsiveComa() {
        Vitals vitals = new Vitals(80, 120, 80, 14, 96.0, 36.8, 0, 6);
        var result = scoringService.calculateScore(vitals, "Found unresponsive at home", 45, 2);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_1);
    }

    @Test
    @DisplayName("Should assign ESI 2 (Emergent) for acute chest pain with high risk profile")
    void testEsi2ChestPain() {
        Vitals vitals = new Vitals(95, 145, 90, 18, 97.0, 36.9, 9, 15);
        var result = scoringService.calculateScore(vitals, "Crushing substernal chest pain radiating to left arm", 58, 2);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_2);
        assertThat(result.priorityScore()).isGreaterThanOrEqualTo(80);
    }

    @Test
    @DisplayName("Should assign ESI 2 for danger zone vitals: SpO2 < 92%")
    void testEsi2DangerZoneSpO2() {
        Vitals vitals = new Vitals(105, 130, 85, 26, 90.0, 37.8, 4, 15);
        var result = scoringService.calculateScore(vitals, "Shortness of breath and productive cough", 68, 2);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_2);
    }

    @Test
    @DisplayName("Should assign ESI 2 for severe pain score >= 8/10")
    void testEsi2SeverePain() {
        Vitals vitals = new Vitals(98, 135, 82, 18, 98.0, 36.7, 9, 15);
        var result = scoringService.calculateScore(vitals, "Acute flank pain, suspected renal colic", 40, 2);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_2);
    }

    @Test
    @DisplayName("Should assign ESI 3 (Urgent) for stable vitals requiring 2+ resources")
    void testEsi3MultipleResources() {
        Vitals vitals = new Vitals(78, 120, 75, 16, 98.5, 37.1, 5, 15);
        var result = scoringService.calculateScore(vitals, "Right lower quadrant abdominal pain for 12 hours", 28, 2);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_3);
        assertThat(result.estimatedResources()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should assign ESI 4 (Less Urgent) for stable vitals requiring 1 resource")
    void testEsi4SingleResource() {
        Vitals vitals = new Vitals(72, 118, 76, 14, 99.0, 36.6, 4, 15);
        var result = scoringService.calculateScore(vitals, "Twisted right ankle while running, needs X-ray", 24, 1);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_4);
        assertThat(result.estimatedResources()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should assign ESI 5 (Non-Urgent) for stable vitals requiring 0 resources")
    void testEsi5ZeroResources() {
        Vitals vitals = new Vitals(70, 115, 75, 14, 99.0, 36.5, 0, 15);
        var result = scoringService.calculateScore(vitals, "Routine blood pressure medication refill request", 50, 0);

        assertThat(result.esiLevel()).isEqualTo(EsiLevel.ESI_5);
        assertThat(result.estimatedResources()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw InvalidVitalsException when mandatory vitals are missing or invalid")
    void testNullVitalsValidation() {
        assertThatThrownBy(() -> scoringService.calculateScore(null, "Headache", 30, 1))
                .isInstanceOf(InvalidVitalsException.class);

        Vitals invalidHeartRate = new Vitals(-5, 120, 80, 16, 98.0, 37.0, 0, 15);
        assertThatThrownBy(() -> scoringService.calculateScore(invalidHeartRate, "Headache", 30, 1))
                .isInstanceOf(InvalidVitalsException.class);
    }
}
