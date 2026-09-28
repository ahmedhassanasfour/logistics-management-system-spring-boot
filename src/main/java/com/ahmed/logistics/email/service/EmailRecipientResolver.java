package com.ahmed.logistics.email.service;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRecipientResolver {

    private final CustomerRepository customerRepository;

    public Optional<String> resolveCustomerEmail(Long customerId) {
        if (customerId == null) {
            log.warn("Cannot resolve email: customerId is null");
            return Optional.empty();
        }

        try {
            return customerRepository.findById(customerId)
                    .map(Customer::getUser)
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isBlank());
        } catch (Exception ex) {
            log.error("Error resolving email for customer ID: {}. Error: {}", customerId, ex.getMessage());
            return Optional.empty();
        }
    }
}
