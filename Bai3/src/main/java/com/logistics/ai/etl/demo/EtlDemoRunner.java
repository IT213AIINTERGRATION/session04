package com.logistics.ai.etl.demo;

import com.logistics.ai.etl.entity.IncidentReport;
import com.logistics.ai.etl.repository.IncidentRepository;
import com.logistics.ai.etl.service.IncidentETLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EtlDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EtlDemoRunner.class);

    private final IncidentETLService etlService;
    private final IncidentRepository repository;

    public EtlDemoRunner(IncidentETLService etlService, IncidentRepository repository) {
        this.etlService = etlService;
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("==========================================================================");
        log.info("🚀 CHẠY DEMO TỐI ƯU & REFACTOR ETL PHÒNG THỦ (DEFENSIVE ETL REFACTOR DEMO)");
        log.info("==========================================================================");

        // KỊCH BẢN 1: Xử lý thành công (Chuỗi AI bị bọc Markdown + Dữ liệu hợp lệ)
        log.info("\n--- [KỊCH BẢN 1]: Tin nhắn hợp lệ (Tự động bóc Markdown block & Lưu DB) ---");
        try {
            String validRawMsg = "Đơn ORD-2026-888 xe 51C-99999 bị va chạm nhẹ ở ngã tư, mức độ HIGH";
            IncidentReport report = etlService.processReport(validRawMsg);
            log.info("🎉 [Kịch bản 1 KẾT QUẢ] Thành công! ID = {}, OrderCode = {}, LicensePlate = {}", 
                    report.getId(), report.getOrderCode(), report.getLicensePlate());
        } catch (Exception e) {
            log.error("❌ [Kịch bản 1 LỖI] Lỗi bất ngờ: {}", e.getMessage());
        }


        // KỊCH BẢN 2: Thất bại do Defensive Validation (Biển số sai định dạng -> Rollback)
        log.info("\n--- [KỊCH BẢN 2]: Dữ liệu lỗi biển số xe (Defensive Validation & Rollback) ---");
        try {
            // Giả lập logic kiểm duyệt biển số sai định dạng bằng cách gọi validate thủ công hoặc truyền tin nhắn lỗi
            String invalidPlateMsg = "Đơn ORD-111 xe INVALID_PLATE_123 bị hỏng";
            log.warn("⚠️ Đang thử nghiệm xử lý tin nhắn có biển số lỗi: INVALID_PLATE_123");
            // Để demo trực tiếp kiểm duyệt:
            com.logistics.ai.etl.dto.IncidentExtraction invalidDto = new com.logistics.ai.etl.dto.IncidentExtraction(
                    "ORD-111", "INVALID_PLATE_123", "Hỏng máy", "HIGH"
            );
            etlService.validateExtractionDTO(invalidDto);
        } catch (Exception e) {
            log.warn("🛡️ [Kịch bản 2 KẾT QUẢ ROLLBACK] Đã kích hoạt Defensive Validation Rollback: {}", e.getMessage());
        }


        // KỊCH BẢN 3: Thất bại do orderCode bị NULL
        log.info("\n--- [KỊCH BẢN 3]: Dữ liệu thiếu orderCode (Defensive Validation & Rollback) ---");
        try {
            com.logistics.ai.etl.dto.IncidentExtraction nullOrderDto = new com.logistics.ai.etl.dto.IncidentExtraction(
                    null, "29A-12345", "Nổ lốp", "CRITICAL"
            );
            etlService.validateExtractionDTO(nullOrderDto);
        } catch (Exception e) {
            log.warn("🛡️ [Kịch bản 3 KẾT QUẢ ROLLBACK] Đã kích hoạt Defensive Validation Rollback: {}", e.getMessage());
        }


        // KIỂM TRA TÍNH TOÀN VẸN CSDL
        log.info("\n==========================================================================");
        log.info("📊 DANH SÁCH BẢN GHI SỰ CỐ ĐÃ LƯU TRONG DATABASE SAU ROLLBACK:");
        log.info("==========================================================================");
        List<IncidentReport> reports = repository.findAll();
        for (IncidentReport r : reports) {
            log.info("📌 Record #{} | OrderCode: {} | LicensePlate: {} | Type: {} | Urgency: {}",
                    r.getId(), r.getOrderCode(), r.getLicensePlate(), r.getIncidentType(), r.getUrgency());
        }
        log.info("==========================================================================\n");
    }
}
