package edu.cit.abella.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<NotificationView>> getNotifications() {
        List<NotificationView> views = notificationRepository.findAllByOrderByNotificationIdDesc().stream()
                .map(n -> new NotificationView(n.getNotificationId(), n.getType(), n.getMessage(), n.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(views);
    }
}
