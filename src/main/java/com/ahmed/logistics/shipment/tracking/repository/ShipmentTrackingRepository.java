package com.ahmed.logistics.shipment.tracking.repository;

import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);

    Optional<ShipmentTracking> findFirstByShipmentIdOrderByCreatedAtDesc(Long shipmentId);
}
