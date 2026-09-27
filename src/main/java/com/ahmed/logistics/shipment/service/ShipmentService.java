package com.ahmed.logistics.shipment.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.shipment.dto.CreateShipmentRequest;
import com.ahmed.logistics.shipment.dto.ShipmentResponse;
import com.ahmed.logistics.shipment.dto.UpdateShipmentRequest;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import com.ahmed.logistics.shipment.tracking.service.ShipmentTrackingService;
import com.ahmed.logistics.user.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final CustomerRepository customerRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final ShipmentPricingCalculator pricingCalculator;
    private final ShipmentTrackingService shipmentTrackingService;

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        log.info("Creating shipment for customer ID: {}", request.customerId());

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.customerId()));

        if (customer.getUser() == null || customer.getUser().getRole() != Role.CUSTOMER) {
            throw new BadRequestException("Only valid customers can have shipments created");
        }

        String trackingNumber = generateUniqueTrackingNumber();
        ShipmentPricingCalculator.PriceBreakdown prices = pricingCalculator.calculate(
                request.shipmentType(),
                request.weightKg()
        );

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .customer(customer)
                .status(ShipmentStatus.CREATED)
                .shipmentType(request.shipmentType())
                .pickupAddress(request.pickupAddress())
                .pickupCity(request.pickupCity())
                .pickupPostalCode(request.pickupPostalCode())
                .deliveryAddress(request.deliveryAddress())
                .deliveryCity(request.deliveryCity())
                .deliveryPostalCode(request.deliveryPostalCode())
                .recipientName(request.recipientName())
                .recipientPhone(request.recipientPhone())
                .packageDescription(request.packageDescription())
                .weightKg(request.weightKg())
                .lengthCm(request.lengthCm())
                .widthCm(request.widthCm())
                .heightCm(request.heightCm())
                .basePrice(prices.basePrice())
                .shippingFee(prices.shippingFee())
                .totalPrice(prices.totalPrice())
                .build();

        Shipment saved = shipmentRepository.save(shipment);
        shipmentTrackingService.recordStatusChange(saved, ShipmentStatus.CREATED);
        log.info("Shipment created successfully with ID: {} and tracking number: {}", saved.getId(), saved.getTrackingNumber());
        return ShipmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {
        Shipment shipment = findEntityById(id);
        return ShipmentResponse.fromEntity(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        return ShipmentResponse.fromEntity(shipment);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getCustomerShipments(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }
        return shipmentRepository.findByCustomerId(customerId, pageable)
                .map(ShipmentResponse::fromEntity);
    }

    @Transactional
    public ShipmentResponse updateShipment(Long id, UpdateShipmentRequest request) {
        log.info("Updating shipment with ID: {}", id);
        Shipment shipment = findEntityById(id);

        shipment.setShipmentType(request.shipmentType());
        shipment.setPickupAddress(request.pickupAddress());
        shipment.setPickupCity(request.pickupCity());
        shipment.setPickupPostalCode(request.pickupPostalCode());
        shipment.setDeliveryAddress(request.deliveryAddress());
        shipment.setDeliveryCity(request.deliveryCity());
        shipment.setDeliveryPostalCode(request.deliveryPostalCode());
        shipment.setRecipientName(request.recipientName());
        shipment.setRecipientPhone(request.recipientPhone());
        shipment.setPackageDescription(request.packageDescription());
        shipment.setWeightKg(request.weightKg());
        shipment.setLengthCm(request.lengthCm());
        shipment.setWidthCm(request.widthCm());
        shipment.setHeightCm(request.heightCm());

        ShipmentPricingCalculator.PriceBreakdown prices = pricingCalculator.calculate(
                request.shipmentType(),
                request.weightKg()
        );
        shipment.setBasePrice(prices.basePrice());
        shipment.setShippingFee(prices.shippingFee());
        shipment.setTotalPrice(prices.totalPrice());

        Shipment updated = shipmentRepository.save(shipment);
        log.info("Shipment updated successfully with ID: {}", updated.getId());
        return ShipmentResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteShipment(Long id) {
        log.info("Deleting shipment with ID: {}", id);
        Shipment shipment = findEntityById(id);
        shipmentRepository.delete(shipment);
        log.info("Shipment deleted successfully with ID: {}", id);
    }

    public Shipment findEntityById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
    }

    private String generateUniqueTrackingNumber() {
        for (int i = 0; i < 5; i++) {
            String trackingNumber = trackingNumberGenerator.generate();
            if (!shipmentRepository.existsByTrackingNumber(trackingNumber)) {
                return trackingNumber;
            }
        }
        throw new IllegalStateException("Failed to generate unique tracking number after multiple attempts");
    }
}
