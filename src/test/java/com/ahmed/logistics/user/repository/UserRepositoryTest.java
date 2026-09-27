package com.ahmed.logistics.user.repository;

import com.ahmed.logistics.user.entity.Role;
import com.ahmed.logistics.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("admin@logistics.com")
                .password("securePassword123")
                .firstName("Super")
                .lastName("Admin")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    void userEntityPersistence_shouldSaveAndGenerateIdAndCreatedAt() {
        User savedUser = userRepository.save(testUser);
        userRepository.flush();

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());
        assertEquals("admin@logistics.com", savedUser.getEmail());
        assertTrue(savedUser.isEnabled());
    }

    @Test
    void findByEmail_whenUserExists_shouldReturnUser() {
        userRepository.saveAndFlush(testUser);

        Optional<User> found = userRepository.findByEmail("admin@logistics.com");

        assertTrue(found.isPresent());
        assertEquals("admin@logistics.com", found.get().getEmail());
        assertEquals("Super", found.get().getFirstName());
        assertEquals(Role.ADMIN, found.get().getRole());
    }

    @Test
    void findByEmail_whenUserDoesNotExist_shouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("notfound@logistics.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_shouldReturnTrueWhenExistsAndFalseOtherwise() {
        userRepository.saveAndFlush(testUser);

        assertTrue(userRepository.existsByEmail("admin@logistics.com"));
        assertFalse(userRepository.existsByEmail("other@logistics.com"));
    }

    @Test
    void roleEnumPersistence_shouldStoreRoleAsStringInDatabase() {
        User savedUser = userRepository.saveAndFlush(testUser);

        String rawRole = (String) entityManager
                .createNativeQuery("SELECT role FROM users WHERE id = :id")
                .setParameter("id", savedUser.getId())
                .getSingleResult();

        assertEquals("ADMIN", rawRole, "Role must be stored as STRING and not an ordinal integer");
    }
}
