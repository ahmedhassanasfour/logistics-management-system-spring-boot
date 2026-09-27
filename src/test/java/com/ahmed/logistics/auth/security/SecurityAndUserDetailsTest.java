package com.ahmed.logistics.auth.security;

import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndUserDetailsTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void passwordEncoder_hashesPasswordCorrectly() {
        String rawPassword = "mySecurePassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("wrongPassword", encoded));
    }

    @Test
    void customUserDetails_mapsEmailPasswordAndRoleCorrectly() {
        User user = User.builder()
                .id(10L)
                .email("driver@logistics.com")
                .password("$2a$10$hashedPasswordHere")
                .firstName("John")
                .lastName("Doe")
                .role(Role.DRIVER)
                .enabled(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertEquals("driver@logistics.com", userDetails.getUsername());
        assertEquals("$2a$10$hashedPasswordHere", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER")));
    }

    @Test
    void customUserDetails_reflectsDisabledState() {
        User disabledUser = User.builder()
                .email("inactive@logistics.com")
                .password("hash")
                .firstName("Inactive")
                .lastName("User")
                .role(Role.CUSTOMER)
                .enabled(false)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(disabledUser);

        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }
}
