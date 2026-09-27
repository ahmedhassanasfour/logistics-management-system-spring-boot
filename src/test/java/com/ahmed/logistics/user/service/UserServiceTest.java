package com.ahmed.logistics.user.service;

import com.ahmed.logistics.exception.ResourceNotFoundException;
import com.ahmed.logistics.user.dto.UserResponse;
import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("driver@logistics.com")
                .password("encoded_pass")
                .firstName("John")
                .lastName("Doe")
                .role(Role.DRIVER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findByEmail_whenUserExists_shouldReturnUserResponse() {
        when(userRepository.findByEmail("driver@logistics.com")).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.findByEmail("driver@logistics.com");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("driver@logistics.com", response.email());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals(Role.DRIVER, response.role());
        assertTrue(response.enabled());
        assertNotNull(response.createdAt());

        verify(userRepository, times(1)).findByEmail("driver@logistics.com");
    }

    @Test
    void findByEmail_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
        when(userRepository.findByEmail("missing@logistics.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findByEmail("missing@logistics.com")
        );

        assertTrue(exception.getMessage().contains("missing@logistics.com"));
        verify(userRepository, times(1)).findByEmail("missing@logistics.com");
    }

    @Test
    void findById_whenUserExists_shouldReturnUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.findById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("driver@logistics.com", response.email());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void findById_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findById(99L)
        );

        assertTrue(exception.getMessage().contains("99"));
        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void existsByEmail_shouldReturnRepositoryResult() {
        when(userRepository.existsByEmail("driver@logistics.com")).thenReturn(true);
        when(userRepository.existsByEmail("other@logistics.com")).thenReturn(false);

        assertTrue(userService.existsByEmail("driver@logistics.com"));
        assertFalse(userService.existsByEmail("other@logistics.com"));

        verify(userRepository, times(1)).existsByEmail("driver@logistics.com");
        verify(userRepository, times(1)).existsByEmail("other@logistics.com");
    }
}
