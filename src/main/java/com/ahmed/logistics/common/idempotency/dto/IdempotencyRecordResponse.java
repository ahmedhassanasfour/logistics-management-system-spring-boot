package com.ahmed.logistics.common.idempotency.dto;

import com.ahmed.logistics.common.idempotency.entity.IdempotencyRecord;
import com.ahmed.logistics.common.idempotency.entity.IdempotencyStatus;

import java.time.LocalDateTime;

public record IdempotencyRecordResponse(
        Long id,
        String idempotencyKey,
        String operation,
        String requestHash,
        String username,
        IdempotencyStatus status,
        Integer responseStatus,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
    public static IdempotencyRecordResponse fromEntity(IdempotencyRecord record) {
        if (record == null) {
            return null;
        }
        return new IdempotencyRecordResponse(
                record.getId(),
                record.getIdempotencyKey(),
                record.getOperation(),
                record.getRequestHash(),
                record.getUsername(),
                record.getStatus(),
                record.getResponseStatus(),
                record.getCreatedAt(),
                record.getExpiresAt()
        );
    }
}
