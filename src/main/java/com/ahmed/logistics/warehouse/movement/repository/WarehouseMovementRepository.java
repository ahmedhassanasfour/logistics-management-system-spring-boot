package com.ahmed.logistics.warehouse.movement.repository;

import com.ahmed.logistics.warehouse.movement.entity.WarehouseMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseMovementRepository extends JpaRepository<WarehouseMovement, Long> {

    List<WarehouseMovement> findByShipmentIdOrderByMovedAtAsc(Long shipmentId);

    Optional<WarehouseMovement> findFirstByShipmentIdOrderByMovedAtDesc(Long shipmentId);
}
