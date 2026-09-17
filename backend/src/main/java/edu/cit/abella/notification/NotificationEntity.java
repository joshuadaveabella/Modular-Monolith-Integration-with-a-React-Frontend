package edu.cit.abella.notification;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    // ORDER_CONFIRMED | ORDER_REJECTED | LOW_STOCK
    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NotificationEntity() {
        // required by JPA
    }

    NotificationEntity(String type, String message) {
        this.type = type;
        this.message = message;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    Long getNotificationId() {
        return notificationId;
    }

    String getType() {
        return type;
    }

    String getMessage() {
        return message;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
