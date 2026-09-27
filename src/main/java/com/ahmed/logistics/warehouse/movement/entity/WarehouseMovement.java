package com.ahmed.logistics.warehouse.movement.entity;

import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouse_movements",
        indexes = {
                @Index(name = "idx_warehouse_movements_shipment_id", columnList = "shipment_id"),
                @Index(name = "idx_warehouse_movements_moved_at", columnList = "moved_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_warehouse_movements_shipment"))
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id", foreignKey = @ForeignKey(name = "fk_warehouse_movements_from_warehouse"))
    private Warehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_warehouse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_warehouse_movements_to_warehouse"))
    private Warehouse toWarehouse;

    @Column(name = "notes")
    private String notes;

    @Column(name = "moved_at", nullable = false, updatable = false)
    private LocalDateTime movedAt;

    @PrePersist
    protected void onCreate() {
        if (this.movedAt == null) {
            this.movedAt = LocalDateTime.now();
        }
    }
}
