package edu.cit.abella.events;

// Published by Inventory every time a product's stock actually changes -
// reserve(), restock(), all of it. "Use your Lab 2 domain events. Do not
// publish on a timer" (Task 3) is satisfied by this: channel's stock
// publisher reacts to this event instead of polling Inventory itself.
// Deliberately generic - no mention of Tiangge, a marketplace, or a
// listing. Inventory has no idea anything downstream cares about this.
public class InventoryStockChangedEvent {

    private final String productId;
    private final int availableStock;

    public InventoryStockChangedEvent(String productId, int availableStock) {
        this.productId = productId;
        this.availableStock = availableStock;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailableStock() {
        return availableStock;
    }
}
