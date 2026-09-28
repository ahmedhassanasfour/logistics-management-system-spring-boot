package com.ahmed.logistics.delivery.failure.entity;

import com.ahmed.logistics.delivery.entity.Delivery;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_failures",
        indexes = {
                @Index(name = "idx_delivery_failures_delivery_id", columnList = "delivery_id"),
                @Index(name = "idx_delivery_failures_failed_at", columnList = "failed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "delivery_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_delivery_failures_delivery")
    )
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private DeliveryFailureReason reason;

    @Column(name = "notes")
    private String notes;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.failedAt == null) {
            this.failedAt = LocalDateTime.now();
        }
    }
}
