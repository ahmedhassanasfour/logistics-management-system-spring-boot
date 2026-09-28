package com.ahmed.logistics.common.idempotency.repository;

import com.ahmed.logistics.common.idempotency.entity.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByOperationAndIdempotencyKey(
            String operation,
            String idempotencyKey
    );

    boolean existsByOperationAndIdempotencyKey(
            String operation,
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from IdempotencyRecord r where r.operation = :operation and r.idempotencyKey = :idempotencyKey")
    Optional<IdempotencyRecord> findByOperationAndIdempotencyKeyForUpdate(
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey
    );
}
