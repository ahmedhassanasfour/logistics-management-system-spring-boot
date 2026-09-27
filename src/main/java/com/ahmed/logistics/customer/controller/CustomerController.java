package com.ahmed.logistics.customer.controller;

import com.ahmed.logistics.customer.dto.CreateCustomerRequest;
import com.ahmed.logistics.customer.dto.CustomerResponse;
import com.ahmed.logistics.customer.dto.UpdateCustomerRequest;
import com.ahmed.logistics.customer.service.CustomerService;
import com.ahmed.logistics.exception.ForbiddenException;
import com.ahmed.logistics.exception.UnauthorizedException;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication
    ) {
        User user = userService.findEntityByEmail(authentication.getName());
        CustomerResponse response = customerService.createCustomer(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        User user = userService.findEntityByEmail(authentication.getName());
        CustomerResponse response = customerService.getCustomerByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        CustomerResponse customer = customerService.getCustomerById(id);
        validateOwnershipOrAdmin(customer.email(), authentication);
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            Authentication authentication
    ) {
        CustomerResponse existing = customerService.getCustomerById(id);
        validateOwnershipOrAdmin(existing.email(), authentication);
        CustomerResponse updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            Authentication authentication
    ) {
        CustomerResponse existing = customerService.getCustomerById(id);
        validateOwnershipOrAdmin(existing.email(), authentication);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    private void validateOwnershipOrAdmin(String customerEmail, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !authentication.getName().equalsIgnoreCase(customerEmail)) {
            throw new ForbiddenException("You do not have permission to access this customer profile");
        }
    }
}
