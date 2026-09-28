package com.ahmed.logistics.shipment.security;

import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("shipmentSecurity")
@RequiredArgsConstructor
public class ShipmentSecurity {

    private final ShipmentRepository shipmentRepository;
    private final CustomerRepository customerRepository;

    public boolean canCreate(Long customerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (isStaff) {
            return true;
        }

        if (customerId == null) {
            return false;
        }

        return customerRepository.findById(customerId)
                .map(customer ->
                        customer.getUser().getEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canRead(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (isStaff) {
            return true;
        }

        if (shipmentId == null) {
            return false;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isAuthorizedToViewShipment(shipment, authentication))
                .orElse(false);
    }

    public boolean canReadByTracking(String trackingNumber, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (isStaff) {
            return true;
        }

        if (trackingNumber == null || trackingNumber.isBlank()) {
            return false;
        }

        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .map(shipment -> isAuthorizedToViewShipment(shipment, authentication))
                .orElse(false);
    }

    private boolean isAuthorizedToViewShipment(Shipment shipment, Authentication authentication) {
        if (shipment == null) {
            return false;
        }

        // 1. Customer ownership
        if (shipment.getCustomer() != null && shipment.getCustomer().getUser() != null) {
            if (shipment.getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
                return true;
            }
        }

        // 2. Assigned driver ownership
        if (shipment.getDriver() != null && shipment.getDriver().getUser() != null) {
            if (shipment.getDriver().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
                return true;
            }
        }

        return false;
    }

    public boolean canReadCustomerShipments(Long customerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (isStaff) {
            return true;
        }

        return customerRepository.findById(customerId)
                .map(customer ->
                        customer.getUser().getEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canModify(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (isStaff) {
            return true;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment ->
                        shipment.getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}
