package com.ahmed.logistics.delivery.reschedule.repository;

import com.ahmed.logistics.delivery.reschedule.entity.DeliveryReschedule;
import com.ahmed.logistics.delivery.reschedule.entity.RescheduleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRescheduleRepository extends JpaRepository<DeliveryReschedule, Long> {

    List<DeliveryReschedule> findByShipmentIdOrderByScheduledAtDesc(Long shipmentId);

    Optional<DeliveryReschedule> findByFailedDeliveryId(Long deliveryId);

    boolean existsByFailedDeliveryId(Long deliveryId);

    Optional<DeliveryReschedule> findFirstByShipmentIdAndStatus(Long shipmentId, RescheduleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from DeliveryReschedule r where r.id = :id")
    Optional<DeliveryReschedule> findByIdForUpdate(@Param("id") Long id);
}
