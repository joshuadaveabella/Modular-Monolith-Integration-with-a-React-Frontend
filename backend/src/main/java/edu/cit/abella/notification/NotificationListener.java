package edu.cit.abella.notification;

import edu.cit.abella.events.LowStockEvent;
import edu.cit.abella.events.OrderPlacedEvent;
import edu.cit.abella.events.OrderRejectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// This class is the ONLY thing in the application that reacts to order
// outcomes for notification purposes. Note what it imports: the three event
// classes from edu.cit.abella.events, and nothing else. It holds no
// reference to OrderService or InventoryService, and neither of those
// imports anything from this package. The coupling runs through the event
// types alone.
@Component
class NotificationListener {

    private final NotificationRepository notificationRepository;

    NotificationListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @EventListener
    void onOrderPlaced(OrderPlacedEvent event) {
        String message = "Order " + event.getOrderId() + " confirmed - "
                + event.getProductIds().size() + " product(s), "
                + event.getTotalQuantity() + " item(s) total";
        notificationRepository.save(new NotificationEntity("ORDER_CONFIRMED", message));
    }

    @EventListener
    void onOrderRejected(OrderRejectedEvent event) {
        String label = event.getOrderId() == null ? "(unsaved)" : String.valueOf(event.getOrderId());
        String message = "Order " + label + " rejected - " + event.getReason();
        notificationRepository.save(new NotificationEntity("ORDER_REJECTED", message));
    }

    @EventListener
    void onLowStock(LowStockEvent event) {
        String message = "Reorder needed - " + event.getProductName()
                + " (" + event.getProductId() + ") down to " + event.getRemainingStock()
                + ", below threshold of " + event.getThreshold();
        notificationRepository.save(new NotificationEntity("LOW_STOCK", message));
    }
}
