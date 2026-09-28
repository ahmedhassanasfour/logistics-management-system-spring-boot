package com.ahmed.logistics.shipment.tracking.repository;

import com.ahmed.logistics.shipment.tracking.entity.ShipmentTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);

    Page<ShipmentTracking> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId, Pageable pageable);

    Optional<ShipmentTracking> findFirstByShipmentIdOrderByCreatedAtDesc(Long shipmentId);
}
