package com.ed.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clinical vital signs captured during the emergency triage intake.
 */
@Embeddable
public class Vitals implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Heart rate is required")
    @Min(value = 0, message = "Heart rate cannot be negative")
    @Max(value = 300, message = "Heart rate exceeds valid clinical range")
    @Column(name = "heart_rate")
    private Integer heartRate; // Beats per minute (bpm)

    @NotNull(message = "Systolic blood pressure is required")
    @Min(value = 0, message = "Systolic BP cannot be negative")
    @Max(value = 350, message = "Systolic BP exceeds valid clinical range")
    @Column(name = "systolic_bp")
    private Integer systolicBp; // mmHg

    @NotNull(message = "Diastolic blood pressure is required")
    @Min(value = 0, message = "Diastolic BP cannot be negative")
    @Max(value = 250, message = "Diastolic BP exceeds valid clinical range")
    @Column(name = "diastolic_bp")
    private Integer diastolicBp; // mmHg

    @NotNull(message = "Respiratory rate is required")
    @Min(value = 0, message = "Respiratory rate cannot be negative")
    @Max(value = 100, message = "Respiratory rate exceeds valid clinical range")
    @Column(name = "respiratory_rate")
    private Integer respiratoryRate; // Breaths per minute

    @NotNull(message = "Oxygen saturation is required")
    @Min(value = 0, message = "Oxygen saturation cannot be negative")
    @Max(value = 100, message = "Oxygen saturation cannot exceed 100%")
    @Column(name = "oxygen_saturation")
    private Double oxygenSaturation; // Percentage (%)

    @NotNull(message = "Body temperature is required")
    @Min(value = 25, message = "Temperature too low for clinical measurement")
    @Max(value = 45, message = "Temperature too high for clinical measurement")
    @Column(name = "temperature")
    private Double temperature; // Celsius (°C)

    @Min(value = 0, message = "Pain score minimum is 0")
    @Max(value = 10, message = "Pain score maximum is 10")
    @Column(name = "pain_score")
    private Integer painScore; // 0 (No pain) to 10 (Worst imaginable)

    @Min(value = 3, message = "Glasgow Coma Scale minimum is 3")
    @Max(value = 15, message = "Glasgow Coma Scale maximum is 15")
    @Column(name = "gcs_score")
    private Integer gcsScore; // Glasgow Coma Scale (3-15)

    public Vitals() {
    }

    public Vitals(Integer heartRate, Integer systolicBp, Integer diastolicBp, Integer respiratoryRate,
                  Double oxygenSaturation, Double temperature, Integer painScore, Integer gcsScore) {
        this.heartRate = heartRate;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.respiratoryRate = respiratoryRate;
        this.oxygenSaturation = oxygenSaturation;
        this.temperature = temperature;
        this.painScore = painScore != null ? painScore : 0;
        this.gcsScore = gcsScore != null ? gcsScore : 15;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(Integer systolicBp) {
        this.systolicBp = systolicBp;
    }

    public Integer getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(Integer diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public Double getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(Double oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getPainScore() {
        return painScore;
    }

    public void setPainScore(Integer painScore) {
        this.painScore = painScore;
    }

    public Integer getGcsScore() {
        return gcsScore;
    }

    public void setGcsScore(Integer gcsScore) {
        this.gcsScore = gcsScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vitals vitals = (Vitals) o;
        return Objects.equals(heartRate, vitals.heartRate) &&
                Objects.equals(systolicBp, vitals.systolicBp) &&
                Objects.equals(diastolicBp, vitals.diastolicBp) &&
                Objects.equals(respiratoryRate, vitals.respiratoryRate) &&
                Objects.equals(oxygenSaturation, vitals.oxygenSaturation) &&
                Objects.equals(temperature, vitals.temperature) &&
                Objects.equals(painScore, vitals.painScore) &&
                Objects.equals(gcsScore, vitals.gcsScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heartRate, systolicBp, diastolicBp, respiratoryRate, oxygenSaturation, temperature, painScore, gcsScore);
    }

    @Override
    public String toString() {
        return "Vitals{" +
                "HR=" + heartRate +
                ", BP=" + systolicBp + "/" + diastolicBp +
                ", RR=" + respiratoryRate +
                ", SpO2=" + oxygenSaturation + "%" +
                ", Temp=" + temperature + "°C" +
                ", Pain=" + painScore + "/10" +
                ", GCS=" + gcsScore +
                '}';
    }
}
