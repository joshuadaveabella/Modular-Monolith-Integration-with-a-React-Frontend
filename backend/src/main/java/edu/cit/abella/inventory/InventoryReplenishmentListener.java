package edu.cit.abella.inventory;

import edu.cit.abella.events.SupplierOrderDeliveredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Satisfies "Order and Inventory never call the supplier module directly
// for [delivery tracking]." The supplier module's OrderStatusPoller
// publishes SupplierOrderDeliveredEvent; this class reacts to it using
// InventoryService.restock(), which already existed from Lab 2. Inventory
// never asks the supplier module anything - it's told, after the fact,
// "this many units of this product arrived."
@Component
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
