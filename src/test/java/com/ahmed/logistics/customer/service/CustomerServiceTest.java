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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomerService customerService;

    private User customerUser;
    private User adminUser;
    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(1L)
                .email("customer@logistics.com")
                .firstName("John")
                .lastName("Customer")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(2L)
                .email("admin@logistics.com")
                .firstName("Super")
                .lastName("Admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        sampleCustomer = Customer.builder()
                .id(10L)
                .user(customerUser)
                .phone("+123456789")
                .address("789 Maple Rd")
                .city("Boston")
                .postalCode("02108")
                .build();
    }

    @Test
    void createCustomer_successfullyCreatesProfile() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "+123456789",
                "789 Maple Rd",
                "Boston",
                "02108"
        );

        when(userService.findEntityById(1L)).thenReturn(customerUser);
        when(customerRepository.existsByUserId(1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponse response = customerService.createCustomer(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("customer@logistics.com", response.email());
        assertEquals("+123456789", response.phone());
        assertEquals("789 Maple Rd", response.address());
        assertEquals("Boston", response.city());
        assertEquals("02108", response.postalCode());

        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void createCustomer_duplicateProfileRejected() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "+123456789",
                "789 Maple Rd",
                "Boston",
                "02108"
        );

        when(userService.findEntityById(1L)).thenReturn(customerUser);
        when(customerRepository.existsByUserId(1L)).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> customerService.createCustomer(1L, request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createCustomer_nonCustomerRoleRejected() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "+123456789",
                "789 Maple Rd",
                "Boston",
                "02108"
        );

        when(userService.findEntityById(2L)).thenReturn(adminUser);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> customerService.createCustomer(2L, request)
        );

        assertTrue(ex.getMessage().contains("CUSTOMER role"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void getCustomerById_returnsCustomerWhenFound() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponse response = customerService.getCustomerById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("customer@logistics.com", response.email());
        verify(customerRepository).findById(10L);
    }

    @Test
    void getCustomerById_throwsResourceNotFoundExceptionWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerService.getCustomerById(99L)
        );
        verify(customerRepository).findById(99L);
    }

    @Test
    void getCustomerByUserId_returnsCustomerWhenFound() {
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponse response = customerService.getCustomerByUserId(1L);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals("customer@logistics.com", response.email());
        verify(customerRepository).findByUserId(1L);
    }

    @Test
    void getCustomerByUserId_throwsResourceNotFoundExceptionWhenMissing() {
        when(customerRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerService.getCustomerByUserId(99L)
        );
        verify(customerRepository).findByUserId(99L);
    }

    @Test
    void updateCustomer_updatesOnlyProfileFields() {
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "+999999999",
                "Updated St",
                "Chicago",
                "60601"
        );

        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomer(10L, updateRequest);

        assertNotNull(response);
        assertEquals("+999999999", response.phone());
        assertEquals("Updated St", response.address());
        assertEquals("Chicago", response.city());
        assertEquals("60601", response.postalCode());
        // User identity and email remain untouched
        assertEquals(1L, response.userId());
        assertEquals("customer@logistics.com", response.email());

        verify(customerRepository).save(sampleCustomer);
    }

    @Test
    void deleteCustomer_deletesCustomerEntity() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        customerService.deleteCustomer(10L);

        verify(customerRepository).delete(sampleCustomer);
    }
}
