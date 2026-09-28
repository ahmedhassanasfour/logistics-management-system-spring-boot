package com.ahmed.logistics.payment.security;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.payment.entity.Payment;
import com.ahmed.logistics.payment.repository.PaymentRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;

    public boolean canManagePayment(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
    }

    public boolean canCreateForShipment(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        if (shipmentId == null) {
            return false;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isShipmentOwner(shipment, authentication))
                .orElse(false);
    }

    public boolean canReadPayment(Long paymentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        if (paymentId == null) {
            return false;
        }

        return paymentRepository.findById(paymentId)
                .map(payment -> isShipmentOwner(payment.getShipment(), authentication)
                        || isShipmentDriver(payment.getShipment(), authentication))
                .orElse(false);
    }

    public boolean canReadShipmentPayment(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        if (shipmentId == null) {
            return false;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isShipmentOwner(shipment, authentication)
                        || isShipmentDriver(shipment, authentication))
                .orElse(false);
    }

    private boolean isShipmentOwner(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getCustomer() == null) {
            return false;
        }
        Customer customer = shipment.getCustomer();
        if (customer.getUser() == null || customer.getUser().getEmail() == null) {
            return false;
        }
        return customer.getUser().getEmail().equalsIgnoreCase(authentication.getName());
    }

    private boolean isShipmentDriver(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getDriver() == null) {
            return false;
        }
        Driver driver = shipment.getDriver();
        if (driver.getUser() == null || driver.getUser().getEmail() == null) {
            return false;
        }
        return driver.getUser().getEmail().equalsIgnoreCase(authentication.getName());
    }
}
