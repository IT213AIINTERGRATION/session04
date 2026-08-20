package com.crm.ai.parser.dto;

/**
 * Java Record DTO đại diện cho dữ liệu bóc tách thô từ LLM (qua BeanOutputConverter).
 * 
 * Đặc điểm thiết kế phòng thủ:
 * - Immutable (Bất biến) hoàn toàn nhờ Java Record.
 * - Cho phép các trường nullable để tiếp nhận linh hoạt kết quả phản hồi từ AI (kể cả khi AI bóc tách thiếu dữ liệu).
 * - Tách biệt hoàn toàn khỏi các ràng buộc cơ sở dữ liệu của JPA Entity.
 */
public record IncidentExtraction(
        String vehiclePlateNumber,
        String incidentType,
        String location,
        Double estimatedCost,
        Boolean emergency,
        String description
) {
    /**
     * Compact constructor hoặc utility constructor hỗ trợ giá trị mặc định.
     */
    public IncidentExtraction {
        // Record giữ nguyên trạng dữ liệu thô từ AI để phục vụ kiểm vết (Audit)
    }
}
