package com.ahmed.logistics.payment.cod.security;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.payment.cod.entity.CashOnDelivery;
import com.ahmed.logistics.payment.cod.repository.CashOnDeliveryRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("codSecurity")
@RequiredArgsConstructor
public class CodSecurity {

    private final CashOnDeliveryRepository cashOnDeliveryRepository;
    private final DeliveryRepository deliveryRepository;

    public boolean canCreate(Long deliveryId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        boolean isDriver = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
        if (!isDriver) {
            return false;
        }

        if (deliveryId == null) {
            return false;
        }

        return deliveryRepository.findById(deliveryId)
                .map(delivery -> isAssignedDriver(delivery, authentication))
                .orElse(false);
    }

    public boolean canCollect(Long codId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        boolean isDriver = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
        if (!isDriver) {
            return false;
        }

        if (codId == null) {
            return false;
        }

        return cashOnDeliveryRepository.findById(codId)
                .map(cod -> isAssignedDriver(cod.getDelivery(), authentication))
                .orElse(false);
    }

    public boolean canManage(Long codId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        boolean isDriver = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
        if (!isDriver) {
            return false;
        }

        if (codId == null) {
            return false;
        }

        return cashOnDeliveryRepository.findById(codId)
                .map(cod -> isAssignedDriver(cod.getDelivery(), authentication))
                .orElse(false);
    }

    public boolean canRead(Long codId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        if (codId == null) {
            return false;
        }

        return cashOnDeliveryRepository.findById(codId)
                .map(cod -> {
                    Shipment shipment = null;
                    if (cod.getPayment() != null && cod.getPayment().getShipment() != null) {
                        shipment = cod.getPayment().getShipment();
                    } else if (cod.getDelivery() != null && cod.getDelivery().getShipment() != null) {
                        shipment = cod.getDelivery().getShipment();
                    }
                    return isAssignedDriver(cod.getDelivery(), authentication)
                            || isShipmentOwner(shipment, authentication);
                })
                .orElse(false);
    }

    public boolean canReadByDelivery(Long deliveryId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        if (deliveryId == null) {
            return false;
        }

        return deliveryRepository.findById(deliveryId)
                .map(delivery -> isAssignedDriver(delivery, authentication)
                        || isShipmentOwner(delivery.getShipment(), authentication))
                .orElse(false);
    }

    private boolean isAssignedDriver(Delivery delivery, Authentication authentication) {
        if (delivery == null || delivery.getDriver() == null) {
            return false;
        }
        Driver driver = delivery.getDriver();
        if (driver.getUser() == null || driver.getUser().getEmail() == null) {
            return false;
        }
        return driver.getUser().getEmail().equalsIgnoreCase(authentication.getName());
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
}
