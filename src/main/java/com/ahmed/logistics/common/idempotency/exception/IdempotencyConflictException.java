package com.ahmed.logistics.common.idempotency.exception;

import com.ahmed.logistics.exception.ConflictException;

public class IdempotencyConflictException extends ConflictException {

    public IdempotencyConflictException(String message) {
        super(message);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
