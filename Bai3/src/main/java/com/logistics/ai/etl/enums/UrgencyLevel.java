package com.logistics.ai.etl.enums;

/**
 * Cấp độ khẩn cấp của sự cố.
 */
public enum UrgencyLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static UrgencyLevel safeParse(String rawUrgency) {
        if (rawUrgency == null || rawUrgency.isBlank()) {
            return null;
        }
        try {
            return UrgencyLevel.valueOf(rawUrgency.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
