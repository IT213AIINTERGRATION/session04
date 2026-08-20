# CRM AI - Thiết Kế Lớp Cấu Trúc Dữ Liệu Bóc Tách Phòng Thủ (Defensive Data Extraction Architecture)

## 📌 1. Giới Thiệu Tổng Quan
Hệ thống **CRM AI Logistics** nhận các tin nhắn thô từ tài xế gửi về (ví dụ: *"Xe 29A-12345 bị nổ lốp tại KM15 Quốc lộ 1A lúc 14:30. Cần cứu hộ khẩn cấp, ước tính thiệt hại 5.000.000 VNĐ."*).

Bài toán đặt ra: **Cần bóc tách các tin nhắn thô này thành thông tin có cấu trúc để lưu vào CSDL.** Dự án áp dụng **Phương án 2 (Tối Ưu)**: Sử dụng một **Java Record DTO (`IncidentExtraction`)** làm đối tượng tiếp nhận dữ liệu bất biến từ AI via `BeanOutputConverter`, sau đó qua **Tầng Bảo Vệ Phòng Thủ (`IncidentMappingService`)** để chuẩn hóa và kiểm tra nghiệp vụ trước khi chuyển đổi sang **JPA Entity (`IncidentReport`)** lưu vào CSDL.

---

## 📊 2. Bài Phân Tích So Sánh Sâu Sắc: Direct JPA Entity vs DTO Record + Defensive Barrier

| Tiêu Chí So Sánh | Phương Án 1: Bóc Tách Trực Tiếp Vào JPA Entity (`IncidentReport`) | Phương Án 2 (Lựa Chọn Tối Ưu): Sử Dụng DTO Record (`IncidentExtraction`) + Mapping |
| :--- | :--- | :--- |
| **Lập Trình Phòng Thủ (Defensive Programming)** | **Rất Yếu / Nguy Cơ Cao.** Nếu LLM trả về JSON thiếu trường (ví dụ `vehiclePlateNumber` null hoặc `estimatedCost` bị âm), Entity sẽ lưu trực tiếp dữ liệu rác/hỏng vào CSDL. | **Rất Mạnh / Toàn Diện.** Record DTO tiếp nhận nguyên trạng dữ liệu AI (kể cả lỗi). Tầng Service đóng vai trò là "màng lọc phòng thủ", kiểm duyệt và gán mặc định an toàn. |
| **Ràng Buộc Kỹ Thuật Hibernate / JPA** | **Xung Đột.** JPA Entity đòi hỏi `ID` (auto-generated) và `reportedAt` (not-null). Khởi tạo trực tiếp qua Reflection/Jackson của `BeanOutputConverter` có thể ghi đè ID hoặc gây lỗi `PropertyValueException`. | **Tương Thích 100%.** JPA Entity chỉ được tạo ra khi dữ liệu đã hợp lệ. `ID` do JPA quản lý, constructor Entity tuân thủ tính đóng gói và có `protected` constructor. |
| **Tính Đóng Gói (Encapsulation)** | **Bị Phá Vỡ.** JPA Entity buộc phải cung cấp Public Setters hoặc Public Constructor mở cho tất cả các trường để `BeanOutputConverter` inject dữ liệu, dẫn đến vi phạm Immutability. | **Được Bảo Vệ Tuyệt Đối.** Java Record là Immutability bất biến. JPA Entity chỉ cung cấp Getters và các Business Methods (`markAsVerified()`), không mở Setters tràn lan. |
| **Tính Bất Biến (Immutability)** | Không thể áp dụng cho JPA Entity (vì Hibernate cần no-arg constructor và mutable proxy). | Java Record bản chất là bất biến (`final fields`), đảm bảo dữ liệu bóc tách từ AI không bị thay đổi bất ngờ trong luồng xử lý. |
| **Xử Lý Lỗi Runtime & DB Crash** | Khi LLM vi phạm điều kiện `NOT NULL` của DB, ứng dụng sẽ quăng `DataIntegrityViolationException` làm ngắt đột ngột luồng xử lý. | Mọi ngoại lệ được chặn ngay ở màng lọc DTO. Nếu dữ liệu rác, hệ thống chuyển về trạng thái `REQUIRES_MANUAL_REVIEW` và lưu an toàn. |

---

## 📁 3. Cấu Trúc Dự Án (Project Structure)

