package com.ahmed.logistics.delivery.security;

import com.ahmed.logistics.delivery.repository.DeliveryRepository;
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

    public boolean canManageDelivery(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER")
                        || a.getAuthority().equals("ROLE_DRIVER"));
    }

    public boolean canRead(Long deliveryId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean hasStaffOrDriverRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER")
                        || a.getAuthority().equals("ROLE_DRIVER"));

        if (hasStaffOrDriverRole) {
            return true;
        }

        if (deliveryId == null) {
            return false;
        }

        return deliveryRepository.findById(deliveryId)
                .map(delivery -> isShipmentOwner(delivery.getShipment(), authentication))
                .orElse(false);
    }

    public boolean canReadByShipment(Long shipmentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean hasStaffOrDriverRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER")
                        || a.getAuthority().equals("ROLE_DRIVER"));

        if (hasStaffOrDriverRole) {
            return true;
        }

        if (shipmentId == null) {
            return false;
        }

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isShipmentOwner(shipment, authentication))
                .orElse(false);
    }

    private boolean isShipmentOwner(Shipment shipment, Authentication authentication) {
        if (shipment == null || shipment.getCustomer() == null || shipment.getCustomer().getUser() == null) {
            return false;
        }
        return shipment.getCustomer().getUser().getEmail()
                .equalsIgnoreCase(authentication.getName());
    }
}
