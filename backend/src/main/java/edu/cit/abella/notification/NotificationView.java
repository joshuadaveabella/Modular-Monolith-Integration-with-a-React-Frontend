package edu.cit.abella.notification;

import java.time.LocalDateTime;

public class NotificationView {

    private final Long notificationId;
    private final String type;
    private final String message;
    private final LocalDateTime createdAt;

    public NotificationView(Long notificationId, String type, String message, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
