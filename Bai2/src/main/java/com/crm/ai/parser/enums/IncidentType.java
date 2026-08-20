package com.crm.ai.parser.enums;

/**
 * Phân loại các loại sự cố Logistics trong hệ thống CRM.
 */
public enum IncidentType {
    TIRE_PUNCTURE("Nổ lốp / Hỏng lốp"),
    ACCIDENT("Tai nạn giao thông"),
    ENGINE_BREAKDOWN("Hỏng động cơ"),
    CARGO_DAMAGE("Thiệt hại hàng hóa"),
    OTHER("Sự cố khác");

    private final String description;

    IncidentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Map từ chuỗi thô (do AI bóc tách) sang IncidentTypeEnum một cách an toàn.
     */
    public static IncidentType safeParse(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return OTHER;
        }
        String normalized = rawType.toUpperCase().trim();
        if (normalized.contains("LỐP") || normalized.contains("TIRE") || normalized.contains("NO_LOP")) {
            return TIRE_PUNCTURE;
        }
        if (normalized.contains("TAI NẠN") || normalized.contains("ACCIDENT") || normalized.contains("VA CHẠM")) {
            return ACCIDENT;
        }
        if (normalized.contains("ĐỘNG CƠ") || normalized.contains("ENGINE") || normalized.contains("HỎNG MÁY")) {
            return ENGINE_BREAKDOWN;
        }
        if (normalized.contains("HÀNG HÓA") || normalized.contains("CARGO")) {
            return CARGO_DAMAGE;
        }
        
        try {
            return IncidentType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
