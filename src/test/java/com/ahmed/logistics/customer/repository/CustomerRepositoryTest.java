package com.ahmed.logistics.customer.repository;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("cust_repo_test@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        savedUser = userRepository.saveAndFlush(user);
    }

    @Test
    void customerEntityPersistence_savesAndGeneratesId() {
        Customer customer = Customer.builder()
                .user(savedUser)
                .phone("+1234567890")
                .address("123 Main St")
                .city("New York")
                .postalCode("10001")
                .build();

        Customer saved = customerRepository.saveAndFlush(customer);

        assertNotNull(saved.getId());
        assertEquals("+1234567890", saved.getPhone());
        assertEquals("123 Main St", saved.getAddress());
        assertEquals("New York", saved.getCity());
        assertEquals("10001", saved.getPostalCode());
        assertEquals(savedUser.getId(), saved.getUser().getId());
    }

    @Test
    void customerToUser_oneToOneUniqueConstraintEnforced() {
        Customer customer1 = Customer.builder()
                .user(savedUser)
                .phone("+1234567890")
                .address("123 Main St")
                .city("New York")
                .postalCode("10001")
                .build();
        customerRepository.saveAndFlush(customer1);

        Customer customer2 = Customer.builder()
                .user(savedUser)
                .phone("+9876543210")
                .address("456 Other Ave")
                .city("New York")
                .postalCode("10002")
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            customerRepository.saveAndFlush(customer2);
        });
    }

    @Test
    void findByUserId_returnsCustomerWhenExists() {
        Customer customer = Customer.builder()
                .user(savedUser)
                .phone("+1234567890")
                .address("123 Main St")
                .city("New York")
                .postalCode("10001")
                .build();
        customerRepository.saveAndFlush(customer);

        Optional<Customer> found = customerRepository.findByUserId(savedUser.getId());

        assertTrue(found.isPresent());
        assertEquals(savedUser.getId(), found.get().getUser().getId());
        assertEquals("+1234567890", found.get().getPhone());
    }

    @Test
    void existsByUserId_returnsTrueWhenExistsAndFalseOtherwise() {
        assertFalse(customerRepository.existsByUserId(savedUser.getId()));

        Customer customer = Customer.builder()
                .user(savedUser)
                .phone("+1234567890")
                .address("123 Main St")
                .city("New York")
                .postalCode("10001")
                .build();
        customerRepository.saveAndFlush(customer);

        assertTrue(customerRepository.existsByUserId(savedUser.getId()));
    }

    @Test
    void deleteCustomer_deletesCustomerProfileWithoutDeletingUser() {
        Customer customer = Customer.builder()
                .user(savedUser)
                .phone("+1234567890")
                .address("123 Main St")
                .city("New York")
                .postalCode("10001")
                .build();
        Customer savedCustomer = customerRepository.saveAndFlush(customer);

        customerRepository.delete(savedCustomer);
        customerRepository.flush();

        // Customer profile is deleted
        assertFalse(customerRepository.findById(savedCustomer.getId()).isPresent());
        // Associated user remains intact
        assertTrue(userRepository.findById(savedUser.getId()).isPresent());
    }
}
