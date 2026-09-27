package com.ahmed.logistics.branch.dto;

import com.ahmed.logistics.branch.entity.Branch;
import com.ahmed.logistics.branch.entity.BranchStatus;

public record BranchResponse(
        Long id,
        String name,
        String code,
        String address,
        String city,
        String postalCode,
        String phone,
        String email,
        BranchStatus status
) {
    public static BranchResponse fromEntity(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getCode(),
                branch.getAddress(),
                branch.getCity(),
                branch.getPostalCode(),
                branch.getPhone(),
                branch.getEmail(),
                branch.getStatus()
        );
    }
}
