package com.crm.ai.parser.entity;

import com.crm.ai.parser.enums.IncidentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity biểu diễn sự cố đã qua kiểm duyệt và lưu trữ trong CSDL.
 * 
 * Đặc điểm thiết kế Lập trình phòng thủ (Defensive Programming):
 * - Đóng gói chặt chẽ (Encapsulation): Không có Setter tự do cho các trường quan trọng (ID, CreatedAt).
 * - Ràng buộc DB nghiêm ngặt: NOT NULL, Length limits, Enums mapped as STRING.
 * - Constructor protected hỗ trợ Hibernate proxy nhưng bảo vệ tính toàn vẹn nghiệp vụ.
 */
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

    /**
     * Constructor no-arg bắt buộc bởi JPA/Hibernate.
     * Để mức truy cập protected để ngăn cản việc khởi tạo trực tiếp entity rỗng từ bên ngoài.
     */
    protected IncidentReport() {
    }

    /**
     * Constructor nghiệp vụ kiểm soát toàn vẹn dữ liệu (Defensive Constructor).
     */
    public IncidentReport(String vehiclePlateNumber,
                          IncidentType incidentType,
                          String location,
                          Double estimatedCost,
                          Boolean emergency,
                          String description,
                          String status) {
        this.vehiclePlateNumber = Objects.requireNonNullElse(vehiclePlateNumber, "UNKNOWN_PLATE").toUpperCase().trim();
        this.incidentType = Objects.requireNonNullElse(incidentType, IncidentType.OTHER);
        this.location = Objects.requireNonNullElse(location, "CHƯA XÁC ĐỊNH VỊ TRÍ").trim();
        this.estimatedCost = (estimatedCost != null && estimatedCost >= 0) ? estimatedCost : 0.0;
        this.emergency = Objects.requireNonNullElse(emergency, Boolean.FALSE);
        this.description = (description != null) ? description.trim() : "Không có mô tả chi tiết";
        this.reportedAt = LocalDateTime.now();
        this.status = Objects.requireNonNullElse(status, "PENDING_VERIFICATION");
    }

    // Getters đại diện cho tính đóng gói (Read-only access)
    public Long getId() {
        return id;
    }

    public String getVehiclePlateNumber() {
        return vehiclePlateNumber;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public String getLocation() {
        return location;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    public Boolean getEmergency() {
        return emergency;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public String getStatus() {
        return status;
    }

    // Business Methods để cập nhật trạng thái an toàn
    public void markAsVerified() {
        this.status = "VERIFIED";
    }

    public void markAsResolved() {
        this.status = "RESOLVED";
    }

    @Override
    public String toString() {
        return "IncidentReport{" +
                "id=" + id +
                ", vehiclePlateNumber='" + vehiclePlateNumber + '\'' +
                ", incidentType=" + incidentType +
                ", location='" + location + '\'' +
                ", estimatedCost=" + estimatedCost +
                ", emergency=" + emergency +
                ", description='" + description + '\'' +
                ", reportedAt=" + reportedAt +
                ", status='" + status + '\'' +
                '}';
    }
}
