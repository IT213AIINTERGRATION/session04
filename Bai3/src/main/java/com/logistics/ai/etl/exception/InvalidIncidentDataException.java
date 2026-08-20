package com.logistics.ai.etl.exception;

/**
 * Ngoại lệ ném ra khi dữ liệu bóc tách từ AI không vượt qua kiểm chứng phòng thủ (Defensive Validation).
 * Kích hoạt Rollback giao dịch trong @Transactional.
 */
public class InvalidIncidentDataException extends RuntimeException {
    public InvalidIncidentDataException(String message) {
        super(message);
    }
}
