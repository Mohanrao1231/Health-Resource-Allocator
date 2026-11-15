package com.ed.triage.enums;

/**
 * Emergency Severity Index (ESI) 5-level clinical triage algorithm.
 * Level 1 indicates the most acute, life-threatening emergency (resuscitation),
 * while Level 5 indicates a non-urgent condition requiring no complex hospital resources.
 */
public enum EsiLevel {
    ESI_1(1, "Resuscitation", "Immediate life-saving intervention required without delay", 0, 100),
    ESI_2(2, "Emergent", "High risk, confused/lethargic, severe pain or danger zone vitals", 10, 80),
    ESI_3(3, "Urgent", "Stable vitals, projected need for two or more ED resources", 60, 60),
    ESI_4(4, "Less Urgent", "Stable vitals, projected need for one ED resource", 120, 40),
    ESI_5(5, "Non-Urgent", "Stable vitals, no projected diagnostic/therapeutic ED resources", 240, 20);

    private final int level;
    private final String title;
    private final String clinicalDescription;
    private final int targetWaitTimeMinutes;
    private final int basePriorityWeight;

    EsiLevel(int level, String title, String clinicalDescription, int targetWaitTimeMinutes, int basePriorityWeight) {
        this.level = level;
        this.title = title;
        this.clinicalDescription = clinicalDescription;
        this.targetWaitTimeMinutes = targetWaitTimeMinutes;
        this.basePriorityWeight = basePriorityWeight;
    }

    public int getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getClinicalDescription() {
        return clinicalDescription;
    }

    public int getTargetWaitTimeMinutes() {
        return targetWaitTimeMinutes;
    }

    public int getBasePriorityWeight() {
        return basePriorityWeight;
    }

    public static EsiLevel fromLevel(int level) {
        for (EsiLevel esi : values()) {
            if (esi.level == level) {
                return esi;
            }
        }
        throw new IllegalArgumentException("Invalid ESI level: " + level + ". Must be between 1 and 5.");
    }
}
