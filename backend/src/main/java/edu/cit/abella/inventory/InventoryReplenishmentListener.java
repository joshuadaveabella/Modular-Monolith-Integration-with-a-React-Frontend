package edu.cit.abella.inventory;

import edu.cit.abella.events.SupplierOrderDeliveredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// @Order(1): this MUST run before shop's BackorderResolutionListener, which
// reacts to the same event and checks current stock to decide whether a
// backordered order can now be filled. If that check ran first, it would
// see stale (pre-delivery) stock and wrongly cancel orders that should
// have been confirmed.
@Component
@Order(1)
class InventoryReplenishmentListener {

    private final InventoryService inventoryService;

    InventoryReplenishmentListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventListener
    void onSupplierOrderDelivered(SupplierOrderDeliveredEvent event) {
        inventoryService.restock(event.getProductId(), event.getUnitsDelivered());
    }
}
