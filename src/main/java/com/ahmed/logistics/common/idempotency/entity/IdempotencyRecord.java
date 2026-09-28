package com.ahmed.logistics.common.idempotency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_operation_key",
                        columnNames = {"operation", "idempotency_key"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_idempotency_operation_key",
                        columnList = "operation, idempotency_key",
                        unique = true
                ),
                @Index(name = "idx_idempotency_status", columnList = "status"),
                @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "username", length = 150)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = IdempotencyStatus.PROCESSING;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.createdAt.plusHours(24);
        }
    }
}
