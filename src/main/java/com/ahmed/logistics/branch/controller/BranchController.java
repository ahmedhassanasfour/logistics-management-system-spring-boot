package com.ahmed.logistics.branch.controller;

import com.ahmed.logistics.branch.dto.CreateBranchRequest;
import com.ahmed.logistics.branch.dto.BranchResponse;
import com.ahmed.logistics.branch.dto.UpdateBranchRequest;
import com.ahmed.logistics.branch.service.BranchService;
import com.ahmed.logistics.exception.ForbiddenException;
import com.ahmed.logistics.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            Authentication authentication
    ) {
        validateAdmin(authentication);
        BranchResponse response = branchService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchById(@PathVariable Long id) {
        BranchResponse response = branchService.getBranchById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<BranchResponse> getBranchByCode(@PathVariable String code) {
        BranchResponse response = branchService.getBranchByCode(code);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBranchRequest request,
            Authentication authentication
    ) {
        validateAdmin(authentication);
        BranchResponse updated = branchService.updateBranch(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable Long id,
            Authentication authentication
    ) {
        validateAdmin(authentication);
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }

    private void validateAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ForbiddenException("Only administrators have permission to perform this operation");
        }
    }
}
