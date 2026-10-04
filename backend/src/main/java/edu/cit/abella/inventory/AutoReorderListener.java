package edu.cit.abella.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.cit.abella.events.LowStockEvent;
import edu.cit.abella.supplier.SupplierGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
class AutoReorderListener {
    private static final Logger log = LoggerFactory.getLogger(AutoReorderListener.class);
    private final SupplierGateway supplierGateway;

    @Value("${inventory.reorder-target-multiplier:3}")
    private int reorderTargetMultiplier;

    AutoReorderListener(SupplierGateway supplierGateway) {
        this.supplierGateway = supplierGateway;
    }

    @EventListener
    void onLowStock(LowStockEvent event) {
        int target = event.getThreshold() * reorderTargetMultiplier;
        int unitsNeeded = Math.max(target - event.getRemainingStock(), 1);

        try {
            supplierGateway.placeReorder(event.getProductId(), unitsNeeded);
        } catch (Exception e) {
            // A reorder failure must never roll back the sale that triggered
            // it - same reasoning as everywhere else a listener touches an
            // external system inside someone else's transaction.
            log.warn("Could not place auto-reorder for {}: {}", event.getProductId(), e.getMessage());
        }
    }
}
