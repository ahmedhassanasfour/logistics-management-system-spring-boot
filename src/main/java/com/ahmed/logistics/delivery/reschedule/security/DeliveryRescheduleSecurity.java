package com.ahmed.logistics.delivery.reschedule.security;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.repository.DeliveryRepository;
import com.ahmed.logistics.delivery.reschedule.entity.DeliveryReschedule;
import com.ahmed.logistics.delivery.reschedule.repository.DeliveryRescheduleRepository;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("deliveryRescheduleSecurity")
@RequiredArgsConstructor
public class DeliveryRescheduleSecurity {

    private final DeliveryRescheduleRepository deliveryRescheduleRepository;
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
                .map(delivery -> isAssignedDriver(delivery.getDriver(), authentication))
                .orElse(false);
    }

    public boolean canRead(Long rescheduleId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (isStaff) {
            return true;
        }

        if (rescheduleId == null) {
            return false;
        }

        Optional<DeliveryReschedule> rescheduleOpt = deliveryRescheduleRepository.findById(rescheduleId);
        if (rescheduleOpt.isEmpty()) {
            return false;
        }

        DeliveryReschedule reschedule = rescheduleOpt.get();

        boolean isDriver = false;
        if (reschedule.getFailedDelivery() != null) {
            isDriver = isAssignedDriver(reschedule.getFailedDelivery().getDriver(), authentication);
        }
        if (!isDriver && reschedule.getShipment() != null) {
            isDriver = isAssignedDriver(reschedule.getShipment().getDriver(), authentication);
        }

        boolean isCustomer = false;
        if (reschedule.getShipment() != null) {
            isCustomer = isShipmentOwner(reschedule.getShipment().getCustomer(), authentication);
        }

        return isDriver || isCustomer;
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

        return shipmentRepository.findById(shipmentId)
                .map(shipment -> isAssignedDriver(shipment.getDriver(), authentication)
                        || isShipmentOwner(shipment.getCustomer(), authentication)
                        || isAnyDeliveryDriver(shipmentId, authentication))
                .orElse(false);
    }

    private boolean isAnyDeliveryDriver(Long shipmentId, Authentication authentication) {
        return deliveryRepository.findByShipmentIdOrderByCreatedAtDesc(shipmentId)
                .stream()
                .anyMatch(delivery -> isAssignedDriver(delivery.getDriver(), authentication));
    }

    private boolean isAssignedDriver(Driver driver, Authentication authentication) {
        if (driver == null || driver.getUser() == null || driver.getUser().getEmail() == null) {
            return false;
        }
        return driver.getUser().getEmail().equalsIgnoreCase(authentication.getName());
    }

    private boolean isShipmentOwner(Customer customer, Authentication authentication) {
        if (customer == null || customer.getUser() == null || customer.getUser().getEmail() == null) {
            return false;
        }
        return customer.getUser().getEmail().equalsIgnoreCase(authentication.getName());
    }
}
