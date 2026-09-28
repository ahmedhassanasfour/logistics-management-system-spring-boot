package com.ahmed.logistics.common.idempotency.service;

import com.ahmed.logistics.common.idempotency.entity.IdempotencyRecord;
import com.ahmed.logistics.common.idempotency.entity.IdempotencyStatus;
import com.ahmed.logistics.common.idempotency.exception.IdempotencyConflictException;
import com.ahmed.logistics.common.idempotency.repository.IdempotencyRecordRepository;
import com.ahmed.logistics.exception.BadRequestException;
import com.ahmed.logistics.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyRecordStorageService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public static final long EXPIRATION_HOURS = 24L;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord acquireOrGetRecord(
            String idempotencyKey,
            String operation,
            String requestHash,
            String username
    ) {
        Optional<IdempotencyRecord> existingOpt = idempotencyRecordRepository
                .findByOperationAndIdempotencyKey(operation, idempotencyKey);

        if (existingOpt.isPresent()) {
            IdempotencyRecord existing = existingOpt.get();

            // Check if record is expired
            if (existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.info("Idempotency record for key {} and operation {} is expired. Re-creating.", idempotencyKey, operation);
                idempotencyRecordRepository.delete(existing);
                idempotencyRecordRepository.flush();
            } else {
                // Validate user identity (prevent cross-user key sharing)
                if (existing.getUsername() != null && username != null
                        && !existing.getUsername().equalsIgnoreCase(username)) {
                    log.warn("User {} attempted to use idempotency key {} owned by {}",
                            username, idempotencyKey, existing.getUsername());
                    throw new ForbiddenException("Idempotency key belongs to another user");
                }

                // Validate request payload hash
                if (!existing.getRequestHash().equals(requestHash)) {
                    log.warn("Payload mismatch for idempotency key: {} and operation: {}", idempotencyKey, operation);
                    throw new BadRequestException("Idempotency key was already used with a different request payload");
                }

                if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                    return existing;
                }

                if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                    log.warn("Concurrent operation in progress for idempotency key: {} and operation: {}", idempotencyKey, operation);
                    throw new IdempotencyConflictException("A request with this idempotency key is currently being processed");
                }

                if (existing.getStatus() == IdempotencyStatus.FAILED) {
                    // Retry allowed for previously failed operation
                    log.info("Retrying previously failed operation for idempotency key: {}", idempotencyKey);
                    existing.setStatus(IdempotencyStatus.PROCESSING);
                    existing.setRequestHash(requestHash);
                    existing.setResponseBody(null);
                    existing.setResponseStatus(null);
                    existing.setCreatedAt(LocalDateTime.now());
                    existing.setExpiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS));
                    return idempotencyRecordRepository.save(existing);
                }
            }
        }

        // Create new PROCESSING record
        IdempotencyRecord newRecord = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .operation(operation)
                .requestHash(requestHash)
                .username(username)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS))
                .build();

        try {
            return idempotencyRecordRepository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException ex) {
            log.info("Caught duplicate key collision for operation: {} and key: {}. Re-evaluating existing record.", operation, idempotencyKey);
            IdempotencyRecord concurrent = idempotencyRecordRepository
                    .findByOperationAndIdempotencyKey(operation, idempotencyKey)
                    .orElseThrow(() -> ex);

            if (concurrent.getUsername() != null && username != null
                    && !concurrent.getUsername().equalsIgnoreCase(username)) {
                throw new ForbiddenException("Idempotency key belongs to another user");
            }
            if (!concurrent.getRequestHash().equals(requestHash)) {
                throw new BadRequestException("Idempotency key was already used with a different request payload");
            }
            if (concurrent.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new IdempotencyConflictException("A request with this idempotency key is currently being processed");
            }
            return concurrent;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(Long recordId, int responseStatus, String responseBody) {
        idempotencyRecordRepository.findById(recordId).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseStatus(responseStatus);
            record.setResponseBody(responseBody);
            idempotencyRecordRepository.save(record);
            log.debug("Marked idempotency record ID: {} as COMPLETED", recordId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long recordId, String errorMessage) {
        idempotencyRecordRepository.findById(recordId).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseBody(errorMessage);
            idempotencyRecordRepository.save(record);
            log.debug("Marked idempotency record ID: {} as FAILED", recordId);
        });
    }
}
