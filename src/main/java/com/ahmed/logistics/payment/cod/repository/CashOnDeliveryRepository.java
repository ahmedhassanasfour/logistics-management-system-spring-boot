package com.ahmed.logistics.payment.cod.repository;

import com.ahmed.logistics.payment.cod.entity.CashOnDelivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashOnDeliveryRepository extends JpaRepository<CashOnDelivery, Long> {

    Optional<CashOnDelivery> findByPaymentId(Long paymentId);

    Optional<CashOnDelivery> findByDeliveryId(Long deliveryId);

    boolean existsByPaymentId(Long paymentId);

    boolean existsByDeliveryId(Long deliveryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashOnDelivery c where c.id = :id")
    Optional<CashOnDelivery> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashOnDelivery c where c.payment.id = :paymentId")
    Optional<CashOnDelivery> findByPaymentIdForUpdate(@Param("paymentId") Long paymentId);
}
