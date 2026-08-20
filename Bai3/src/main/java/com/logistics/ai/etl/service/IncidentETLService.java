package com.logistics.ai.etl.service;

import com.logistics.ai.etl.dto.IncidentExtraction;
import com.logistics.ai.etl.entity.IncidentReport;
import com.logistics.ai.etl.enums.UrgencyLevel;
import com.logistics.ai.etl.exception.InvalidIncidentDataException;
import com.logistics.ai.etl.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Service ETL bóc tách tin nhắn sự cố đã được Refactor theo tiêu chuẩn doanh nghiệp.
 * 
 * Các cải tiến chính:
 * 1. Làm sạch phản hồi AI (Loại bỏ Markdown block bằng Regex).
 * 2. Kiểm chứng dữ liệu phòng thủ (Defensive Validation) thủ công trước khi ánh xạ sang Entity.
 * 3. Quản lý giao dịch dữ liệu chặt chẽ với @Transactional (Tự động Rollback khi xảy ra lỗi).
 * 4. Tích hợp SLF4J Logging ghi nhận đầy đủ context luồng xử lý.
 */
@Service
public class IncidentETLService {

    private static final Logger log = LoggerFactory.getLogger(IncidentETLService.class);

    // Regex kiểm tra định dạng biển số xe Việt Nam (VD: 29A-12345, 51C-99999)
    private static final Pattern LICENSE_PLATE_PATTERN = Pattern.compile("^[0-9]{2}[A-Z0-9]-[0-9]{4,5}$");

    private final ChatModel chatModel;
    private final IncidentRepository repository;
    private final BeanOutputConverter<IncidentExtraction> converter;

    @Autowired
    public IncidentETLService(@Autowired(required = false) ChatModel chatModel, IncidentRepository repository) {
        this.chatModel = chatModel;
        this.repository = repository;
        this.converter = new BeanOutputConverter<>(IncidentExtraction.class);
    }

    /**
     * Xử lý bóc tách tin nhắn thô từ tài xế và lưu vào CSDL trong một Transaction.
     * 
     * @param rawMessage Tin nhắn thô từ tài xế
     * @return Entity IncidentReport đã được lưu thành công
     */
    @Transactional(rollbackFor = Exception.class)
    public IncidentReport processReport(String rawMessage) {
        log.info("📩 [ETL Step 1] Nhận tin nhắn sự cố thô từ tài xế: '{}'", rawMessage);

        if (rawMessage == null || rawMessage.isBlank()) {
            log.error("❌ [ETL Validation Error] Tin nhắn thô rỗng hoặc null!");
            throw new InvalidIncidentDataException("Tin nhắn sự cố thô không được để rỗng");
        }

        // Gọi LLM thông qua Spring AI
        String rawAiResponse;
        try {
            String formatInstructions = converter.getFormat();
            Prompt prompt = new Prompt("Phân tích tin nhắn sự cố sau: " + rawMessage + "\n" + formatInstructions);
            
            log.debug("🤖 [ETL Step 2] Gửi prompt tới ChatModel...");
            if (chatModel != null) {
                rawAiResponse = chatModel.call(prompt).getResult().getOutput().getText();
            } else {
                // Mock response chứa Markdown block cho kịch bản thử nghiệm
                rawAiResponse = "```json\n{\"orderCode\":\"ORD-2026-888\",\"licensePlate\":\"51C-99999\",\"incidentType\":\"Va chạm giao thông\",\"urgency\":\"HIGH\"}\n```";
            }
            log.debug("📄 [ETL Step 3] Nhận phản hồi thô từ AI: '{}'", rawAiResponse);
        } catch (Exception e) {
            log.error("❌ [ETL AI Call Failed] Lỗi kết nối tới mô hình AI: {}", e.getMessage(), e);
            throw new InvalidIncidentDataException("Không thể gọi tới dịch vụ AI: " + e.getMessage());
        }

        // 1. Làm sạch phản hồi từ AI (Loại bỏ Markdown block ```json ... ```)
        String cleanedJson = cleanJsonResponse(rawAiResponse);
        log.info("🧹 [ETL Step 4] Chuỗi JSON sau khi làm sạch: {}", cleanedJson);

        // Parse JSON sang DTO
        IncidentExtraction dto;
        try {
            dto = converter.convert(cleanedJson);
            log.info("✅ [ETL Step 5] Parse JSON sang DTO thành công: {}", dto);
        } catch (Exception e) {
            log.error("❌ [ETL JSON Parse Error] Không thể parse chuỗi JSON từ AI: {}", e.getMessage());
            throw new InvalidIncidentDataException("Cấu trúc JSON phản hồi từ AI không đúng định dạng: " + e.getMessage());
        }

        // 2. Kiểm chứng dữ liệu phòng thủ (Defensive Validation) thủ công
        validateExtractionDTO(dto);

        // 3. Ánh xạ sang Entity và Lưu DB
        UrgencyLevel urgencyLevel = UrgencyLevel.safeParse(dto.urgency());
        IncidentReport entity = new IncidentReport(
                dto.orderCode().trim(),
                dto.licensePlate().trim().toUpperCase(),
                dto.incidentType().trim(),
                urgencyLevel
        );

        IncidentReport savedEntity = repository.save(entity);
        log.info("💾 [ETL Step 6] Lưu bản ghi sự cố vào CSDL thành công! ID = {}, OrderCode = {}", 
                savedEntity.getId(), savedEntity.getOrderCode());

        return savedEntity;
    }

