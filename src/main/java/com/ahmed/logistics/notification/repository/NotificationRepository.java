package com.ahmed.logistics.notification.repository;

import com.ahmed.logistics.notification.entity.Notification;
import com.ahmed.logistics.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdInOrderByCreatedAtDesc(Collection<Long> userIds, Pageable pageable);

    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByUserIdInAndStatusOrderByCreatedAtDesc(Collection<Long> userIds, NotificationStatus status, Pageable pageable);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    List<Notification> findByUserIdInAndStatusOrderByCreatedAtDesc(Collection<Long> userIds, NotificationStatus status);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    long countByUserIdInAndStatus(Collection<Long> userIds, NotificationStatus status);
}
