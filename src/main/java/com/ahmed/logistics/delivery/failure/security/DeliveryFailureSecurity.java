package com.ahmed.logistics.delivery.failure.security;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("deliveryFailureSecurity")
@RequiredArgsConstructor
public class DeliveryFailureSecurity {

    private final DeliveryRepository deliveryRepository;
    private final ShipmentRepository shipmentRepository;

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

    public boolean canRead(Long deliveryId, Authentication authentication) {
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

    public boolean canReadByShipment(Long shipmentId, Authentication authentication) {
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

        boolean isAnyDriver = deliveryRepository.findByShipmentIdOrderByCreatedAtDesc(shipmentId)
                .stream()
                .anyMatch(delivery -> isAssignedDriver(delivery, authentication));
        if (isAnyDriver) {
            return true;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isShipmentDriver(shipment, authentication)
                        || isShipmentOwner(shipment, authentication))
                .orElse(false);
    }

    private boolean isAssignedDriver(Delivery delivery, Authentication authentication) {
        if (delivery == null || delivery.getDriver() == null || delivery.getDriver().getUser() == null) {
            return false;
        }
        return delivery.getDriver().getUser().getEmail()
                .equalsIgnoreCase(authentication.getName());
    }

    private boolean isShipmentDriver(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getDriver() == null || shipment.getDriver().getUser() == null) {
            return false;
        }
        return shipment.getDriver().getUser().getEmail()
                .equalsIgnoreCase(authentication.getName());
    }

    private boolean isShipmentOwner(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getCustomer() == null || shipment.getCustomer().getUser() == null) {
            return false;
        }
        return shipment.getCustomer().getUser().getEmail()
                .equalsIgnoreCase(authentication.getName());
    }
}
