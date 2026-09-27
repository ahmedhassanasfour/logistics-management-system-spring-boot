package com.ahmed.logistics.vehicle.service;

import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.vehicle.dto.CreateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.UpdateVehicleRequest;
import com.ahmed.logistics.vehicle.dto.VehicleResponse;
import com.ahmed.logistics.vehicle.entity.Vehicle;
import com.ahmed.logistics.vehicle.entity.VehicleStatus;
import com.ahmed.logistics.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        String plateNumber = request.plateNumber().trim();
        if (vehicleRepository.existsByPlateNumber(plateNumber)) {
            throw new BadRequestException("Vehicle with plate number already exists: " + plateNumber);
        }

        Vehicle vehicle = Vehicle.builder()
                .plateNumber(plateNumber)
                .type(request.type())
                .status(VehicleStatus.AVAILABLE)
                .brand(request.brand().trim())
                .model(request.model().trim())
                .manufacturingYear(request.manufacturingYear())
                .maxWeightKg(request.maxWeightKg())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(saved);
    }

    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        return VehicleResponse.fromEntity(vehicle);
    }

    public VehicleResponse getVehicleByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumber(plateNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with plate number: " + plateNumber.trim()));
        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        String newPlateNumber = request.plateNumber().trim();
        if (!vehicle.getPlateNumber().equalsIgnoreCase(newPlateNumber)) {
            if (vehicleRepository.existsByPlateNumber(newPlateNumber)) {
                throw new BadRequestException("Vehicle with plate number already exists: " + newPlateNumber);
            }
        }

        vehicle.setPlateNumber(newPlateNumber);
        vehicle.setType(request.type());
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setManufacturingYear(request.manufacturingYear());
        vehicle.setMaxWeightKg(request.maxWeightKg());

        Vehicle updated = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        vehicleRepository.delete(vehicle);
    }
}
