package com.ahmed.logistics.branch.controller;

import com.ahmed.logistics.branch.dto.BranchResponse;
import com.ahmed.logistics.branch.dto.CreateBranchRequest;
import com.ahmed.logistics.branch.dto.UpdateBranchRequest;
import com.ahmed.logistics.branch.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody CreateBranchRequest request
    ) {
        BranchResponse response = branchService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchById(@PathVariable Long id) {
        BranchResponse response = branchService.getBranchById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')")
    @GetMapping("/code/{code}")
    public ResponseEntity<BranchResponse> getBranchByCode(@PathVariable String code) {
        BranchResponse response = branchService.getBranchByCode(code);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBranchRequest request
    ) {
        BranchResponse updated = branchService.updateBranch(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
