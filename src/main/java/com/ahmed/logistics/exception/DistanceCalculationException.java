package com.ahmed.logistics.exception;

public class DistanceCalculationException extends RuntimeException {

    public DistanceCalculationException(String message) {
        super(message);
    }

    public DistanceCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
