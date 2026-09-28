package com.ahmed.logistics.shipment.entity;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipments_tracking_number", columnNames = "tracking_number")
        },
        indexes = {
                @Index(name = "idx_shipments_tracking_number", columnList = "tracking_number", unique = true),
                @Index(name = "idx_shipments_customer_id", columnList = "customer_id"),
                @Index(name = "idx_shipments_driver_id", columnList = "driver_id"),
                @Index(name = "idx_shipments_vehicle_id", columnList = "vehicle_id"),
                @Index(name = "idx_shipments_current_warehouse_id", columnList = "current_warehouse_id")
        }
)
@Check(constraints = "weight_kg > 0 AND base_price >= 0 AND shipping_fee >= 0 AND total_price >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", foreignKey = @ForeignKey(name = "fk_shipments_driver"))
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", foreignKey = @ForeignKey(name = "fk_shipments_vehicle"))
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_warehouse_id", foreignKey = @ForeignKey(name = "fk_shipments_current_warehouse"))
    private Warehouse currentWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_type", nullable = false)
    private ShipmentType shipmentType;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_city", nullable = false)
    private String pickupCity;

    @Column(name = "pickup_postal_code", nullable = false)
    private String pickupPostalCode;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_city", nullable = false)
    private String deliveryCity;

    @Column(name = "delivery_postal_code", nullable = false)
    private String deliveryPostalCode;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "package_description")
    private String packageDescription;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "length_cm")
    private Double lengthCm;

    @Column(name = "width_cm")
    private Double widthCm;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ShipmentStatus.CREATED;
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
