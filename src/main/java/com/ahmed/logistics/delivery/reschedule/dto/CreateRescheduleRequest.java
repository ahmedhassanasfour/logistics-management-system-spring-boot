package com.ahmed.logistics.delivery.reschedule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateRescheduleRequest(
        @NotNull(message = "Scheduled time is required")
        @Future(message = "Scheduled time must be in the future")
        LocalDateTime scheduledAt,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
