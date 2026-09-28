package com.ahmed.logistics.customer.controller;

import com.ahmed.logistics.customer.dto.CreateCustomerRequest;
import com.ahmed.logistics.customer.dto.CustomerResponse;
import com.ahmed.logistics.customer.dto.UpdateCustomerRequest;
import com.ahmed.logistics.customer.service.CustomerService;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customers", description = "Customer account creation and profile management")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;

    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication
    ) {
        User user = userService.findEntityByEmail(authentication.getName());
        CustomerResponse response = customerService.createCustomer(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        User user = userService.findEntityByEmail(authentication.getName());
        CustomerResponse response = customerService.getCustomerByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@customerSecurity.canRead(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @PreAuthorize("@customerSecurity.isOwnerOrAdmin(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            Authentication authentication
    ) {
        CustomerResponse updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("@customerSecurity.isOwnerOrAdmin(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            Authentication authentication
    ) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
