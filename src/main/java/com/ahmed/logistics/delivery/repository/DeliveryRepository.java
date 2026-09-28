package com.ahmed.logistics.delivery.repository;

import com.ahmed.logistics.delivery.entity.Delivery;
import com.ahmed.logistics.delivery.entity.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByShipmentIdOrderByCreatedAtDesc(Long shipmentId);

    Optional<Delivery> findTopByShipmentIdOrderByCreatedAtDesc(Long shipmentId);

    Optional<Delivery> findFirstByShipmentIdAndStatusIn(Long shipmentId, Collection<DeliveryStatus> statuses);

    boolean existsByShipmentIdAndStatusIn(Long shipmentId, Collection<DeliveryStatus> statuses);

    boolean existsByShipmentId(Long shipmentId);

    default Optional<Delivery> findByShipmentId(Long shipmentId) {
        return findTopByShipmentIdOrderByCreatedAtDesc(shipmentId);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.shipment.id = :shipmentId and d.status in :statuses")
    Optional<Delivery> findActiveDeliveryForUpdate(@Param("shipmentId") Long shipmentId, @Param("statuses") Collection<DeliveryStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.shipment.id = :shipmentId order by d.createdAt desc")
    List<Delivery> findByShipmentIdForUpdateList(@Param("shipmentId") Long shipmentId);

    default Optional<Delivery> findByShipmentIdForUpdate(Long shipmentId) {
        return findByShipmentIdForUpdateList(shipmentId).stream().findFirst();
    }
}

