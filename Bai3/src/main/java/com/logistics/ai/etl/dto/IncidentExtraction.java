package com.logistics.ai.etl.dto;

/**
 * Java Record DTO đại diện cho dữ liệu bóc tách thô từ LLM.
 */
public record IncidentExtraction(
        String orderCode,
        String licensePlate,
        String incidentType,
        String urgency
) {}
