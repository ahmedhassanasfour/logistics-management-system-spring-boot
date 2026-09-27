package com.ahmed.logistics.warehouse.movement.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.movement.dto.WarehouseMovementResponse;
import com.ahmed.logistics.warehouse.movement.entity.WarehouseMovement;
import com.ahmed.logistics.warehouse.movement.repository.WarehouseMovementRepository;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseMovementService {

    private final ShipmentRepository shipmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMovementRepository warehouseMovementRepository;
    private final ShipmentTrackingService shipmentTrackingService;

    @Transactional
    public WarehouseMovementResponse moveShipmentToWarehouse(Long shipmentId, Long warehouseId, String notes) {
        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }
        if (warehouseId == null) {
            throw new BadRequestException("Warehouse ID must not be null");
        }

        log.info("Attempting to move shipment ID: {} to warehouse ID: {}", shipmentId, warehouseId);

        // 1. Lock Shipment Row
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // 2. Validate Target Warehouse
        Warehouse targetWarehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + warehouseId));

        if (targetWarehouse.getStatus() != WarehouseStatus.ACTIVE) {
            log.warn("Target warehouse ID: {} is not ACTIVE. Current status: {}", warehouseId, targetWarehouse.getStatus());
            throw new BadRequestException("Cannot move shipment to warehouse with status: " + targetWarehouse.getStatus());
        }

        // 3. Prevent Moving to Same Current Warehouse
        if (shipment.getCurrentWarehouse() != null && shipment.getCurrentWarehouse().getId().equals(warehouseId)) {
            log.warn("Shipment ID: {} is already at warehouse ID: {}", shipmentId, warehouseId);
            throw new BadRequestException("Shipment is already currently at warehouse: " + targetWarehouse.getName());
        }

        // 4. Update Current Warehouse & Record Movement
        Warehouse fromWarehouse = shipment.getCurrentWarehouse();
        shipment.setCurrentWarehouse(targetWarehouse);
        shipmentRepository.save(shipment);

        WarehouseMovement movement = WarehouseMovement.builder()
                .shipment(shipment)
                .fromWarehouse(fromWarehouse)
                .toWarehouse(targetWarehouse)
                .notes(notes)
                .build();

        WarehouseMovement savedMovement = warehouseMovementRepository.save(movement);

        // 5. Tracking Event in Same Transaction
        String trackingDescription = "Shipment moved to warehouse: " + targetWarehouse.getName();
        shipmentTrackingService.recordStatusChange(
                shipment,
                shipment.getStatus(),
                trackingDescription,
                targetWarehouse.getName()
        );

        log.info("Shipment ID: {} successfully moved from warehouse: {} to warehouse: {}",
                shipmentId,
                fromWarehouse != null ? fromWarehouse.getName() : "None",
                targetWarehouse.getName());

        return WarehouseMovementResponse.fromEntity(savedMovement);
    }

    @Transactional(readOnly = true)
    public List<WarehouseMovementResponse> getWarehouseMovements(Long shipmentId) {
        if (shipmentId == null) {
            throw new BadRequestException("Shipment ID must not be null");
        }

        log.info("Retrieving warehouse movements for shipment ID: {}", shipmentId);

        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }

        return warehouseMovementRepository.findByShipmentIdOrderByMovedAtAsc(shipmentId)
                .stream()
                .map(WarehouseMovementResponse::fromEntity)
                .toList();
    }
}