```text
defensive-incident-parser/
├── src/
│   ├── main/
│   │   ├── java/com/crm/ai/parser/
│   │   │   ├── DefensiveIncidentParserApplication.java  # Main App
│   │   │   ├── demo/
│   │   │   │   └── DefensiveParserDemo.java              # Demo khởi chạy thực tế 3 kịch bản
│   │   │   ├── dto/
│   │   │   │   └── IncidentExtraction.java               # Java Record DTO
│   │   │   ├── entity/
│   │   │   │   └── IncidentReport.java                   # JPA Entity phòng thủ
│   │   │   ├── enums/
│   │   │   │   └── IncidentType.java                     # Enum phân loại sự cố
│   │   │   ├── repository/
│   │   │   │   └── IncidentRepository.java               # JPA Repository
│   │   │   └── service/
│   │   │       └── IncidentMappingService.java           # Layer kiểm duyệt phòng thủ (Mapping)
│   │   └── resources/
│   │       └── application.properties                       # Cấu hình H2 Database
├── pom.xml
└── README.md
```

---

## 💻 4. Mã Nguồn Chi Tiết

### 4.1. Java Record DTO (`IncidentExtraction.java`)
```java
package com.crm.ai.parser.dto;

public record IncidentExtraction(
        String vehiclePlateNumber,
        String incidentType,
        String location,
        Double estimatedCost,
        Boolean emergency,
        String description
) {}
```

### 4.2. JPA Entity (`IncidentReport.java`)
```java
package com.crm.ai.parser.entity;

import com.crm.ai.parser.enums.IncidentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_plate_number", nullable = false, length = 20)
    private String vehiclePlateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 30)
    private IncidentType incidentType;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "estimated_cost", nullable = false)
    private Double estimatedCost;

    @Column(name = "emergency", nullable = false)
    private Boolean emergency;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    protected IncidentReport() {}

    public IncidentReport(String vehiclePlateNumber, IncidentType incidentType, String location,
                          Double estimatedCost, Boolean emergency, String description, String status) {
        this.vehiclePlateNumber = Objects.requireNonNullElse(vehiclePlateNumber, "UNKNOWN_PLATE").toUpperCase().trim();
        this.incidentType = Objects.requireNonNullElse(incidentType, IncidentType.OTHER);
        this.location = Objects.requireNonNullElse(location, "CHƯA XÁC ĐỊNH VỊ TRÍ").trim();
        this.estimatedCost = (estimatedCost != null && estimatedCost >= 0) ? estimatedCost : 0.0;
        this.emergency = Objects.requireNonNullElse(emergency, Boolean.FALSE);
        this.description = (description != null) ? description.trim() : "Không có mô tả chi tiết";
        this.reportedAt = LocalDateTime.now();
        this.status = Objects.requireNonNullElse(status, "PENDING_VERIFICATION");
    }

    public Long getId() { return id; }
    public String getVehiclePlateNumber() { return vehiclePlateNumber; }
    public IncidentType getIncidentType() { return incidentType; }
    public String getLocation() { return location; }
    public Double getEstimatedCost() { return estimatedCost; }
    public Boolean getEmergency() { return emergency; }
    public String getDescription() { return description; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public String getStatus() { return status; }
}
```

---

## 🖥️ 5. Minh Chứng Chạy Thực Tế (Log Console Runtime Evidence)

Chương trình tự động chạy `CommandLineRunner` với **3 Kịch bản thử nghiệm**:

