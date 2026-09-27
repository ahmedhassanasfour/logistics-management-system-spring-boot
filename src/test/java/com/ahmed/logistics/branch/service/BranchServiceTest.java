package com.ahmed.logistics.branch.service;

import com.ahmed.logistics.branch.dto.CreateBranchRequest;
import com.ahmed.logistics.branch.dto.BranchResponse;
import com.ahmed.logistics.branch.dto.UpdateBranchRequest;
import com.ahmed.logistics.branch.entity.Branch;
import com.ahmed.logistics.branch.entity.BranchStatus;
import com.ahmed.logistics.branch.repository.BranchRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
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
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchService branchService;

    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        sampleBranch = Branch.builder()
                .id(1L)
                .name("Austin Central Branch")
                .code("BR-ATX-001")
                .address("100 Congress Ave")
                .city("Austin")
                .postalCode("78701")
                .phone("+15125550100")
                .email("austin@logistics.com")
                .status(BranchStatus.ACTIVE)
                .build();
    }

    @Test
    void createBranch_validCreationSucceedsWithInitialStatusActive() {
        CreateBranchRequest request = new CreateBranchRequest(
                "Austin Central Branch",
                "BR-ATX-001",
                "100 Congress Ave",
                "Austin",
                "78701",
                "+15125550100",
                "austin@logistics.com"
        );

        when(branchRepository.existsByCode("BR-ATX-001")).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);

        BranchResponse response = branchService.createBranch(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Austin Central Branch", response.name());
        assertEquals("BR-ATX-001", response.code());
        assertEquals("100 Congress Ave", response.address());
        assertEquals("Austin", response.city());
        assertEquals("78701", response.postalCode());
        assertEquals("+15125550100", response.phone());
        assertEquals("austin@logistics.com", response.email());
        assertEquals(BranchStatus.ACTIVE, response.status());

        verify(branchRepository, times(1)).save(any(Branch.class));
    }

    @Test
    void createBranch_duplicateCodeRejected() {
        CreateBranchRequest request = new CreateBranchRequest(
                "Austin Central Branch",
                "BR-ATX-001",
                "100 Congress Ave",
                "Austin",
                "78701",
                "+15125550100",
                "austin@logistics.com"
        );

        when(branchRepository.existsByCode("BR-ATX-001")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> branchService.createBranch(request)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(branchRepository, never()).save(any());
    }

    @Test
    void getBranchById_returnsBranchWhenFound() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));

        BranchResponse response = branchService.getBranchById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Austin Central Branch", response.name());
        assertEquals("BR-ATX-001", response.code());
        verify(branchRepository).findById(1L);
    }

    @Test
    void getBranchById_throwsResourceNotFoundExceptionWhenMissing() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> branchService.getBranchById(99L)
        );
        verify(branchRepository).findById(99L);
    }

    @Test
    void getBranchByCode_returnsBranchWhenFound() {
        when(branchRepository.findByCode("BR-ATX-001")).thenReturn(Optional.of(sampleBranch));

        BranchResponse response = branchService.getBranchByCode("BR-ATX-001");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("BR-ATX-001", response.code());
        verify(branchRepository).findByCode("BR-ATX-001");
    }

    @Test
    void getBranchByCode_throwsResourceNotFoundExceptionWhenMissing() {
        when(branchRepository.findByCode("NON-EXISTENT")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> branchService.getBranchByCode("NON-EXISTENT")
        );
        verify(branchRepository).findByCode("NON-EXISTENT");
    }

    @Test
    void updateBranch_updatesProfileFieldsWithoutChangingStatus() {
        sampleBranch.setStatus(BranchStatus.MAINTENANCE);

        UpdateBranchRequest updateRequest = new UpdateBranchRequest(
                "Austin South Branch",
                "BR-ATX-002",
                "200 South Congress Ave",
                "Austin",
                "78704",
                "+15125550200",
                "austin-south@logistics.com"
        );

        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));
        when(branchRepository.existsByCode("BR-ATX-002")).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchService.updateBranch(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Austin South Branch", response.name());
        assertEquals("BR-ATX-002", response.code());
        assertEquals("200 South Congress Ave", response.address());
        assertEquals("Austin", response.city());
        assertEquals("78704", response.postalCode());
        assertEquals("+15125550200", response.phone());
        assertEquals("austin-south@logistics.com", response.email());
        // Status remains unchanged
        assertEquals(BranchStatus.MAINTENANCE, response.status());

        verify(branchRepository).save(sampleBranch);
    }

    @Test
    void updateBranch_sameCodeAllowedWithoutDuplicateError() {
        UpdateBranchRequest updateRequest = new UpdateBranchRequest(
                "Austin Central Branch Updated",
                "BR-ATX-001", // same code
                "100 Congress Ave Suite 500",
                "Austin",
                "78701",
                "+15125550100",
                "austin@logistics.com"
        );

        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchService.updateBranch(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Austin Central Branch Updated", response.name());
        assertEquals("BR-ATX-001", response.code());
        verify(branchRepository, never()).existsByCode(any());
        verify(branchRepository).save(sampleBranch);
    }

    @Test
    void updateBranch_duplicateCodeRejected() {
        UpdateBranchRequest updateRequest = new UpdateBranchRequest(
                "Branch Name",
                "BR-TAKEN-001",
                "Address",
                "City",
                "12345",
                "+1234567890",
                "branch@logistics.com"
        );

        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));
        when(branchRepository.existsByCode("BR-TAKEN-001")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> branchService.updateBranch(1L, updateRequest)
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(branchRepository, never()).save(any());
    }

    @Test
    void updateBranch_throwsResourceNotFoundExceptionWhenMissing() {
        UpdateBranchRequest updateRequest = new UpdateBranchRequest(
                "Name",
                "BR-001",
                "Address",
                "City",
                "12345",
                "+1234567890",
                "test@logistics.com"
        );

        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> branchService.updateBranch(99L, updateRequest)
        );
        verify(branchRepository, never()).save(any());
    }

    @Test
    void deleteBranch_deletesBranchEntity() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));

        branchService.deleteBranch(1L);

        verify(branchRepository).delete(sampleBranch);
    }

    @Test
    void deleteBranch_throwsResourceNotFoundExceptionWhenMissing() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> branchService.deleteBranch(99L)
        );
        verify(branchRepository, never()).delete(any());
    }
}
