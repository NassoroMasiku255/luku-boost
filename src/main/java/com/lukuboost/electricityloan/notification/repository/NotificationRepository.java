package com.lukuboost.electricityloan.notification.repository;

import com.lukuboost.electricityloan.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStatusAndRetryCountLessThan(String status, int maxRetries);

    @Query("SELECT n FROM Notification n WHERE " +
            "(:recipient IS NULL OR n.recipient = :recipient) AND " +
            "(:channel IS NULL OR n.channel = :channel) AND " +
            "(:status IS NULL OR n.status = :status) AND " +
            "(:from IS NULL OR n.createdAt >= :from) AND " +
            "(:to IS NULL OR n.createdAt <= :to)")
    Page<Notification> searchNotifications(
            @Param("recipient") String recipient,
            @Param("channel") String channel,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
