package com.ahmed.logistics.branch.service;

import com.ahmed.logistics.branch.dto.CreateBranchRequest;
import com.ahmed.logistics.branch.dto.BranchResponse;
import com.ahmed.logistics.branch.dto.UpdateBranchRequest;
import com.ahmed.logistics.branch.entity.Branch;
import com.ahmed.logistics.branch.entity.BranchStatus;
import com.ahmed.logistics.branch.repository.BranchRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {
        String code = request.code().trim();
        if (branchRepository.existsByCode(code)) {
            throw new BadRequestException("Branch with code already exists: " + code);
        }

        Branch branch = Branch.builder()
                .name(request.name().trim())
                .code(code)
                .address(request.address().trim())
                .city(request.city().trim())
                .postalCode(request.postalCode().trim())
                .phone(request.phone().trim())
                .email(request.email().trim().toLowerCase())
                .status(BranchStatus.ACTIVE)
                .build();

        Branch saved = branchRepository.save(branch);
        return BranchResponse.fromEntity(saved);
    }

    public BranchResponse getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        return BranchResponse.fromEntity(branch);
    }

    public BranchResponse getBranchByCode(String code) {
        Branch branch = branchRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with code: " + code.trim()));
        return BranchResponse.fromEntity(branch);
    }

    @Transactional
    public BranchResponse updateBranch(Long id, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));

        String newCode = request.code().trim();
        if (!branch.getCode().equalsIgnoreCase(newCode)) {
            if (branchRepository.existsByCode(newCode)) {
                throw new BadRequestException("Branch with code already exists: " + newCode);
            }
        }

        branch.setName(request.name().trim());
        branch.setCode(newCode);
        branch.setAddress(request.address().trim());
        branch.setCity(request.city().trim());
        branch.setPostalCode(request.postalCode().trim());
        branch.setPhone(request.phone().trim());
        branch.setEmail(request.email().trim().toLowerCase());
        // Status remains unchanged

        Branch updated = branchRepository.save(branch);
        return BranchResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));

        branchRepository.delete(branch);
    }
}
