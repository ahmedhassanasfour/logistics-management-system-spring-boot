package com.ahmed.logistics.delivery.repository;

import com.ahmed.logistics.delivery.entity.Delivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByShipmentId(Long shipmentId);

    boolean existsByShipmentId(Long shipmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.shipment.id = :shipmentId")
    Optional<Delivery> findByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);
}
