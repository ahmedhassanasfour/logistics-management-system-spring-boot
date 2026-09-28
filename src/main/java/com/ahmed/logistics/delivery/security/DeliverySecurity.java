package com.ahmed.logistics.delivery.security;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("deliverySecurity")
@RequiredArgsConstructor
public class DeliverySecurity {

    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;

    public boolean canManageShipmentDelivery(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || shipmentId == null) {
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

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isShipmentDriver(shipment, authentication))
                .orElse(false);
    }

    public boolean canRead(Long deliveryId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || deliveryId == null) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        return deliveryRepository.findById(deliveryId)
                .map(delivery -> isAssignedDriver(delivery, authentication)
                        || isShipmentOwner(delivery.getShipment(), authentication))
                .orElse(false);
    }

    public boolean canReadByShipment(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || shipmentId == null) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        boolean isAnyAssignedDriver = deliveryRepository.findByShipmentIdOrderByCreatedAtDesc(shipmentId)
                .stream()
                .anyMatch(delivery -> isAssignedDriver(delivery, authentication));
        if (isAnyAssignedDriver) {
            return true;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isShipmentDriver(shipment, authentication)
                        || isShipmentOwner(shipment, authentication))
                .orElse(false);
    }

    private boolean isAssignedDriver(Delivery delivery, Authentication authentication) {
        if (delivery == null || delivery.getDriver() == null) {
            return false;
        }
        return isDriverUser(delivery.getDriver(), authentication);
    }

    private boolean isShipmentDriver(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getDriver() == null) {
            return false;
        }
        return isDriverUser(shipment.getDriver(), authentication);
    }

    private boolean isDriverUser(Driver driver, Authentication authentication) {
        if (driver == null || driver.getUser() == null || driver.getUser().getEmail() == null) {
            return false;
        }
        return driver.getUser().getEmail().equalsIgnoreCase(authentication.getName());
    }

    private boolean isShipmentOwner(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getCustomer() == null || shipment.getCustomer().getUser() == null) {
            return false;
        }
        return shipment.getCustomer().getUser().getEmail()
                .equalsIgnoreCase(authentication.getName());
    }
}
