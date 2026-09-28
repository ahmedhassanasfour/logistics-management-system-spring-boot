package com.ahmed.logistics.delivery.reschedule.entity;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.shipment.entity.Shipment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_reschedules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_delivery_reschedules_failed_delivery_id", columnNames = "failed_delivery_id")
        },
        indexes = {
                @Index(name = "idx_delivery_reschedules_shipment_id", columnList = "shipment_id"),
                @Index(name = "idx_delivery_reschedules_failed_delivery_id", columnList = "failed_delivery_id", unique = true),
                @Index(name = "idx_delivery_reschedules_status", columnList = "status"),
                @Index(name = "idx_delivery_reschedules_scheduled_at", columnList = "scheduled_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryReschedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "shipment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_delivery_reschedules_shipment")
    )
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "failed_delivery_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_delivery_reschedules_failed_delivery")
    )
    private Delivery failedDelivery;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RescheduleStatus status = RescheduleStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = RescheduleStatus.SCHEDULED;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