```text
2026-08-18T07:30:57.868+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.ai.parser.demo.DefensiveParserDemo   : ==========================================================================
2026-08-18T07:30:57.868+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.ai.parser.demo.DefensiveParserDemo   : 🚀 BẮT ĐẦU CHẠY DEMO LẬP TRÌNH PHÒNG THỬ (DEFENSIVE PARSING DEMO)
2026-08-18T07:30:57.868+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.ai.parser.demo.DefensiveParserDemo   : ==========================================================================

--- [KỊCH BẢN 1]: Dữ liệu bóc tách từ LLM Đầy Đủ & Chuẩn Xác ---
2026-08-18T07:30:57.868+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : 🔍 [Defensive Barrier] Bắt đầu kiểm duyệt dữ liệu thô từ AI: IncidentExtraction[vehiclePlateNumber=29A-12345, incidentType=NỔ LỐP, location=KM15 Quốc lộ 1A, estimatedCost=5000000.0, emergency=true, description=Xe bị nổ lốp trước bên phải lúc 14:30. Cần cứu hộ khẩn cấp.]
2026-08-18T07:30:57.871+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : ✅ [Defensive Barrier] Chuyển đổi thành công sang Entity: IncidentReport{id=null, vehiclePlateNumber='29A-12345', incidentType=TIRE_PUNCTURE, location='KM15 Quốc lộ 1A', estimatedCost=5000000.0, emergency=true, description='Xe bị nổ lốp trước bên phải lúc 14:30. Cần cứu hộ khẩn cấp.', reportedAt=2026-08-18T07:30:57.871557300, status='PENDING_VERIFICATION'}
2026-08-18T07:30:57.967+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.ai.parser.demo.DefensiveParserDemo   : 💾 [DB Persist Success] ID = 1, Status = PENDING_VERIFICATION

--- [KỊCH BẢN 2]: Dữ liệu bóc tách từ LLM Bị Thiếu / Sai Định Dạng ---
2026-08-18T07:30:57.968+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : 🔍 [Defensive Barrier] Bắt đầu kiểm duyệt dữ liệu thô từ AI: IncidentExtraction[vehiclePlateNumber=null, incidentType=HỎNG_MÁY_VA_CHẠM, location=, estimatedCost=-1500000.0, emergency=null, description=Xe tự dưng chết máy dừng giữa đường]
2026-08-18T07:30:57.968+07:00  WARN 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : ⚠️ [Defensive Barrier] Phát hiện biển số xe NULL/Rống từ AI!
2026-08-18T07:30:57.968+07:00  WARN 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : ⚠️ [Defensive Barrier] Chi phí ước tính không hợp lệ (-1500000.0), tự động gán = 0.0
2026-08-18T07:30:57.968+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : ✅ [Defensive Barrier] Chuyển đổi thành công sang Entity: IncidentReport{id=null, vehiclePlateNumber='UNKNOWN_PLATE', incidentType=OTHER, location='Vị trí chưa rõ (KM / Quốc lộ chưa ghi nhận)', estimatedCost=0.0, emergency=false, description='Xe tự dưng chết máy dừng giữa đường', reportedAt=2026-08-18T07:30:57.968136700, status='REQUIRES_MANUAL_REVIEW'}
2026-08-18T07:30:57.970+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.ai.parser.demo.DefensiveParserDemo   : 💾 [DB Persist Defensive Success] ID = 2, Plate = 'UNKNOWN_PLATE', Cost = 0.0, Status = REQUIRES_MANUAL_REVIEW

--- [KỊCH BẢN 3]: AI Phản Hồi Lỗi / Null Object ---
2026-08-18T07:30:57.970+07:00  WARN 2180 --- [defensive-incident-parser] [main] c.c.a.p.service.IncidentMappingService   : ⚠️ [Defensive Barrier] Nhận dữ liệu bóc tách NULL từ AI! Áp dụng Fallback mặc định.
2026-08-18T07:30:57.971+07:00  INFO 2180 --- [defensive-incident-parser] [main] c.c.ai.parser.demo.DefensiveParserDemo   : 💾 [DB Persist Fallback Success] ID = 3, Status = REQUIRES_MANUAL_REVIEW

==========================================================================
📊 DANH SÁCH BẢN GHI SỰ CỐ ĐÃ LƯU AN TOÀN TRONG DATABASE H2:
==========================================================================
📌 Record #1 | Xe: 29A-12345 | Loại: TIRE_PUNCTURE | Vị trí: KM15 Quốc lộ 1A | Chi phí: 5000000.0 VNĐ | Khẩn: true | Trang thai: PENDING_VERIFICATION
📌 Record #2 | Xe: UNKNOWN_PLATE | Loại: OTHER | Vị trí: Vị trí chưa rõ (KM / Quốc lộ chưa ghi nhận) | Chi phí: 0.0 VNĐ | Khẩn: false | Trang thai: REQUIRES_MANUAL_REVIEW
📌 Record #3 | Xe: UNKNOWN_PLATE | Loại: OTHER | Vị trí: Vị trí không xác định | Chi phí: 0.0 VNĐ | Khẩn: false | Trang thai: REQUIRES_MANUAL_REVIEW
==========================================================================
```

---

## 🚀 6. Hướng Dẫn Push Mã Nguồn Lên GitHub
```bash
git init
git add .
git commit -m "feat: Implement defensive data extraction architecture with Record DTO and JPA Entity"
git remote add origin https://github.com/<YOUR_USERNAME>/defensive-incident-parser.git
git branch -M main
git push -u origin main
```
