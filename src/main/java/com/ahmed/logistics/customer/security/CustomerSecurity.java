package com.ahmed.logistics.customer.security;

import com.ahmed.logistics.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("customerSecurity")
@RequiredArgsConstructor
public class CustomerSecurity {

    private final CustomerRepository customerRepository;

    public boolean isOwnerOrAdmin(Long customerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        return customerRepository.findById(customerId)
                .map(customer ->
                        customer.getUser().getEmail()
                                .equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canRead(Long customerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean canReadRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));

        if (canReadRole) {
            return true;
        }

        return customerRepository.findById(customerId)
                .map(customer ->
                        customer.getUser().getEmail()
                                .equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}
