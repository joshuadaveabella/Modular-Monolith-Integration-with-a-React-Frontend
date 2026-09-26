package edu.cit.abella.inventory;

import edu.cit.abella.events.LowStockEvent;
import edu.cit.abella.supplier.SupplierGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// This class replaces Lab 2's "just log it" behavior. It lives in the
// inventory package (reacting to inventory's own event) and depends on
// SupplierGateway - the ONE public type the supplier module exposes - plus
// LowStockEvent, a neutral event class. It never sees a SupplierSku, a
// LegacySupply status code, or an XML class. That is the whole point of the
// Anti-Corruption Layer: this class could be pointed at a totally different
// supplier system tomorrow and would not need to change at all.
@Component
class AutoReorderListener {

    private final SupplierGateway supplierGateway;

    // Reorder policy: bring stock back up to threshold * multiplier. This is
    // a business decision for OUR system, not something LegacySupply
    // dictates - it lives here, not in the supplier module.
    @Value("${inventory.reorder-target-multiplier:3}")
    private int reorderTargetMultiplier;

    AutoReorderListener(SupplierGateway supplierGateway) {
        this.supplierGateway = supplierGateway;
    }

    @EventListener
    void onLowStock(LowStockEvent event) {
        int target = event.getThreshold() * reorderTargetMultiplier;
        int unitsNeeded = Math.max(target - event.getRemainingStock(), 1);

        supplierGateway.placeReorder(event.getProductId(), unitsNeeded);
    }
}
