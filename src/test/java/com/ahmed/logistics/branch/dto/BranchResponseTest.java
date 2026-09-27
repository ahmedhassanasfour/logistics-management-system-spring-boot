package com.ahmed.logistics.branch.dto;

import com.ahmed.logistics.branch.entity.Branch;
import com.ahmed.logistics.branch.entity.BranchStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BranchResponseTest {

    @Test
    void passwordIsNeverPresentInBranchResponse() {
        // 1. Verify no field named 'password' exists in BranchResponse
        boolean hasPasswordField = Arrays.stream(BranchResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("password"));
        assertFalse(hasPasswordField, "BranchResponse must not declare a password field");

        // 2. Verify no getter or method exposes password
        boolean hasPasswordMethod = Arrays.stream(BranchResponse.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("password"));
        assertFalse(hasPasswordMethod, "BranchResponse must not declare any method exposing password");

        // 3. Verify mapping from entity exposes all expected fields
        Branch branch = Branch.builder()
                .id(301L)
                .name("Austin Downtown Branch")
                .code("BR-ATX-001")
                .address("500 Congress Ave")
                .city("Austin")
                .postalCode("78701")
                .phone("+15125550100")
                .email("austin-branch@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();

        BranchResponse response = BranchResponse.fromEntity(branch);

        assertEquals(301L, response.id());
        assertEquals("Austin Downtown Branch", response.name());
        assertEquals("BR-ATX-001", response.code());
        assertEquals("500 Congress Ave", response.address());
        assertEquals("Austin", response.city());
        assertEquals("78701", response.postalCode());
        assertEquals("+15125550100", response.phone());
        assertEquals("austin-branch@logistics.com", response.email());
        assertEquals(BranchStatus.ACTIVE, response.status());
    }
}
