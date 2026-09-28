package com.ahmed.logistics.delivery.failure.repository;

import com.ahmed.logistics.delivery.failure.entity.DeliveryFailure;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryFailureRepository extends JpaRepository<DeliveryFailure, Long> {

    List<DeliveryFailure> findByDeliveryIdOrderByFailedAtDesc(Long deliveryId);

    @Query("select f from DeliveryFailure f where f.delivery.shipment.id = :shipmentId order by f.failedAt desc")
    List<DeliveryFailure> findByShipmentIdOrderByFailedAtDesc(@Param("shipmentId") Long shipmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from DeliveryFailure f where f.id = :id")
    Optional<DeliveryFailure> findByIdForUpdate(@Param("id") Long id);
}
