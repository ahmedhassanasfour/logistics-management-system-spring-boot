package com.ahmed.logistics.common.idempotency.service;

import com.ahmed.logistics.common.idempotency.entity.IdempotencyRecord;
import com.ahmed.logistics.common.idempotency.entity.IdempotencyStatus;
import com.ahmed.logistics.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
public class IdempotencyService {

    private final IdempotencyRecordStorageService storageService;
    private final ObjectMapper objectMapper;

    public IdempotencyService(
            IdempotencyRecordStorageService storageService,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.storageService = storageService;
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            this.objectMapper = mapper;
        }
    }

    public <T> T execute(
            String idempotencyKey,
            String operation,
            String requestHash,
            String username,
            Class<T> responseType,
            int successStatusCode,
            Supplier<T> businessAction
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }

        String cleanKey = idempotencyKey.trim();

        // 1. Acquire PROCESSING record or retrieve existing COMPLETED record
        IdempotencyRecord record = storageService.acquireOrGetRecord(
                cleanKey,
                operation,
                requestHash,
                username
        );

        // 2. Replay stored response if already completed
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            log.info("Replaying stored response for idempotency key: '{}' and operation: '{}'", cleanKey, operation);
            return deserialize(record.getResponseBody(), responseType);
        }

        // 3. Execute the actual business operation
        T result;
        try {
            result = businessAction.get();
        } catch (Exception ex) {
            log.warn("Business operation for idempotency key '{}' failed with exception: {}", cleanKey, ex.getMessage());
            storageService.markFailed(record.getId(), ex.getMessage());
            throw ex;
        }

        // 4. Record successful execution result
        String responseJson = serialize(result);
        storageService.markCompleted(record.getId(), successStatusCode, responseJson);

        return result;
    }

    private <T> String serialize(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response", e);
            throw new RuntimeException("Failed to serialize idempotency response", e);
        }
    }

    private <T> T deserialize(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize stored idempotency response", e);
            throw new RuntimeException("Failed to deserialize stored idempotency response", e);
        }
    }
}
