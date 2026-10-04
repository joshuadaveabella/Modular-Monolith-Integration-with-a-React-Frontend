package edu.cit.abella.shop;

import edu.cit.abella.events.SupplierOrderDeliveredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// @Order(2): must run AFTER inventory's InventoryReplenishmentListener, so
// the stock check inside resolveBackordersForProduct sees the restocked
// number, not the pre-delivery number.
@Component
@Order(2)
class BackorderResolutionListener {

    private final OrderService orderService;

    BackorderResolutionListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @EventListener
    void onSupplierOrderDelivered(SupplierOrderDeliveredEvent event) {
        orderService.resolveBackordersForProduct(event.getProductId());
    }
}
