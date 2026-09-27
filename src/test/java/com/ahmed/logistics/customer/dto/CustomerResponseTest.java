package com.ahmed.logistics.customer.dto;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CustomerResponseTest {

    @Test
    void passwordIsNeverPresentInCustomerResponse() {
        // 1. Verify no field named 'password' exists in CustomerResponse
        boolean hasPasswordField = Arrays.stream(CustomerResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("password"));
        assertFalse(hasPasswordField, "CustomerResponse must not declare a password field");

        // 2. Verify no getter or method exposes password
        boolean hasPasswordMethod = Arrays.stream(CustomerResponse.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("password"));
        assertFalse(hasPasswordMethod, "CustomerResponse must not declare any method exposing password");

        // 3. Verify mapping from entity does not expose password
        User user = User.builder()
                .id(5L)
                .email("test_cust@logistics.com")
                .password("$2a$10$superSecretHash")
                .firstName("Sara")
                .lastName("Connor")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        Customer customer = Customer.builder()
                .id(12L)
                .user(user)
                .phone("+15551234")
                .address("100 Elm St")
                .city("Denver")
                .postalCode("80201")
                .build();

        CustomerResponse response = CustomerResponse.fromEntity(customer);

        assertEquals(12L, response.id());
        assertEquals(5L, response.userId());
        assertEquals("test_cust@logistics.com", response.email());
        assertEquals("Sara", response.firstName());
        assertEquals("Connor", response.lastName());
        assertEquals("+15551234", response.phone());
        assertEquals("100 Elm St", response.address());
        assertEquals("Denver", response.city());
        assertEquals("80201", response.postalCode());
    }
}
