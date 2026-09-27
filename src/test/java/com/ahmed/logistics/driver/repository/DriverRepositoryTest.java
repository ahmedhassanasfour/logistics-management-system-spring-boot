package com.ahmed.logistics.driver.repository;

import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User savedUser1;
    private User savedUser2;

    @BeforeEach
    void setUp() {
        User user1 = User.builder()
                .email("driver_repo_test1@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("John")
                .lastName("Driver")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        savedUser1 = userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .email("driver_repo_test2@logistics.com")
                .password("$2a$10$hashedPass")
                .firstName("Jane")
                .lastName("Driver")
                .role(Role.DRIVER)
                .enabled(true)
                .build();
        savedUser2 = userRepository.saveAndFlush(user2);
    }

    @Test
    void driverEntityPersistence_savesAndGeneratesId() {
        Driver driver = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-12345")
                .licenseExpiryDate(LocalDate.now().plusYears(3))
                .status(DriverStatus.OFFLINE)
                .build();

        Driver saved = driverRepository.saveAndFlush(driver);

        assertNotNull(saved.getId());
        assertEquals("+1234567890", saved.getPhone());
        assertEquals("LIC-12345", saved.getLicenseNumber());
        assertEquals(DriverStatus.OFFLINE, saved.getStatus());
        assertEquals(savedUser1.getId(), saved.getUser().getId());
    }

    @Test
    void driverToUser_oneToOneUniqueConstraintEnforced() {
        Driver driver1 = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-11111")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build();
        driverRepository.saveAndFlush(driver1);

        Driver driver2 = Driver.builder()
                .user(savedUser1)
                .phone("+9876543210")
                .licenseNumber("LIC-22222")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            driverRepository.saveAndFlush(driver2);
        });
    }

    @Test
    void driverLicenseNumber_uniqueConstraintEnforced() {
        Driver driver1 = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("DUPLICATE-LIC")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build();
        driverRepository.saveAndFlush(driver1);

        Driver driver2 = Driver.builder()
                .user(savedUser2)
                .phone("+9876543210")
                .licenseNumber("DUPLICATE-LIC")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            driverRepository.saveAndFlush(driver2);
        });
    }

    @Test
    void findByUserId_returnsDriverWhenExists() {
        Driver driver = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-FIND")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .status(DriverStatus.OFFLINE)
                .build();
        driverRepository.saveAndFlush(driver);

        Optional<Driver> found = driverRepository.findByUserId(savedUser1.getId());

        assertTrue(found.isPresent());
        assertEquals(savedUser1.getId(), found.get().getUser().getId());
        assertEquals("LIC-FIND", found.get().getLicenseNumber());
    }

    @Test
    void findByUserId_returnsEmptyWhenNotFound() {
        Optional<Driver> found = driverRepository.findByUserId(9999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByUserId_returnsTrueWhenExistsAndFalseOtherwise() {
        assertFalse(driverRepository.existsByUserId(savedUser1.getId()));

        Driver driver = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-EXISTS-USER")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .status(DriverStatus.OFFLINE)
                .build();
        driverRepository.saveAndFlush(driver);

        assertTrue(driverRepository.existsByUserId(savedUser1.getId()));
    }

    @Test
    void existsByLicenseNumber_returnsTrueWhenExistsAndFalseOtherwise() {
        assertFalse(driverRepository.existsByLicenseNumber("LIC-EXISTS-TEST"));

        Driver driver = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-EXISTS-TEST")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .status(DriverStatus.OFFLINE)
                .build();
        driverRepository.saveAndFlush(driver);

        assertTrue(driverRepository.existsByLicenseNumber("LIC-EXISTS-TEST"));
    }

    @Test
    void deleteDriver_deletesDriverProfileWithoutDeletingUser() {
        Driver driver = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-DELETE")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .status(DriverStatus.OFFLINE)
                .build();
        Driver savedDriver = driverRepository.saveAndFlush(driver);

        driverRepository.delete(savedDriver);
        driverRepository.flush();

        // Driver profile is deleted
        assertFalse(driverRepository.findById(savedDriver.getId()).isPresent());
        // Associated user remains intact
        assertTrue(userRepository.findById(savedUser1.getId()).isPresent());
    }

    @Test
    void driverStatus_persistedAsStringInDatabase() {
        Driver driver = Driver.builder()
                .user(savedUser1)
                .phone("+1234567890")
                .licenseNumber("LIC-STATUS-TEST")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .status(DriverStatus.OFFLINE)
                .build();
        Driver saved = driverRepository.saveAndFlush(driver);

        // Native query to inspect the raw column value in drivers table
        Object rawStatus = entityManager.createNativeQuery(
                "SELECT status FROM drivers WHERE id = :id"
        ).setParameter("id", saved.getId()).getSingleResult();

        assertEquals("OFFLINE", rawStatus.toString());
    }
}
