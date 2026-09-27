package com.ahmed.logistics.driver.service;

import com.ahmed.logistics.driver.dto.CreateDriverRequest;
import com.ahmed.logistics.driver.dto.DriverResponse;
import com.ahmed.logistics.driver.dto.UpdateDriverRequest;
import com.ahmed.logistics.driver.entity.Driver;
import com.ahmed.logistics.driver.entity.DriverStatus;
import com.ahmed.logistics.driver.repository.DriverRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private DriverService driverService;

    private User driverUser;
    private User adminUser;
    private Driver sampleDriver;

    @BeforeEach
    void setUp() {
        driverUser = User.builder()
                .id(1L)
                .email("driver@logistics.com")
                .firstName("Michael")
                .lastName("Schumacher")
                .role(Role.DRIVER)
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

        sampleDriver = Driver.builder()
                .id(10L)
                .user(driverUser)
                .phone("+123456789")
                .licenseNumber("DL-ABC-1234")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .status(DriverStatus.OFFLINE)
                .build();
    }

    @Test
    void createDriver_successfullyCreatesProfile() {
        CreateDriverRequest request = new CreateDriverRequest(
                1L,
                "+123456789",
                "DL-ABC-1234",
                LocalDate.now().plusYears(2)
        );

        when(userService.findEntityById(1L)).thenReturn(driverUser);
        when(driverRepository.existsByUserId(1L)).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-ABC-1234")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(sampleDriver);

        DriverResponse response = driverService.createDriver(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("driver@logistics.com", response.email());
        assertEquals("Michael", response.firstName());
        assertEquals("Schumacher", response.lastName());
        assertEquals("+123456789", response.phone());
        assertEquals("DL-ABC-1234", response.licenseNumber());
        assertEquals(DriverStatus.OFFLINE, response.status());

        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void createDriver_nonDriverRoleRejected() {
        CreateDriverRequest request = new CreateDriverRequest(
                2L,
                "+123456789",
                "DL-ABC-1234",
                LocalDate.now().plusYears(2)
        );

        when(userService.findEntityById(2L)).thenReturn(adminUser);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> driverService.createDriver(2L, request)
        );

        assertTrue(ex.getMessage().contains("DRIVER role"));
        verify(driverRepository, never()).save(any());
    }

    @Test
    void createDriver_duplicateProfileRejected() {
        CreateDriverRequest request = new CreateDriverRequest(
                1L,
                "+123456789",
                "DL-ABC-1234",
                LocalDate.now().plusYears(2)
        );

        when(userService.findEntityById(1L)).thenReturn(driverUser);
        when(driverRepository.existsByUserId(1L)).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> driverService.createDriver(1L, request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(driverRepository, never()).save(any());
    }

    @Test
    void createDriver_duplicateLicenseNumberRejected() {
        CreateDriverRequest request = new CreateDriverRequest(
                1L,
                "+123456789",
                "DL-ABC-1234",
                LocalDate.now().plusYears(2)
        );

        when(userService.findEntityById(1L)).thenReturn(driverUser);
        when(driverRepository.existsByUserId(1L)).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-ABC-1234")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> driverService.createDriver(1L, request)
        );

        assertTrue(ex.getMessage().contains("License number already exists"));
        verify(driverRepository, never()).save(any());
    }

    @Test
    void getDriverById_returnsDriverWhenFound() {
        when(driverRepository.findById(10L)).thenReturn(Optional.of(sampleDriver));

        DriverResponse response = driverService.getDriverById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("driver@logistics.com", response.email());
        assertEquals(DriverStatus.OFFLINE, response.status());
        verify(driverRepository).findById(10L);
    }

    @Test
    void getDriverById_throwsResourceNotFoundExceptionWhenMissing() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.getDriverById(99L)
        );
        verify(driverRepository).findById(99L);
    }

    @Test
    void getDriverByUserId_returnsDriverWhenFound() {
        when(driverRepository.findByUserId(1L)).thenReturn(Optional.of(sampleDriver));

        DriverResponse response = driverService.getDriverByUserId(1L);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals("driver@logistics.com", response.email());
        verify(driverRepository).findByUserId(1L);
    }

    @Test
    void getDriverByUserId_throwsResourceNotFoundExceptionWhenMissing() {
        when(driverRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.getDriverByUserId(99L)
        );
        verify(driverRepository).findByUserId(99L);
    }

    @Test
    void updateDriver_updatesOnlyProfileFields() {
        LocalDate newExpiry = LocalDate.now().plusYears(5);
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+999999999",
                "DL-NEW-5678",
                newExpiry
        );

        when(driverRepository.findById(10L)).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.existsByLicenseNumber("DL-NEW-5678")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.updateDriver(10L, updateRequest);

        assertNotNull(response);
        assertEquals("+999999999", response.phone());
        assertEquals("DL-NEW-5678", response.licenseNumber());
        assertEquals(newExpiry, response.licenseExpiryDate());
        // Status remains unchanged
        assertEquals(DriverStatus.OFFLINE, response.status());
        // User identity remains untouched
        assertEquals(1L, response.userId());
        assertEquals("driver@logistics.com", response.email());

        verify(driverRepository).save(sampleDriver);
    }

    @Test
    void updateDriver_sameLicenseNumberAllowed() {
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+999999999",
                "DL-ABC-1234", // same license number
                LocalDate.now().plusYears(3)
        );

        when(driverRepository.findById(10L)).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.updateDriver(10L, updateRequest);

        assertNotNull(response);
        assertEquals("+999999999", response.phone());
        assertEquals("DL-ABC-1234", response.licenseNumber());
        verify(driverRepository, never()).existsByLicenseNumber(any());
        verify(driverRepository).save(sampleDriver);
    }

    @Test
    void updateDriver_duplicateLicenseNumberRejected() {
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+999999999",
                "DL-OTHER-TAKEN",
                LocalDate.now().plusYears(3)
        );

        when(driverRepository.findById(10L)).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.existsByLicenseNumber("DL-OTHER-TAKEN")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> driverService.updateDriver(10L, updateRequest)
        );

        assertTrue(ex.getMessage().contains("License number already exists"));
        verify(driverRepository, never()).save(any());
    }

    @Test
    void updateDriver_statusRemainsUnchanged() {
        sampleDriver.setStatus(DriverStatus.BUSY);

        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+888888888",
                "DL-ABC-1234",
                LocalDate.now().plusYears(2)
        );

        when(driverRepository.findById(10L)).thenReturn(Optional.of(sampleDriver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.updateDriver(10L, updateRequest);

        assertEquals(DriverStatus.BUSY, response.status());
    }

    @Test
    void updateDriver_throwsResourceNotFoundExceptionWhenMissing() {
        UpdateDriverRequest updateRequest = new UpdateDriverRequest(
                "+999999999",
                "DL-NEW-5678",
                LocalDate.now().plusYears(3)
        );

        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.updateDriver(99L, updateRequest)
        );
        verify(driverRepository, never()).save(any());
    }

    @Test
    void deleteDriver_deletesDriverEntity() {
        when(driverRepository.findById(10L)).thenReturn(Optional.of(sampleDriver));

        driverService.deleteDriver(10L);

        verify(driverRepository).delete(sampleDriver);
    }

    @Test
    void deleteDriver_throwsResourceNotFoundExceptionWhenMissing() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> driverService.deleteDriver(99L)
        );
        verify(driverRepository, never()).delete(any());
    }
}
