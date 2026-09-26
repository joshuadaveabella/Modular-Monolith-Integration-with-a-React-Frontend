package edu.cit.abella.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findAllByOrderByNotificationIdDesc();
}
