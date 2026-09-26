package edu.cit.abella.notification;

import edu.cit.abella.events.LowStockEvent;
import edu.cit.abella.events.OrderPlacedEvent;
import edu.cit.abella.events.OrderRejectedEvent;
import edu.cit.abella.events.SupplierOrderDeliveredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Still imports only event classes from edu.cit.abella.events - no
// reference to OrderService, InventoryService, or anything in the
// supplier package. Adding the new delivered-order listener did not
// require importing a single thing from edu.cit.abella.supplier.
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

    @EventListener
    void onSupplierOrderDelivered(SupplierOrderDeliveredEvent event) {
        String message = "Restocked " + event.getUnitsDelivered() + " unit(s) of "
                + event.getProductId() + " from supplier order " + event.getPoNumber();
        notificationRepository.save(new NotificationEntity("SUPPLIER_ORDER_DELIVERED", message));
    }
}
