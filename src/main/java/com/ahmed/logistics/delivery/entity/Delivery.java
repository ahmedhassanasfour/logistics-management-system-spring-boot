package com.ahmed.logistics.delivery.entity;

import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.shipment.entity.Shipment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "deliveries",
        indexes = {
                @Index(name = "idx_deliveries_shipment_id", columnList = "shipment_id"),
                @Index(name = "idx_deliveries_driver_id", columnList = "driver_id"),
                @Index(name = "idx_deliveries_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "shipment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deliveries_shipment")
    )
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "driver_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deliveries_driver")
    )
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = DeliveryStatus.ASSIGNED;
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
