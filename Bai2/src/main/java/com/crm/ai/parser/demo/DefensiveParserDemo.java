package com.crm.ai.parser.demo;

import com.crm.ai.parser.dto.IncidentExtraction;
import com.crm.ai.parser.entity.IncidentReport;
import com.crm.ai.parser.repository.IncidentRepository;
import com.crm.ai.parser.service.IncidentMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Class minh chứng khởi chạy thực tế (Runtime Execution Demonstration).
 * Kiểm chứng tính đúng đắn của Lập trình phòng thủ khi bóc tách tin nhắn từ AI.
 */
@Component
public class DefensiveParserDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefensiveParserDemo.class);

    private final IncidentMappingService mappingService;
    private final IncidentRepository repository;

    public DefensiveParserDemo(IncidentMappingService mappingService, IncidentRepository repository) {
        this.mappingService = mappingService;
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("==========================================================================");
        log.info("🚀 BẮT ĐẦU CHẠY DEMO LẬP TRÌNH PHÒNG THỬ (DEFENSIVE PARSING DEMO)");
        log.info("==========================================================================");

        // KỊCH BẢN 1: Tin nhắn từ tài xế bóc tách HOÀN HẢO từ LLM
        log.info("\n--- [KỊCH BẢN 1]: Dữ liệu bóc tách từ LLM Đầy Đủ & Chuẩn Xác ---");
        IncidentExtraction extraction1 = new IncidentExtraction(
                "29A-12345",
                "NỔ LỐP",
                "KM15 Quốc lộ 1A",
                5000000.0,
                true,
                "Xe bị nổ lốp trước bên phải lúc 14:30. Cần cứu hộ khẩn cấp."
        );
        IncidentReport report1 = mappingService.mapToEntity(extraction1);
        IncidentReport savedReport1 = repository.save(report1);
        log.info("💾 [DB Persist Success] ID = {}, Status = {}", savedReport1.getId(), savedReport1.getStatus());


        // KỊCH BẢN 2: Dữ liệu bóc tách KHÔNG HOÀN HẢO (Thiếu biển số, chi phí âm, loại sự cố lạ)
        log.info("\n--- [KỊCH BẢN 2]: Dữ liệu bóc tách từ LLM Bị Thiếu / Sai Định Dạng ---");
        IncidentExtraction extraction2 = new IncidentExtraction(
                null, // Biển số bị null do tài xế không ghi
                "HỎNG_MÁY_VA_CHẠM",
                "",   // Vị trí rỗng
                -1500000.0, // Chi phí âm
                null, // Mức độ khẩn cấp null
                "Xe tự dưng chết máy dừng giữa đường"
        );
        IncidentReport report2 = mappingService.mapToEntity(extraction2);
        IncidentReport savedReport2 = repository.save(report2);
        log.info("💾 [DB Persist Defensive Success] ID = {}, Plate = '{}', Cost = {}, Status = {}",
                savedReport2.getId(), savedReport2.getVehiclePlateNumber(), savedReport2.getEstimatedCost(), savedReport2.getStatus());


        // KỊCH BẢN 3: Dữ liệu thô từ AI bị Null hoàn toàn (Lỗi Parse JSON)
        log.info("\n--- [KỊCH BẢN 3]: AI Phản Hồi Lỗi / Null Object ---");
        IncidentReport report3 = mappingService.mapToEntity(null);
        IncidentReport savedReport3 = repository.save(report3);
        log.info("💾 [DB Persist Fallback Success] ID = {}, Status = {}", savedReport3.getId(), savedReport3.getStatus());


        // TRUY VẤN VÀ IN TOÀN BỘ KẾT QUẢ TỪ DATABASE
        log.info("\n==========================================================================");
        log.info("📊 DANH SÁCH BẢN GHI SỰ CỐ ĐÃ LƯU AN TOÀN TRONG DATABASE H2:");
        log.info("==========================================================================");
        List<IncidentReport> allReports = repository.findAll();
        for (IncidentReport r : allReports) {
            log.info("📌 Record #{} | Xe: {} | Loại: {} | Vị trí: {} | Chi phí: {} VNĐ | Khẩn: {} | Trang thai: {}",
                    r.getId(), r.getVehiclePlateNumber(), r.getIncidentType(), r.getLocation(),
                    r.getEstimatedCost(), r.getEmergency(), r.getStatus());
        }
        log.info("==========================================================================\n");
    }
}
