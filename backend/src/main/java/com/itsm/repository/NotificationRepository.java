package com.itsm.repository;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.NotificationChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdAndChannelOrderByCreatedAtDesc(Long recipientId, NotificationChannelType channel, Pageable pageable);

    long countByRecipientIdAndChannelAndReadFlagFalse(Long recipientId, NotificationChannelType channel);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findByChannelOrderByCreatedAtDesc(NotificationChannelType channel, Pageable pageable);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.recipient.id = :userId and n.readFlag = false")
    int markAllRead(@Param("userId") Long userId);
}
