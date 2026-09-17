package edu.cit.abella.shop;

import edu.cit.abella.inventory.InventoryItem;

import java.util.List;

public class OrderResponse {

    private final Long orderId;
    private final String status;
    private final String reason;
    private final List<ItemOutcome> items;
    // Full inventory snapshot taken after the order settled, so the
    // frontend can refresh its table straight from the order response.
    private final List<InventoryItem> inventory;

    public OrderResponse(Long orderId, String status, String reason,
                         List<ItemOutcome> items, List<InventoryItem> inventory) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.items = items;
        this.inventory = inventory;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public List<ItemOutcome> getItems() {
        return items;
    }

    public List<InventoryItem> getInventory() {
        return inventory;
    }
}
