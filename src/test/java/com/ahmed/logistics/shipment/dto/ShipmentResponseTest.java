package com.ahmed.logistics.shipment.dto;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.shipment.entity.Shipment;
import com.ahmed.logistics.shipment.entity.ShipmentStatus;
import com.ahmed.logistics.shipment.entity.ShipmentType;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentResponseTest {

    @Test
    @DisplayName("fromEntity maps all Shipment fields correctly to ShipmentResponse")
    void fromEntity_mapsAllFieldsCorrectly() {
        User user = User.builder()
                .id(1L)
                .email("john.doe@logistics.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.CUSTOMER)
                .build();

        Customer customer = Customer.builder()
                .id(10L)
                .user(user)
                .phone("+1234567890")
                .address("123 Main St")
                .city("Dallas")
                .postalCode("75001")
                .build();

        LocalDateTime now = LocalDateTime.now();

        Shipment shipment = Shipment.builder()
                .id(100L)
                .trackingNumber("SHP-ABCD1234")
                .customer(customer)
                .status(ShipmentStatus.CREATED)
                .shipmentType(ShipmentType.EXPRESS)
                .pickupAddress("100 Pickup Rd")
                .pickupCity("Dallas")
                .pickupPostalCode("75001")
                .deliveryAddress("200 Delivery Ave")
                .deliveryCity("Houston")
                .deliveryPostalCode("77001")
                .recipientName("Alice Smith")
                .recipientPhone("+1987654321")
                .packageDescription("Electronics and fragile components")
                .weightKg(4.5)
                .lengthCm(30.0)
                .widthCm(20.0)
                .heightCm(15.0)
                .basePrice(new BigDecimal("25.00"))
                .shippingFee(new BigDecimal("22.50"))
                .totalPrice(new BigDecimal("47.50"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        ShipmentResponse response = ShipmentResponse.fromEntity(shipment);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("SHP-ABCD1234", response.trackingNumber());
        assertEquals(10L, response.customerId());
        assertEquals("John Doe", response.customerName());
        assertEquals("john.doe@logistics.com", response.customerEmail());
        assertEquals(ShipmentStatus.CREATED, response.status());
        assertEquals(ShipmentType.EXPRESS, response.shipmentType());
        assertEquals("100 Pickup Rd", response.pickupAddress());
        assertEquals("Dallas", response.pickupCity());
        assertEquals("75001", response.pickupPostalCode());
        assertEquals("200 Delivery Ave", response.deliveryAddress());
        assertEquals("Houston", response.deliveryCity());
        assertEquals("77001", response.deliveryPostalCode());
        assertEquals("Alice Smith", response.recipientName());
        assertEquals("+1987654321", response.recipientPhone());
        assertEquals("Electronics and fragile components", response.packageDescription());
        assertEquals(4.5, response.weightKg());
        assertEquals(30.0, response.lengthCm());
        assertEquals(20.0, response.widthCm());
        assertEquals(15.0, response.heightCm());
        assertEquals(new BigDecimal("25.00"), response.basePrice());
        assertEquals(new BigDecimal("22.50"), response.shippingFee());
        assertEquals(new BigDecimal("47.50"), response.totalPrice());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
}
