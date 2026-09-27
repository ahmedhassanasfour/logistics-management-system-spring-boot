package com.ahmed.logistics.branch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank(message = "Branch name is required")
        @Size(max = 100, message = "Branch name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Branch code is required")
        @Size(max = 50, message = "Branch code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @NotBlank(message = "Phone number is required")
        @Size(min = 5, max = 20, message = "Phone must be between 5 and 20 characters")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email
) {}
