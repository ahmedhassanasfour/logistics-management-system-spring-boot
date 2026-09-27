package com.ahmed.logistics.delivery.pod.repository;

import com.ahmed.logistics.delivery.pod.entity.ProofOfDelivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {

    Optional<ProofOfDelivery> findByDeliveryId(Long deliveryId);

    boolean existsByDeliveryId(Long deliveryId);

    @Query("select p from ProofOfDelivery p where p.delivery.shipment.id = :shipmentId")
    Optional<ProofOfDelivery> findByShipmentId(@Param("shipmentId") Long shipmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProofOfDelivery p where p.id = :id")
    Optional<ProofOfDelivery> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProofOfDelivery p where p.delivery.id = :deliveryId")
    Optional<ProofOfDelivery> findByDeliveryIdForUpdate(@Param("deliveryId") Long deliveryId);
}
