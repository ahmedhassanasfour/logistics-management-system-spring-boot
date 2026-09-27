package com.ahmed.logistics.delivery.pod.entity;

import com.ahmed.logistics.delivery.entity.Delivery;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "proof_of_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pod_delivery_id", columnNames = "delivery_id")
        },
        indexes = {
                @Index(name = "idx_pod_delivery_id", columnList = "delivery_id", unique = true),
                @Index(name = "idx_pod_confirmed_at", columnList = "confirmed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "delivery_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_proof_of_delivery_delivery")
    )
    private Delivery delivery;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "recipient_id")
    private String recipientId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        }
    }
}
