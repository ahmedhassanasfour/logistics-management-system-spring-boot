package com.ahmed.logistics.customer.dto;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.user.entity.User;

public record CustomerResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        String address,
        String city,
        String postalCode
) {
    public static CustomerResponse fromEntity(Customer customer) {
        User user = customer.getUser();
        return new CustomerResponse(
                customer.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCity(),
                customer.getPostalCode()
        );
    }
}