    /**
     * Phương thức làm sạch chuỗi phản hồi từ AI bằng Regex.
     * Loại bỏ các thẻ markdown code block ```json ... ``` hoặc ``` ... ``` thừa.
     */
    public String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "{}";
        }
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "")
                             .replaceAll("\\s*```$", "")
                             .trim();
        }
        return cleaned;
    }

    /**
     * Thực hiện kiểm chứng dữ liệu phòng thủ (Defensive Validation) thủ công trên DTO.
     * Thỏa mãn các quy tắc nghiệp vụ chặt chẽ trước khi đụng tới Persistence Context.
     */
    public void validateExtractionDTO(IncidentExtraction dto) {
        log.info("🔍 [ETL Defensive Validation] Bắt đầu kiểm chứng DTO: {}", dto);

        if (dto == null) {
            log.error("❌ [Defensive Check Failed] DTO bóc tách bị NULL!");
            throw new InvalidIncidentDataException("DTO bóc tách từ AI không được null");
        }

        // Rule 1: orderCode không được rỗng
        if (dto.orderCode() == null || dto.orderCode().isBlank()) {
            log.error("❌ [Defensive Check Failed] Mã đơn hàng (orderCode) bị rỗng hoặc null!");
            throw new InvalidIncidentDataException("Mã đơn hàng (orderCode) là trường bắt buộc và không được rỗng");
        }

        // Rule 2: licensePlate phải đúng định dạng biển số xe
        if (dto.licensePlate() == null || dto.licensePlate().isBlank()) {
            log.error("❌ [Defensive Check Failed] Biển số xe (licensePlate) bị rỗng!");
            throw new InvalidIncidentDataException("Biển số xe (licensePlate) không được để rỗng");
        }
        String normalizedPlate = dto.licensePlate().trim().toUpperCase();
        if (!LICENSE_PLATE_PATTERN.matcher(normalizedPlate).matches()) {
            log.error("❌ [Defensive Check Failed] Biển số xe '{}' không đúng định dạng chuẩn (ví dụ chuẩn: 29A-12345)!", dto.licensePlate());
            throw new InvalidIncidentDataException("Biển số xe '" + dto.licensePlate() + "' không đúng định dạng tiêu chuẩn");
        }

        // Rule 3: urgency phải nằm trong Enum hợp lệ (LOW, MEDIUM, HIGH, CRITICAL)
        if (dto.urgency() == null || dto.urgency().isBlank()) {
            log.error("❌ [Defensive Check Failed] Mức độ khẩn cấp (urgency) bị rỗng!");
            throw new InvalidIncidentDataException("Mức độ khẩn cấp (urgency) không được để rỗng");
        }
        UrgencyLevel urgencyLevel = UrgencyLevel.safeParse(dto.urgency());
        if (urgencyLevel == null) {
            log.error("❌ [Defensive Check Failed] Mức độ khẩn cấp '{}' không thuộc danh sách Enum hợp lệ (LOW, MEDIUM, HIGH, CRITICAL)!", dto.urgency());
            throw new InvalidIncidentDataException("Mức độ khẩn cấp '" + dto.urgency() + "' không hợp lệ. Phải thuộc: LOW, MEDIUM, HIGH, CRITICAL");
        }

        log.info("✅ [ETL Defensive Validation] DTO vượt qua toàn bộ quy tắc kiểm chứng phòng thủ!");
    }
}
