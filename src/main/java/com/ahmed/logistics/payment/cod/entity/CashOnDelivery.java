package com.ahmed.logistics.payment.cod.entity;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cash_on_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cod_payment_id", columnNames = "payment_id")
        },
        indexes = {
                @Index(name = "idx_cod_payment_id", columnList = "payment_id", unique = true),
                @Index(name = "idx_cod_delivery_id", columnList = "delivery_id"),
                @Index(name = "idx_cod_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashOnDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_cod_payment")
    )
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "delivery_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cod_delivery")
    )
    private Delivery delivery;

    @Column(name = "amount_to_collect", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountToCollect;

    @Column(name = "collected_amount", precision = 12, scale = 2)
    private BigDecimal collectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CodStatus status = CodStatus.PENDING;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "collected_by_driver_id",
            foreignKey = @ForeignKey(name = "fk_cod_driver")
    )
    private Driver collectedByDriver;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = CodStatus.PENDING;
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
