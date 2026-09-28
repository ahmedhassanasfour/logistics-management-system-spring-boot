package com.ahmed.logistics.delivery.failure.entity;

public enum DeliveryFailureReason {
    RECIPIENT_UNAVAILABLE,
    WRONG_ADDRESS,
    RECIPIENT_REFUSED,
    PHONE_UNREACHABLE,
    VEHICLE_ISSUE,
    DRIVER_ISSUE,
    WEATHER,
    ACCESS_DENIED,
    DAMAGED_PACKAGE,
    OTHER
}
