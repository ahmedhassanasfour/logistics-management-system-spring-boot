package com.ahmed.logistics.driver.service;

import com.ahmed.logistics.config.CacheNames;
import com.ahmed.logistics.driver.dto.CreateDriverRequest;
import com.ahmed.logistics.driver.dto.DriverResponse;
import com.ahmed.logistics.driver.dto.UpdateDriverRequest;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserService userService;

    @Transactional
    @CacheEvict(value = CacheNames.DRIVER, allEntries = true)
    public DriverResponse createDriver(Long userId, CreateDriverRequest request) {
        Long targetUserId = (userId != null) ? userId : request.userId();
        if (targetUserId == null) {
            throw new BadRequestException("User ID is required to create a driver profile");
        }

        User user = userService.findEntityById(targetUserId);

        if (user.getRole() != Role.DRIVER) {
            throw new BadRequestException("User must have DRIVER role to create a driver profile");
        }

        if (driverRepository.existsByUserId(targetUserId)) {
            throw new BadRequestException("Driver profile already exists for user id: " + targetUserId);
        }

        String licenseNumber = request.licenseNumber().trim();
        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BadRequestException("License number already exists: " + licenseNumber);
        }

        Driver driver = Driver.builder()
                .user(user)
                .phone(request.phone().trim())
                .licenseNumber(licenseNumber)
                .licenseExpiryDate(request.licenseExpiryDate())
                .status(DriverStatus.OFFLINE)
                .build();

        Driver saved = driverRepository.save(driver);
        return DriverResponse.fromEntity(saved);
    }

    @Cacheable(value = CacheNames.DRIVER, key = "#id")
    public DriverResponse getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return DriverResponse.fromEntity(driver);
    }

    @Cacheable(value = CacheNames.DRIVER, key = "'all'")
    public List<DriverResponse> getDrivers() {
        return driverRepository.findAll().stream()
                .map(DriverResponse::fromEntity)
                .toList();
    }

    @Cacheable(value = CacheNames.DRIVER, key = "'user:' + #userId")
    public DriverResponse getDriverByUserId(Long userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found for user id: " + userId));
        return DriverResponse.fromEntity(driver);
    }

    @Transactional
    @CacheEvict(value = CacheNames.DRIVER, allEntries = true)
    public DriverResponse updateDriver(Long id, UpdateDriverRequest request) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        String newLicenseNumber = request.licenseNumber().trim();
        if (!driver.getLicenseNumber().equalsIgnoreCase(newLicenseNumber)) {
            if (driverRepository.existsByLicenseNumber(newLicenseNumber)) {
                throw new BadRequestException("License number already exists: " + newLicenseNumber);
            }
        }

        driver.setPhone(request.phone().trim());
        driver.setLicenseNumber(newLicenseNumber);
        driver.setLicenseExpiryDate(request.licenseExpiryDate());

        Driver updated = driverRepository.save(driver);
        return DriverResponse.fromEntity(updated);
    }

    @Transactional
    @CacheEvict(value = CacheNames.DRIVER, allEntries = true)
    public DriverResponse updateStatus(Long id, DriverStatus status) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        driver.setStatus(status);
        Driver updated = driverRepository.save(driver);
        return DriverResponse.fromEntity(updated);
    }

    @Transactional
    @CacheEvict(value = CacheNames.DRIVER, allEntries = true)
    public void deleteDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        driverRepository.delete(driver);
    }
}

