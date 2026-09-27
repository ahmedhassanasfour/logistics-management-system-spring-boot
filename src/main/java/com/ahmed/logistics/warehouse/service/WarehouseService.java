package com.ahmed.logistics.warehouse.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.warehouse.dto.CreateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.UpdateWarehouseRequest;
import com.ahmed.logistics.warehouse.dto.WarehouseResponse;
import com.ahmed.logistics.warehouse.entity.Warehouse;
import com.ahmed.logistics.warehouse.entity.WarehouseStatus;
import com.ahmed.logistics.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        String name = request.name().trim();
        if (warehouseRepository.existsByName(name)) {
            throw new BadRequestException("Warehouse with name already exists: " + name);
        }

        Warehouse warehouse = Warehouse.builder()
                .name(name)
                .address(request.address().trim())
                .city(request.city().trim())
                .postalCode(request.postalCode().trim())
                .phone(request.phone().trim())
                .email(request.email().trim().toLowerCase())
                .status(WarehouseStatus.ACTIVE)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        return WarehouseResponse.fromEntity(saved);
    }

    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return WarehouseResponse.fromEntity(warehouse);
    }

    public WarehouseResponse getWarehouseByName(String name) {
        Warehouse warehouse = warehouseRepository.findByName(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with name: " + name.trim()));
        return WarehouseResponse.fromEntity(warehouse);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        String newName = request.name().trim();
        if (!warehouse.getName().equalsIgnoreCase(newName)) {
            if (warehouseRepository.existsByName(newName)) {
                throw new BadRequestException("Warehouse with name already exists: " + newName);
            }
        }

        warehouse.setName(newName);
        warehouse.setAddress(request.address().trim());
        warehouse.setCity(request.city().trim());
        warehouse.setPostalCode(request.postalCode().trim());
        warehouse.setPhone(request.phone().trim());
        warehouse.setEmail(request.email().trim().toLowerCase());
        // Status remains unchanged

        Warehouse updated = warehouseRepository.save(warehouse);
        return WarehouseResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        warehouseRepository.delete(warehouse);
    }
}
