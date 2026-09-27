package com.ahmed.logistics.customer.service;

import com.ahmed.logistics.customer.dto.CreateCustomerRequest;
import com.ahmed.logistics.customer.dto.CustomerResponse;
import com.ahmed.logistics.customer.dto.UpdateCustomerRequest;
import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserService userService;

    @Transactional
    public CustomerResponse createCustomer(Long userId, CreateCustomerRequest request) {
        User user = userService.findEntityById(userId);

        if (user.getRole() != Role.CUSTOMER) {
            throw new BadRequestException("User must have CUSTOMER role to create a customer profile");
        }

        if (customerRepository.existsByUserId(userId)) {
            throw new BadRequestException("Customer profile already exists for user id: " + userId);
        }

        Customer customer = Customer.builder()
                .user(user)
                .phone(request.phone().trim())
                .address(request.address().trim())
                .city(request.city().trim())
                .postalCode(request.postalCode().trim())
                .build();

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return CustomerResponse.fromEntity(customer);
    }

    public CustomerResponse getCustomerByUserId(Long userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for user id: " + userId));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setPhone(request.phone().trim());
        customer.setAddress(request.address().trim());
        customer.setCity(request.city().trim());
        customer.setPostalCode(request.postalCode().trim());

        Customer updated = customerRepository.save(customer);
        return CustomerResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customerRepository.delete(customer);
    }
}
