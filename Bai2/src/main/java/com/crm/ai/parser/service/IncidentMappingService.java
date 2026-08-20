package com.crm.ai.parser.service;

import com.crm.ai.parser.dto.IncidentExtraction;
import com.crm.ai.parser.entity.IncidentReport;
import com.crm.ai.parser.enums.IncidentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service thực hiện kiểm tra nghiệp vụ và chuyển đổi phòng thủ (Defensive Mapping)
 * từ Record DTO bóc tách từ LLM sang JPA Entity lưu trữ Database.
 */
@Service
public class IncidentMappingService {

    private static final Logger log = LoggerFactory.getLogger(IncidentMappingService.class);

    /**
     * Chuyển đổi dữ liệu bóc tách thô (DTO) sang Entity đã qua kiểm duyệt phòng thủ.
     * 
     * @param extraction Record DTO nhận về từ AI qua BeanOutputConverter
     * @return JPA Entity IncidentReport sẵn sàng lưu vào CSDL
     */
    public IncidentReport mapToEntity(IncidentExtraction extraction) {
        if (extraction == null) {
            log.warn("⚠️ [Defensive Barrier] Nhận dữ liệu bóc tách NULL từ AI! Áp dụng Fallback mặc định.");
            return new IncidentReport(
                    "UNKNOWN_PLATE",
                    IncidentType.OTHER,
                    "Vị trí không xác định",
                    0.0,
                    false,
                    "Không thể bóc tách tin nhắn từ AI",
                    "REQUIRES_MANUAL_REVIEW"
            );
        }

        log.info("🔍 [Defensive Barrier] Bắt đầu kiểm duyệt dữ liệu thô từ AI: {}", extraction);

        // 1. Kiểm duyệt Biển số xe
        String safePlate = sanitizePlateNumber(extraction.vehiclePlateNumber());

        // 2. Chuyển đổi loại sự cố an toàn sang Enum
        IncidentType safeType = IncidentType.safeParse(extraction.incidentType());

        // 3. Kiểm duyệt và chuẩn hóa vị trí
        String safeLocation = (extraction.location() != null && !extraction.location().isBlank())
                ? extraction.location().trim()
                : "Vị trí chưa rõ (KM / Quốc lộ chưa ghi nhận)";

        // 4. Kiểm duyệt chi phí ước tính (Không cho phép giá trị âm hoặc null)
        Double safeCost = extraction.estimatedCost();
        if (safeCost == null || safeCost < 0) {
            log.warn("⚠️ [Defensive Barrier] Chi phí ước tính không hợp lệ ({}), tự động gán = 0.0", safeCost);
            safeCost = 0.0;
        }

        // 5. Kiểm duyệt mức độ khẩn cấp
        Boolean safeEmergency = (extraction.emergency() != null) ? extraction.emergency() : false;

        // 6. Chuẩn hóa mô tả chi tiết
        String safeDescription = (extraction.description() != null && !extraction.description().isBlank())
                ? extraction.description().trim()
                : "Mô tả sự cố được bóc tách tự động bởi AI";

        // Xác định trạng thái xử lý
        String initialStatus = (safePlate.equals("UNKNOWN_PLATE") || safeType == IncidentType.OTHER)
                ? "REQUIRES_MANUAL_REVIEW"
                : "PENDING_VERIFICATION";

        IncidentReport entity = new IncidentReport(
                safePlate,
                safeType,
                safeLocation,
                safeCost,
                safeEmergency,
                safeDescription,
                initialStatus
        );

        log.info("✅ [Defensive Barrier] Chuyển đổi thành công sang Entity: {}", entity);
        return entity;
    }

    private String sanitizePlateNumber(String rawPlate) {
        if (rawPlate == null || rawPlate.isBlank()) {
            log.warn("⚠️ [Defensive Barrier] Phát hiện biển số xe NULL/Rống từ AI!");
            return "UNKNOWN_PLATE";
        }
        // Chuẩn hóa định dạng biển số (loại bỏ ký tự đặc biệt thừa)
        String cleaned = rawPlate.replaceAll("[^a-zA-Z0-9\\-]", "").toUpperCase().trim();
        return cleaned.isEmpty() ? "UNKNOWN_PLATE" : cleaned;
    }
}
