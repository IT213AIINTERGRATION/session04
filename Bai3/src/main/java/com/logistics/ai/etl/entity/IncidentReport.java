package com.logistics.ai.etl.entity;

import com.logistics.ai.etl.enums.UrgencyLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 50)
    private String orderCode;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "incident_type", nullable = false, length = 100)
    private String incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private UrgencyLevel urgency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected IncidentReport() {
    }

    public IncidentReport(String orderCode, String licensePlate, String incidentType, UrgencyLevel urgency) {
        this.orderCode = orderCode;
        this.licensePlate = licensePlate;
        this.incidentType = incidentType;
        this.urgency = urgency;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public String getLicensePlate() { return licensePlate; }
    public String getIncidentType() { return incidentType; }
    public UrgencyLevel getUrgency() { return urgency; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "IncidentReport{" +
                "id=" + id +
                ", orderCode='" + orderCode + '\'' +
                ", licensePlate='" + licensePlate + '\'' +
                ", incidentType='" + incidentType + '\'' +
                ", urgency=" + urgency +
                ", createdAt=" + createdAt +
                '}';
    }
}
